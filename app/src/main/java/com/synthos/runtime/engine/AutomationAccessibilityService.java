package com.synthos.runtime.engine;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import app.memo.android.runtime.MemoRuntimeBridge;
import com.synthos.runtime.data.RunRecord;
import com.synthos.runtime.data.SynthStore;
import com.synthos.runtime.data.Workflow;
import com.synthos.runtime.data.WorkflowStep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Executes only explicit, user-authored workflow steps. UI actions pause while the device is locked. */
public class AutomationAccessibilityService extends AccessibilityService {
    private static volatile AutomationAccessibilityService active;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SynthStore store;
    private Workflow workflow;
    private RunRecord run;
    private int stepIndex = 0;
    private int attempts = 0;
    private long waitDeadline = 0L;
    private boolean waitingForText = false;
    private boolean closing = false;
    private boolean storageFailureHandled = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams overlayParams;
    private View overlayRoot;
    private LinearLayout overlayList;
    private LinearLayout overlayBody;
    private TextView overlayTitle;
    private TextView overlayStatus;
    private TextView overlayCollapse;
    private TextView overlaySubtitle;
    private View overlayHeaderDetails;
    private boolean overlayCollapsed = false;
    private boolean globalAssistantMode = false;
    private boolean globalAssistantEnabled = false;
    private final Runnable restoreGlobalAssistant = () -> {
        if (globalAssistantEnabled && overlayRoot == null && !isDeviceLocked()) installGlobalOverlay();
    };
    private String monitoredPackage = "";
    private String monitoredLabel = "";
    private long lastObservationBuildAt = 0L;
    private boolean targetIsForeground = false;

    /** A privacy-conscious, in-memory snapshot of the target app's exposed accessibility UI. */
    public static final class SurfaceItem {
        public final String label;
        public final boolean clickable;
        SurfaceItem(String label, boolean clickable) { this.label = label; this.clickable = clickable; }
    }

    public static final class SurfaceSnapshot {
        public final String packageName;
        public final String appLabel;
        public final long observedAt;
        public final List<SurfaceItem> items;
        public final boolean foreground;
        SurfaceSnapshot(String packageName, String appLabel, long observedAt, List<SurfaceItem> items, boolean foreground) {
            this.packageName = packageName; this.appLabel = appLabel; this.observedAt = observedAt;
            this.items = Collections.unmodifiableList(new ArrayList<>(items)); this.foreground = foreground;
        }
    }

    private static volatile SurfaceSnapshot latestSnapshot = new SurfaceSnapshot("", "", 0L, Collections.emptyList(), false);

    public static SurfaceSnapshot getLatestSnapshot() { return latestSnapshot; }
    public static boolean isOverlayVisible() { return active != null && active.overlayRoot != null; }
    public static boolean isMonitoredTargetForeground() { return active != null && active.targetIsForeground; }

    /** Opens a touchable, floating Memo control panel over the real target app. */
    public static boolean showFloatingInspector(Context context, String packageName, String label) {
        AutomationAccessibilityService service = active;
        if (service == null || packageName == null || packageName.trim().isEmpty()) return false;
        service.handler.post(() -> service.installOverlay(packageName, label == null ? packageName : label));
        return true;
    }

    /** Enable a user-requested floating action menu that follows the user across Android apps. */
    public static boolean showGlobalAssistant(Context context) {
        AutomationAccessibilityService service = active;
        if (service == null) return false;
        service.handler.post(service::installGlobalOverlay);
        return true;
    }

    public static boolean isConnected() { return active != null; }
    public static boolean isExecuting() { return active != null && active.run != null && (RunRecord.RUNNING.equals(active.run.status) || RunRecord.PAUSED_LOCKED.equals(active.run.status) || RunRecord.PAUSED_EXTERNAL.equals(active.run.status)); }

    public static boolean requestRun(Context context, Workflow workflow) {
        AutomationAccessibilityService service = active;
        if (service == null || workflow == null) return false;
        service.handler.post(() -> service.begin(workflow));
        return true;
    }

