package app.memo.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.memo.android.ui.MemoText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import app.memo.android.data.MemoWorkspaceViewModel
import app.memo.android.notifications.MemoNotificationCenter
import app.memo.android.ui.LocalMemoLanguage
import app.memo.android.ui.MemoLocalization
import app.memo.android.ui.MemoTheme
import app.memo.android.ui.MemoTokens
import app.memo.android.ui.MemoWorkspaceApp

class MainActivity : FragmentActivity() {
    private val viewModel: MemoWorkspaceViewModel by viewModels()
    private var unlocked by mutableStateOf(false)
    private var authMessage by mutableStateOf("")
    private var pendingSharedText by mutableStateOf<String?>(null)
    private var pendingSharedSubject by mutableStateOf<String?>(null)
    private var pendingWorkspace by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MemoNotificationCenter.initialize(this)
        captureShareIntent(intent)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val executor = ContextCompat.getMainExecutor(this)
        setContent {
            val theme by viewModel.themeMode.collectAsState()
            val uiLanguage by viewModel.uiLanguage.collectAsState()
            CompositionLocalProvider(LocalMemoLanguage provides uiLanguage) {
                val darkSystem = isSystemInDarkTheme()
                val darkBars = when (theme) { "DARK" -> true; "LIGHT" -> false; else -> darkSystem }
                SideEffect {
                    val barColor = android.graphics.Color.parseColor(if (darkBars) "#11131D" else "#F6F7FB")
                    window.statusBarColor = barColor
                    window.navigationBarColor = barColor
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !darkBars
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !darkBars
                }
                MemoTheme(themeMode = theme) {
                if (unlocked) {
                    MemoWorkspaceApp(
                        viewModel = viewModel,
                        sharedText = pendingSharedText,
                        sharedSubject = pendingSharedSubject,
                        initialWorkspace = pendingWorkspace,
                        onSharedTextConsumed = { pendingSharedText = null; pendingSharedSubject = null },
                        onInitialWorkspaceConsumed = { pendingWorkspace = null },
                        onLock = {
                            viewModel.clearSensitiveMemory()
                            authMessage = "Workspace locked. Authenticate to reopen it."
                            unlocked = false
                        }
                    )
                } else {
                    UnlockScreen(
                        message = authMessage,
                        onUnlock = {
                            authMessage = ""
                            val manager = BiometricManager.from(this)
                            val requiredAuthenticators = if (Build.VERSION.SDK_INT >= 30) {
                                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            } else {
                                BiometricManager.Authenticators.BIOMETRIC_STRONG
                            }
                            val result = manager.canAuthenticate(requiredAuthenticators)
                            if (result != BiometricManager.BIOMETRIC_SUCCESS && Build.VERSION.SDK_INT >= 30) {
                                authMessage = when (result) {
                                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Set up a fingerprint, face unlock or secure device PIN in Android settings, then try again."
                                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No supported biometric hardware was found. Use Android 11 or newer with a secure device lock."
                                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Device authentication is temporarily unavailable. Try again."
                                    else -> "Enable a secure device lock before opening your private Memo workspace."
                                }
                            } else {
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle(MemoLocalization.translate("Unlock Memo", viewModel.uiLanguage.value))
                                    .setSubtitle(MemoLocalization.translate("Authenticate to access your private Memo workspace", viewModel.uiLanguage.value))
                                    .apply {
                                        if (Build.VERSION.SDK_INT >= 30) {
                                            setAllowedAuthenticators(requiredAuthenticators)
                                        } else {
                                            setDeviceCredentialAllowed(true)
                                        }
                                    }
                                    .build()
                                val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        viewModel.unlockAndLoad()
                                        unlocked = true
                                    }

                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        authMessage = errString.toString().take(200)
                                    }

                                    override fun onAuthenticationFailed() {
                                        super.onAuthenticationFailed()
                                        authMessage = "Authentication was not recognized. Try again."
                                    }
                                })
                                runCatching { prompt.authenticate(promptInfo) }
                                    .onFailure { authMessage = it.message?.take(200) ?: "Could not start device authentication." }
                            }
                        }
                    )
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureShareIntent(intent)
    }

    private fun captureShareIntent(incoming: Intent?) {
        if (incoming == null) return
        incoming.getStringExtra("memo_screen")?.trim()?.takeIf { it.isNotBlank() }?.let { pendingWorkspace = it }
        val capturedText = incoming.getStringExtra("memo_capture_text")?.trim().orEmpty()
        if (capturedText.isNotBlank()) {
            pendingSharedText = capturedText.take(24_000)
            val appLabel = incoming.getStringExtra("memo_capture_label")?.trim().orEmpty().ifBlank { "Android app" }
            val sourcePackage = incoming.getStringExtra("memo_capture_package")?.trim().orEmpty()
            val sourceDetail = sourcePackage.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
            pendingSharedSubject = "Screen capture · $appLabel$sourceDetail"
            pendingWorkspace = "CHATS"
            return
        }
        if (incoming.getBooleanExtra("memo_quick_task", false)) {
            pendingSharedText = "Create a task from this request. Keep it as a draft for review before running: " +
                incoming.getStringExtra("memo_task_context").orEmpty().take(2_000)
            pendingSharedSubject = "Quick task from Memo floating controls"
            pendingWorkspace = "CHATS"
            return
        }
        if (incoming.action != Intent.ACTION_SEND) return
        val text = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isBlank()) return
        pendingSharedText = text.take(24_000)
        pendingSharedSubject = incoming.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()?.trim()?.take(240)
        pendingWorkspace = "CHATS"
    }
}

@androidx.compose.runtime.Composable
private fun UnlockScreen(message: String, onUnlock: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(MemoTokens.Spacing.xl), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(Modifier.height(48.dp))
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Text("MEMO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Your models.\nYour workflow.\nYour decision.", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "A private workspace for AI conversations, app runtimes, structured outcomes and traceable jobs. Install a compatible local model or explicitly connect a remote provider. The browser is a separate runtime, not the native app environment.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                Button(
                    onClick = onUnlock,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Unlock workspace")
                }
                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onUnlock) { Text("Try again") }
                }
                Spacer(Modifier.weight(1f))
                Text("Encrypted on this device · No analytics · No account required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

