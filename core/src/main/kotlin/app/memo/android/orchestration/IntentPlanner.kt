package app.memo.android.orchestration

import java.text.Normalizer
import java.util.Locale

/** A deterministic, explainable first-pass intent planner. It proposes work; it never executes side effects. */
enum class IntentDomain(val id: String, val label: String) {
    GENERAL("general", "General work"), RESEARCH("research", "Research"), BUILD("build", "Build / engineering"),
    CAREER("career", "Career"), CONTENT("content", "Content"), CRM("crm", "People / CRM"),
    CAPTURE("capture", "Capture / organize"), AUTOMATION("automation", "Automation")
}

data class PlannedStep(
    val title: String,
    val instruction: String,
    val checkpointKind: String = "USER_CHECKPOINT",
    val requiresApproval: Boolean = false
)

data class IntentPlan(
    val title: String,
    val goal: String,
    val domain: IntentDomain,
    val rationale: String,
    val steps: List<PlannedStep>,
    val requiresExternalApproval: Boolean
)

object IntentPlanner {
    private val riskyAction = Regex(
        "\\b(send|submit|publish|post|apply|delete|remove|buy|purchase|pay|transfer|invite|message|email|book|order|upload|share|zaplat|wyślij|wyslij|opublikuj|usuń|usun|kup|zastosuj|wyślij|gönder|gonder|yayınla|yayinla|sil|satın al|satin al|başvur|basvur|öde|ode)\\b",
        RegexOption.IGNORE_CASE
    )

    fun plan(rawIntent: String, language: String = "EN"): IntentPlan {
        val intent = rawIntent.trim().replace(Regex("\\s+"), " ").take(8_000)
        require(intent.length >= 4) { "Describe the outcome you want in at least a few words." }
        val normalized = normalize(intent)
        val domain = classify(normalized)
        val externalWrite = riskyAction.containsMatchIn(normalized)
        val title = makeTitle(intent)
        val steps = mutableListOf<PlannedStep>()
        steps += PlannedStep(
            "Frame the outcome and constraints",
            "Restate the desired result, deadline, constraints and definition of done from the request. Preserve unknowns as questions; do not invent answers."
        )
        steps += PlannedStep(
            "Retrieve relevant Memo context",
            "Review matching local projects, conversations, notes, entities, prior jobs and saved evidence. Record which sources were actually inspected and their timestamps; do not treat missing context as a fact."
        )
        when (domain) {
            IntentDomain.RESEARCH -> steps += PlannedStep("Gather and compare source evidence", "Collect source material through an available, user-authorized channel. Separate sourced facts, model inferences, opinions and unresolved questions. Save source URLs or excerpts for important claims.")
            IntentDomain.BUILD -> {
                steps += PlannedStep("Inspect the current implementation", "Read the actual source and existing acceptance criteria before editing. Identify dependencies, ownership boundaries and regression risks.")
                steps += PlannedStep("Implement a coherent vertical slice", "Change working behavior rather than adding a mock screen. Keep the diff bounded, add tests for critical logic, and preserve existing data and interfaces.")
            }
            IntentDomain.CAREER -> steps += PlannedStep("Build an evidence-backed career artifact", "Use only confirmed experience, metrics and preferences. Mark missing details for the user instead of filling them with invented claims; tailor the artifact to the stated target.")
            IntentDomain.CONTENT -> steps += PlannedStep("Create a sourced, editable draft", "Use the user's actual positioning and supplied evidence. Separate factual claims from opinions, preserve uncertainty and create an editable draft before any distribution.")
            IntentDomain.CRM -> steps += PlannedStep("Resolve identity and propose a structured record", "Check existing people and organizations before creating anything. Attach source evidence and confidence to each field; keep new or enriched records in review until approved.")
            IntentDomain.CAPTURE -> steps += PlannedStep("Capture and structure the material", "Preserve the original source and capture time. Extract useful claims, tasks and entities only where supported; deduplicate against existing records and retain provenance.")
            IntentDomain.AUTOMATION -> steps += PlannedStep("Map the actual runtime and safe actions", "Identify the exact app/site, capabilities and permissions available. Prefer visible, bounded actions; define a postcondition and stop condition before execution.")
            IntentDomain.GENERAL -> steps += PlannedStep("Break the goal into deliverables", "Split the request into small, ordered outputs with explicit dependencies. Prefer a useful first deliverable; keep irreversible actions outside the automatic plan.")
        }
        steps += PlannedStep("Produce the working artifact or result", "Create the deliverable inside Memo or through an available runtime. Save the actual result and its source links; if a required capability is unavailable, record the blocker and a concrete fallback.")
        steps += PlannedStep("Verify the result against the goal", "Check every acceptance condition against the actual artifact, output or visible postcondition. Record failures and uncertainty; do not equate a model's claim with completed work.")
        if (externalWrite) {
            steps += PlannedStep(
                "Review external action before execution",
                "This request contains an external-write intent. Prepare the exact content and destination, then use Memo's action approval gate. Approval must bind to the exact current content hash; changed content requires new approval. This checkpoint cannot itself send, publish, apply, pay, delete or submit anything.",
                checkpointKind = "EXTERNAL_ACTION",
                requiresApproval = true
            )
        }
        steps += PlannedStep(
            "Review, accept or revise",
            "Inspect the artifact, evidence and remaining limitations. Choose what to accept, change or defer. Preserve a truthful record of what was actually verified.",
            checkpointKind = "HUMAN_REVIEW"
        )
        val reason = when (domain) {
            IntentDomain.RESEARCH -> "Research signals detected; the plan prioritizes source quality and claim verification."
            IntentDomain.BUILD -> "Build signals detected; the plan starts with source inspection and a tested implementation slice."
            IntentDomain.CAREER -> "Career-document signals detected; factual accuracy and user-owned claims are explicit checkpoints."
            IntentDomain.CONTENT -> "Content signals detected; draft and review precede any external distribution."
            IntentDomain.CRM -> "People/CRM signals detected; identity resolution and approval precede committing a new record."
            IntentDomain.CAPTURE -> "Capture signals detected; source provenance and deduplication are first-class steps."
            IntentDomain.AUTOMATION -> "Automation signals detected; capability, permission and postcondition checks precede execution."
            IntentDomain.GENERAL -> "No specialized route clearly matched. A general plan is proposed and can be edited before work starts."
        }
        val localeNote = when (language.uppercase(Locale.ROOT)) {
            "PL" -> " Język interfejsu: polski. Tekst użytkownika pozostaje niezmieniony."
            "TR" -> " Arayüz dili: Türkçe. Kullanıcının metni değiştirilmez."
            else -> " Interface language: English. User-authored intent is preserved."
        }
        return IntentPlan(title, intent, domain, reason + localeNote, steps, externalWrite)
    }

