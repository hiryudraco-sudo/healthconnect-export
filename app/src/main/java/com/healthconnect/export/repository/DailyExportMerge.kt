package com.healthconnect.export.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Merge a refreshed daily JSON export with its previous version.
 *
 * Fresh, non-null metric sections win; absent sections retain their last known
 * values. This is deliberately conservative: a temporary permission/sync gap
 * must never turn a previously collected metric into missing data.
 *
 * Only the metric-section keys are merged; the latest metadata wins. Provenance
 * is reconstructed to match the retained or replaced sections.
 */
internal object DailyExportMerge {
    private val json = Json { ignoreUnknownKeys = true }
    private val metrics = setOf(
        "steps", "heart_rate", "sleep", "calories", "distance",
        "floors_climbed", "active_calories", "weight", "body_fat",
        "blood_pressure", "blood_glucose", "oxygen_saturation",
        "body_temperature", "respiratory_rate", "hydration",
        "resting_heart_rate", "exercises", "nutrition", "speed", "menstruation",
    )

    fun merge(previous: String, refreshed: String): String {
        val old = json.parseToJsonElement(previous).jsonObject
        val fresh = json.parseToJsonElement(refreshed).jsonObject
        val oldDate = old["date"]?.jsonPrimitive?.contentOrNull
        val freshDate = fresh["date"]?.jsonPrimitive?.contentOrNull
        require(!oldDate.isNullOrBlank() && oldDate == freshDate) {
            "Cannot merge exports for different or missing dates"
        }
        require(fresh["metadata"] is JsonObject) { "Missing refreshed export metadata" }

        val result = old.toMutableMap()
        // Preserve unknown fields for forwards compatibility.
        result.putAll(fresh)
        for (key in metrics) {
            val replacement = fresh[key]
            val isMissing = replacement == null ||
                replacement is JsonPrimitive && replacement.isString.not() && replacement.content == "null"
            if (isMissing && old[key] != null) {
                result[key] = old.getValue(key)
            }
        }

        val oldProvenance = old["provenance"] as? JsonObject
        val newProvenance = fresh["provenance"] as? JsonObject
        if (oldProvenance != null || newProvenance != null) {
            val oldSources = oldProvenance?.get("sources_by_type") as? JsonObject
            val newSources = newProvenance?.get("sources_by_type") as? JsonObject
            val sources = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            for (key in metrics) {
                val isFresh = fresh[key] != null && fresh[key].toString() != "null"
                val source = if (isFresh) newSources?.get(key) else oldSources?.get(key)
                if (source != null) sources[key] = source
            }
            val p = (newProvenance ?: oldProvenance!!).toMutableMap()
            p["sources_by_type"] = JsonObject(sources)
            val oldSelected = oldProvenance?.get("selected_source")
            val newSelected = newProvenance?.get("selected_source")
            if (oldSelected != null && newSelected != null && oldSelected != newSelected) {
                p["selection_mode"] = JsonPrimitive("mixed_history")
                p["selected_source"] = kotlinx.serialization.json.JsonNull
            }
            result["provenance"] = JsonObject(p)
        }
        return JsonObject(result).toString()
    }
}
