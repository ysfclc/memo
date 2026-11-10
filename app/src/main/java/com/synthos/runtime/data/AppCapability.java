package com.synthos.runtime.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

/** A cautious app capability note with source evidence and a non-tested status. */
public final class AppCapability {
    public static final String OBSERVED = "OBSERVED_UI";
    public static final String INFERRED = "INFERRED";

    public String id;
    public String packageName;
    public String appLabel;
    public String name;
    public String description;
    public String status;
    public double confidence;
    public long observedAt;
    public final ArrayList<String> evidence = new ArrayList<>();

    public AppCapability(String packageName, String appLabel, String name, String description,
                         String status, double confidence, long observedAt) {
        this.id = UUID.randomUUID().toString();
        this.packageName = packageName == null ? "" : packageName;
        this.appLabel = appLabel == null ? this.packageName : appLabel;
        this.name = name == null ? "Unnamed capability" : name.trim();
        this.description = description == null ? "" : description.trim();
        this.status = OBSERVED.equals(status) ? OBSERVED : INFERRED;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.observedAt = observedAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("packageName", packageName); o.put("appLabel", appLabel);
        o.put("name", name); o.put("description", description); o.put("status", status);
        o.put("confidence", confidence); o.put("observedAt", observedAt);
        JSONArray a = new JSONArray(); for (String item : evidence) a.put(item); o.put("evidence", a);
        return o;
    }

    public static AppCapability fromJson(JSONObject o) {
        AppCapability c = new AppCapability(o.optString("packageName", ""), o.optString("appLabel", ""),
                o.optString("name", "Unnamed capability"), o.optString("description", ""),
                o.optString("status", INFERRED), o.optDouble("confidence", 0.0),
                o.optLong("observedAt", System.currentTimeMillis()));
        c.id = o.optString("id", UUID.randomUUID().toString());
        JSONArray a = o.optJSONArray("evidence");
        if (a != null) for (int i=0;i<a.length();i++) { String e=a.optString(i, ""); if (!e.isEmpty()) c.evidence.add(e); }
        return c;
    }
}