    public static void requestCancel() {
        AutomationAccessibilityService service = active;
        if (service != null) service.handler.post(() -> service.finish(RunRecord.CANCELLED, "Stopped by you."));
    }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        active = this;
        try { store = SynthStore.shared(this); } catch (IOException e) { store = null; }
        MemoRuntimeBridge.notifyRuntimeChanged();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isDeviceLocked()) {
            targetIsForeground = false;
            if (overlayRoot != null) removeOverlay(true); // Never leave controls or observed labels over the lock screen.
            if (workflow != null && run != null && !closing && RunRecord.RUNNING.equals(run.status)) pause("Device locked. UI execution paused.");
            return;
        }
        try { observeSurface(event); } catch (RuntimeException ignored) { /* A target can replace its window between events. */ }
        if (globalAssistantEnabled && overlayRoot == null && event != null) {
            CharSequence eventPackage = event.getPackageName();
            String pkg = eventPackage == null ? "" : eventPackage.toString();
            if (!pkg.isEmpty() && !pkg.equals(getPackageName()) && !pkg.equals("com.android.systemui")) {
                handler.removeCallbacks(restoreGlobalAssistant);
                handler.postDelayed(restoreGlobalAssistant, 450L);
            }
        }
        if (workflow == null || run == null || closing) return;
        boolean targetForeground = isTargetForeground();
        if (RunRecord.PAUSED_LOCKED.equals(run.status) || RunRecord.PAUSED_EXTERNAL.equals(run.status)) {
            if (!targetForeground) return;
            run.status = RunRecord.RUNNING;
            record("Target app is foreground and the device is unlocked; resumed at step " + (stepIndex + 1) + ".");
            waitingForText = false;
            advance();
        } else if (!targetForeground) {
            if (stepIndex < workflow.steps.size() && !WorkflowStep.LAUNCH_APP.equals(workflow.steps.get(stepIndex).type)
                    && RunRecord.RUNNING.equals(run.status)) pauseExternal("Target app left the foreground. Paused to avoid touching another app.");
        } else if (waitingForText) {
            if (containsVisibleText(currentStepValue())) completeCurrentStep("Required text appeared.");
        }
    }

    @Override public void onInterrupt() {
        if (run != null && RunRecord.RUNNING.equals(run.status)) pause("Android interrupted UI access; waiting to resume safely.");
    }

    @Override public boolean onUnbind(Intent intent) {
        removeOverlay(true);
        if (active == this) active = null;
        MemoRuntimeBridge.notifyRuntimeChanged();
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        removeOverlay(true);
        if (active == this) active = null;
        handler.removeCallbacksAndMessages(null);
        MemoRuntimeBridge.notifyRuntimeChanged();
        super.onDestroy();
    }

    private void begin(Workflow requested) {
        if (run != null && (RunRecord.RUNNING.equals(run.status) || RunRecord.PAUSED_LOCKED.equals(run.status) || RunRecord.PAUSED_EXTERNAL.equals(run.status))) {
            record("Another workflow is active; finish or stop it first.");
            MemoRuntimeBridge.notifyRuntimeChanged();
            return;
        }
        String validation = WorkflowPolicy.validate(requested);
        if (validation != null) { MemoRuntimeBridge.showServiceMessage(this, validation); return; }
        if (isDeviceLocked()) { MemoRuntimeBridge.showServiceMessage(this, "Unlock the device before starting a UI workflow."); return; }
        workflow = requested;
        run = new RunRecord(workflow.id, workflow.name); run.workflowVersion = workflow.version; run.addEvent("Pinned workflow definition v" + workflow.version);
        run.status = RunRecord.RUNNING;
        stepIndex = 0; attempts = 0; waitingForText = false; waitDeadline = 0L; closing = false;
        record("Run started · " + workflow.targetLabel);
        advance();
    }

    private void advance() {
        if (workflow == null || run == null || closing) return;
        if (isDeviceLocked()) { pause("Device locked. Workflow paused before the next UI action."); return; }
        if (stepIndex >= workflow.steps.size()) {
            WorkflowStep last = workflow.steps.isEmpty() ? null : workflow.steps.get(workflow.steps.size() - 1);
            if (last != null && WorkflowStep.WAIT_TEXT.equals(last.type)) {
                finish(RunRecord.SUCCEEDED, "Configured steps completed and the final visible-text condition was observed.");
            } else {
                finish(RunRecord.UNVERIFIED, "All configured steps ran, but no final-state condition was verified. This is not proof that the intended outcome occurred.");
            }
            return;
        }
        WorkflowStep step = workflow.steps.get(stepIndex);
        if (!WorkflowStep.LAUNCH_APP.equals(step.type) && !WorkflowStep.DELAY.equals(step.type) && !isTargetForeground()) {
            pauseExternal("Target app is not foreground. Paused to avoid acting in another app."); return;
        }
        run.status = RunRecord.RUNNING;
        attempts = 0;
        record("Step " + (stepIndex + 1) + "/" + workflow.steps.size() + " · " + step.label());
        execute(step);
    }

    private void execute(WorkflowStep step) {
        if (isDeviceLocked()) { pause("Device locked before action. Unlock to continue."); return; }
        switch (step.type) {
            case WorkflowStep.LAUNCH_APP:
                try {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(workflow.targetPackage);
                    if (intent == null) { finish(RunRecord.FAILED, "Target app has no launchable activity."); return; }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    stepIndex++;
                    handler.postDelayed(this::advance, 1200L);
                } catch (Exception e) { finish(RunRecord.FAILED, "Could not launch target app: " + safeMessage(e)); }
                break;
            case WorkflowStep.TAP_TEXT:
                if (findAndClick(step.value)) {
                    stepIndex++;
                    handler.postDelayed(this::advance, 480L);
                } else if (++attempts < 18) {
                    handler.postDelayed(() -> execute(step), 450L);
                } else {
                    finish(RunRecord.FAILED, "Could not find a clickable match for “" + step.value + "”. The app may not expose this control to Accessibility.");
                }
                break;
            case WorkflowStep.WAIT_TEXT:
                waitingForText = true;
                waitDeadline = System.currentTimeMillis() + 15000L;
                pollWaitText(step.value);
                break;
            case WorkflowStep.BACK:
                performGlobalAction(GLOBAL_ACTION_BACK);
                stepIndex++;
                handler.postDelayed(this::advance, 420L);
                break;
            case WorkflowStep.DELAY:
                stepIndex++;
                handler.postDelayed(this::advance, Math.max(0, Math.min(step.delayMillis, 30000)));
                break;
            default: finish(RunRecord.FAILED, "Unsupported workflow action.");
        }
    }

    private void pollWaitText(String value) {
        if (workflow == null || run == null || closing || !waitingForText) return;
        if (isDeviceLocked()) { pause("Device locked while waiting for visible text."); return; }
        if (containsVisibleText(value)) { completeCurrentStep("Required text found."); return; }
        if (System.currentTimeMillis() >= waitDeadline) { waitingForText = false; finish(RunRecord.FAILED, "Timed out waiting for “" + value + "”."); return; }
        handler.postDelayed(() -> pollWaitText(value), 450L);
    }

    private void completeCurrentStep(String note) {
        if (workflow == null || run == null || closing) return;
        waitingForText = false;
        record(note);
        stepIndex++;
        handler.postDelayed(this::advance, 250L);
    }

    private String currentStepValue() {
        if (workflow == null || stepIndex >= workflow.steps.size()) return "";
        return workflow.steps.get(stepIndex).value;
    }

    private boolean findAndClick(String desired) {
        if (desired == null || desired.trim().isEmpty()) return false;
        for (AccessibilityNodeInfo root : roots()) {
            try {
                if (clickMatch(root, desired.trim().toLowerCase(Locale.ROOT))) return true;
            } catch (RuntimeException ignored) {
                // A screen transition can invalidate a node while the tree is being traversed.
            } finally { try { root.recycle(); } catch (Exception ignored) {} }
        }
        return false;
    }

    private boolean clickMatch(AccessibilityNodeInfo node, String desired) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if (matches(text, desired) || matches(description, desired)) {
            AccessibilityNodeInfo cursor = AccessibilityNodeInfo.obtain(node);
            for (int depth=0; cursor != null && depth<8; depth++) {
                if (cursor.isClickable() && cursor.isEnabled()) {
                    boolean performed = cursor.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    cursor.recycle();
                    if (performed) return true;
                    break;
                }
                AccessibilityNodeInfo parent = cursor.getParent();
                cursor.recycle(); cursor = parent;
            }
            if (node.isEnabled() && node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        }
        for (int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean found = clickMatch(child, desired);
                child.recycle();
                if (found) return true;
            }
        }
        return false;
    }

    private boolean containsVisibleText(String desired) {
        if (desired == null || desired.trim().isEmpty()) return false;
        String target = desired.trim().toLowerCase(Locale.ROOT);
        for (AccessibilityNodeInfo root : roots()) {
            try { if (containsInTree(root, target)) return true; }
            catch (RuntimeException ignored) { }
            finally { try { root.recycle(); } catch (Exception ignored) {} }
        }
        return false;
    }

    private boolean containsInTree(AccessibilityNodeInfo node, String target) {
        if (node == null) return false;
        if (contains(node.getText(), target) || contains(node.getContentDescription(), target)) return true;
        for (int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) { boolean found = containsInTree(child,target); child.recycle(); if(found) return true; }
        }
        return false;
    }

    private List<AccessibilityNodeInfo> roots() {
        ArrayList<AccessibilityNodeInfo> out = new ArrayList<>();
        String expected = workflow != null && workflow.targetPackage != null && !workflow.targetPackage.isEmpty()
                ? workflow.targetPackage : monitoredPackage;
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            for (AccessibilityWindowInfo window : windows) {
                if (window == null) continue;
                AccessibilityNodeInfo root = null;
                try { root = window.getRoot(); } catch (Exception ignored) {}
                if (root == null) continue;
                CharSequence pkg = root.getPackageName();
                boolean matchesTarget = expected == null || expected.isEmpty()
                        ? (pkg != null && !getPackageName().contentEquals(pkg))
                        : (pkg != null && expected.contentEquals(pkg));
                if (matchesTarget) out.add(root); else root.recycle();
            }
        } catch (Exception ignored) {}
        if (out.isEmpty()) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                CharSequence pkg = root.getPackageName();
                boolean matchesTarget = expected == null || expected.isEmpty()
                        ? pkg != null && !getPackageName().contentEquals(pkg)
                        : pkg != null && expected.contentEquals(pkg);
                if (matchesTarget) out.add(root); else root.recycle();
            }
        }
        return out;
    }

    private boolean matches(CharSequence value, String desired) {
        return value != null && value.toString().trim().toLowerCase(Locale.ROOT).equals(desired);
    }
    private boolean contains(CharSequence value, String desired) {
        return value != null && value.toString().toLowerCase(Locale.ROOT).contains(desired);
    }
    private boolean isDeviceLocked() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    private void pause(String message) {
        if (run == null || closing) return;
        run.status = RunRecord.PAUSED_LOCKED;
        record(message);
        MemoRuntimeBridge.notifyRuntimeChanged();
    }

    private void pauseExternal(String message) {
        if (run == null || closing) return;
        run.status = RunRecord.PAUSED_EXTERNAL;
        record(message);
        MemoRuntimeBridge.notifyRuntimeChanged();
    }

    private boolean isTargetForeground() {
        if (workflow == null || workflow.targetPackage == null || workflow.targetPackage.isEmpty()) return false;
        return isPackageActiveWindow(workflow.targetPackage);
    }

    private List<AccessibilityNodeInfo> rootsForPackage(String packageName) {
        ArrayList<AccessibilityNodeInfo> out = new ArrayList<>();
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            for (AccessibilityWindowInfo window : windows) {
                if (window == null) continue;
                AccessibilityNodeInfo root = null;
                try { root = window.getRoot(); } catch (Exception ignored) {}
                if (root == null) continue;
                CharSequence pkg = root.getPackageName();
                if (pkg != null && packageName.contentEquals(pkg)) out.add(root); else root.recycle();
            }
        } catch (Exception ignored) {}
        if (out.isEmpty()) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                CharSequence pkg = root.getPackageName();
                if (pkg != null && packageName.contentEquals(pkg)) out.add(root); else root.recycle();
            }
        }
        return out;
    }

    private void observeSurface(AccessibilityEvent event) {
        long now = System.currentTimeMillis();
        if (now - lastObservationBuildAt < 350L && event != null
                && event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String eventPackage = event == null || event.getPackageName() == null ? "" : event.getPackageName().toString();
        if (monitoredPackage.isEmpty()) return; // Observe only the app the user explicitly chose to watch.
        if (eventPackage.equals(getPackageName())) {
            targetIsForeground = isPackageActiveWindow(monitoredPackage);
            markSnapshotForeground(targetIsForeground);
            if (overlayRoot != null) renderOverlay();
            return;
        }
        if (!eventPackage.isEmpty() && !eventPackage.equals(monitoredPackage)) {
            targetIsForeground = isPackageActiveWindow(monitoredPackage);
            markSnapshotForeground(targetIsForeground);
            if (overlayRoot != null) renderOverlay();
            return;
        }
        String expected = monitoredPackage;
        List<AccessibilityNodeInfo> roots = rootsForPackage(expected);
        if (roots.isEmpty()) {
            targetIsForeground = false;
            markSnapshotForeground(false);
            if (overlayRoot != null) renderOverlay();
            return;
        }
        targetIsForeground = isPackageActiveWindow(expected);
        lastObservationBuildAt = now;
        ArrayList<SurfaceItem> items = new ArrayList<>();
        Map<String, Boolean> labels = new LinkedHashMap<>();
        int[] visited = new int[]{0};
        for (AccessibilityNodeInfo root : roots) {
            try { collectSurfaceItems(root, false, labels, visited, 0); }
            catch (RuntimeException ignored) { }
            finally { try { root.recycle(); } catch (Exception ignored) {} }
        }
        for (Map.Entry<String, Boolean> entry : labels.entrySet()) {
            if (items.size() >= 60) break;
            items.add(new SurfaceItem(entry.getKey(), entry.getValue()));
        }
        String appLabel = monitoredLabel;
        if (appLabel.isEmpty() || !expected.equals(monitoredPackage)) {
            try { appLabel = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(expected, 0)).toString(); }
            catch (Exception ignored) { appLabel = expected; }
        }
        latestSnapshot = new SurfaceSnapshot(expected, appLabel, now, items, targetIsForeground);
        if (overlayRoot != null) renderOverlay();
    }

    private void collectSurfaceItems(AccessibilityNodeInfo node, boolean parentClickable,
                                     Map<String, Boolean> labels, int[] visited, int depth) {
        if (node == null || depth > 28 || visited[0] >= 260) return;
        visited[0]++;
        boolean clickable = parentClickable || (node.isClickable() && node.isEnabled());
        CharSequence cls = node.getClassName();
        String className = cls == null ? "" : cls.toString().toLowerCase(Locale.ROOT);
        // Never read entered values: editable controls and password fields are excluded.
        if (!node.isPassword() && !node.isEditable() && !className.contains("edittext")) {
            String label = cleanLabel(node.getText());
            if (label.isEmpty()) label = cleanLabel(node.getContentDescription());
            if (!label.isEmpty()) {
                Boolean prior = labels.get(label);
                labels.put(label, Boolean.valueOf(clickable || (prior != null && prior.booleanValue())));
            }
        }
        for (int i = 0; i < node.getChildCount() && visited[0] < 260; i++) {
            AccessibilityNodeInfo child = null;
            try { child = node.getChild(i); if (child != null) collectSurfaceItems(child, clickable, labels, visited, depth + 1); }
            finally { if (child != null) try { child.recycle(); } catch (Exception ignored) {} }
        }
    }

    private String cleanLabel(CharSequence value) {
        if (value == null) return "";
        String text = value.toString().replaceAll("\\s+", " ").trim();
        if (text.isEmpty() || text.length() > 140) return "";
        for (int i = 0; i < text.length(); i++) if (Character.isISOControl(text.charAt(i))) return "";
        return text;
    }

    private void installGlobalOverlay() {
        globalAssistantEnabled = true;
        monitoredPackage = "";
        monitoredLabel = "Across apps";
        installOverlay("", "Across apps");
    }

    private void installOverlay(String packageName, String label) {
        removeOverlay(false);
        globalAssistantMode = packageName == null || packageName.trim().isEmpty();
        if (globalAssistantMode) globalAssistantEnabled = true;
        overlayCollapsed = globalAssistantMode;
        monitoredPackage = packageName == null ? "" : packageName;
        monitoredLabel = label;
        targetIsForeground = isPackageActiveWindow(packageName);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) { MemoRuntimeBridge.showServiceMessage(this, "Memo could not access Android's window manager."); return; }
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(246, 20, 29, 33)); background.setCornerRadius(dp(22));
        background.setStroke(dp(1), Color.argb(220, 84, 118, 121));
        panel.setBackground(background);
        panel.setElevation(dp(12));

        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); head.setOrientation(LinearLayout.HORIZONTAL);
        TextView mark = overlayText("M", 17, Color.rgb(101, 224, 201), true);
        mark.setGravity(Gravity.CENTER); mark.setBackground(overlayShape(Color.argb(45, 101, 224, 201), dp(11)));
        head.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout headText = new LinearLayout(this); headText.setOrientation(LinearLayout.VERTICAL); headText.setPadding(dp(8), 0, dp(3), 0);
        overlayHeaderDetails = headText;
        overlayTitle = overlayText(globalAssistantMode ? "MEMO QUICK ACTIONS" : "MEMO LIVE", 12, Color.WHITE, true); headText.addView(overlayTitle);
        overlaySubtitle = overlayText(globalAssistantMode ? "Across apps · only when you tap" : label, 10, Color.rgb(177, 193, 195), false); headText.addView(overlaySubtitle);
        head.addView(headText, new LinearLayout.LayoutParams(0, -2, 1f));
        overlayCollapse = overlayText("−", 20, Color.WHITE, true); overlayCollapse.setGravity(Gravity.CENTER);
        overlayCollapse.setContentDescription("Collapse or expand Memo Live controls");
        head.addView(overlayCollapse, new LinearLayout.LayoutParams(dp(26), dp(34)));
        overlayCollapse.setOnClickListener(v -> toggleOverlayCollapsed());
        TextView close = overlayText("×", 24, Color.WHITE, true); close.setGravity(Gravity.CENTER); close.setContentDescription("Close Memo Live controls");
        head.addView(close, new LinearLayout.LayoutParams(dp(26), dp(34))); close.setOnClickListener(v -> { globalAssistantEnabled = false; removeOverlay(true); });
        panel.addView(head);
        head.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY; private float rawX, rawY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (overlayParams == null || windowManager == null) return false;
                if (e.getAction() == MotionEvent.ACTION_DOWN) { startX = overlayParams.x; startY = overlayParams.y; rawX = e.getRawX(); rawY = e.getRawY(); return true; }
                if (e.getAction() == MotionEvent.ACTION_MOVE) { overlayParams.x = startX + Math.round(rawX - e.getRawX()); overlayParams.y = startY + Math.round(e.getRawY() - rawY); try { windowManager.updateViewLayout(overlayRoot, overlayParams); } catch (Exception ignored) {} return true; }
                return e.getAction() == MotionEvent.ACTION_UP;
            }
        });

        overlayBody = new LinearLayout(this); overlayBody.setOrientation(LinearLayout.VERTICAL);
        overlayStatus = overlayText(globalAssistantMode ? "READY · available over other apps" : "Waiting for app UI…", 10, Color.rgb(101, 224, 201), true);
        overlayStatus.setPadding(dp(3), dp(7), dp(3), dp(6)); overlayBody.addView(overlayStatus);
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        if (globalAssistantMode) {
            addOverlayAction(actions, "Talk", () -> openMemoScreen("Home"));
            addOverlayAction(actions, "Browse", () -> openMemoScreen("Browser"));
            addOverlayAction(actions, "Capture", this::captureCurrentSurface);
            addOverlayAction(actions, "Task", () -> openQuickTask());
            LinearLayout workspaceActions = new LinearLayout(this); workspaceActions.setOrientation(LinearLayout.HORIZONTAL);
            addOverlayAction(workspaceActions, "Work", () -> openMemoScreen("Work"));
            addOverlayAction(workspaceActions, "Library", () -> openMemoScreen("Library"));
            overlayBody.addView(workspaceActions);
        } else {
            addOverlayAction(actions, "Back", () -> { if (isDeviceLocked()) return; if (isPackageActiveWindow(monitoredPackage)) performGlobalAction(GLOBAL_ACTION_BACK); renderOverlay(); });
            addOverlayAction(actions, "Refresh", () -> { observeSurface(null); renderOverlay(); });
            addOverlayAction(actions, "Memo", () -> openMemoScreen("Apps"));
        }
        overlayBody.addView(actions);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(false); scroll.setVerticalScrollBarEnabled(true);
        overlayList = new LinearLayout(this); overlayList.setOrientation(LinearLayout.VERTICAL); overlayList.setPadding(0, dp(6), 0, dp(3)); scroll.addView(overlayList);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, dp(228)); scrollLp.topMargin = dp(4); overlayBody.addView(scroll, scrollLp);
        TextView note = overlayText(globalAssistantMode ? "Capture reads accessible screen labels only after your tap. Passwords and editable values are excluded." : "Tap a highlighted label to activate it. Values in editable fields are never read.", 9, Color.rgb(159, 177, 180), false);
        note.setPadding(dp(3), dp(5), dp(3), dp(1)); overlayBody.addView(note);
        panel.addView(overlayBody);
        if (overlayCollapsed) {
            overlayBody.setVisibility(View.GONE);
            if (overlayHeaderDetails != null) overlayHeaderDetails.setVisibility(View.GONE);
            if (overlaySubtitle != null) overlaySubtitle.setVisibility(View.GONE);
            if (overlayCollapse != null) overlayCollapse.setText("+");
            if (overlayTitle != null) overlayTitle.setText("MEMO");
        }

        overlayRoot = panel;
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        overlayParams = new WindowManager.LayoutParams(dp(overlayCollapsed ? 126 : 276), dp(overlayCollapsed ? 56 : 410), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        overlayParams.x = dp(8); overlayParams.y = dp(10);
        try { windowManager.addView(overlayRoot, overlayParams); if (globalAssistantMode) renderOverlay(); else { observeSurface(null); renderOverlay(); } }
        catch (Exception e) {
            removeOverlay(true);
            Toast.makeText(this, "Could not open Memo Live controls. Check the display-over-other-apps permission.", Toast.LENGTH_LONG).show();
            MemoRuntimeBridge.showServiceMessage(this, "Could not open Memo Live controls. Check the display-over-other-apps permission.");
        }
    }

    private void overlayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void toggleOverlayCollapsed() {
        if (overlayRoot == null || overlayParams == null || overlayBody == null || windowManager == null) return;
        overlayCollapsed = !overlayCollapsed;
        overlayBody.setVisibility(overlayCollapsed ? View.GONE : View.VISIBLE);
        if (overlayCollapse != null) overlayCollapse.setText(overlayCollapsed ? "+" : "−");
        if (overlayTitle != null) overlayTitle.setText(globalAssistantMode ? (overlayCollapsed ? "MEMO" : "MEMO QUICK ACTIONS") : (overlayCollapsed ? "LIVE · " + monitoredLabel : "MEMO LIVE"));
        if (overlayHeaderDetails != null) overlayHeaderDetails.setVisibility(overlayCollapsed ? View.GONE : View.VISIBLE);
        if (overlaySubtitle != null) overlaySubtitle.setVisibility(overlayCollapsed ? View.GONE : View.VISIBLE);
        overlayParams.height = dp(overlayCollapsed ? 56 : 410);
        overlayParams.width = dp(overlayCollapsed ? 126 : 276);
        try { windowManager.updateViewLayout(overlayRoot, overlayParams); } catch (Exception ignored) {}
    }

    private void markSnapshotForeground(boolean foreground) {
        SurfaceSnapshot current = latestSnapshot;
        if (current != null && !current.packageName.isEmpty() && (monitoredPackage.isEmpty() || current.packageName.equals(monitoredPackage))) {
            latestSnapshot = new SurfaceSnapshot(current.packageName, current.appLabel, current.observedAt, current.items, foreground);
        }
    }

    private boolean isPackageActiveWindow(String packageName) {
        AccessibilityNodeInfo root = null;
        try {
            root = getRootInActiveWindow();
            return root != null && root.getPackageName() != null && packageName != null && packageName.contentEquals(root.getPackageName());
        } catch (Exception ignored) { return false; }
        finally { if (root != null) try { root.recycle(); } catch (Exception ignored) {} }
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable overlayShape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private TextView overlayText(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false); t.setPadding(dp(4), dp(3), dp(4), dp(3)); return t;
    }

    private void addOverlayAction(LinearLayout row, String label, Runnable action) {
        TextView t = overlayText(label, 10, Color.WHITE, true); t.setGravity(Gravity.CENTER);
        t.setBackground(overlayShape(Color.argb(255, 44, 60, 64), dp(10)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1f); lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        row.addView(t, lp); t.setOnClickListener(v -> action.run());
    }

    private void renderOverlay() {
        if (overlayRoot == null || overlayList == null || overlayStatus == null) return;
        if (globalAssistantMode) {
            overlayStatus.setText("READY · menu available over other apps");
            overlayStatus.setTextColor(Color.rgb(101, 224, 201));
            overlayList.removeAllViews();
            TextView note = overlayText("Talk opens Memo. Capture saves a source-linked snapshot of accessible labels from the current app. Task opens a quick task editor. Nothing is captured until you tap Capture.", 11, Color.rgb(205, 218, 220), false);
            note.setPadding(dp(8), dp(12), dp(8), dp(12)); overlayList.addView(note);
            if (overlayTitle != null) overlayTitle.setText(overlayCollapsed ? "MEMO · ACTIONS" : "MEMO QUICK ACTIONS");
            return;
        }
        boolean foreground = isPackageActiveWindow(monitoredPackage);
        targetIsForeground = foreground;
        overlayStatus.setText(foreground ? "● LIVE · app is foreground" : "○ PAUSED · open target app to control it");
        overlayStatus.setTextColor(foreground ? Color.rgb(101, 224, 201) : Color.rgb(255, 198, 96));
        overlayList.removeAllViews();
        SurfaceSnapshot snapshot = latestSnapshot;
        if (snapshot == null || !monitoredPackage.equals(snapshot.packageName) || snapshot.items.isEmpty()) {
            TextView empty = overlayText("No readable controls yet. Some apps expose only part of their UI to Android Accessibility.", 11, Color.rgb(177, 193, 195), false);
            empty.setPadding(dp(8), dp(12), dp(8), dp(12)); overlayList.addView(empty); return;
        }
        int shown = 0;
        for (SurfaceItem item : snapshot.items) {
            if (shown >= 18) break;
            TextView row = overlayText((item.clickable ? "●  " : "   ") + item.label, 11,
                    item.clickable ? Color.WHITE : Color.rgb(177, 193, 195), item.clickable);
            row.setMaxLines(2); row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setBackground(overlayShape(item.clickable ? Color.argb(255, 42, 62, 64) : Color.argb(110, 37, 48, 51), dp(9)));
            row.setPadding(dp(9), dp(8), dp(7), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(2), 0, dp(2)); overlayList.addView(row, lp);
            if (item.clickable) row.setOnClickListener(v -> {
                if (isDeviceLocked()) { overlayToast("Unlock the device before using Memo Live controls."); return; }
                if (!isPackageActiveWindow(monitoredPackage)) { overlayToast("The target app is not the active screen. Reopen it to use these controls."); return; }
                boolean clicked = findAndClick(item.label);
                overlayToast(clicked ? "Tap sent to “" + item.label + "”. Watching for the resulting state…" : "Could not activate “" + item.label + "”. The app may expose a read-only or custom-drawn control.");
                handler.postDelayed(() -> { observeSurface(null); renderOverlay(); }, 350L);
            });
            shown++;
        }
        overlayTitle.setText(overlayCollapsed ? "LIVE · " + monitoredLabel : "MEMO LIVE");
    }

    private void removeOverlay(boolean clearTarget) {
        if (overlayRoot != null && windowManager != null) {
            try { windowManager.removeView(overlayRoot); } catch (Exception ignored) {}
        }
        overlayRoot = null; overlayParams = null; overlayList = null; overlayBody = null;
        overlayTitle = null; overlayStatus = null; overlayCollapse = null; overlaySubtitle = null; overlayHeaderDetails = null; overlayCollapsed = false;
        globalAssistantMode = false;
        if (clearTarget) {
            targetIsForeground = false;
            markSnapshotForeground(false);
            monitoredPackage = ""; monitoredLabel = "";
        }
    }

    private void openMemoScreen(String destination) {
        boolean restoreGlobal = globalAssistantMode && globalAssistantEnabled;
        if (restoreGlobal) {
            removeOverlay(false);
            monitoredPackage = ""; monitoredLabel = "";
        } else {
            globalAssistantEnabled = false;
            removeOverlay(true);
        }
        Intent intent = new Intent(this, app.memo.android.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("memo_screen", destination == null ? "Home" : destination);
        startActivity(intent);
    }

    private void openQuickTask() {
        boolean restoreGlobal = globalAssistantMode && globalAssistantEnabled;
        if (restoreGlobal) { removeOverlay(false); monitoredPackage = ""; monitoredLabel = ""; }
        else { globalAssistantEnabled = false; removeOverlay(true); }
        MemoRuntimeBridge.openForQuickTask(this);
    }

    private void captureCurrentSurface() {
        if (isDeviceLocked()) { overlayToast("Unlock the device before capturing app context."); return; }
        String packageName = findForegroundExternalPackage();
        if (packageName.isEmpty()) { overlayToast("No accessible third-party app surface is available to capture."); return; }
        String label = packageName;
        try { label = String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0))); }
        catch (Exception ignored) { }
        monitoredPackage = packageName; monitoredLabel = label; lastObservationBuildAt = 0L;
        try { observeSurface(null); } catch (RuntimeException ignored) { }
        SurfaceSnapshot snapshot = latestSnapshot;
        if (snapshot == null || !packageName.equals(snapshot.packageName) || snapshot.items.isEmpty()) {
            overlayToast("This app exposes no readable labels to Android Accessibility. Nothing was saved.");
            return;
        }
        StringBuilder content = new StringBuilder("Source app: ").append(label)
                .append(" ( ").append(packageName).append(" )\nCaptured: ")
                .append(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        content.append("\n\nAccessible labels observed on the visible screen. Editable field values and passwords were excluded:\n");
        for (SurfaceItem item : snapshot.items) {
            content.append(item.clickable ? "• [action] " : "• ").append(item.label).append('\n');
        }
        boolean restoreGlobal = globalAssistantMode && globalAssistantEnabled;
        if (restoreGlobal) { removeOverlay(false); monitoredPackage = ""; monitoredLabel = ""; }
        else { globalAssistantEnabled = false; removeOverlay(true); }
        MemoRuntimeBridge.openForExternalCapture(this, packageName, label, content.toString());
    }

    private String findForegroundExternalPackage() {
        try {
            AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
            if (activeRoot != null) {
                CharSequence pkg = activeRoot.getPackageName();
                String value = pkg == null ? "" : pkg.toString();
                activeRoot.recycle();
                if (!value.isEmpty() && !isSystemOrMemoPackage(value)) return value;
            }
        } catch (Exception ignored) { }
        try {
            for (AccessibilityWindowInfo window : getWindows()) {
                if (window == null) continue;
                AccessibilityNodeInfo root = null;
                try { root = window.getRoot(); } catch (Exception ignored) { }
                if (root == null) continue;
                CharSequence pkg = root.getPackageName();
                String value = pkg == null ? "" : pkg.toString();
                root.recycle();
                if (!value.isEmpty() && !isSystemOrMemoPackage(value)) return value;
            }
        } catch (Exception ignored) { }
        return "";
    }


    private boolean isSystemOrMemoPackage(String packageName) {
        return packageName == null || packageName.isEmpty() || packageName.equals(getPackageName())
                || packageName.equals("com.android.systemui")
                || packageName.startsWith("com.android.permissioncontroller")
                || packageName.startsWith("com.google.android.permissioncontroller");
    }

    private void openConsole() {
        String targetPackage = monitoredPackage;
        removeOverlay(true);
        Intent intent = new Intent(this, app.memo.android.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("memo_screen", "Apps");
        intent.putExtra("synthos_target_package", targetPackage);
        startActivity(intent);
    }


    private void finish(String status, String message) {
        if (run == null || closing) return;
        handler.removeCallbacksAndMessages(null);
        closing = true; waitingForText = false;
        run.status = status; run.finishedAt = System.currentTimeMillis(); record(message);
        persistRun(); MemoRuntimeBridge.notifyRuntimeChanged();
        workflow = null; run = null; stepIndex = 0;
        closing = false;
    }

    private void record(String message) {
        if (run == null) return;
        run.stepIndex = stepIndex;
        run.addEvent(message);
        persistRun();
        MemoRuntimeBridge.notifyRuntimeChanged();
    }

    private void persistRun() {
        try { if (store == null) store = SynthStore.shared(this); if (store != null && run != null) store.saveRun(run); }
        catch (IOException e) {
            MemoRuntimeBridge.showServiceMessage(this, "Run history could not be saved securely. The workflow was stopped.");
            if (!storageFailureHandled) {
                storageFailureHandled = true;
                handler.post(() -> { waitingForText = false; workflow = null; run = null; stepIndex = 0; closing = false; MemoRuntimeBridge.notifyRuntimeChanged(); });
            }
        }
    }

    private String safeMessage(Exception e) {
        String m = e.getMessage(); return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }
}
