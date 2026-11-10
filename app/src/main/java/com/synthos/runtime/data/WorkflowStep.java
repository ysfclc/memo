package com.synthos.runtime.data;

import org.json.JSONException;
import org.json.JSONObject;

public class WorkflowStep {
    public static final String LAUNCH_APP = "LAUNCH_APP";
    public static final String TAP_TEXT = "TAP_TEXT";
    public static final String WAIT_TEXT = "WAIT_TEXT";
    public static final String BACK = "BACK";
    public static final String DELAY = "DELAY";

    public String type;
    public String value;
    public int delayMillis;

    public WorkflowStep(String type, String value, int delayMillis) {
        this.type = type == null ? DELAY : type;
        this.value = value == null ? "" : value.trim();
        this.delayMillis = Math.max(0, Math.min(delayMillis, 30000));
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", type); o.put("value", value); o.put("delayMillis", delayMillis);
        return o;
    }

    public static WorkflowStep fromJson(JSONObject o) {
        return new WorkflowStep(o.optString("type", DELAY), o.optString("value", ""), o.optInt("delayMillis", 0));
    }

    public String label() {
        switch (type) {
            case LAUNCH_APP: return "Open target app";
            case TAP_TEXT: return "Tap text: “" + value + "”";
            case WAIT_TEXT: return "Wait for text: “" + value + "”";
            case BACK: return "Press Back";
            case DELAY: return "Wait " + Math.max(0, delayMillis / 1000f) + "s";
            default: return "Unknown step";
        }
    }
}
