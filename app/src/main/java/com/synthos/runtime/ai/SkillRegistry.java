package com.synthos.runtime.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Versioned, reusable instruction skills. Skills shape reasoning; they never grant permissions. */
public final class SkillRegistry {
    public static final class Skill {
        public final String id, name, version, purpose, instruction;
        Skill(String id, String name, String version, String purpose, String instruction) {
            this.id=id; this.name=name; this.version=version; this.purpose=purpose; this.instruction=instruction;
        }
    }
    private static final Map<String, Skill> SKILLS;
    static {
        LinkedHashMap<String, Skill> s = new LinkedHashMap<>();
        add(s,"normalize-intent","Normalize intent","1.0.0","Turn conversational requests into outcomes and constraints.","Identify the user's desired outcome, target, constraints and observable completion criteria. Preserve ambiguity instead of inventing missing facts.");
        add(s,"inspect-app-surface","Inspect app surface","1.0.0","Interpret an explicitly selected app's observed UI.","Treat the supplied accessibility snapshot as partial evidence, not the whole application. State what is directly observed, what is inferred, and what is unknown. Never claim to have seen content absent from the snapshot.");
        add(s,"map-app-capabilities","Map app capabilities","1.0.0","Derive candidate capabilities from observed controls and transitions.","Propose only capabilities supported by observed labels or explicit app metadata. Attach evidence labels and confidence. Mark untested hypotheses as inferred, not proven.");
        add(s,"plan-workflow","Plan workflow","1.0.0","Turn an outcome into a short, reviewable workflow.","Create the smallest sequence of explicit supported steps. Use semantic visible text, not guessed coordinates. Include waits only when needed. A plan is a proposal and must not be executed automatically.");
        add(s,"validate-workflow","Validate workflow","1.0.0","Check the plan against the available action contract.","Only use LAUNCH_APP, TAP_TEXT, WAIT_TEXT, BACK, and DELAY. TAP_TEXT and WAIT_TEXT need a specific visible string. Limit to 12 steps. Reject unsupported, destructive, payment, authentication, security-bypass, CAPTCHA, or provider-fraud actions.");
        add(s,"propose-action","Propose a bounded action","1.0.0","Recommend a single next step using current evidence.","A model may propose an action but never perform it. Do not imply an action was sent unless the executor returns a result.");
        add(s,"verify-state","Verify state","1.0.0","Compare a result with observed evidence.","Completion requires a later observation that matches the requested postcondition. A tap acknowledgment is not completion. If evidence is missing, report unverified.");
        add(s,"recover-from-failure","Recover from failure","1.0.0","Suggest safe recovery when a step fails.","Explain the mismatch, prefer re-observation and one bounded alternative, and request user help when state or intent is uncertain. Do not repeat a side effect blindly.");
        add(s,"create-task","Create task entity","1.0.0","Turn a clear request into a durable, user-visible task.","Create a task only when the user asks to track or remember work. Use a concise title; preserve relevant details in description. Do not invent a deadline or mark it done.");
        add(s,"manage-task","Manage task state","1.0.0","Find and update one existing user-owned task.","Use only task references present in supplied task context. Set state only when the user clearly requests completion or reopening. If the reference is ambiguous, ask a question rather than guessing. Do not delete tasks.");
        add(s,"record-evidence","Record evidence","1.0.0","Keep source, timestamp and outcome attached to jobs.","Reference supplied observation labels and their timestamp. Do not store passwords or editable-field values. Separate observed facts from model interpretation.");
        add(s,"policy-gate","Apply policy gate","1.0.0","Keep the proposal within the app's explicit capability limits.","No shell, arbitrary code execution, network exfiltration, hidden screen capture, permission bypass, stealth actions, or autonomous external side effects. Require user review before workflow execution.");
        SKILLS = Collections.unmodifiableMap(s);
    }
    private static void add(Map<String, Skill> s, String id, String name, String version, String purpose, String instruction) { s.put(id,new Skill(id,name,version,purpose,instruction)); }

    public static List<Skill> all() { return Collections.unmodifiableList(new ArrayList<>(SKILLS.values())); }
    public static Skill get(String id) { return SKILLS.get(id); }

    public static List<Skill> select(String goal, boolean hasObservation) {
        String q = goal == null ? "" : goal.toLowerCase(Locale.ROOT);
        ArrayList<String> ids = new ArrayList<>();
        ids.add("normalize-intent"); ids.add("policy-gate"); ids.add("record-evidence");
        if (hasAny(q,"create a task","create task","make a task","new task","add a task","remind me","remember to","add this to my list","follow up","task to")) ids.add("create-task");
        if (hasAny(q,"task","todo","to-do","my list","mark complete","mark done","complete task","complete the task","reopen task","reopen the task")) ids.add("manage-task");
        if (hasObservation || hasAny(q,"inspect","study","watch","screen","what is on","what can this app","understand this app","analyze this app","analyse this app")) {
            ids.add("inspect-app-surface"); ids.add("map-app-capabilities");
        }
        if (hasAny(q,"workflow","automate","repeat","every day","every time","when i","when the","create a flow","make a flow","steps")) {
            ids.add("plan-workflow"); ids.add("validate-workflow");
        }
        if (hasAny(q,"tap","click","press","open","navigate","do this","action")) ids.add("propose-action");
        if (hasAny(q,"verify","confirm","did it","check if","result")) ids.add("verify-state");
        if (hasAny(q,"failed","doesn't work","does not work","stuck","error","recover")) ids.add("recover-from-failure");
        if (!ids.contains("plan-workflow") && hasAny(q,"make","create","build","set up","setup")) { ids.add("plan-workflow"); ids.add("validate-workflow"); }
        ArrayList<Skill> selected = new ArrayList<>(); for (String id : ids) if (SKILLS.containsKey(id)) selected.add(SKILLS.get(id));
        return Collections.unmodifiableList(selected);
    }

    private static boolean hasAny(String q, String... terms) { for (String t : terms) if (q.contains(t)) return true; return false; }
    public static String buildInstructions(List<Skill> selected) {
        StringBuilder out = new StringBuilder();
        for (Skill s : selected) out.append("\n[SKILL ").append(s.id).append(" v").append(s.version).append(" — ").append(s.name).append("]\n")
                .append("Purpose: ").append(s.purpose).append("\nInstruction: ").append(s.instruction).append('\n');
        return out.toString();
    }
}
