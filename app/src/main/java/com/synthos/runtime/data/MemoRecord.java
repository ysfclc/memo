package com.synthos.runtime.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;

/** One extensible record type for people, organizations, opportunities, projects and private drafts. */
public final class MemoRecord {
    public static final String CONTACT = "contact";
    public static final String COMPANY = "company";
    public static final String OPPORTUNITY = "opportunity";
    public static final String PROJECT = "project";
    public static final String STUDIO_DRAFT = "studio_draft";

    public String id;
    public String kind;
    public String title;
    /** Company, role, channel, format or other short descriptor depending on kind. */
    public String subtitle;
    public String body;
    public String status;
    public String sourceUrl;
    public String sourceCaptureId;
    public long createdAt;
    public long updatedAt;

    public MemoRecord(String kind, String title, String subtitle, String body, String status,
                      String sourceUrl, String sourceCaptureId) {
        long now = System.currentTimeMillis();
        this.id = UUID.randomUUID().toString();
        this.kind = normalizeKind(kind);
        this.title = bounded(title, 160, "Untitled record");
        this.subtitle = bounded(subtitle, 220, "");
        this.body = bounded(body, 30000, "");
        this.status = bounded(status, 40, defaultStatus(this.kind)).toUpperCase(Locale.ROOT);
        this.sourceUrl = safeHttpsUrl(sourceUrl);
        this.sourceCaptureId = bounded(sourceCaptureId, 80, "");
        this.createdAt = now;
        this.updatedAt = now;
    }

    private static String normalizeKind(String value) {
        String kind = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (CONTACT.equals(kind) || COMPANY.equals(kind) || OPPORTUNITY.equals(kind) || PROJECT.equals(kind) || STUDIO_DRAFT.equals(kind)) return kind;
        return PROJECT;
    }

    public static String defaultStatus(String kind) {
        return STUDIO_DRAFT.equals(kind) ? "DRAFT" : (PROJECT.equals(kind) ? "ACTIVE" : "NEW");
    }

    private static String bounded(String value, int max, String fallback) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return fallback;
        return text.length() > max ? text.substring(0, max) : text;
    }

    private static String safeHttpsUrl(String value) {
        String url = value == null ? "" : value.trim();
        return url.regionMatches(true, 0, "https://", 0, 8) && url.length() <= 2048 ? url : "";
    }

    public String kindLabel() {
        switch (kind) {
            case CONTACT: return "PERSON";
            case COMPANY: return "COMPANY";
            case OPPORTUNITY: return "OPPORTUNITY";
            case STUDIO_DRAFT: return "STUDIO DRAFT";
            default: return "PROJECT";
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("kind", kind); o.put("title", title); o.put("subtitle", subtitle);
        o.put("body", body); o.put("status", status); o.put("sourceUrl", sourceUrl);
        o.put("sourceCaptureId", sourceCaptureId); o.put("createdAt", createdAt); o.put("updatedAt", updatedAt);
        return o;
    }

    public static MemoRecord fromJson(JSONObject o) {
        String kind = o.optString("kind", PROJECT);
        MemoRecord record = new MemoRecord(kind, o.optString("title", "Untitled record"),
                o.optString("subtitle", ""), o.optString("body", ""),
                o.optString("status", defaultStatus(kind)), o.optString("sourceUrl", ""),
                o.optString("sourceCaptureId", ""));
        record.id = o.optString("id", UUID.randomUUID().toString());
        record.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        record.updatedAt = o.optLong("updatedAt", record.createdAt);
        return record;
    }
}
