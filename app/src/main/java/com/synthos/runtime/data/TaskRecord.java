package com.synthos.runtime.data;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

/** A durable user-owned task created by the local job runner. */
public final class TaskRecord {
    public static final String OPEN = "OPEN";
    public static final String DONE = "DONE";

    public String id;
    public String title;
    public String description;
    public String status;
    public String sourceJobId;
    public long createdAt;
    public long updatedAt;

    public TaskRecord(String title, String description, String sourceJobId) {
        this.id = UUID.randomUUID().toString();
        this.title = title == null ? "Untitled task" : title.trim();
        this.description = description == null ? "" : description.trim();
        this.sourceJobId = sourceJobId == null ? "" : sourceJobId;
        this.status = OPEN;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("title", title); o.put("description", description);
        o.put("status", status); o.put("sourceJobId", sourceJobId);
        o.put("createdAt", createdAt); o.put("updatedAt", updatedAt);
        return o;
    }

    public static TaskRecord fromJson(JSONObject o) {
        TaskRecord t = new TaskRecord(o.optString("title", "Untitled task"), o.optString("description", ""), o.optString("sourceJobId", ""));
        t.id = o.optString("id", UUID.randomUUID().toString());
        t.status = o.optString("status", OPEN);
        if (!OPEN.equals(t.status) && !DONE.equals(t.status)) t.status = OPEN;
        t.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        t.updatedAt = o.optLong("updatedAt", t.createdAt);
        return t;
    }
}
