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
    val expectedSha256: String,
    val license: String,
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
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/e668717/Qwen3-0.6B.litertlm?download=true",
            expectedBytes = 614_000_000L,
            minimumBytes = 580_000_000L,
            expectedSha256 = "c5f39b4191926786c3e6668b62af4d6eb15248dfc7a5348252a9b406f33f2108",
            license = "Apache-2.0",
            ramNote = "614 MB download. Published phone benchmarks used about 2.7–4.3 GB peak memory depending on device/backend; this may not fit lower-RAM phones."
        ),
        ModelCatalogEntry(
            id = "qwen3-1.7b-int4",
            name = "Qwen3 1.7B · INT4",
            description = "A larger local reasoning option in a compact dynamic-INT4 package. 4K context; experimental on phones until benchmarked on your device.",
            fileName = "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/d9b8a91/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm?download=true",
            expectedBytes = 977_000_000L,
            minimumBytes = 950_000_000L,
            expectedSha256 = "2eeffef7b51bc3e1225ea69fe7aa5f417397934b56a5b6c20cc068d6fd2c918b",
            license = "Apache-2.0",
            ramNote = "977 MB download. Larger runtime memory demand than Qwen3 0.6B; no Redmi Note 12 Pro 5G benchmark is claimed. Close heavy apps and keep several GB free before testing."
        ),
        ModelCatalogEntry(
            id = "gemma4-e2b",
            name = "Gemma 4 E2B · Mixed quantized",
            description = "Higher-capability local option. Large download; install only if you have ample free storage and several GB of available memory.",
            fileName = "gemma-4-E2B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1/gemma-4-E2B-it.litertlm?download=true",
            expectedBytes = 2_590_000_000L,
            minimumBytes = 2_470_000_000L,
            expectedSha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            license = "Apache-2.0",
            ramNote = "About 2.59 GB on disk. Published Galaxy S26 Ultra benchmarks measured about 1.7 GB CPU memory or 676 MB GPU memory; your phone may need more or fail to load."
        )
    )
}
