package com.synthos.runtime.engine;

import com.synthos.runtime.data.Workflow;
import com.synthos.runtime.data.WorkflowStep;
import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative guardrail for the native UI runner. It is not a guarantee about third-party terms. */
public final class WorkflowPolicy {
    private static final Pattern AD_OR_REWARD_ACTION = Pattern.compile("(?i)\\b(watch\\s*(an?\\s*)?(ad|video)|view\\s*(an?\\s*)?ad|play\\s*(an?\\s*)?ad|rewarded\\s*ad|advertisement|claim\\s*(a\\s*)?reward|cash\\s*out|withdraw\\s*reward|earn\\s*cash)\\b");
    private static final Pattern SENSITIVE_ACTION = Pattern.compile("(?i)\\b(enter\\s*password|type\\s*password|submit\\s*payment|confirm\\s*payment|solve\\s*captcha|bypass\\s*captcha|disable\\s*security)\\b");
    private WorkflowPolicy() {}
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
            if (!WorkflowStep.LAUNCH_APP.equals(s.type) && !WorkflowStep.TAP_TEXT.equals(s.type) && !WorkflowStep.WAIT_TEXT.equals(s.type) && !WorkflowStep.BACK.equals(s.type) && !WorkflowStep.DELAY.equals(s.type)) return "Unsupported action type.";
            if ((WorkflowStep.TAP_TEXT.equals(s.type) || WorkflowStep.WAIT_TEXT.equals(s.type)) && (s.value == null || s.value.trim().isEmpty())) return "Tap and wait steps need visible text.";
            if (s.value != null && (AD_OR_REWARD_ACTION.matcher(s.value).find() || SENSITIVE_ACTION.matcher(s.value).find())) return "This runner does not automate ad engagement, reward claims, payment confirmations, CAPTCHA or security bypasses. Remove or rewrite this step.";
            if (WorkflowStep.DELAY.equals(s.type) && (s.delayMillis < 0 || s.delayMillis > 30000)) return "Wait steps must be between 0 and 30 seconds.";
        }
        return null;
    }
}
