package com.synthos.runtime.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

public class RunRecord {
    public static final String QUEUED="QUEUED", RUNNING="RUNNING", PAUSED_LOCKED="PAUSED_LOCKED", PAUSED_EXTERNAL="PAUSED_EXTERNAL", SUCCEEDED="SUCCEEDED", UNVERIFIED="UNVERIFIED", FAILED="FAILED", CANCELLED="CANCELLED", INTERRUPTED="INTERRUPTED";
    public String id = UUID.randomUUID().toString();
    public String workflowId, workflowName, status = QUEUED, message = "Queued";
    public long startedAt = System.currentTimeMillis(), finishedAt = 0;
    public int stepIndex = 0;
    /** Definition revision pinned at run creation; never follow mutable latest implicitly. */
    public int workflowVersion = 1;
    public ArrayList<String> events = new ArrayList<>();

    public RunRecord(String workflowId, String workflowName) { this.workflowId = workflowId; this.workflowName = workflowName; addEvent("Run created"); }
    public void addEvent(String value) {
        events.add(System.currentTimeMillis() + "|" + value);
        while (events.size() > 80) events.remove(0);
        message = value;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject(); o.put("id",id); o.put("workflowId",workflowId); o.put("workflowName",workflowName);
        o.put("status",status); o.put("message",message); o.put("startedAt",startedAt); o.put("finishedAt",finishedAt); o.put("stepIndex",stepIndex); o.put("workflowVersion",workflowVersion);
        JSONArray a = new JSONArray(); for (String e : events) a.put(e); o.put("events",a); return o;
    }
    public static RunRecord fromJson(JSONObject o) {
        RunRecord r = new RunRecord(o.optString("workflowId",""), o.optString("workflowName","Workflow"));
        r.id=o.optString("id", UUID.randomUUID().toString()); r.status=o.optString("status",INTERRUPTED); r.message=o.optString("message","");
        r.startedAt=o.optLong("startedAt",System.currentTimeMillis()); r.finishedAt=o.optLong("finishedAt",0); r.stepIndex=o.optInt("stepIndex",0); r.workflowVersion=Math.max(1,o.optInt("workflowVersion",1));
        r.events.clear(); JSONArray a=o.optJSONArray("events"); if(a!=null) for(int i=0;i<a.length();i++) r.events.add(a.optString(i));
        return r;
    }
}
