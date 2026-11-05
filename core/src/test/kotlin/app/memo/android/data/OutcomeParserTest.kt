package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeParserTest {
    @Test fun onlySourceGroundedProposalsAreReturned() {
        val source = "We decided to launch the Polish store in November. Yusuf will compare three suppliers by Friday."
        val raw = """```json
          {"entities":[
            {"type":"PROJECT","title":"Polish store launch","description":"Launch plan","evidence":"launch the Polish store in November"},
            {"type":"TASK","title":"Compare suppliers","description":"Compare three suppliers","evidence":"compare three suppliers by Friday"},
            {"type":"PERSON","title":"Invented person","description":"No evidence","evidence":"Alice owns all operations in perpetuity"}
          ]}
        ```"""
        val result = OutcomeParser.parse(raw, source)
        assertEquals(2, result.size)
        assertTrue(result.all { it.evidence.isNotBlank() })
    }

    @Test fun selfModelFactsRequireAnExactSourceExcerpt() {
        val source = "I managed 15 DACH partners at SALESmanago and built an outbound workflow in Airtable."
        val raw = """{"entities":[
          {"type":"SELF_MODEL_FACT","title":"Partner management","description":"Managed 15 DACH partners","properties":{"claimType":"experience"},"evidence":"15 DACH partners at SALESmanago"},
          {"type":"SELF_MODEL_FACT","title":"Invented revenue growth","description":"Claimed 300% growth","evidence":"grew revenue by 300%"}
        ]}"""
        val result = OutcomeParser.parse(raw, source)
        assertEquals(1, result.size)
        assertEquals("SELF_MODEL_FACT", result.single().type)
        assertTrue(result.single().evidence.isNotBlank())
    }

    @Test fun invalidTypesAndDuplicateNamesAreIgnored() {
        val source = "Create an online store before December."
        val raw = """{"entities":[
          {"type":"EXECUTE_CODE","title":"Online store","evidence":"online store before December"},
          {"type":"PROJECT","title":"Online store","evidence":"online store before December"},
          {"type":"PROJECT","title":" online store ","evidence":"online store before December"}
        ]}"""
        assertEquals(1, OutcomeParser.parse(raw, source).size)
    }
}
