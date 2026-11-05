package com.synthos.runtime.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class Workflow {
    public String id;
    public String name;
    public String targetPackage;
    public String targetLabel;
    public long createdAt;
    public long updatedAt;
    /** Immutable definition revision. Runs pin this value so edits never silently change queued work. */
    public int version = 1;
    public ArrayList<WorkflowStep> steps = new ArrayList<>();

    public Workflow(String name, String targetPackage, String targetLabel) {
        this.id = UUID.randomUUID().toString();
        this.name = name == null ? "Untitled workflow" : name.trim();
        this.targetPackage = targetPackage == null ? "" : targetPackage;
        this.targetLabel = targetLabel == null ? "Application" : targetLabel;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("name", name); o.put("targetPackage", targetPackage); o.put("targetLabel", targetLabel);
        o.put("createdAt", createdAt); o.put("updatedAt", updatedAt); o.put("version", Math.max(1, version));
        JSONArray a = new JSONArray();
        for (WorkflowStep step : steps) a.put(step.toJson());
        o.put("steps", a);
        return o;
    }

    public static Workflow fromJson(JSONObject o) {
        Workflow w = new Workflow(o.optString("name", "Untitled workflow"), o.optString("targetPackage", ""), o.optString("targetLabel", "Application"));
        w.id = o.optString("id", UUID.randomUUID().toString());
        w.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        w.updatedAt = o.optLong("updatedAt", w.createdAt);
        w.version = Math.max(1, o.optInt("version", 1));
        JSONArray a = o.optJSONArray("steps");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject item = a.optJSONObject(i);
            if (item != null) w.steps.add(WorkflowStep.fromJson(item));
        }
        return w;
    }
}
