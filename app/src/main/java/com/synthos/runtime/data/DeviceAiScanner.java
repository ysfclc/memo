package com.synthos.runtime.data;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One-tap device intelligence inventory. It matches visible installed apps and model files surfaced
 * through Android's shared MediaStore index. It deliberately does not request all-files access and
 * cannot inspect other apps' private directories; Android does not permit that on ordinary apps.
 */
public final class DeviceAiScanner {
    private DeviceAiScanner() {}

    public static final class AppMatch {
        public final String label;
        public final String packageName;
        public final String kind;
        public final String evidence;
        public final String suggestedMatch;

        AppMatch(String label, String packageName, String kind, String evidence, String suggestedMatch) {
            this.label = label;
            this.packageName = packageName;
            this.kind = kind;
            this.evidence = evidence;
            this.suggestedMatch = suggestedMatch;
        }
    }

    public static final class ModelCandidate {
        public final Uri uri;
        public final String name;
        public final String relativePath;
        public final String format;
        public final long sizeBytes;
        public final boolean readable;
        public final boolean memoRuntimeCompatible;
        public final String match;

        ModelCandidate(Uri uri, String name, String relativePath, String format, long sizeBytes,
                       boolean readable, boolean memoRuntimeCompatible, String match) {
            this.uri = uri;
            this.name = name;
            this.relativePath = relativePath;
            this.format = format;
            this.sizeBytes = sizeBytes;
            this.readable = readable;
            this.memoRuntimeCompatible = memoRuntimeCompatible;
            this.match = match;
        }
    }

    public static final class Result {
        public final List<AppMatch> aiApps;
        public final List<ModelCandidate> sharedModels;
        public final int appCountScanned;
        public final int mediaStoreRowsScanned;
        public final List<String> notes;

        Result(List<AppMatch> aiApps, List<ModelCandidate> sharedModels, int appCountScanned,
               int mediaStoreRowsScanned, List<String> notes) {
            this.aiApps = Collections.unmodifiableList(new ArrayList<>(aiApps));
            this.sharedModels = Collections.unmodifiableList(new ArrayList<>(sharedModels));
            this.appCountScanned = appCountScanned;
            this.mediaStoreRowsScanned = mediaStoreRowsScanned;
            this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        }
    }

    public static Result scan(Context context, File privateModelDirectory) {
        ArrayList<String> notes = new ArrayList<>();
        List<AppMatch> apps = scanLaunchableApps(context);
        int[] rowsScanned = new int[]{0};
        List<ModelCandidate> models = scanSharedModels(context, notes, rowsScanned);
        boolean fullFileAccess = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager();
        if (fullFileAccess) {
            List<ModelCandidate> fileMatches = scanSharedStorageTrees(context, notes);
            LinkedHashMap<String, ModelCandidate> merged = new LinkedHashMap<>();
            for (ModelCandidate item : models) merged.put(candidateKey(item), item);
            for (ModelCandidate item : fileMatches) merged.put(candidateKey(item), item);
            models = new ArrayList<>(merged.values());
            notes.add("Device-wide shared-storage scan enabled: filenames and sizes were inspected locally. Memo did not upload files or read their contents; a compatible model is read only after you choose Import.");
        } else if (Build.VERSION.SDK_INT >= 30) {
            notes.add("For a broader scan of shared storage (including non-media files in Downloads and other shared folders), tap ‘Enable full shared-storage scan’ and explicitly grant Android’s All files access. This is optional and sensitive. App-private directories remain inaccessible.");
        } else {
            notes.add("This Android version does not expose modern All files access. The scan is limited to files the MediaStore index and Memo itself can read.");
        }
        File[] privateModels = privateModelDirectory == null ? null : privateModelDirectory.listFiles();
        if (privateModels != null) {
            for (File file : privateModels) {
                if (file == null || !file.isFile() || !file.canRead() || !isModelFile(file.getName())) continue;
                String format = formatOf(file.getName());
                models.add(new ModelCandidate(Uri.fromFile(file), file.getName(), "Memo private library", format,
                        file.length(), true, ".litertlm".equals(format), matchForFormat(format, apps)));
            }
        }
        // Enrich file candidates with installed runtime matches after both inventories are available.
        for (int i = 0; i < models.size(); i++) {
            ModelCandidate item = models.get(i);
            String match = matchForFormat(item.format, apps);
            if (!match.equals(item.match)) models.set(i, new ModelCandidate(item.uri, item.name, item.relativePath,
                    item.format, item.sizeBytes, item.readable, item.memoRuntimeCompatible, match));
        }
        // Prefer files usable by Memo and then known names for a readable result ordering.
        models.sort((a, b) -> {
            if (a.memoRuntimeCompatible != b.memoRuntimeCompatible) return a.memoRuntimeCompatible ? -1 : 1;
            int n = a.name.compareToIgnoreCase(b.name);
            return n;
        });
        if (apps.isEmpty()) notes.add("No known AI app matched the installed launchable apps. You can still use a compatible local model in Memo.");
        if (models.isEmpty()) notes.add("No supported model files were found in Memo storage or the files Android exposed through its shared-media index.");
        notes.add("Android isolates each app's private storage. Even with optional all-files access, Memo can inspect only shared storage—not other apps' private model directories.");
        notes.add("Memo's built-in local LLM runtime currently accepts compatible .litertlm files only. A matching extension is not proof that inference will succeed; use the runtime test after import.");
        return new Result(apps, models, countLaunchableApps(context), rowsScanned[0], notes);
    }

