package com.synthos.runtime.engine;

import com.synthos.runtime.data.Workflow;
import com.synthos.runtime.data.WorkflowStep;
import java.util.regex.Pattern;

/**
 * Fail-closed capability gate for the native UI runner.
 *
 * Until native actions are bound to the shared, one-use approval ledger, this runner
 * permits observation and bounded navigation only. A visible label is not proof that
 * clicking it is harmless, and BACK can discard or submit state in some applications.
 */
public final class WorkflowPolicy {
    private static final Pattern AD_OR_REWARD_ACTION = Pattern.compile("(?i)\\b(watch\\s*(an?\\s*)?(ad|video)|view\\s*(an?\\s*)?ad|play\\s*(an?\\s*)?ad|rewarded\\s*ad|advertisement|claim\\s*(a\\s*)?reward|cash\\s*out|withdraw\\s*reward|earn\\s*cash)\\b");
    private static final Pattern SENSITIVE_ACTION = Pattern.compile("(?i)\\b(enter\\s*password|type\\s*password|submit\\s*payment|confirm\\s*payment|solve\\s*captcha|bypass\\s*captcha|disable\\s*security)\\b");

    private WorkflowPolicy() {}

    /** Stable capability classification; unknown and mutating actions fail closed. */
    public static String capabilityFor(String stepType) {
        if (WorkflowStep.LAUNCH_APP.equals(stepType)) return "native.app.launch";
        if (WorkflowStep.WAIT_TEXT.equals(stepType)) return "native.ui.observe_text";
        if (WorkflowStep.DELAY.equals(stepType)) return "native.runtime.delay";
        if (WorkflowStep.TAP_TEXT.equals(stepType)) return "native.ui.click";
        if (WorkflowStep.BACK.equals(stepType)) return "native.ui.navigate_back";
        return "native.unknown";
    }

    /** True only for steps that do not invoke a target-app control or navigation action. */
    public static boolean isAllowedWithoutApproval(String stepType) {
        return "native.app.launch".equals(capabilityFor(stepType))
                || "native.ui.observe_text".equals(capabilityFor(stepType))
                || "native.runtime.delay".equals(capabilityFor(stepType));
    }

    public static String validate(Workflow workflow) {
        if (workflow == null) return "Workflow is missing.";
        if (workflow.name == null || workflow.name.trim().isEmpty() || workflow.name.length() > 80) return "Give the workflow a name of 1–80 characters.";
        if (workflow.targetPackage == null || workflow.targetPackage.trim().isEmpty()) return "Choose an installed target app.";
        if (workflow.steps == null || workflow.steps.isEmpty()) return "Add at least one workflow step.";
        if (workflow.steps.size() > 30) return "A workflow can contain at most 30 steps.";
        WorkflowStep last = workflow.steps.get(workflow.steps.size() - 1);
        if (last == null || !WorkflowStep.WAIT_TEXT.equals(last.type) || last.value == null || last.value.trim().isEmpty()) {
            return "The final step must wait for an exact visible-text success condition. Memo will not mark an unverified run as successful.";
        }
        for (WorkflowStep s : workflow.steps) {
            if (s == null) return "Workflow contains an invalid step.";
            if (!isAllowedWithoutApproval(s.type)) {
                if (WorkflowStep.TAP_TEXT.equals(s.type) || WorkflowStep.BACK.equals(s.type)) {
                    return "Native tap and Back actions are temporarily disabled until Memo can request and consume an exact, one-use approval for the target app and action. No action was executed.";
                }
                return "Unsupported or unclassified native action. The workflow was blocked.";
            }
            if (WorkflowStep.WAIT_TEXT.equals(s.type) && (s.value == null || s.value.trim().isEmpty())) return "Wait steps need visible text.";
            if (s.value != null && (AD_OR_REWARD_ACTION.matcher(s.value).find() || SENSITIVE_ACTION.matcher(s.value).find())) return "This runner does not automate ad engagement, reward claims, payment confirmations, CAPTCHA or security bypasses. Remove or rewrite this step.";
            if (WorkflowStep.DELAY.equals(s.type) && (s.delayMillis < 0 || s.delayMillis > 30000)) return "Wait steps must be between 0 and 30 seconds.";
        }
        return null;
    }
}
