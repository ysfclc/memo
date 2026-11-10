package com.synthos.runtime.ai;

import com.synthos.runtime.data.Workflow;
import com.synthos.runtime.data.WorkflowStep;
import com.synthos.runtime.data.AppCapability;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Strictly validates model proposals before mapping them onto app-owned workflow types. */
public final class PlanProposalParser {
    public static final class CapabilityProposal {
        public String name, description, status;
        public double confidence;
        public final ArrayList<String> evidence = new ArrayList<>();
    }
    public static final class Proposal {
        public String kind, assistantMessage, jobTitle, targetPackage, targetLabel, rawJson, taskTitle, taskDescription, taskId, taskStatus;
        public double confidence;
        public JSONArray successCriteria = new JSONArray();
        public JSONArray evidence = new JSONArray();
        public final ArrayList<WorkflowStep> steps = new ArrayList<>();
        public final ArrayList<CapabilityProposal> capabilities = new ArrayList<>();
    }
    private PlanProposalParser() {}

    public static Proposal parse(String output) throws JSONException {
        if (output == null) throw new JSONException("The local model returned no content.");
        String raw = output.trim();
        if (raw.startsWith("```") ) {
            int first = raw.indexOf('\n'); if (first >= 0) raw = raw.substring(first + 1);
            int last = raw.lastIndexOf("```"); if (last >= 0) raw = raw.substring(0, last);
            raw = raw.trim();
        }
        int firstBrace = raw.indexOf('{'), lastBrace = raw.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) raw = raw.substring(firstBrace, lastBrace + 1);
        JSONObject o = new JSONObject(raw);
        Proposal p = new Proposal(); p.rawJson = o.toString();
        p.kind = o.optString("kind", "needs_input");
        if (!"answer".equals(p.kind) && !"inspect_app".equals(p.kind) && !"create_task".equals(p.kind) && !"set_task_status".equals(p.kind) && !"draft_workflow".equals(p.kind) && !"action_plan".equals(p.kind) && !"needs_input".equals(p.kind))
            throw new JSONException("Unsupported job kind: " + p.kind);
        p.assistantMessage = o.optString("assistant_message", "").trim();
        p.jobTitle = o.optString("job_title", "Memo job").trim();
        p.targetPackage = o.optString("target_package", "").trim(); p.targetLabel = o.optString("target_label", "").trim();
        p.taskTitle = o.optString("task_title", "").trim(); p.taskDescription = o.optString("task_description", "").trim();
        p.taskId = o.optString("task_id", "").trim(); p.taskStatus = o.optString("task_status", "").trim().toUpperCase(java.util.Locale.ROOT);
        if (p.taskTitle.length() > 120) p.taskTitle = p.taskTitle.substring(0, 120);
        if (p.taskDescription.length() > 1000) p.taskDescription = p.taskDescription.substring(0, 1000);
        p.confidence = Math.max(0.0, Math.min(1.0, o.optDouble("confidence", 0.0)));
        p.successCriteria = o.optJSONArray("success_criteria") == null ? new JSONArray() : o.optJSONArray("success_criteria");
        p.evidence = o.optJSONArray("evidence") == null ? new JSONArray() : o.optJSONArray("evidence");
        JSONArray capabilityArray = o.optJSONArray("capabilities");
        if (capabilityArray != null) {
            if (capabilityArray.length() > 10) throw new JSONException("Capability report exceeded the 10-item limit.");
            for (int i=0;i<capabilityArray.length();i++) {
                JSONObject c = capabilityArray.optJSONObject(i); if (c == null) throw new JSONException("Capability " + (i + 1) + " was not an object.");
                CapabilityProposal cp = new CapabilityProposal();
                cp.name = c.optString("name", "").trim(); cp.description = c.optString("description", "").trim();
                if (cp.name.isEmpty()) continue;
                if (cp.name.length() > 100) cp.name = cp.name.substring(0, 100);
                if (cp.description.length() > 350) cp.description = cp.description.substring(0, 350);
                cp.confidence = Math.max(0.0, Math.min(1.0, c.optDouble("confidence", 0.0)));
                cp.status = AppCapability.OBSERVED.equals(c.optString("status", "")) ? AppCapability.OBSERVED : AppCapability.INFERRED;
                JSONArray ev = c.optJSONArray("evidence");
                if (ev != null) for (int j=0;j<ev.length() && cp.evidence.size()<5;j++) {
                    String e = ev.optString(j, "").trim(); if (!e.isEmpty()) cp.evidence.add(e.length()>160?e.substring(0,160):e);
                }
                p.capabilities.add(cp);
            }
        }
        JSONArray steps = o.optJSONArray("steps");
        if (steps != null) {
            if (steps.length() > 12) throw new JSONException("Proposal exceeded the 12-step limit.");
            for (int i=0;i<steps.length();i++) {
                JSONObject s = steps.optJSONObject(i); if (s == null) throw new JSONException("Step " + (i + 1) + " was not an object.");
                String type = s.optString("type", "").trim(); String value = s.optString("value", "").trim();
                int delay = Math.max(0, Math.min(30000, s.optInt("delayMillis", 0)));
                if (!WorkflowStep.LAUNCH_APP.equals(type) && !WorkflowStep.TAP_TEXT.equals(type) && !WorkflowStep.WAIT_TEXT.equals(type)
                        && !WorkflowStep.BACK.equals(type) && !WorkflowStep.DELAY.equals(type)) throw new JSONException("Unsupported action type: " + type);
                if ((WorkflowStep.TAP_TEXT.equals(type) || WorkflowStep.WAIT_TEXT.equals(type)) && value.isEmpty()) throw new JSONException(type + " needs a non-empty visible text value.");
                if ((WorkflowStep.BACK.equals(type) || WorkflowStep.LAUNCH_APP.equals(type)) && !value.isEmpty()) value = "";
                if (WorkflowStep.DELAY.equals(type) && delay <= 0) delay = 1000;
                p.steps.add(new WorkflowStep(type, value, delay));
            }
        }
        if (p.assistantMessage.isEmpty()) throw new JSONException("Proposal contained no user-facing explanation.");
        if ("create_task".equals(p.kind) && p.taskTitle.isEmpty()) throw new JSONException("Task proposal must include a non-empty task_title.");
        if ("set_task_status".equals(p.kind) && (p.taskId.isEmpty() || (!"OPEN".equals(p.taskStatus) && !"DONE".equals(p.taskStatus)))) throw new JSONException("Task status proposal needs an existing task_ref and OPEN or DONE status.");
        if (("draft_workflow".equals(p.kind) || "action_plan".equals(p.kind)) && (p.targetPackage.isEmpty() || p.steps.isEmpty()))
            throw new JSONException("Workflow proposal must include a target package and at least one supported step.");
        return p;
    }

    public static Workflow toWorkflow(Proposal p, String targetPackage, String targetLabel) {
        Workflow w = new Workflow(p.jobTitle.isEmpty() ? "AI workflow draft" : p.jobTitle, targetPackage, targetLabel);
        for (WorkflowStep step : p.steps) w.steps.add(new WorkflowStep(step.type, step.value, step.delayMillis));
        if (w.steps.stream().noneMatch(s -> WorkflowStep.LAUNCH_APP.equals(s.type))) w.steps.add(0, new WorkflowStep(WorkflowStep.LAUNCH_APP, "", 0));
        return w;
    }
}
