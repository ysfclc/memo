package app.memo.android.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver as AndroidBroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.memo.android.MainActivity
import app.memo.android.R
import app.memo.android.ui.MemoLocalization
import java.util.Calendar

/** Local notifications only. Bodies deliberately avoid private task or conversation content. */
object MemoNotificationCenter {
    private const val CHANNEL_ID = "memo_private_updates"
    private const val PREFS = "memo_notification_preferences"
    private const val DAILY_ENABLED = "daily_enabled"
    private const val WORKFLOW_NOTIFICATIONS_ENABLED = "workflow_notifications_enabled"
    private const val DAILY_HOUR = "daily_hour"
    private const val DAILY_MINUTE = "daily_minute"
    private const val ACTION_DAILY_NUDGE = "app.memo.android.action.DAILY_NUDGE"
    private const val DAILY_REQUEST_CODE = 78041

    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, localized(context, "Memo updates"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = localized(context, "Private reminders and workflow status updates.")
                    setShowBadge(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                }
            )
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isDailyNudgeEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(DAILY_ENABLED, false)

    fun areWorkflowNotificationsEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(WORKFLOW_NOTIFICATIONS_ENABLED, false)

    fun setWorkflowNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(WORKFLOW_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun dailyNudgeTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(DAILY_HOUR, 9).coerceIn(0, 23) to prefs.getInt(DAILY_MINUTE, 0).coerceIn(0, 59)
    }

    fun setDailyNudge(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(DAILY_ENABLED, enabled)
            .putInt(DAILY_HOUR, hour.coerceIn(0, 23))
            .putInt(DAILY_MINUTE, minute.coerceIn(0, 59))
            .apply()
        if (enabled) scheduleNextNudge(app) else cancelNudge(app)
    }

    fun showTestNotification(context: Context) {
        show(context, localized(context, "Memo notification test"), localized(context, "Notifications are working. Private work details stay inside Memo."), "test".hashCode())
    }

    fun notifyWorkflowFinished(context: Context, runId: String, status: String) {
        if (!areWorkflowNotificationsEnabled(context)) return
        val body = when (status.uppercase()) {
            "SUCCEEDED" -> "A workflow finished. Unlock Memo to inspect its verified checkpoint."
            "FAILED" -> "A workflow needs attention. Unlock Memo to review the run trace."
            "UNVERIFIED", "INTERRUPTED", "CANCELLED" -> "A workflow stopped before verified completion. Unlock Memo to review it."
            else -> "A workflow has an update. Unlock Memo to inspect the run trace."
        }
        show(context, localized(context, "Memo workflow update"), localized(context, body), runId.hashCode())
    }

    internal fun handleDailyNudge(context: Context) {
        if (!isDailyNudgeEnabled(context)) return
        show(
            context,
            localized(context, "A small step, when you're ready"),
            localized(context, "Open Memo to choose your next step or capture what's on your mind."),
            DAILY_REQUEST_CODE
        )
        scheduleNextNudge(context.applicationContext)
    }

    internal fun rescheduleAfterSystemChange(context: Context) {
        if (isDailyNudgeEnabled(context)) scheduleNextNudge(context.applicationContext)
    }

    private fun show(context: Context, title: String, body: String, id: Int) {
        initialize(context)
        if (!areNotificationsEnabled(context)) return
        // Android 13+ makes notification delivery a runtime permission. The UI asks
        // for this permission before enabling reminders/workflow notifications, but
        // it can be revoked at any time in system settings.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) return
        }
        val openMemo = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                putExtra("memo_screen", "APPS")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openMemo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // The permission can be revoked after the check and before notify().
            // A background reminder must never crash the process in that race.
        }
    }

    private fun scheduleNextNudge(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(DAILY_ENABLED, false)) return
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.getInt(DAILY_HOUR, 9).coerceIn(0, 23))
            set(Calendar.MINUTE, prefs.getInt(DAILY_MINUTE, 0).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, dailyPendingIntent(context)) }
    }

    private fun cancelNudge(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { alarmManager.cancel(dailyPendingIntent(context)) }
    }

    private fun dailyPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        DAILY_REQUEST_CODE,
        Intent(context, MemoDailyNudgeReceiver::class.java).setAction(ACTION_DAILY_NUDGE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun localized(context: Context, source: String): String {
        val language = context.getSharedPreferences("synthos_preferences", Context.MODE_PRIVATE)
            .getString("ui_language", "EN") ?: "EN"
        return MemoLocalization.translate(source, language)
    }
}

class MemoDailyNudgeReceiver : AndroidBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "app.memo.android.action.DAILY_NUDGE") MemoNotificationCenter.handleDailyNudge(context)
    }
}

class MemoReminderBootReceiver : AndroidBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> MemoNotificationCenter.rescheduleAfterSystemChange(context)
        }
    }
}
