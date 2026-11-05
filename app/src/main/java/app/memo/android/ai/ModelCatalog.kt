package app.memo.android.ai

import java.io.File

/** Model weights are downloaded on demand and are never bundled inside the APK. */
data class ModelCatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val fileName: String,
    val downloadUrl: String,
    val expectedBytes: Long,
    val minimumBytes: Long,
    val ramNote: String
)

data class InstalledModel(
    val id: String,
    val name: String,
    val file: File,
    val bytes: Long,
    val isCatalogModel: Boolean
)

data class DownloadStatus(
    val modelId: String,
    val bytesRead: Long = 0,
    val totalBytes: Long = 0,
    val stage: String = "Preparing download",
    val error: String? = null
) {
    val progress: Float? get() = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else null
}

object ModelCatalog {
    val entries = listOf(
        ModelCatalogEntry(
            id = "qwen3-0.6b",
            name = "Qwen3 0.6B · INT8",
            description = "Recommended first install. Official LiteRT-LM CPU/GPU artifact for lightweight offline extraction, summaries and first-pass reasoning.",
            fileName = "Qwen3-0.6B.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm?download=true",
            expectedBytes = 586_000_000L,
            minimumBytes = 550_000_000L,
            ramNote = "586 MB download. Published phone benchmarks used about 2.7–4.3 GB peak memory depending on device/backend; this may not fit lower-RAM phones."
        ),
        ModelCatalogEntry(
            id = "gemma4-e2b",
            name = "Gemma 4 E2B · Mixed quantized",
            description = "Higher-capability local option. Large download; install only if you have ample free storage and several GB of available memory.",
            fileName = "gemma-4-E2B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
            expectedBytes = 2_600_000_000L,
            minimumBytes = 2_470_000_000L,
            ramNote = "About 2.6 GB on disk. Official benchmark on one flagship Android phone measured about 1.7 GB CPU memory or 676 MB GPU memory; other phones may require more or fail to load."
        )
    )
}
