package app.memo.android.data

import java.net.URI
import java.util.Locale

enum class SalesAccountSegment { CNC_METALWORKING, INJECTION_MOULDING, INDUSTRIAL_DISTRIBUTION, OTHER }
enum class SalesAccessRoute { UNKNOWN, WARM_INTRODUCTION, EXPLICIT_INBOUND, EVENT_OR_FACE_TO_FACE, REVIEWED_PUBLISHED_ROUTE, NONE }
enum class SalesAccountResearchState { RESEARCH_NEEDED, READY_FOR_REVIEW, DISQUALIFIED }

enum class IcpFactor(val propertyKey: String, val weight: Int) {
    SEGMENT_FIT("segment_fit", 20),
    QUOTATION_FLOW("quotation_flow", 20),
    PROCESS_IMPROVEMENT_SIGNAL("process_improvement_signal", 20),
    ABILITY_AND_WILLINGNESS_TO_PAY("ability_and_willingness_to_pay", 15),
    ACCESS_ROUTE("access_route", 15),
    LANGUAGE_AND_GEOGRAPHY("language_and_geography", 10)
}

data class IcpFactorAssessment(
    val factor: IcpFactor,
    val points: Int? = null,
    val rationale: String = "",
    val evidenceIds: List<String> = emptyList()
)

data class SalesAccountProfile(
    val website: String,
    val location: String,
    val segment: SalesAccountSegment,
    val employeeRange: String = "",
    val accessRoute: SalesAccessRoute = SalesAccessRoute.UNKNOWN,
    val researchState: SalesAccountResearchState = SalesAccountResearchState.RESEARCH_NEEDED,
    val disqualificationReason: String = "",
    val assessments: List<IcpFactorAssessment> = IcpFactor.entries.map { IcpFactorAssessment(it) }
)

data class IcpScoreSummary(
    val pointsOutOf100: Int,
    val assessedWeight: Int,
    val unassessedWeight: Int,
    val percentOfAssessedFactors: Int?,
    val bestPossiblePointsOutOf100: Int,
    val isDisqualified: Boolean
) {
    val evidenceCoveragePercent: Int get() = assessedWeight
}

data class SalesAccountDuplicate(val entityId: String, val title: String, val reason: String)

/** Sales account research reuses ORGANIZATION entities and existing evidence; it adds no CRM table. */
object SalesAccountResearch {
    const val PROFILE_VERSION = 1
    const val PROFILE_VERSION_KEY = "sales.account.schema_version"
    const val WEBSITE_KEY = "sales.account.website"
    const val LOCATION_KEY = "sales.account.location"
    const val SEGMENT_KEY = "sales.account.segment"
    const val EMPLOYEE_RANGE_KEY = "sales.account.employee_range"
    const val ACCESS_ROUTE_KEY = "sales.account.access_route"
    const val STATE_KEY = "sales.account.research_state"
    const val DISQUALIFICATION_KEY = "sales.account.disqualification_reason"
    private const val MAX_RATIONALE_CHARS = 500
    private const val MAX_EVIDENCE_PER_FACTOR = 8

    fun validate(profile: SalesAccountProfile, evidence: List<EvidenceRecord>): List<String> {
        val errors = mutableListOf<String>()
        if (profile.website.length > 512 || normalizeDomain(profile.website) == null) {
            errors += "Add a valid absolute HTTP or HTTPS company website (maximum 512 characters)."
        }
        if (profile.location.isBlank() || profile.location.length > 200) {
            errors += "Add a location or explicitly enter Unknown (maximum 200 characters)."
        }
        if (profile.employeeRange.length > 80) errors += "Employee range must be 80 characters or fewer."
        if (profile.disqualificationReason.length > MAX_RATIONALE_CHARS) errors += "Disqualification reason must be 500 characters or fewer."
        if (profile.researchState == SalesAccountResearchState.DISQUALIFIED && profile.disqualificationReason.isBlank()) {
            errors += "A disqualified account needs a reason."
        }
        if (profile.researchState != SalesAccountResearchState.DISQUALIFIED && profile.disqualificationReason.isNotBlank()) {
            errors += "A disqualification reason is only valid for a disqualified account."
        }

        val duplicates = profile.assessments.groupBy { it.factor }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) errors += "Each ICP factor can be assessed only once: ${duplicates.joinToString { it.name }}."
        val byFactor = profile.assessments.associateBy { it.factor }
        val evidenceById = evidence.associateBy { it.id }

