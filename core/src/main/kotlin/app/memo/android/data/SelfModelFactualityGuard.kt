package app.memo.android.data

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic, deliberately narrow guard for personal drafts.
 * It flags numeric/metric claims that cannot be found in approved source material. It is not a
 * general truth detector; results are review prompts, never proof that a draft is factual.
 */
data class FactualityFinding(
    val code: String,
    val claim: String,
    val explanation: String
)

data class FactualityReview(
    val findings: List<FactualityFinding>,
    val checkedClaims: Int
) {
    val needsReview: Boolean get() = findings.isNotEmpty()
}

object SelfModelFactualityGuard {
    private val numericClaim = Regex(
        """(?i)(?<![\p{L}\p{N}])(?:[$€£]\s*)?(?<number>\d[\d,]*(?:[.]\d+)?)(?:\s*(?<unit>%|percent|per cent|years?|yrs?|months?|weeks?|days?|employees?|clients?|customers?|partners?|users?|projects?|deals?|accounts?|markets?|countries?|languages?|tools?|hours?|minutes?|pln|eur|usd|gbp|million|billion|thousand|leads?|prospects?|meetings?|calls?|emails?|contacts?|records?|pages?|tasks?|x|mm|cm|km))(?!\p{L})"""
    )
    private val year = Regex("(?<!\\d)(?:19|20)\\d{2}(?!\\d)")
    private val numberPattern = Regex("\\d[\\d,.]*")
    private val numericWithoutUnit = Regex("(?i)\\b(?:by|increased|reduced|generated|managed|led|built|closed|converted|grew|growth|revenue|pipeline|budget|achieved|delivered|target|quota|reply|ctr|roi|conversion|outreach|performance)\\b.{0,28}?\\b\\d[\\d,.]*\\b")

    private data class Claim(val key: String, val number: String, val unit: String?, val display: String)

    fun review(draft: String, approvedSourceMaterial: Collection<String>): FactualityReview {
        if (draft.isBlank()) return FactualityReview(emptyList(), 0)
        val sourceText = approvedSourceMaterial.joinToString("\n").take(100_000)
        val sourceNumbers = numberPattern.findAll(sourceText).map { normalizeNumber(it.value) }.toSet()
        val matches = linkedMapOf<String, Claim>()
        numericClaim.findAll(draft).forEach { match ->
            val number = match.groups["number"]?.value ?: return@forEach
            val unit = match.groups["unit"]?.value
            val canonical = normalizeNumber(number)
            val normalizedUnit = normalizeUnit(unit)
            val key = "$canonical|${normalizedUnit.orEmpty()}"
            matches.putIfAbsent(key, Claim(key, canonical, normalizedUnit, match.value.trim()))
        }
        year.findAll(draft).forEach { match ->
            val canonical = normalizeNumber(match.value)
            matches.putIfAbsent("$canonical|year", Claim("$canonical|year", canonical, "year", match.value))
        }
        numericWithoutUnit.findAll(draft).forEach { match ->
            val number = numberPattern.find(match.value)?.value ?: return@forEach
            val canonical = normalizeNumber(number)
            if (matches.values.none { it.number == canonical }) {
                matches.putIfAbsent("$canonical|number", Claim("$canonical|number", canonical, null, match.value.trim()))
            }
        }
        val findings = matches.values
            .filter { claim ->
                when {
                    claim.unit == null -> claim.number !in sourceNumbers
                    claim.unit == "year" -> claim.number !in sourceNumbers
                    else -> !sourceHasMeasure(sourceText, claim.number, claim.unit)
                }
            }
            .take(30)
            .map { claim ->
                FactualityFinding(
                    code = "UNSUPPORTED_NUMERIC_CLAIM",
                    claim = claim.display,
                    explanation = "This number or metric could not be matched to the same measure in approved Self Model source text. Confirm it or remove it; this check is heuristic."
                )
            }
        return FactualityReview(findings = findings, checkedClaims = matches.size)
    }

    private fun sourceHasMeasure(source: String, number: String, unit: String): Boolean {
        val canonicalSource = normalize(source)
        numberPattern.findAll(source).forEach { match ->
            if (normalizeNumber(match.value) != number) return@forEach
            val start = (match.range.first - 45).coerceAtLeast(0)
            val end = (match.range.last + 75).coerceAtMost(source.length - 1)
            val window = normalize(source.substring(start, end + 1))
            if (window.contains(unit)) return true
        }
        // `year` is deliberately exact. Other units were handled in the surrounding source window.
        return unit == "year" && canonicalSource.contains(number)
    }

    private fun normalizeNumber(value: String): String {
        val clean = value.trim().replace(" ", "")
        val canonical = if (clean.contains(',') && !clean.contains('.') && clean.substringAfterLast(',').length == 3) {
            clean.replace(",", "")
        } else {
            clean.replace(',', '.')
        }
        return canonical.trim('.', ',')
    }

    private fun normalizeUnit(value: String?): String? {
        val normalized = value?.let(::normalize)?.replace(Regex("\\s+"), "") ?: return null
        return when {
            normalized in setOf("percent", "per cent", "%") -> "%"
            normalized.startsWith("year") || normalized == "yrs" || normalized == "yr" -> "year"
            normalized.startsWith("partner") -> "partner"
            normalized.startsWith("client") -> "client"
            normalized.startsWith("customer") -> "customer"
            normalized.startsWith("employee") -> "employee"
            normalized.startsWith("project") -> "project"
            normalized.startsWith("user") -> "user"
            normalized.startsWith("deal") -> "deal"
            normalized.startsWith("account") -> "account"
            normalized.startsWith("market") -> "market"
            normalized.startsWith("country") -> "countr"
            normalized.startsWith("language") -> "language"
            normalized.startsWith("tool") -> "tool"
            normalized.startsWith("hour") -> "hour"
            normalized.startsWith("minute") -> "minute"
            normalized.startsWith("week") -> "week"
            normalized.startsWith("month") -> "month"
            normalized.startsWith("day") -> "day"
            normalized.startsWith("lead") -> "lead"
            normalized.startsWith("prospect") -> "prospect"
            normalized.startsWith("meeting") -> "meeting"
            normalized.startsWith("call") -> "call"
            normalized.startsWith("email") -> "email"
            normalized.startsWith("contact") -> "contact"
            normalized.startsWith("record") -> "record"
            normalized.startsWith("page") -> "page"
            normalized.startsWith("task") -> "task"
            else -> normalized
        }
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
}
