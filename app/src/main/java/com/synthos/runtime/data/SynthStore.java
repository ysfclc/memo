package com.synthos.runtime.data;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Encrypted, offline-first store. The AES-GCM key is generated and retained by Android Keystore. */
public final class SynthStore {
    private static final String KEY_ALIAS = "synthos.runtime.store.v1";
    private static final String FILE_NAME = "synthos-vault.bin";
    private static volatile SynthStore sharedInstance;
    private final Context appContext;
    private final File storeFile;
    private JSONObject cached;

    public static synchronized SynthStore shared(Context context) throws IOException {
        if (sharedInstance == null) sharedInstance = new SynthStore(context);
        return sharedInstance;
    }

    public SynthStore(Context context) throws IOException {
        appContext = context.getApplicationContext();
        storeFile = new File(appContext.getFilesDir(), FILE_NAME);
        synchronized (this) { cached = readFile(); }
    }

    private JSONObject emptyData() {
        JSONObject o = new JSONObject();
        try { o.put("schemaVersion", 1); o.put("workflows", new JSONArray()); o.put("workflowVersions", new JSONArray()); o.put("runs", new JSONArray());
            o.put("jobs", new JSONArray()); o.put("chatMessages", new JSONArray()); o.put("tasks", new JSONArray());
            o.put("appCapabilities", new JSONArray()); o.put("captures", new JSONArray()); o.put("records", new JSONArray());
            o.put("surfaceSchemas", new JSONArray()); o.put("surfaceSchemaVersions", new JSONArray()); o.put("updatedAt", System.currentTimeMillis()); }
        catch (JSONException ignored) {}
        return o;
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        java.security.Key existing = ks.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private JSONObject readFile() throws IOException {
        if (!storeFile.exists()) return emptyData();
        try (FileInputStream in = new FileInputStream(storeFile); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int n; while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            byte[] payload = out.toByteArray();
            if (payload.length < 14) throw new IOException("Encrypted workspace is incomplete.");
            int ivLength = payload[0] & 0xff;
            if (ivLength < 12 || ivLength > 16 || payload.length <= ivLength + 1) throw new IOException("Encrypted workspace has an invalid header.");
            byte[] iv = new byte[ivLength]; System.arraycopy(payload, 1, iv, 0, ivLength);
            byte[] cipherText = new byte[payload.length - 1 - ivLength]; System.arraycopy(payload, 1 + ivLength, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(cipherText);
            JSONObject result = new JSONObject(new String(plain, StandardCharsets.UTF_8));
            if (result.optInt("schemaVersion", 0) != 1) throw new IOException("Unsupported workspace schema version.");
            if (!result.has("workflowVersions")) result.put("workflowVersions", new JSONArray());
            if (!result.has("captures")) result.put("captures", new JSONArray());
            if (!result.has("records")) result.put("records", new JSONArray());
            if (!result.has("surfaceSchemas")) result.put("surfaceSchemas", new JSONArray());
            if (!result.has("surfaceSchemaVersions")) result.put("surfaceSchemaVersions", new JSONArray());
            return result;
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Could not decrypt workspace. It was not overwritten; export or restore a valid backup if available.", e); }
    }

    private void persist() throws IOException {
        try {
            cached.put("updatedAt", System.currentTimeMillis());
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV(); byte[] encrypted = cipher.doFinal(cached.toString().getBytes(StandardCharsets.UTF_8));
            File temp = new File(storeFile.getParentFile(), FILE_NAME + ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(iv.length); out.write(iv); out.write(encrypted); out.flush(); out.getFD().sync();
            }
            android.system.Os.rename(temp.getAbsolutePath(), storeFile.getAbsolutePath());
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Could not encrypt workspace with Android Keystore.", e); }
    }

    public synchronized List<Workflow> getWorkflows() {
        JSONArray a = cached.optJSONArray("workflows"); ArrayList<Workflow> out = new ArrayList<>();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if(o!=null) out.add(Workflow.fromJson(o)); }
        Collections.sort(out, (x,y) -> Long.compare(y.updatedAt, x.updatedAt)); return out;
    }
    public synchronized Workflow getWorkflow(String id) {
        for (Workflow w : getWorkflows()) if (w.id.equals(id)) return w; return null;
    }
    public synchronized void saveWorkflow(Workflow workflow) throws IOException {
        JSONArray current = cached.optJSONArray("workflows"); if (current == null) current = new JSONArray();
        JSONArray history = cached.optJSONArray("workflowVersions"); if (history == null) history = new JSONArray();
        JSONArray next = new JSONArray(); boolean replaced = false;
        try {
            for (int i = 0; i < current.length(); i++) {
                JSONObject old = current.optJSONObject(i); if (old == null) continue;
                if (!old.optString("id").equals(workflow.id)) { next.put(old); continue; }
                replaced = true;
                Workflow previous = Workflow.fromJson(old);
                JSONObject before = previous.toJson(); before.remove("updatedAt");
                JSONObject proposed = workflow.toJson(); proposed.remove("updatedAt");
                if (!before.toString().equals(proposed.toString())) {
                    // Preserve the complete prior definition before advancing the active revision.
                    JSONObject archived = new JSONObject(old.toString());
                    archived.put("archivedAt", System.currentTimeMillis());
                    history.put(archived);
                    while (history.length() > 200) { JSONArray trimmed = new JSONArray(); for (int h = history.length()-200; h < history.length(); h++) trimmed.put(history.get(h)); history = trimmed; }
                    workflow.version = previous.version + 1;
                } else {
                    workflow.version = previous.version;
                }
                workflow.createdAt = previous.createdAt;
                workflow.updatedAt = System.currentTimeMillis();
                next.put(workflow.toJson());
            }
            if (!replaced) { workflow.version = Math.max(1, workflow.version); workflow.updatedAt = System.currentTimeMillis(); next.put(workflow.toJson()); }
            cached.put("workflows", next); cached.put("workflowVersions", history); persist();
        } catch (JSONException e) { throw new IOException("Could not serialize workflow revision.", e); }
    }

    public synchronized List<JSONObject> getWorkflowVersions(String workflowId) {
        JSONArray a = cached.optJSONArray("workflowVersions"); ArrayList<JSONObject> out = new ArrayList<>();
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && o.optString("id").equals(workflowId)) out.add(o);
        }
        Collections.sort(out, (x, y) -> Integer.compare(y.optInt("version", 1), x.optInt("version", 1)));
        return out;
    }
    public synchronized void deleteWorkflow(String id) throws IOException {
        JSONArray a=cached.optJSONArray("workflows"), next=new JSONArray();
        if(a!=null) for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o!=null&&!o.optString("id").equals(id)) next.put(o); }
        try { cached.put("workflows",next); persist(); } catch(JSONException e){ throw new IOException(e); }
    }
    public synchronized void saveRun(RunRecord run) throws IOException {
        JSONArray a=cached.optJSONArray("runs"), next=new JSONArray(); boolean replaced=false;
        try {
            if(a!=null) for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o==null) continue; if(o.optString("id").equals(run.id)){next.put(run.toJson());replaced=true;}else next.put(o);}
            if(!replaced) next.put(run.toJson());
            while(next.length()>60) { JSONArray trimmed=new JSONArray(); for(int i=next.length()-60;i<next.length();i++) trimmed.put(next.get(i)); next=trimmed; }
            cached.put("runs",next); cached.put("activeRunId", (RunRecord.RUNNING.equals(run.status)||RunRecord.PAUSED_LOCKED.equals(run.status)||RunRecord.PAUSED_EXTERNAL.equals(run.status)) ? run.id : ""); persist();
        } catch(JSONException e) { throw new IOException("Could not save run history.", e); }
    }
    public synchronized List<RunRecord> getRuns() {
        JSONArray a=cached.optJSONArray("runs"); ArrayList<RunRecord> out=new ArrayList<>();
        if(a!=null) for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)out.add(RunRecord.fromJson(o));}
        out.sort((x,y)->Long.compare(y.startedAt,x.startedAt)); return out;
    }
    public synchronized void recoverInterruptedRuns() throws IOException {
        boolean changed=false;
        for(RunRecord run:getRuns()) if(RunRecord.RUNNING.equals(run.status)||RunRecord.PAUSED_LOCKED.equals(run.status)||RunRecord.PAUSED_EXTERNAL.equals(run.status)) {
            run.status=RunRecord.INTERRUPTED; run.finishedAt=System.currentTimeMillis(); run.addEvent("App process restarted; execution did not resume automatically."); saveRun(run); changed=true;
        }
        if(!changed && cached.optString("activeRunId", "").length()>0) { try { cached.put("activeRunId",""); persist(); } catch(JSONException e){ throw new IOException(e); } }
    }
    public synchronized String exportPlainJson() {
        try { return cached.toString(2); }
        catch (JSONException e) { return cached.toString(); }
    }

    public synchronized List<JobRecord> getJobs() {
        JSONArray a = cached.optJSONArray("jobs"); ArrayList<JobRecord> out = new ArrayList<>();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null) out.add(JobRecord.fromJson(o)); }
        out.sort((x,y) -> Long.compare(y.updatedAt, x.updatedAt)); return out;
    }
    public synchronized JobRecord getJob(String id) {
        for (JobRecord job : getJobs()) if (job.id.equals(id)) return job;
        return null;
    }
    public synchronized void saveJob(JobRecord job) throws IOException {
        job.updatedAt = System.currentTimeMillis(); JSONArray a = cached.optJSONArray("jobs"); if (a == null) a = new JSONArray();
        JSONArray next = new JSONArray(); boolean replaced = false;
        try {
            for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o==null) continue;
                if (o.optString("id").equals(job.id)) { next.put(job.toJson()); replaced=true; } else next.put(o); }
            if (!replaced) next.put(job.toJson());
            while (next.length() > 80) { JSONArray trimmed = new JSONArray(); for (int i=next.length()-80;i<next.length();i++) trimmed.put(next.get(i)); next=trimmed; }
            cached.put("jobs", next); persist();
        } catch (JSONException e) { throw new IOException("Could not save local AI job.", e); }
    }
    public synchronized List<ChatMessage> getChatMessages() {
        JSONArray a = cached.optJSONArray("chatMessages"); ArrayList<ChatMessage> out = new ArrayList<>();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null) out.add(ChatMessage.fromJson(o)); }
        return out;
    }
    public synchronized void appendChatMessage(ChatMessage message) throws IOException {
        JSONArray a = cached.optJSONArray("chatMessages"); if (a == null) a = new JSONArray();
        JSONArray next = new JSONArray();
        try {
            for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o!=null) next.put(o); }
            next.put(message.toJson());
            while (next.length() > 100) { JSONArray trimmed = new JSONArray(); for (int i=next.length()-100;i<next.length();i++) trimmed.put(next.get(i)); next=trimmed; }
            cached.put("chatMessages", next); persist();
        } catch (JSONException e) { throw new IOException("Could not save conversation message.", e); }
    }
    public synchronized void clearChatMessages() throws IOException {
        try { cached.put("chatMessages", new JSONArray()); persist(); } catch (JSONException e) { throw new IOException(e); }
    }


    public synchronized List<TaskRecord> getTasks() {
        JSONArray a = cached.optJSONArray("tasks"); ArrayList<TaskRecord> out = new ArrayList<>();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null) out.add(TaskRecord.fromJson(o)); }
        out.sort((x,y) -> Long.compare(y.updatedAt, x.updatedAt)); return out;
    }
    public synchronized void saveTask(TaskRecord task) throws IOException {
        task.updatedAt = System.currentTimeMillis(); JSONArray a = cached.optJSONArray("tasks"); if (a == null) a = new JSONArray();
        JSONArray next = new JSONArray(); boolean replaced = false;
        try {
            for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o==null) continue;
                if (o.optString("id").equals(task.id)) { next.put(task.toJson()); replaced=true; } else next.put(o); }
            if (!replaced) next.put(task.toJson());
            while (next.length() > 100) { JSONArray trimmed = new JSONArray(); for (int i=next.length()-100;i<next.length();i++) trimmed.put(next.get(i)); next=trimmed; }
            cached.put("tasks", next); persist();
        } catch (JSONException e) { throw new IOException("Could not save task.", e); }
    }

    public synchronized List<AppCapability> getAppCapabilities() {
        JSONArray a = cached.optJSONArray("appCapabilities"); ArrayList<AppCapability> out = new ArrayList<>();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null) out.add(AppCapability.fromJson(o)); }
        out.sort((x,y) -> Long.compare(y.observedAt, x.observedAt)); return out;
    }
    public synchronized void saveAppCapability(AppCapability capability) throws IOException {
        JSONArray a = cached.optJSONArray("appCapabilities"); if (a == null) a = new JSONArray();
        JSONArray next = new JSONArray(); boolean replaced = false;
        try {
            for (int i=0;i<a.length();i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                boolean same = o.optString("packageName").equals(capability.packageName)
                        && o.optString("name").trim().equalsIgnoreCase(capability.name.trim());
                if (same) { capability.id = o.optString("id", capability.id); next.put(capability.toJson()); replaced=true; }
                else next.put(o);
            }
            if (!replaced) next.put(capability.toJson());
            while (next.length() > 120) { JSONArray trimmed = new JSONArray(); for (int i=next.length()-120;i<next.length();i++) trimmed.put(next.get(i)); next=trimmed; }
            cached.put("appCapabilities", next); persist();
        } catch (JSONException e) { throw new IOException("Could not save app capability evidence.", e); }
    }

    public synchronized List<CaptureRecord> getCaptures() {
        JSONArray a = cached.optJSONArray("captures"); ArrayList<CaptureRecord> out = new ArrayList<>();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null) out.add(CaptureRecord.fromJson(o)); }
        out.sort((x,y) -> Long.compare(y.createdAt, x.createdAt)); return out;
    }

    public synchronized void saveCapture(CaptureRecord capture) throws IOException {
        JSONArray a = cached.optJSONArray("captures"); if (a == null) a = new JSONArray();
        JSONArray next = new JSONArray();
        try {
            for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null) next.put(o); }
            next.put(capture.toJson());
            while (next.length() > 200) { JSONArray trimmed = new JSONArray(); for (int i=next.length()-200;i<next.length();i++) trimmed.put(next.get(i)); next=trimmed; }
            cached.put("captures", next); persist();
        } catch (JSONException e) { throw new IOException("Could not save source capture.", e); }
    }

    public synchronized void deleteCapture(String id) throws IOException {
        JSONArray a = cached.optJSONArray("captures"), next = new JSONArray();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null && !o.optString("id").equals(id)) next.put(o); }
        try { cached.put("captures", next); persist(); } catch (JSONException e) { throw new IOException("Could not delete source capture.", e); }
    }

    public synchronized List<MemoRecord> getRecords() {
        JSONArray a = cached.optJSONArray("records"); ArrayList<MemoRecord> out = new ArrayList<>();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null) out.add(MemoRecord.fromJson(o)); }
        out.sort((x,y) -> Long.compare(y.updatedAt, x.updatedAt));
        return out;
    }

    public synchronized void saveRecord(MemoRecord record) throws IOException {
        JSONArray a = cached.optJSONArray("records"); if (a == null) a = new JSONArray();
        JSONArray next = new JSONArray(); boolean replaced = false;
        try {
            for (int i=0;i<a.length();i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                if (o.optString("id").equals(record.id)) { record.updatedAt = System.currentTimeMillis(); next.put(record.toJson()); replaced = true; }
                else next.put(o);
            }
            if (!replaced) next.put(record.toJson());
            while (next.length() > 500) { JSONArray trimmed = new JSONArray(); for (int i=next.length()-500;i<next.length();i++) trimmed.put(next.get(i)); next = trimmed; }
            cached.put("records", next); persist();
        } catch (JSONException e) { throw new IOException("Could not save Memo record.", e); }
    }

    public synchronized void deleteRecord(String id) throws IOException {
        JSONArray a = cached.optJSONArray("records"), next = new JSONArray();
        if (a != null) for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if (o != null && !o.optString("id").equals(id)) next.put(o); }
        try { cached.put("records", next); persist(); } catch (JSONException e) { throw new IOException("Could not delete Memo record.", e); }
    }

    public synchronized void recoverInterruptedJobs() throws IOException {
        for (JobRecord job : getJobs()) {
            if (JobRecord.PREPARING.equals(job.state)) {
                job.state = JobRecord.REVIEW_REQUIRED;
                job.result = "The app process ended while the local model was working. This job was not replayed automatically. Review and submit it again if needed.";
                saveJob(job);
            }
        }
    }

    /** Current user-named accessibility surface snapshots. Each item records a visible label, never an editable value. */
    public synchronized List<JSONObject> getSurfaceSchemas() {
        JSONArray array = cached.optJSONArray("surfaceSchemas");
        ArrayList<JSONObject> out = new ArrayList<>();
        if (array != null) for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) out.add(item);
        }
        out.sort((a, b) -> Long.compare(b.optLong("updatedAt", 0L), a.optLong("updatedAt", 0L)));
        return out;
    }

    /** Includes the active definition and archived revisions for a stable schema ID. */
    public synchronized List<JSONObject> getSurfaceSchemaVersions(String schemaId) {
        ArrayList<JSONObject> out = new ArrayList<>();
        JSONArray history = cached.optJSONArray("surfaceSchemaVersions");
        if (history != null) for (int i = 0; i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            if (item != null && item.optString("id").equals(schemaId)) out.add(item);
        }
        JSONArray current = cached.optJSONArray("surfaceSchemas");
        if (current != null) for (int i = 0; i < current.length(); i++) {
            JSONObject item = current.optJSONObject(i);
            if (item != null && item.optString("id").equals(schemaId)) out.add(item);
        }
        out.sort((a, b) -> Integer.compare(b.optInt("version", 1), a.optInt("version", 1)));
        return out;
    }

    /**
     * Create or revise a named accessible-surface snapshot. Re-capturing an identical set of labels
     * updates the observation time but does not create a needless schema revision.
     */
    public synchronized JSONObject saveSurfaceSchema(String packageName, String appLabel, String surfaceName,
                                                       JSONArray items, long observedAt) throws IOException {
        String pkg = packageName == null ? "" : packageName.trim();
        String app = appLabel == null ? "" : appLabel.trim();
        String name = surfaceName == null ? "" : surfaceName.trim();
        if (pkg.isEmpty() || pkg.length() > 255) throw new IOException("Choose a valid target package before saving a schema.");
        if (name.isEmpty() || name.length() > 100) throw new IOException("Give this screen schema a name of 1–100 characters.");
        if (items == null || items.length() == 0 || items.length() > 60) throw new IOException("A surface schema must contain 1–60 observed labels.");

        JSONArray safeItems = new JSONArray();
        try {
            for (int i = 0; i < items.length(); i++) {
                JSONObject raw = items.optJSONObject(i);
                if (raw == null) continue;
                String label = raw.optString("label", "").trim();
                if (label.isEmpty() || label.length() > 140) continue;
                JSONObject cleaned = new JSONObject();
                cleaned.put("label", label);
                cleaned.put("clickable", raw.optBoolean("clickable", false));
                safeItems.put(cleaned);
            }
            if (safeItems.length() == 0) throw new IOException("No valid visible labels were found in this screen snapshot.");

            String key = pkg + "::" + name.toLowerCase(Locale.ROOT);
            JSONArray current = cached.optJSONArray("surfaceSchemas");
            if (current == null) current = new JSONArray();
            JSONArray history = cached.optJSONArray("surfaceSchemaVersions");
            if (history == null) history = new JSONArray();
            JSONObject previous = null;
            JSONArray next = new JSONArray();
            for (int i = 0; i < current.length(); i++) {
                JSONObject item = current.optJSONObject(i);
                if (item == null) continue;
                if (item.optString("schemaKey").equals(key)) previous = item;
                else next.put(item);
            }

            long now = System.currentTimeMillis();
            String id = previous == null ? UUID.randomUUID().toString() : previous.optString("id", UUID.randomUUID().toString());
            int priorVersion = previous == null ? 0 : Math.max(1, previous.optInt("version", 1));
            JSONArray oldItems = previous == null ? null : previous.optJSONArray("items");
            boolean unchanged = previous != null
                    && previous.optString("appLabel").equals(app)
                    && oldItems != null
                    && oldItems.toString().equals(safeItems.toString());

            if (previous != null && !unchanged) {
                JSONObject archived = new JSONObject(previous.toString());
                archived.put("archivedAt", now);
                history.put(archived);
                while (history.length() > 240) {
                    JSONArray trimmed = new JSONArray();
                    for (int i = history.length() - 240; i < history.length(); i++) trimmed.put(history.get(i));
                    history = trimmed;
                }
            }

            JSONObject schema = unchanged ? new JSONObject(previous.toString()) : new JSONObject();
            schema.put("id", id);
            schema.put("schemaKey", key);
            schema.put("packageName", pkg);
            schema.put("appLabel", app.isEmpty() ? pkg : app);
            schema.put("surfaceName", name);
            schema.put("version", unchanged ? priorVersion : priorVersion + 1);
            schema.put("items", safeItems);
            schema.put("observedAt", Math.max(0L, observedAt));
            if (previous == null) schema.put("createdAt", now);
            schema.put("updatedAt", now);
            next.put(schema);
            while (next.length() > 160) {
                JSONArray trimmed = new JSONArray();
                for (int i = next.length() - 160; i < next.length(); i++) trimmed.put(next.get(i));
                next = trimmed;
            }
            cached.put("surfaceSchemas", next);
            cached.put("surfaceSchemaVersions", history);
            persist();
            return new JSONObject(schema.toString());
        } catch (JSONException e) {
            throw new IOException("Could not save the surface schema revision.", e);
        }
    }

    /** Archive history is kept when an active schema is removed, so the action is reversible by review. */
    public synchronized void deleteSurfaceSchema(String schemaId) throws IOException {
        JSONArray current = cached.optJSONArray("surfaceSchemas");
        JSONArray next = new JSONArray();
        if (current != null) for (int i = 0; i < current.length(); i++) {
            JSONObject item = current.optJSONObject(i);
            if (item != null && !item.optString("id").equals(schemaId)) next.put(item);
        }
        try { cached.put("surfaceSchemas", next); persist(); }
        catch (JSONException e) { throw new IOException("Could not remove the active surface schema.", e); }
    }

    public synchronized boolean hasWorkflows() { JSONArray a=cached.optJSONArray("workflows"); return a!=null && a.length()>0; }
}
