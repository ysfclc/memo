package app.memo.android.data

import org.json.JSONObject
import java.util.Locale

/** A bounded, source-checked proposal returned by the selected model. */
data class OutcomeProposal(
    val type: String,
    val title: String,
    val description: String,
    val properties: Map<String, String>,
    val evidence: String
)

object OutcomeParser {
    private val allowedTypes = setOf("PROJECT", "TASK", "DECISION", "KNOWLEDGE", "PERSON", "ORGANIZATION", "ARTIFACT", "SELF_MODEL_FACT")

    /**
     * Parses a model response as data, never as instructions. Only proposals with an exact source excerpt
     * (whitespace-normalized and case-insensitive) are returned. A bad row cannot invalidate valid rows.
     */
    fun parse(raw: String, sourceText: String, maxItems: Int = 20): List<OutcomeProposal> {
        require(maxItems in 1..50) { "Proposal limit must be between 1 and 50." }
        require(raw.length <= 32_000) { "Model output is too large to parse safely." }
        val sourceNormalized = normalize(sourceText)
        require(sourceNormalized.isNotBlank()) { "The source conversation is empty." }
        val json = extractJson(raw)
        val root = JSONObject(json)
        val rows = root.optJSONArray("entities") ?: throw IllegalArgumentException("The model response did not contain an entities array.")
        val results = mutableListOf<OutcomeProposal>()
        val seen = mutableSetOf<String>()
        for (index in 0 until minOf(rows.length(), maxItems * 3)) {
            val row = rows.optJSONObject(index) ?: continue
            val type = row.optString("type", "").uppercase(Locale.ROOT).trim()
            val title = row.optString("title", "").trim().take(240)
            val description = row.optString("description", "").trim().take(12_000)
            val evidence = row.optString("evidence", "").trim().take(2_000)
            if (type !in allowedTypes || title.length < 2 || evidence.length < 8) continue
            if (!sourceNormalized.contains(normalize(evidence))) continue
            val duplicateKey = "$type|${title.lowercase(Locale.ROOT)}"
            if (!seen.add(duplicateKey)) continue
            val properties = linkedMapOf<String, String>()
            val rawProperties = row.optJSONObject("properties")
            if (rawProperties != null) {
                val keys = rawProperties.keys()
                var count = 0
                while (keys.hasNext() && count < 20) {
                    val key = keys.next().trim().take(80)
                    val value = rawProperties.optString(key, "").trim().take(1_000)
                    if (key.isNotBlank() && value.isNotBlank()) properties[key] = value
                    count++
                }
            }
            results += OutcomeProposal(type, title, description, properties, evidence)
            if (results.size >= maxItems) break
        }
        return results
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val withoutFence = trimmed.replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```$"), "").trim()
        if (withoutFence.startsWith("{") && withoutFence.endsWith("}")) return withoutFence
        val start = withoutFence.indexOf('{')
        if (start < 0) throw IllegalArgumentException("No JSON object was found in the model response.")
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until withoutFence.length) {
            val char = withoutFence[index]
            if (inString) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') inString = false
            } else {
                when (char) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return withoutFence.substring(start, index + 1)
                    }
                }
            }
        }
        throw IllegalArgumentException("The model response contained incomplete JSON.")
    }

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
