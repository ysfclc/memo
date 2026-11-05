package com.synthos.runtime.data;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

/** A user-triggered, local-only source capture with source identity and time provenance. */
public final class CaptureRecord {
    public String id;
    public String title;
    public String sourceType;
    public String sourceLabel;
    public String sourcePackage;
    public String sourceUrl;
    public String content;
    public long createdAt;

    public CaptureRecord(String title, String sourceType, String sourceLabel, String sourcePackage,
                         String sourceUrl, String content) {
        this.id = UUID.randomUUID().toString();
        this.title = clean(title, "Untitled capture");
        this.sourceType = clean(sourceType, "screen");
        this.sourceLabel = clean(sourceLabel, "Unknown source");
        this.sourcePackage = clean(sourcePackage, "");
        this.sourceUrl = clean(sourceUrl, "");
        String safeContent = content == null ? "" : content.trim();
        this.content = safeContent.length() > 60000 ? safeContent.substring(0, 60000) + "\n[Truncated by Memo at 60,000 characters.]" : safeContent;
        this.createdAt = System.currentTimeMillis();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("title", title); o.put("sourceType", sourceType);
        o.put("sourceLabel", sourceLabel); o.put("sourcePackage", sourcePackage);
        o.put("sourceUrl", sourceUrl); o.put("content", content); o.put("createdAt", createdAt);
        return o;
    }

    public static CaptureRecord fromJson(JSONObject o) {
        CaptureRecord record = new CaptureRecord(o.optString("title", "Untitled capture"),
                o.optString("sourceType", "screen"), o.optString("sourceLabel", "Unknown source"),
                o.optString("sourcePackage", ""), o.optString("sourceUrl", ""), o.optString("content", ""));
        record.id = o.optString("id", UUID.randomUUID().toString());
        record.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        return record;
    }
}
