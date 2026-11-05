package com.synthos.runtime.data;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

/** One persisted item in the private Memo conversation. */
public final class ChatMessage {
    public final String id;
    public final String role;
    public final String text;
    public final long createdAt;
    public final String jobId;

    public ChatMessage(String role, String text, String jobId) {
        this(UUID.randomUUID().toString(), role, text, System.currentTimeMillis(), jobId);
    }

    public ChatMessage(String id, String role, String text, long createdAt, String jobId) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.role = "user".equals(role) ? "user" : "assistant";
        this.text = text == null ? "" : text;
        this.createdAt = createdAt;
        this.jobId = jobId == null ? "" : jobId;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("role", role); o.put("text", text); o.put("createdAt", createdAt); o.put("jobId", jobId);
        return o;
    }

    public static ChatMessage fromJson(JSONObject o) {
        return new ChatMessage(o.optString("id", UUID.randomUUID().toString()), o.optString("role", "assistant"),
                o.optString("text", ""), o.optLong("createdAt", System.currentTimeMillis()), o.optString("jobId", ""));
    }
}
