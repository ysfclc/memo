package com.synthos.runtime.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.synthos.runtime.data.Workflow;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.HttpsURLConnection;

/** HTTPS-only client for the separately configured guest runner. AI prompts and model context are never sent. */
public final class RemoteRunnerClient {
    private static final String PREFS = "synthos.remote.runner";
    private static final String KEY_ALIAS = "synthos.remote.runner.token.v1";
    private final Context context;
    public RemoteRunnerClient(Context context) { this.context = context.getApplicationContext(); }

    public void saveConfig(String endpoint, String token) throws Exception {
        String normalized = normalizeEndpoint(endpoint);
        if (token != null && !token.trim().isEmpty() && token.trim().length() < 32) throw new IllegalArgumentException("Bearer token must be at least 32 characters.");
        SharedPreferences.Editor editor = prefs().edit().putString("endpoint", normalized);
        if (token != null && !token.trim().isEmpty()) editor.putString("token", encrypt(token.trim()));
        editor.apply();
    }
    public String endpoint() { return prefs().getString("endpoint", ""); }
    public boolean isConfigured() { return !endpoint().isEmpty() && !prefs().getString("token", "").isEmpty(); }
    public void clearConfig() {
        prefs().edit().remove("endpoint").remove("token").apply();
        try { KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null); store.deleteEntry(KEY_ALIAS); } catch (Exception ignored) {}
    }
    public JSONObject testConnection() throws Exception { return request("GET", "health", null); }
    public JSONObject submit(Workflow workflow) throws Exception {
        JSONObject body = new JSONObject(); body.put("workflow", workflow.toJson()); body.put("approved", true);
        return request("POST", "jobs", body);
    }
    public JSONObject getJob(String id) throws Exception { return request("GET", "jobs/" + id, null); }
    public JSONObject cancel(String id) throws Exception { return request("POST", "jobs/" + id + "/cancel", new JSONObject()); }

    private JSONObject request(String method, String path, JSONObject body) throws Exception {
        if (!isConfigured()) throw new IllegalStateException("Configure the HTTPS guest runner and bearer token in Settings first.");
        String base = endpoint();
        URL url = new URL(base + path);
        if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("Remote runner connections must use HTTPS.");
        String encrypted = prefs().getString("token", "");
        String token = decrypt(encrypted);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod(method); connection.setConnectTimeout(8000); connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(false); connection.setUseCaches(false);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 131072) throw new IllegalArgumentException("Remote request exceeds the 128 KiB limit.");
            connection.setDoOutput(true); connection.setRequestProperty("Content-Type", "application/json; charset=utf-8"); connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
        }
        int code = connection.getResponseCode();
        InputStream input = code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream();
        String response = input == null ? "{}" : readBounded(input, 256 * 1024);
        connection.disconnect();
        JSONObject result;
        try { result = new JSONObject(response); } catch (Exception e) { throw new IllegalStateException("Remote runner returned invalid JSON (HTTP " + code + ")."); }
        if (code < 200 || code >= 300) throw new IllegalStateException("Remote runner rejected the request (HTTP " + code + "): " + result.optString("error", "unknown error"));
        return result;
    }
    private String normalizeEndpoint(String endpoint) throws Exception {
        String value = endpoint == null ? "" : endpoint.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Enter the HTTPS base URL of your guest runner.");
        URL url = new URL(value.endsWith("/") ? value : value + "/");
        if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getHost().isEmpty() || url.getUserInfo() != null || url.getQuery() != null || url.getRef() != null)
            throw new IllegalArgumentException("Enter a valid HTTPS URL without embedded credentials, query or fragment.");
        return url.toString();
    }
    private String readBounded(InputStream in, int limit) throws Exception {
        try (InputStream source = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int n;
            while ((n = source.read(buffer)) >= 0) { if (out.size() + n > limit) throw new IllegalStateException("Remote response exceeded the size limit."); out.write(buffer,0,n); }
            return out.toString("UTF-8");
        }
    }
    private SharedPreferences prefs() { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        java.security.Key existing = store.getKey(KEY_ALIAS, null); if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return generator.generateKey();
    }
    private String encrypt(String value) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,key()); byte[] iv=cipher.getIV(); byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] all=new byte[1+iv.length+encrypted.length]; all[0]=(byte)iv.length; System.arraycopy(iv,0,all,1,iv.length); System.arraycopy(encrypted,0,all,1+iv.length,encrypted.length);
        return Base64.encodeToString(all,Base64.NO_WRAP);
    }
    private String decrypt(String value) throws Exception {
        try {
            byte[] all=Base64.decode(value,Base64.NO_WRAP); int ivLength=all[0]&0xff; if(ivLength<12||ivLength>16||all.length<=1+ivLength) throw new IllegalStateException("Stored guest token is invalid; save it again.");
            byte[] iv=new byte[ivLength]; System.arraycopy(all,1,iv,0,ivLength); byte[] data=new byte[all.length-1-ivLength]; System.arraycopy(all,1+ivLength,data,0,data.length);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv)); return new String(cipher.doFinal(data),StandardCharsets.UTF_8);
        } catch (IllegalStateException e) { throw e; } catch (Exception e) { throw new IllegalStateException("Could not decrypt the stored guest token. Re-enter the token in Settings.",e); }
    }
}
