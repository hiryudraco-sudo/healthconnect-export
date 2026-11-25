package com.healthconnect.export.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class DailyExportMergeTest {
    private val json = Json

    @Test
    fun `missing distance is retained while new step total wins`() {
        val previous = """{"date":"2026-11-24","steps":{"total_steps":100},"distance":{"total_distance_meters":500},"metadata":{"export_timestamp":"old"}}"""
        val refreshed = """{"date":"2026-11-24","steps":{"total_steps":120},"metadata":{"export_timestamp":"new"}}"""
        val merged = json.parseToJsonElement(DailyExportMerge.merge(previous, refreshed)).jsonObject
        assertEquals("120", merged.getValue("steps").jsonObject.getValue("total_steps").jsonPrimitive.content)
        assertEquals("500", merged.getValue("distance").jsonObject.getValue("total_distance_meters").jsonPrimitive.content)
        assertEquals("new", merged.getValue("metadata").jsonObject.getValue("export_timestamp").jsonPrimitive.content)
    }

    @Test
    fun `new metrics are added to old export`() {
        val old = """{"date":"2026-11-24","steps":{"total_steps":100},"metadata":{}}"""
        val fresh = """{"date":"2026-11-24","distance":{"total_distance_meters":999},"metadata":{}}"""
        val merged = json.parseToJsonElement(DailyExportMerge.merge(old, fresh)).jsonObject
        assertTrue("steps" in merged)
        assertTrue("distance" in merged)
    }

    @Test
    fun `provenance follows retained and refreshed metrics`() {
        val old = """{"date":"2026-11-24","steps":{"total_steps":100},"distance":{"total_distance_meters":500},"metadata":{},"provenance":{"selection_mode":"explicit","selected_source":"fit","sources_by_type":{"steps":["fit"],"distance":["fit"]}}}"""
        val fresh = """{"date":"2026-11-24","steps":{"total_steps":101},"metadata":{},"provenance":{"selection_mode":"explicit","selected_source":"zepp","sources_by_type":{"steps":["zepp"]}}}"""
        val merged = json.parseToJsonElement(DailyExportMerge.merge(old, fresh)).jsonObject
        val provenance = merged.getValue("provenance").jsonObject
        assertEquals("mixed_history", provenance.getValue("selection_mode").jsonPrimitive.content)
        val sources = provenance.getValue("sources_by_type").jsonObject
        assertEquals("zepp", sources.getValue("steps").toString().trim('[',']','"'))
        assertEquals("fit", sources.getValue("distance").toString().trim('[',']','"'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `different dates are never merged`() {
        DailyExportMerge.merge(
            """{"date":"2026-11-23","metadata":{}}""",
            """{"date":"2026-11-24","metadata":{}}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `broken previous export cannot be overwritten silently`() {
        DailyExportMerge.merge("not json", """{"date":"2026-11-24","metadata":{}}""")
    }
}
