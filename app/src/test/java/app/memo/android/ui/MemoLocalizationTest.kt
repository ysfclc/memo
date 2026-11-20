package app.memo.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoLocalizationTest {
    @Test
    fun englishIsTheSourceLanguageAndPassesThrough() {
        assertEquals("Settings", MemoLocalization.translate("Settings", "EN"))
    }

    @Test
    fun polishTranslatesCoreNavigationAndSettings() {
        assertEquals("Ustawienia", MemoLocalization.translate("Settings", "PL"))
        assertEquals("Język", MemoLocalization.translate("Language", "PL"))
        assertEquals("Strona główna", MemoLocalization.translate("Home", "PL"))
    }

    @Test
    fun turkishTranslatesCoreNavigationAndSettings() {
        assertEquals("Ayarlar", MemoLocalization.translate("Settings", "TR"))
        assertEquals("Dil", MemoLocalization.translate("Language", "TR"))
        assertEquals("Ana sayfa", MemoLocalization.translate("Home", "TR"))
    }

    @Test
    fun memoPrimarySurfacesAndVoiceActionTranslate() {
        assertEquals("Rozmowa", MemoLocalization.translate("Talk", "PL"))
        assertEquals("Sohbet", MemoLocalization.translate("Talk", "TR"))
        assertEquals("Na żywo", MemoLocalization.translate("Live", "PL"))
        assertEquals("Canlı", MemoLocalization.translate("Live", "TR"))
        assertEquals("ROZMOWA / NA ŻYWO / PRACA / STUDIO", MemoLocalization.translate("TALK / LIVE / WORK / STUDIO", "PL"))
        assertEquals("SOHBET / CANLI / İŞ / STÜDYO", MemoLocalization.translate("TALK / LIVE / WORK / STUDIO", "TR"))
        assertEquals("Studio", MemoLocalization.translate("Studio", "PL"))
        assertEquals("Stüdyo", MemoLocalization.translate("Studio", "TR"))
        assertEquals("Głos", MemoLocalization.translate("Voice", "PL"))
        assertEquals("Ses", MemoLocalization.translate("Voice", "TR"))
    }

    @Test
    fun universalComposerExplainerTranslatesInPolishAndTurkish() {
        assertEquals("Kontynuuj z Memo", MemoLocalization.translate("Continue with Memo", "PL"))
        assertEquals("Memo ile devam et", MemoLocalization.translate("Continue with Memo", "TR"))
        assertTrue(MemoLocalization.translate(
            "Memo routes action requests into an editable plan and questions or brainstorming into Talk. Nothing external is sent or changed automatically.",
            "PL"
        ).contains("edytowalnego planu"))
        assertTrue(MemoLocalization.translate(
            "Memo routes action requests into an editable plan and questions or brainstorming into Talk. Nothing external is sent or changed automatically.",
            "TR"
        ).contains("düzenlenebilir bir plana"))
        assertEquals("Plany · 3", MemoLocalization.translate("Plans · 3", "PL"))
        assertEquals("Planlar · 3", MemoLocalization.translate("Plans · 3", "TR"))
        assertEquals("Brak planów", MemoLocalization.translate("No plans yet", "PL"))
        assertEquals("Henüz plan yok", MemoLocalization.translate("No plans yet", "TR"))
    }

    @Test
    fun userContentAndUnknownStringsRemainUntouched() {
        val userText = "I met a person called Deniz at the event."
        assertEquals(userText, MemoLocalization.translate(userText, "TR"))
    }

    @Test
    fun voiceAndNotificationControlsTranslateInPolishAndTurkish() {
        assertEquals("Wprowadzanie głosowe", MemoLocalization.translate("Voice input", "PL"))
        assertEquals("Sesle giriş", MemoLocalization.translate("Voice input", "TR"))
        assertEquals("Powiadomienia i przypomnienia", MemoLocalization.translate("Notifications & reminders", "PL"))
        assertEquals("Bildirimler ve hatırlatıcılar", MemoLocalization.translate("Notifications & reminders", "TR"))
        assertEquals("Wybierz godzinę przypomnienia", MemoLocalization.translate("Choose reminder time", "PL"))
        assertEquals("Hatırlatma saatini seç", MemoLocalization.translate("Choose reminder time", "TR"))
        assertEquals("Włączono powiadomienia o przepływach pracy.", MemoLocalization.translate("Workflow notifications enabled.", "PL"))
        assertEquals("İş akışı bildirimleri etkinleştirildi.", MemoLocalization.translate("Workflow notifications enabled.", "TR"))
    }

    @Test
    fun localWorkRetryAndStopControlsTranslateInPolishAndTurkish() {
        val keys = listOf(
            "Four attempts recorded. Review the evidence or create a fresh plan; Memo will not replay this checkpoint.",
            "Four attempts recorded. Review the saved versions or create a fresh plan; Memo will not replay this checkpoint.",
            "Generate another draft",
            "Local work generation is running",
            "Stop generation",
            "Stopping keeps the checkpoint in review. Any partial text is not saved as a completed artifact."
        )
        keys.forEach { key ->
            assertTrue("Polish translation missing for: $key", MemoLocalization.translate(key, "PL") != key)
            assertTrue("Turkish translation missing for: $key", MemoLocalization.translate(key, "TR") != key)
        }
        assertEquals("Utwórz kolejny szkic", MemoLocalization.translate("Generate another draft", "PL"))
        assertEquals("Bir taslak daha oluştur", MemoLocalization.translate("Generate another draft", "TR"))
    }

    @Test
    fun liveBrowserWorkflowActionsTranslateInPolishAndTurkish() {
        assertEquals("Sprawdź polecenie i elementy sterujące", MemoLocalization.translate("Review prompt and controls", "PL"))
        assertEquals("İstemi ve kontrolleri incele", MemoLocalization.translate("Review prompt and controls", "TR"))
        assertEquals("Otwórz elementy sterujące przepływem", MemoLocalization.translate("Open workflow controls", "PL"))
        assertEquals("İş akışı kontrollerini aç", MemoLocalization.translate("Open workflow controls", "TR"))
    }

    @Test
    fun contextualRoutesTranslateInPolishAndTurkish() {
        assertEquals("Sesja przeglądarki", MemoLocalization.translate("Browser session", "PL"))
        assertEquals("Tarayıcı oturumu", MemoLocalization.translate("Browser session", "TR"))
        assertEquals("Aplikacje telefonu", MemoLocalization.translate("Phone apps", "PL"))
        assertEquals("Telefon uygulamaları", MemoLocalization.translate("Phone apps", "TR"))
        assertEquals("Skrzynka zatwierdzeń", MemoLocalization.translate("Approval inbox", "PL"))
        assertEquals("Onay kutusu", MemoLocalization.translate("Approval inbox", "TR"))
    }

    @Test
    fun workExecutionRoutesTranslateInPolishAndTurkish() {
        assertEquals("Przepływy", MemoLocalization.translate("Workflows", "PL"))
        assertEquals("İş akışları", MemoLocalization.translate("Workflows", "TR"))
        assertEquals("Zadania przeglądarki", MemoLocalization.translate("Browser tasks", "PL"))
        assertEquals("Tarayıcı görevleri", MemoLocalization.translate("Browser tasks", "TR"))
        assertEquals("Przepływy przeglądarki", MemoLocalization.translate("Browser workflows", "PL"))
        assertEquals("Tarayıcı iş akışları", MemoLocalization.translate("Browser workflows", "TR"))
        assertEquals("Narzędzia wykonawcze", MemoLocalization.translate("Execution tools", "PL"))
        assertEquals("Yürütme araçları", MemoLocalization.translate("Execution tools", "TR"))
        assertTrue(MemoLocalization.translate("Inspect captured source material and saved workflow results.", "PL").contains("materiały źródłowe"))
        assertTrue(MemoLocalization.translate("Inspect captured source material and saved workflow results.", "TR").contains("kaynak materyalleri"))
    }

    @Test
    fun workflowRecoveryAndExecutionLabelsTranslateInPolishAndTurkish() {
        val expected = mapOf(
            "Back to workflow" to ("Wróć do przepływu" to "İş akışına dön"),
            "Open Work" to ("Otwórz obszar pracy" to "Çalışma alanını aç"),
            "Open browser" to ("Otwórz przeglądarkę" to "Tarayıcıyı aç"),
            "Plan only · no action has been executed" to ("Tylko plan — nie wykonano żadnego działania" to "Yalnızca plan — hiçbir işlem gerçekleştirilmedi"),
            "Workflow browser" to ("Przeglądarka przepływu pracy" to "İş akışı tarayıcısı")
        )
        expected.forEach { (source, translations) ->
            assertEquals("Polish translation for: $source", translations.first, MemoLocalization.translate(source, "PL"))
            assertEquals("Turkish translation for: $source", translations.second, MemoLocalization.translate(source, "TR"))
        }
    }

    @Test
    fun unsupportedLanguageFallsBackToOriginalText() {
        assertEquals("Settings", MemoLocalization.translate("Settings", "XX"))
    }
}