        IcpFactor.entries.forEach { factor ->
            val assessment = byFactor[factor]
            if (assessment == null) {
                errors += "Missing ICP factor: ${factor.name}."
                return@forEach
            }
            val points = assessment.points
            if (points == null) {
                if (assessment.evidenceIds.isNotEmpty()) errors += "Unknown factor ${factor.name} cannot claim supporting evidence before it is scored."
                return@forEach
            }
            if (points !in 0..factor.weight) errors += "${factor.name} must be scored between 0 and ${factor.weight}."
            if (assessment.rationale.isBlank() || assessment.rationale.length > MAX_RATIONALE_CHARS) {
                errors += "${factor.name} needs a concise rationale of 1–$MAX_RATIONALE_CHARS characters."
            }
            if (assessment.evidenceIds.isEmpty()) errors += "${factor.name} needs at least one linked source, or it must remain Unknown."
            if (assessment.evidenceIds.size > MAX_EVIDENCE_PER_FACTOR) errors += "${factor.name} can cite at most $MAX_EVIDENCE_PER_FACTOR sources."
            if (assessment.evidenceIds.distinct().size != assessment.evidenceIds.size) errors += "${factor.name} contains duplicate evidence references."
            assessment.evidenceIds.distinct().forEach { evidenceId ->
                if (evidenceId.length > 128 || evidenceId.contains('\n') || evidenceId.contains('\r')) {
                    errors += "${factor.name} contains an invalid evidence reference."
                }
                val source = evidenceById[evidenceId]
                if (source == null) {
                    errors += "${factor.name} references missing evidence $evidenceId."
                } else {
                    if (source.title.isBlank() || source.capturedAt <= 0L || source.sourceUrl.length > 2_048 || !isValidSourceUrl(source.sourceUrl)) {
                        errors += "Evidence ${source.title.ifBlank { evidenceId }} needs a title, retrieval time and valid HTTP(S) source URL."
                    }
                    if (source.content.isBlank() && source.summary.isBlank()) errors += "Evidence ${source.title.ifBlank { evidenceId }} has no captured content or summary."
                }
            }
        }
        if (profile.researchState == SalesAccountResearchState.READY_FOR_REVIEW) {
            val unknown = IcpFactor.entries.filter { byFactor[it]?.points == null }
            if (unknown.isNotEmpty()) errors += "Ready for review requires all ICP factors to be assessed; still Unknown: ${unknown.joinToString { it.name }}."
        }
        return errors.distinct()
    }

    fun score(profile: SalesAccountProfile): IcpScoreSummary {
        val byFactor = profile.assessments.associateBy { it.factor }
        require(profile.assessments.groupBy { it.factor }.values.none { it.size > 1 }) { "Each ICP factor can be assessed only once." }
        IcpFactor.entries.forEach { factor ->
            val points = byFactor[factor]?.points
            require(points == null || points in 0..factor.weight) { "${factor.name} must be scored between 0 and ${factor.weight}." }
        }
        val assessed = IcpFactor.entries.filter { byFactor[it]?.points != null }
        val assessedWeight = assessed.sumOf { it.weight }
        val earned = assessed.sumOf { byFactor.getValue(it).points!! }
        val unknownWeight = 100 - assessedWeight
        val normalised = if (assessedWeight == 0) null else (earned * 100 + assessedWeight / 2) / assessedWeight
        return IcpScoreSummary(earned, assessedWeight, unknownWeight, normalised, earned + unknownWeight,
            profile.researchState == SalesAccountResearchState.DISQUALIFIED)
    }

    fun toProperties(profile: SalesAccountProfile): Map<String, String> {
        val properties = linkedMapOf(
            PROFILE_VERSION_KEY to PROFILE_VERSION.toString(),
            WEBSITE_KEY to profile.website.trim(),
            LOCATION_KEY to profile.location.trim(),
            SEGMENT_KEY to profile.segment.name,
            EMPLOYEE_RANGE_KEY to profile.employeeRange.trim(),
            ACCESS_ROUTE_KEY to profile.accessRoute.name,
            STATE_KEY to profile.researchState.name,
            DISQUALIFICATION_KEY to profile.disqualificationReason.trim()
        )
        IcpFactor.entries.forEach { factor ->
            val assessment = profile.assessments.firstOrNull { it.factor == factor } ?: IcpFactorAssessment(factor)
            val prefix = "sales.account.factor.${factor.propertyKey}"
            properties["$prefix.points"] = assessment.points?.toString().orEmpty()
            properties["$prefix.rationale"] = assessment.rationale.trim()
            properties["$prefix.evidence_ids"] = assessment.evidenceIds.take(MAX_EVIDENCE_PER_FACTOR).joinToString("\n")
        }
        return properties
    }

    fun fromProperties(properties: Map<String, String>): SalesAccountProfile? = runCatching {
        if (properties[PROFILE_VERSION_KEY]?.toIntOrNull() != PROFILE_VERSION) return null
        val assessments = IcpFactor.entries.map { factor ->
            val prefix = "sales.account.factor.${factor.propertyKey}"
            val ids = properties["$prefix.evidence_ids"].orEmpty().lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
            IcpFactorAssessment(factor, properties["$prefix.points"]?.toIntOrNull(), properties["$prefix.rationale"].orEmpty(), ids)
        }
        SalesAccountProfile(
            website = properties[WEBSITE_KEY].orEmpty(),
            location = properties[LOCATION_KEY].orEmpty(),
            segment = SalesAccountSegment.valueOf(properties[SEGMENT_KEY] ?: SalesAccountSegment.OTHER.name),
            employeeRange = properties[EMPLOYEE_RANGE_KEY].orEmpty(),
            accessRoute = SalesAccessRoute.valueOf(properties[ACCESS_ROUTE_KEY] ?: SalesAccessRoute.UNKNOWN.name),
            researchState = SalesAccountResearchState.valueOf(properties[STATE_KEY] ?: SalesAccountResearchState.RESEARCH_NEEDED.name),
            disqualificationReason = properties[DISQUALIFICATION_KEY].orEmpty(),
            assessments = assessments
        )
    }.getOrNull()

    /** Exact domain/name matches are warnings only; never merge, mutate or delete records. */
    fun findPotentialDuplicates(title: String, profile: SalesAccountProfile, existingEntities: List<EntityRecord>): List<SalesAccountDuplicate> {
        val domain = normalizeDomain(profile.website)
        val name = normalizeCompanyName(title)
        if (domain == null && name.isBlank()) return emptyList()
        return existingEntities.asSequence()
            .filter { it.type.equals("ORGANIZATION", true) }
            .filter { it.reviewStatus !in setOf("REJECTED", "MERGED") }
            .mapNotNull { existing ->
                val existingProfile = fromProperties(existing.properties)
                val website = existingProfile?.website ?: existing.properties.entries.firstOrNull { (key, _) ->
                    key.equals("website", true) || key.equals("url", true) || key.equals("domain", true)
                }?.value.orEmpty()
                val existingDomain = normalizeDomain(website)
                val reason = when {
                    domain != null && existingDomain != null && domain == existingDomain -> "Same company website domain: $domain"
                    name.isNotBlank() && name == normalizeCompanyName(existing.title) -> "Same normalised organisation name"
                    else -> null
                }
                reason?.let { SalesAccountDuplicate(existing.id, existing.title, it) }
            }
            .sortedWith(compareBy<SalesAccountDuplicate> { if (it.reason.startsWith("Same company website")) 0 else 1 }.thenBy { it.title.lowercase(Locale.ROOT) })
            .toList()
    }

    fun normalizeDomain(website: String): String? = runCatching {
        val raw = website.trim()
        if (raw.isBlank()) return null
        val uri = URI(raw)
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") || uri.userInfo != null) return null
        uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.trimEnd('.')?.takeIf { it.isNotBlank() && it.contains('.') }
    }.getOrNull()

    private fun isValidSourceUrl(value: String): Boolean = normalizeDomain(value) != null
    private fun normalizeCompanyName(value: String): String = value.lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().replace(Regex("\\s+"), " ")
}
