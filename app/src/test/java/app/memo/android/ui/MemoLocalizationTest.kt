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
        assertEquals("ZAMIAR / PRACA / DOWODY", MemoLocalization.translate("INTENT / WORK / EVIDENCE", "PL"))
        assertEquals("NİYET / İŞ / KANIT", MemoLocalization.translate("INTENT / WORK / EVIDENCE", "TR"))
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
    fun unsupportedLanguageFallsBackToOriginalText() {
        assertEquals("Settings", MemoLocalization.translate("Settings", "XX"))
    }
}