    private fun classify(text: String): IntentDomain = when {
        // Explicit automation verbs must outrank the generic "app"/"code" BUILD signals.
        // Keep broader terms such as "browser" and "workflow" below BUILD so a request
        // to implement a browser/workflow feature remains a BUILD task.
        containsAny(
            text,
            "automate", "automating", "automated", "automatyz", "otomatikleştir", "otomatiklestir",
            "click through", "tap through", "fill out this form", "operate the app", "control the app"
        ) -> IntentDomain.AUTOMATION
        containsAny(text, "cv", "resume", "cover letter", "job search", "job offer", "interview", "career", "recruiter", "praca", "życiorys", "zyciorys", "list motywacyjny", "kariera", "iş", "is ilan", "özgeçmiş", "ozgecmis", "kariyer") -> IntentDomain.CAREER
        containsAny(text, "linkedin post", "content", "social media", "carousel", "newsletter", "article", "post idea", "publish content", "post na linkedin", "treść", "tresc", "wpis", "içerik", "icerik", "sosyal medya") -> IntentDomain.CONTENT
        containsAny(text, "crm", "contact", "person", "company", "met at", "lead", "follow-up", "kontakt", "firma", "klient", "osobę", "osobe", "kişi", "kisi", "şirket", "sirket", "takip") -> IntentDomain.CRM
        containsAny(text, "research", "investigate", "compare", "sources", "evidence", "find out", "analyze the market", "zbadaj", "porównaj", "porownaj", "źródła", "zrodla", "badanie", "araştır", "arastir", "karşılaştır", "karsilastir", "kanıt", "kanit") -> IntentDomain.RESEARCH
        containsAny(text, "build", "implement", "code", "debug", "fix", "repository", "app", "feature", "refactor", "architecture", "zbuduj", "wdroż", "wdroz", "kod", "napraw", "aplikac", "buduj", "uygula", "kodla", "hata", "özellik", "ozellik", "mimari") -> IntentDomain.BUILD
        containsAny(text, "workflow", "browser", "click", "tap", "fill form", "automation", "automatyz", "przepływ", "przeplyw", "otomasyon", "iş akışı", "is akisi", "tarayıcı", "tarayici") -> IntentDomain.AUTOMATION
        containsAny(text, "remember", "capture", "save this", "organize", "summarize this", "extract from", "note this", "zapisz", "zapamiętaj", "zapamietaj", "notatka", "uporządkuj", "uporzadkuj", "kaydet", "hatırla", "hatirla", "özetle", "ozetle") -> IntentDomain.CAPTURE
        else -> IntentDomain.GENERAL
    }

    private fun containsAny(text: String, vararg terms: String) = terms.any { normalize(it).let(text::contains) }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)

    private fun makeTitle(intent: String): String {
        val first = intent.lineSequence().firstOrNull().orEmpty()
            .replace(Regex("^(please|can you|could you|help me|i want to|i need to|lütfen|lutfen|bana|şunu|sunu)\\s+", RegexOption.IGNORE_CASE), "")
            .trim().trimEnd('.', '!', '?', ':', ';')
        return first.take(76).ifBlank { "New work plan" }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
