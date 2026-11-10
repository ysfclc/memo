package com.synthos.runtime.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

/** Durable job envelope: intent, selected skills, proposal, state and evidence-backed result. */
public final class JobRecord {
    public static final String PREPARING = "PREPARING";
    public static final String BLOCKED_MODEL = "BLOCKED_MODEL";
    public static final String NEEDS_INPUT = "NEEDS_INPUT";
    public static final String NEEDS_APPROVAL = "NEEDS_APPROVAL";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    public static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";

    public String id;
    public String goal;
    public String state;
    public String targetPackage = "";
    public String targetLabel = "";
    public String planJson = "";
    public String result = "";
    public String linkedWorkflowId = "";
    public String modelPath = "";
    public long createdAt;
    public long updatedAt;
    public final ArrayList<String> skillIds = new ArrayList<>();

    public JobRecord(String goal) {
        this.id = UUID.randomUUID().toString();
        this.goal = goal == null ? "" : goal.trim();
        this.state = PREPARING;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("goal", goal); o.put("state", state); o.put("targetPackage", targetPackage);
        o.put("targetLabel", targetLabel); o.put("planJson", planJson); o.put("result", result);
        o.put("linkedWorkflowId", linkedWorkflowId); o.put("modelPath", modelPath);
        o.put("createdAt", createdAt); o.put("updatedAt", updatedAt);
        JSONArray a = new JSONArray(); for (String id : skillIds) a.put(id); o.put("skillIds", a);
        return o;
    }

    public static JobRecord fromJson(JSONObject o) {
        JobRecord j = new JobRecord(o.optString("goal", ""));
        j.id = o.optString("id", UUID.randomUUID().toString());
        j.state = o.optString("state", PREPARING);
        j.targetPackage = o.optString("targetPackage", ""); j.targetLabel = o.optString("targetLabel", "");
        j.planJson = o.optString("planJson", ""); j.result = o.optString("result", "");
        j.linkedWorkflowId = o.optString("linkedWorkflowId", ""); j.modelPath = o.optString("modelPath", "");
        j.createdAt = o.optLong("createdAt", System.currentTimeMillis()); j.updatedAt = o.optLong("updatedAt", j.createdAt);
        JSONArray ids = o.optJSONArray("skillIds"); if (ids != null) for (int i=0;i<ids.length();i++) { String value = ids.optString(i, ""); if (!value.isEmpty()) j.skillIds.add(value); }
        return j;
    }
}
