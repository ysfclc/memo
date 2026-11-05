package app.memo.android.runtime;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Narrow UI boundary for the optional Android accessibility runtime.
 * The runtime never owns Memo navigation or a second Activity tree.
 */
public final class MemoRuntimeBridge {
    public interface Listener {
        void onRuntimeChanged();
        void onServiceMessage(String message);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<>();

    private MemoRuntimeBridge() { }

    public static void addListener(Listener listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        if (listener != null) LISTENERS.remove(listener);
    }

    public static void notifyRuntimeChanged() {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) {
                try { listener.onRuntimeChanged(); } catch (RuntimeException ignored) { }
            }
        });
    }

    public static void showServiceMessage(Context context, String message) {
        final String safeMessage = message == null || message.trim().isEmpty()
                ? "The Memo runtime reported an issue."
                : message.trim().substring(0, Math.min(message.trim().length(), 240));
        MAIN.post(() -> {
            boolean delivered = false;
            for (Listener listener : LISTENERS) {
                try {
                    listener.onServiceMessage(safeMessage);
                    delivered = true;
                } catch (RuntimeException ignored) { }
            }
            if (!delivered && context != null) {
                Toast.makeText(context.getApplicationContext(), safeMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void openForExternalCapture(Context context, String packageName, String label, String content) {
        if (context == null) return;
        Intent intent = new Intent(context, app.memo.android.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("memo_screen", "CHATS");
        intent.putExtra("memo_capture_package", packageName == null ? "" : packageName);
        intent.putExtra("memo_capture_label", label == null ? "External app" : label);
        intent.putExtra("memo_capture_text", content == null ? "" : content);
        context.startActivity(intent);
    }

    public static void openForQuickTask(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, app.memo.android.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("memo_screen", "CHATS");
        intent.putExtra("memo_quick_task", true);
        context.startActivity(intent);
    }
}
