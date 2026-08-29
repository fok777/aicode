package com.aicode.feature.settings.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelMetadataServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(raw: String): List<String> =
        ModelMetadataService.parseReasoningOptions(json.parseToJsonElement(raw))

    @Test
    fun effortType_extractsValues() {
        assertEquals(
            listOf("none", "low", "medium", "high", "xhigh", "max"),
            parse("""[{"type":"effort","values":["none","low","medium","high","xhigh","max"]}]""")
        )
    }

    @Test
    fun mixedToggleThenEffort_extractsEffort() {
        assertEquals(
            listOf("low", "medium", "high", "xhigh", "max"),
            parse("""[{"type":"toggle"},{"type":"effort","values":["low","medium","high","xhigh","max"]}]""")
        )
    }

    @Test
    fun toggleOnly_returnsEmpty() {
        assertEquals(emptyList<String>(), parse("""[{"type":"toggle"}]"""))
    }

    @Test
    fun budgetTokens_returnsEmpty() {
        assertEquals(emptyList<String>(), parse("""[{"type":"budget_tokens","min":1024}]"""))
    }

    @Test
    fun emptyArray_returnsEmpty() {
        assertEquals(emptyList<String>(), parse("""[]"""))
    }

    @Test
    fun nullJson_returnsEmpty() {
        assertEquals(emptyList<String>(), parse("""null"""))
    }
}