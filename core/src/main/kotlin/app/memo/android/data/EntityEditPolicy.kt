package app.memo.android.data

/** Deterministic validation for user-edited structured entity fields. */
object EntityEditPolicy {
    fun propertyValidationError(properties: Map<String, String>): String? {
        if (properties.size > 40) return "An entity can have at most 40 properties."
        val seen = mutableSetOf<String>()
        properties.forEach { (rawKey, value) ->
            val key = rawKey.trim()
            if (key.isBlank()) return "Property keys cannot be empty."
            if (key.length > 80) return "Property keys must be 80 characters or fewer."
            if (value.length > 2_000) return "Property values must be 2,000 characters or fewer."
            if (!seen.add(key.lowercase())) return "Property keys must be unique, ignoring letter case."
        }
        return null
    }
}
