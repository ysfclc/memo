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
    fun talkApprovalInboxActionsTranslateInPolishAndTurkish() {
        assertEquals("Zatwierdź dokładne działanie", MemoLocalization.translate("Approve exact action", "PL"))
        assertEquals("Tam eylemi onayla", MemoLocalization.translate("Approve exact action", "TR"))
        assertEquals("Odrzuć i zatrzymaj to działanie", MemoLocalization.translate("Reject and stop this action", "PL"))
        assertEquals("Reddet ve bu eylemi durdur", MemoLocalization.translate("Reject and stop this action", "TR"))
        assertTrue(MemoLocalization.translate(
            "This action is waiting for the matching Talk execution. Review the destination and full exact payload above. Approval is one-use and tied to the displayed content hash.", "PL"
        ).contains("Zatwierdzenie jest jednorazowe"))
        assertTrue(MemoLocalization.translate(
            "This action is waiting for the matching Talk execution. Review the destination and full exact payload above. Approval is one-use and tied to the displayed content hash.", "TR"
        ).contains("Onay tek kullanımlıktır"))
        assertTrue(MemoLocalization.translate(
            "Pending Talk actions and live browser prompts appear here for exact-content review. Completed or uncertain actions stay in their own history and review states.", "PL"
        ).contains("Oczekujące działania z Rozmowy"))
        assertTrue(MemoLocalization.translate(
            "Pending Talk actions and live browser prompts appear here for exact-content review. Completed or uncertain actions stay in their own history and review states.", "TR"
        ).contains("Bekleyen Talk eylemleri"))
    }

    @Test
    fun approvalReviewAcknowledgementTranslatesInPolishAndTurkish() {
        assertEquals("Potwierdzam sprawdzenie pełnego celu i dokładnej treści działania.", MemoLocalization.translate("I confirm that I reviewed the full destination and exact payload.", "PL"))
        assertEquals("Tam hedefi ve eylemin eksiksiz içeriğini incelediğimi onaylıyorum.", MemoLocalization.translate("I confirm that I reviewed the full destination and exact payload.", "TR"))
    }

    @Test
    fun durableExecutionLabelsTranslateInPolishAndTurkish() {
        assertEquals("Wykonania Memo", MemoLocalization.translate("Memo executions", "PL"))
        assertEquals("Memo yürütmeleri", MemoLocalization.translate("Memo executions", "TR"))
        assertEquals("Oczekuje na zatwierdzenie", MemoLocalization.translate("Waiting for approval", "PL"))
        assertEquals("Onay bekliyor", MemoLocalization.translate("Waiting for approval", "TR"))
        assertEquals("Zapisywanie wyniku", MemoLocalization.translate("Saving result", "PL"))
        assertEquals("Sonuç kaydediliyor", MemoLocalization.translate("Saving result", "TR"))
        assertEquals("Wymaga sprawdzenia", MemoLocalization.translate("Needs review", "PL"))
        assertEquals("İnceleme gerekiyor", MemoLocalization.translate("Needs review", "TR"))
        assertTrue(MemoLocalization.translate(
            "Memo restarted during this run. Review saved events and any action that may have started. No step was replayed.", "PL"
        ).contains("Żaden krok nie został odtworzony"))
        assertTrue(MemoLocalization.translate(
            "Memo restarted during this run. Review saved events and any action that may have started. No step was replayed.", "TR"
        ).contains("Hiçbir adım yeniden oynatılmadı"))
    }

    @Test
    fun talkRunInspectionDetailsTranslateInPolishAndTurkish() {
        assertEquals("Ostatnie trwałe zdarzenie", MemoLocalization.translate("Last durable event", "PL"))
        assertEquals("Son kalıcı olay", MemoLocalization.translate("Last durable event", "TR"))
        assertEquals("Ostatnia funkcja", MemoLocalization.translate("Last capability", "PL"))
        assertEquals("Son yetenek", MemoLocalization.translate("Last capability", "TR"))
        assertEquals("Ostatnie odwołanie do dowodu", MemoLocalization.translate("Last evidence reference", "PL"))
        assertEquals("Son kanıt referansı", MemoLocalization.translate("Last evidence reference", "TR"))
        assertEquals("Wywołanie zatwierdzenia wymaga sprawdzenia", MemoLocalization.translate("Approval call requires inspection", "PL"))
        assertEquals("Onay çağrısı inceleme gerektiriyor", MemoLocalization.translate("Approval call requires inspection", "TR"))
        assertTrue(MemoLocalization.translate(
            "A pending approval reference is recorded. It is not reusable after restart.", "PL"
        ).contains("Nie można go ponownie użyć"))
        assertTrue(MemoLocalization.translate(
            "A pending approval reference is recorded. It is not reusable after restart.", "TR"
        ).contains("tekrar kullanılamaz"))
        assertTrue(MemoLocalization.translate(
            "This run is preserved as history. Memo will not replay its last step automatically.", "PL"
        ).contains("nie odtworzy automatycznie"))
        assertTrue(MemoLocalization.translate(
            "This run is preserved as history. Memo will not replay its last step automatically.", "TR"
        ).contains("otomatik olarak yeniden yürütmez"))
    }

    @Test
    fun interruptedTalkContinuationLabelsAndReasonsTranslateInPolishAndTurkish() {
        assertEquals("Kontynuuj w Rozmowie", MemoLocalization.translate("Continue in Talk", "PL"))
        assertEquals("Talk'ta devam et", MemoLocalization.translate("Continue in Talk", "TR"))
        assertEquals(
            "Kontynuujesz zapisane wykonanie · sprawdź wersję roboczą przed wysłaniem",
            MemoLocalization.translate("Continuing a saved run · review this draft before sending", "PL")
        )
        assertEquals(
            "Kaydedilmiş bir çalışmaya devam ediyorsun · göndermeden önce taslağı incele",
            MemoLocalization.translate("Continuing a saved run · review this draft before sending", "TR")
        )
        assertTrue(MemoLocalization.translate(
            "A capability may have started without a verified completion. Inspect its destination before continuing.", "PL"
        ).contains("Przed kontynuacją sprawdź miejsce docelowe"))
        assertTrue(MemoLocalization.translate(
            "A capability may have started without a verified completion. Inspect its destination before continuing.", "TR"
        ).contains("Devam etmeden önce hedefi incele"))
        assertEquals("Kontynuuje poprzednie wykonanie", MemoLocalization.translate("Continues prior run", "PL"))
        assertEquals("Önceki çalışmanın devamı", MemoLocalization.translate("Continues prior run", "TR"))
        assertTrue(MemoLocalization.translate(
            "The saved run changed or is no longer eligible. Review it in Live before trying again.", "PL"
        ).contains("Sprawdź je w Live"))
        assertTrue(MemoLocalization.translate(
            "The saved run changed or is no longer eligible. Review it in Live before trying again.", "TR"
        ).contains("Live alanında incele"))
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
    fun unsupportedLanguageFallsBackToOriginalText() {
        assertEquals("Settings", MemoLocalization.translate("Settings", "XX"))
    }
}