    private static int countLaunchableApps(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            Set<String> packages = new HashSet<>();
            String ownPackage = context.getPackageName();
            for (ResolveInfo info : queryActivities(context.getPackageManager(), intent)) {
                if (info != null && info.activityInfo != null && info.activityInfo.packageName != null
                        && !ownPackage.equals(info.activityInfo.packageName)) packages.add(info.activityInfo.packageName);
            }
            return packages.size();
        } catch (Exception ignored) { return 0; }
    }

    private static List<ResolveInfo> queryActivities(PackageManager pm, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33) {
            return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0));
        }
        //noinspection deprecation
        return pm.queryIntentActivities(intent, 0);
    }

    private static List<AppMatch> scanLaunchableApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> entries;
        try { entries = queryActivities(pm, intent); }
        catch (Exception e) { return new ArrayList<>(); }
        LinkedHashMap<String, AppMatch> result = new LinkedHashMap<>();
        String ownPackage = context.getPackageName();
        for (ResolveInfo info : entries) {
            if (info == null || info.activityInfo == null || info.activityInfo.packageName == null) continue;
            String pkg = info.activityInfo.packageName;
            if (ownPackage.equals(pkg)) continue;
            String label;
            try { label = String.valueOf(info.loadLabel(pm)); }
            catch (Exception e) { label = pkg; }
            AppMatch match = classify(label, pkg);
            if (match != null) result.put(pkg, match);
        }
        ArrayList<AppMatch> sorted = new ArrayList<>(result.values());
        sorted.sort(Comparator.comparing((AppMatch a) -> !a.kind.toLowerCase(Locale.ROOT).contains("on-device"))
                .thenComparing(a -> a.label.toLowerCase(Locale.ROOT)));
        return sorted;
    }

    private static AppMatch classify(String label, String packageName) {
        String searchable = (label + " " + packageName).toLowerCase(Locale.ROOT);
        String knownLocal = firstMatch(searchable, new String[]{"pocketpal", "mlcchat", "layla", "chatterui", "local llm", "localllm", "llama.cpp", "llamacpp", "offline ai", "private llm", "kobold", "llm hub", "ollama"});
        if (knownLocal != null) {
            String match = (searchable.contains("mlcchat") || searchable.contains("pocketpal") || searchable.contains("kobold") || searchable.contains("llama"))
                    ? "Likely local model runner; GGUF is a common match, but confirm this app's import rules."
                    : "Likely local inference tool; check the app's model format and import support.";
            return new AppMatch(label, packageName, "Likely on-device AI runtime", "Matched app label/package token: " + knownLocal, match);
        }
        String knownAssistant = firstMatch(searchable, new String[]{"chatgpt", "openai", "gemini", "claude", "anthropic", "copilot", "perplexity", "grok", "poe ai", "meta ai", "character.ai", "pi ai"});
        if (knownAssistant != null) {
            return new AppMatch(label, packageName, "AI assistant app", "Matched app label/package token: " + knownAssistant,
                    "Use Memo's internal browser for web workflows where the site supports it; this app itself may use a remote service.");
        }
        String localHint = firstMatch(searchable, new String[]{"neural", "language model", "ai model", "local model", "llm", "inference", "generative ai"});
        if (localHint != null) {
            return new AppMatch(label, packageName, "Possible AI / model tool", "Name contains AI/model-related text: " + localHint,
                    "Candidate only. Open the app and verify whether it runs locally and what model formats it accepts.");
        }
        return null;
    }

    private static String firstMatch(String value, String[] terms) {
        for (String term : terms) if (value.contains(term)) return term;
        return null;
    }

    private static String candidateKey(ModelCandidate item) {
        String path = item.relativePath == null ? "" : item.relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path + "/" + item.name.toLowerCase(Locale.ROOT);
    }

    /** Bounded, metadata-only walk of user-visible shared storage. Android app-private folders are excluded. */
    private static List<ModelCandidate> scanSharedStorageTrees(Context context, List<String> notes) {
        final int maxEntries = 100000;
        final int maxDepth = 18;
        LinkedHashMap<String, ModelCandidate> found = new LinkedHashMap<>();
        LinkedHashMap<String, File> roots = new LinkedHashMap<>();
        try {
            File primary = Environment.getExternalStorageDirectory();
            if (primary != null && primary.isDirectory() && primary.canRead()) roots.put(primary.getCanonicalPath(), primary);
            File[] appDirs = context.getExternalFilesDirs(null);
            if (appDirs != null) for (File appDir : appDirs) {
                if (appDir == null) continue;
                String path = appDir.getAbsolutePath();
                int marker = path.indexOf("/Android/data/");
                if (marker <= 0) continue;
                File root = new File(path.substring(0, marker));
                if (root.isDirectory() && root.canRead()) roots.put(root.getCanonicalPath(), root);
            }
        } catch (Exception e) { notes.add("Could not enumerate all shared-storage roots: " + safeMessage(e)); }
        int visitedCount = 0;
        boolean limitReached = false;
        for (File root : roots.values()) {
            ArrayDeque<FileDepth> queue = new ArrayDeque<>();
            Set<String> visitedDirs = new HashSet<>();
            queue.add(new FileDepth(root, 0));
            while (!queue.isEmpty()) {
                FileDepth current = queue.removeFirst();
                if (current.depth > maxDepth || current.file == null) continue;
                String canonical;
                try { canonical = current.file.getCanonicalPath(); }
                catch (Exception ignored) { canonical = current.file.getAbsolutePath(); }
                if (!visitedDirs.add(canonical)) continue;
                File[] children = current.file.listFiles();
                if (children == null) continue;
                for (File child : children) {
                    if (++visitedCount > maxEntries) { limitReached = true; break; }
                    if (child.isDirectory()) {
                        if (!shouldSkipDirectory(child)) queue.addLast(new FileDepth(child, current.depth + 1));
                    } else if (child.isFile() && isModelFile(child.getName())) {
                        try {
                            String filePath = child.getCanonicalPath();
                            String relative;
                            String rootPath = root.getCanonicalPath();
                            relative = filePath.startsWith(rootPath) ? filePath.substring(rootPath.length()).replace('\\', '/') : filePath;
                            if (relative.startsWith("/")) relative = relative.substring(1);
                            int slash = relative.lastIndexOf('/');
                            String folder = slash < 0 ? "" : relative.substring(0, slash);
                            ModelCandidate candidate = new ModelCandidate(Uri.fromFile(child), child.getName(), folder,
                                    formatOf(child.getName()), child.length(), child.canRead(), ".litertlm".equals(formatOf(child.getName())),
                                    matchForFormat(formatOf(child.getName()), Collections.<AppMatch>emptyList()));
                            found.put(folder.toLowerCase(Locale.ROOT) + "/" + child.getName().toLowerCase(Locale.ROOT), candidate);
                        } catch (Exception ignored) { }
                    }
                }
                if (limitReached) break;
            }
            if (limitReached) break;
        }
        if (limitReached) notes.add("The shared-storage walk reached its safety limit of 100,000 entries. Run it again after reducing very large folders, or import a model directly by file selection.");
        return new ArrayList<>(found.values());
    }

    private static boolean shouldSkipDirectory(File directory) {
        String name = directory.getName().toLowerCase(Locale.ROOT);
        if (name.startsWith(".") || "cache".equals(name) || "lost+found".equals(name)) return true;
        String path = directory.getAbsolutePath().replace('\\', '/').toLowerCase(Locale.ROOT);
        return path.contains("/android/data/") || path.endsWith("/android/data") || path.contains("/android/obb/") || path.endsWith("/android/obb");
    }

    private static final class FileDepth {
        final File file; final int depth;
        FileDepth(File file, int depth) { this.file = file; this.depth = depth; }
    }

    private static List<ModelCandidate> scanSharedModels(Context context, List<String> notes, int[] rowsScanned) {
        LinkedHashMap<String, ModelCandidate> found = new LinkedHashMap<>();
        String[] projection = new String[]{
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.RELATIVE_PATH
        };
        Set<String> volumes = new HashSet<>();
        try { volumes.addAll(MediaStore.getExternalVolumeNames(context)); }
        catch (Exception e) { volumes.add(MediaStore.VOLUME_EXTERNAL); }
        int scanned = 0;
        for (String volume : volumes) {
            Uri collection;
            try { collection = MediaStore.Files.getContentUri(volume); }
            catch (Exception ignored) { continue; }
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(collection, projection, null, null,
                        MediaStore.MediaColumns.DISPLAY_NAME + " COLLATE NOCASE ASC");
                if (cursor == null) continue;
                int idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
                int nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                int sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                int pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH);
                while (cursor.moveToNext() && scanned < 30000) {
                    scanned++;
                    rowsScanned[0]++;
                    if (nameCol < 0) continue;
                    String name = cursor.getString(nameCol);
                    if (!isModelFile(name)) continue;
                    long id = idCol >= 0 ? cursor.getLong(idCol) : -1;
                    if (id < 0) continue;
                    Uri fileUri = ContentUris.withAppendedId(collection, id);
                    String path = pathCol >= 0 && !cursor.isNull(pathCol) ? cursor.getString(pathCol) : "Shared storage";
                    long size = sizeCol >= 0 && !cursor.isNull(sizeCol) ? cursor.getLong(sizeCol) : -1;
                    boolean readable = canRead(context, fileUri);
                    String format = formatOf(name);
                    ModelCandidate candidate = new ModelCandidate(fileUri, name, path, format, size, readable,
                            ".litertlm".equals(format), matchForFormat(format, Collections.<AppMatch>emptyList()));
                    found.put(fileUri.toString(), candidate);
                }
            } catch (SecurityException e) {
                notes.add("Android restricted shared-file metadata access for storage volume " + volume + ". App-private and model files explicitly shared to Memo remain discoverable.");
            } catch (Exception e) {
                notes.add("Could not inspect storage volume " + volume + ": " + safeMessage(e));
            } finally { if (cursor != null) cursor.close(); }
            if (scanned >= 30000) break;
        }
        if (scanned >= 30000) notes.add("The shared-media scan reached its safety cap of 30,000 indexed rows.");
        return new ArrayList<>(found.values());
    }

    private static boolean canRead(Context context, Uri uri) {
        try (android.os.ParcelFileDescriptor ignored = context.getContentResolver().openFileDescriptor(uri, "r")) {
            return ignored != null;
        } catch (Exception e) { return false; }
    }

    public static boolean isModelFile(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".litertlm") || n.endsWith(".gguf") || n.endsWith(".tflite")
                || n.endsWith(".task") || n.endsWith(".onnx") || n.endsWith(".safetensors")
                || n.endsWith(".ggml") || n.endsWith(".pte");
    }

    public static String formatOf(String name) {
        if (name == null) return "unknown";
        String n = name.toLowerCase(Locale.ROOT);
        String[] extensions = {".litertlm", ".gguf", ".tflite", ".task", ".onnx", ".safetensors", ".ggml", ".pte"};
        for (String ext : extensions) if (n.endsWith(ext)) return ext;
        return "unknown";
    }

    private static String matchForFormat(String format, List<AppMatch> apps) {
        if (".litertlm".equals(format)) return "Memo built-in runtime: compatible format; import and run the on-device inference test.";
        if (".gguf".equals(format)) {
            for (AppMatch app : apps) if (app.kind.contains("on-device")) return "GGUF model. Compare with installed local runner " + app.label + "; Memo's built-in runtime cannot load it yet.";
            return "GGUF model. Requires a GGUF-compatible runtime; Memo's built-in runtime cannot load it yet.";
        }
        if (".tflite".equals(format) || ".task".equals(format)) return "TensorFlow Lite / LiteRT artifact. Not a compatible LiteRT-LM conversation model in this build.";
        if (".onnx".equals(format)) return "ONNX artifact. Requires an ONNX runtime integration; not loadable by Memo's current LLM runtime.";
        if (".safetensors".equals(format)) return "Model weights only. Architecture, tokenizer and runtime are also required.";
        if (".ggml".equals(format) || ".pte".equals(format)) return "Model/runtime-specific artifact. Memo cannot load this format in the current build.";
        return "Unknown model format.";
    }

    private static String safeMessage(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m.trim();
    }
}
