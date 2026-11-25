package com.healthconnect.export.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ===== Individual readings =====
// Blood pressure, blood glucose, oxygen saturation, weight, body fat and heart rate
// also carry the raw readings their daily aggregate was computed from, so a consumer
// that needs every measurement (a report for a doctor, a trend chart) does not lose
// them to averaging.
//
// - "time": ISO-8601 local time with the record's own offset, e.g.
//   "2026-09-19T06:22:41.123+03:00" (milliseconds preserved).
// - "source": data-origin package name of the app that wrote the record.
// - Both lists default to empty, so JSON written before they existed still decodes.
// - The lists are capped at `HealthConnectRepository.MAX_READINGS_PER_DAY` (2000)
//   readings per type per day, evenly subsampled beyond that; JSON/CSV aggregates
//   and `records_count` are unaffected.
// - Only JSON carries them: CSV keeps daily totals only.

@Serializable
data class BloodPressureReading(
    @SerialName("time") val time: String,
    @SerialName("systolic_mmhg") val systolicMmHg: Double,
    @SerialName("diastolic_mmhg") val diastolicMmHg: Double,
    @SerialName("source") val source: String,
)

@Serializable
data class BloodGlucoseReading(
    @SerialName("time") val time: String,
    @SerialName("level_mmol_per_l") val levelMmolPerL: Double,
    @SerialName("level_mg_per_dl") val levelMgPerDl: Double,
    @SerialName("source") val source: String,
)

@Serializable
data class OxygenSaturationReading(
    @SerialName("time") val time: String,
    @SerialName("percentage") val percentage: Double,
    @SerialName("source") val source: String,
)

@Serializable
data class HeartRateSample(
    @SerialName("time") val time: String,
    @SerialName("bpm") val bpm: Long,
    @SerialName("source") val source: String,
)

@Serializable
data class WeightReading(
    @SerialName("time") val time: String,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("source") val source: String,
)

@Serializable
data class BodyFatReading(
    @SerialName("time") val time: String,
    @SerialName("percentage") val percentage: Double,
    @SerialName("source") val source: String,
)

// ===== Core data classes for Health Connect =====

@Serializable
data class DailyHealthRecord(
    @SerialName("date") val date: String,
    @SerialName("steps") val steps: StepsData? = null,
    @SerialName("heart_rate") val heartRate: HeartRateData? = null,
    @SerialName("sleep") val sleep: SleepData? = null,
    @SerialName("calories") val calories: CaloriesData? = null,
    @SerialName("distance") val distance: DistanceData? = null,
    @SerialName("floors_climbed") val floorsClimbed: FloorsClimbedData? = null,
    @SerialName("active_calories") val activeCalories: ActiveCaloriesData? = null,
    @SerialName("weight") val weight: WeightData? = null,
    @SerialName("body_fat") val bodyFat: BodyFatData? = null,
    @SerialName("blood_pressure") val bloodPressure: BloodPressureData? = null,
    @SerialName("blood_glucose") val bloodGlucose: BloodGlucoseData? = null,
    @SerialName("oxygen_saturation") val oxygenSaturation: OxygenSaturationData? = null,
    @SerialName("body_temperature") val bodyTemperature: BodyTemperatureData? = null,
    @SerialName("respiratory_rate") val respiratoryRate: RespiratoryRateData? = null,
    @SerialName("hydration") val hydration: HydrationData? = null,
    @SerialName("resting_heart_rate") val restingHeartRate: RestingHeartRateData? = null,
    @SerialName("exercises") val exercises: List<ExerciseData>? = null,
    @SerialName("nutrition") val nutrition: List<NutritionData>? = null,
    @SerialName("speed") val speed: SpeedData? = null,
    @SerialName("menstruation") val menstruation: MenstruationData? = null,
    @SerialName("metadata") val metadata: ExportMetadata,
    @SerialName("provenance") val provenance: ExportProvenance? = null,
) {
    /**
     * True when at least one section carries a value.
     *
     * [com.healthconnect.export.repository.HealthConnectRepository.readPeriodInBatch]
     * pre-seeds every day of the requested window, so a day without a single
     * record is still present in the result. Delivering or "confirming" such a
     * day would move the catch-up resume point past data that was never sent.
     */
    fun hasAnyData(): Boolean =
        steps != null ||
            heartRate != null ||
            sleep != null ||
            calories != null ||
            distance != null ||
            floorsClimbed != null ||
            activeCalories != null ||
            weight != null ||
            bodyFat != null ||
            bloodPressure != null ||
            bloodGlucose != null ||
            oxygenSaturation != null ||
            bodyTemperature != null ||
            respiratoryRate != null ||
            hydration != null ||
            restingHeartRate != null ||
            speed != null ||
            menstruation != null ||
            !exercises.isNullOrEmpty() ||
            !nutrition.isNullOrEmpty()
}

@Serializable
data class StepsData(
    @SerialName("total_steps") val totalSteps: Long,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class HeartRateData(
    @SerialName("avg_bpm") val avgBpm: Double,
    @SerialName("min_bpm") val minBpm: Int,
    @SerialName("max_bpm") val maxBpm: Int,
    @SerialName("records_count") val recordsCount: Int,
    @SerialName("samples") val samples: List<HeartRateSample> = emptyList(),
)

@Serializable
data class SleepData(
    @SerialName("total_duration_minutes") val totalDurationMinutes: Long,
    @SerialName("sleep_stages") val sleepStages: Map<String, Long>,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class CaloriesData(
    @SerialName("total_calories_kcal") val totalCalories: Double,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class DistanceData(
    @SerialName("total_distance_meters") val totalDistanceMeters: Double,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class FloorsClimbedData(
    @SerialName("total_floors") val totalFloors: Double,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class ActiveCaloriesData(
    @SerialName("total_calories_kcal") val totalCalories: Double,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class WeightData(
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("records_count") val recordsCount: Int,
    @SerialName("readings") val readings: List<WeightReading> = emptyList(),
)

@Serializable
data class BodyFatData(
    @SerialName("percentage") val percentage: Double,
    @SerialName("records_count") val recordsCount: Int,
    @SerialName("readings") val readings: List<BodyFatReading> = emptyList(),
)

@Serializable
data class BloodPressureData(
    @SerialName("systolic_mmhg") val systolicMmHg: Double,
    @SerialName("diastolic_mmhg") val diastolicMmHg: Double,
    @SerialName("body_position") val bodyPosition: String? = null,
    @SerialName("records_count") val recordsCount: Int,
    @SerialName("readings") val readings: List<BloodPressureReading> = emptyList(),
)

@Serializable
data class BloodGlucoseData(
    @SerialName("level_mmol_per_l") val level: Double,
    @SerialName("specimen_source") val specimenSource: String? = null,
    @SerialName("meal_type") val mealType: String? = null,
    @SerialName("records_count") val recordsCount: Int,
    @SerialName("readings") val readings: List<BloodGlucoseReading> = emptyList(),
)

@Serializable
data class OxygenSaturationData(
    @SerialName("percentage") val percentage: Double,
    @SerialName("records_count") val recordsCount: Int,
    @SerialName("readings") val readings: List<OxygenSaturationReading> = emptyList(),
)

@Serializable
data class BodyTemperatureData(
    @SerialName("temperature_celsius") val temperatureCelsius: Double,
    @SerialName("measurement_location") val measurementLocation: String? = null,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class RespiratoryRateData(
    @SerialName("rate") val rate: Double,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class HydrationData(
    @SerialName("total_volume_liters") val totalVolumeLiters: Double,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class RestingHeartRateData(
    @SerialName("avg_bpm") val avgBpm: Double,
    @SerialName("min_bpm") val minBpm: Long,
    @SerialName("max_bpm") val maxBpm: Long,
    @SerialName("records_count") val recordsCount: Int,
)

@Serializable
data class ExerciseData(
    @SerialName("exercise_type") val exerciseType: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("duration_minutes") val durationMinutes: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("notes") val notes: String? = null,
)

@Serializable
data class NutritionData(
    @SerialName("name") val name: String? = null,
    @SerialName("meal_type") val mealType: String? = null,
    @SerialName("energy_kcal") val energyKcal: Double? = null,
    @SerialName("protein_g") val proteinG: Double? = null,
    @SerialName("total_carbohydrate_g") val totalCarbohydrateG: Double? = null,
    @SerialName("fat_g") val fatG: Double? = null,
    @SerialName("dietary_fiber_g") val dietaryFiberG: Double? = null,
    @SerialName("sugar_g") val sugarG: Double? = null,
    @SerialName("saturated_fat_g") val saturatedFatG: Double? = null,
    @SerialName("trans_fat_g") val transFatG: Double? = null,
    @SerialName("cholesterol_mg") val cholesterolMg: Double? = null,
    @SerialName("sodium_mg") val sodiumMg: Double? = null,
    @SerialName("potassium_mg") val potassiumMg: Double? = null,
    @SerialName("caffeine_mg") val caffeineMg: Double? = null,
    @SerialName("calcium_mg") val calciumMg: Double? = null,
    @SerialName("iron_mg") val ironMg: Double? = null,
    @SerialName("magnesium_mg") val magnesiumMg: Double? = null,
    @SerialName("vitamin_c_mg") val vitaminCMg: Double? = null,
    @SerialName("vitamin_d_mcg") val vitaminDMcg: Double? = null,
)

@Serializable
data class MenstruationData(
    @SerialName("flow_type") val flowType: String? = null,
    @SerialName("time") val time: String,
)

@Serializable
data class SpeedData(
    @SerialName("avg_speed_meters_per_second") val avgSpeedMetersPerSecond: Double? = null,
    @SerialName("records_count") val recordsCount: Int = 0,
)

@Serializable
data class ExportProvenance(
    @SerialName("selection_mode") val selectionMode: String,
    @SerialName("selected_source") val selectedSource: String? = null,
    @SerialName("sources_by_type") val sourcesByType: Map<String, List<String>> = emptyMap(),
)

@Serializable
data class ExportMetadata(
    @SerialName("app_version") val appVersion: String,
    @SerialName("export_timestamp") val exportTimestamp: String,
    @SerialName("timezone") val timezone: String,
    @SerialName("source_device") val sourceDevice: String? = null,
)

// ===== Export Configuration =====

@Serializable
enum class ExportFrequency(
    val displayName: String,
    val hours: Long,
) {
    MANUAL("Manual", 0),
    DAILY("Daily", 24),
    WEEKLY("Weekly", 168),
}

/**
 * Output format for exported health data files.
 */
@Serializable
enum class ExportFormat(
    val displayName: String,
) {
    JSON("JSON"),
    CSV("CSV"),
}

/**
 * Preset period selection for the export date range.
 *
 * When [LAST_7_DAYS] or [LAST_30_DAYS] is active, the actual start/end dates are
 * recomputed from the current date on every launch and before each export, so the
 * window keeps sliding instead of freezing on the dates first picked.
 */
@Serializable
enum class DateRangePreset(
    val displayName: String,
) {
    NONE("Custom"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    ;

    /**
     * Computes the [start, end] date window for this preset relative to [referenceDate].
     * 7 days → last 7 days including today; 30 days → last 30 days including today.
     * [NONE] is a custom range and has no meaning here — callers must handle it separately.
     */
    fun calcRange(referenceDate: LocalDate): Pair<LocalDate, LocalDate> =
        when (this) {
            LAST_7_DAYS -> referenceDate.minusDays(6) to referenceDate
            LAST_30_DAYS -> referenceDate.minusDays(29) to referenceDate
            NONE -> referenceDate to referenceDate
        }
}

@Serializable
data class ExportConfig(
    val enabledTypes: Set<HealthDataType>,
    val frequency: ExportFrequency,
    val autoSyncDrive: Boolean,
    val webhookUrl: String = "",
    val webhookAuthToken: String = "",
    val autoSendWebhook: Boolean = false,
    val autoSendWebhookEvery2Hours: Boolean = false,
    val outputDirectory: String = "HealthConnectExport",
    val selectedSourcePackage: String? = null,
    val exportFormat: ExportFormat = ExportFormat.JSON,
    val scheduleHour: Int? = null,
)

@Serializable
enum class HealthDataType(
    val displayName: String,
) {
    STEPS("Steps"),
    HEART_RATE("Heart Rate"),
    SLEEP("Sleep"),
    CALORIES("Calories"),
    DISTANCE("Distance"),
    FLOORS_CLIMBED("Floors Climbed"),
    ACTIVE_CALORIES("Active Calories"),
    WEIGHT("Weight"),
    BODY_FAT("Body Fat"),
    BLOOD_PRESSURE("Blood Pressure"),
    BLOOD_GLUCOSE("Blood Glucose"),
    OXYGEN_SATURATION("Oxygen Saturation"),
    BODY_TEMPERATURE("Body Temperature"),
    RESPIRATORY_RATE("Respiratory Rate"),
    HYDRATION("Hydration"),
    RESTING_HEART_RATE("Resting Heart Rate"),
    EXERCISE("Exercise"),
    NUTRITION("Nutrition"),
    SPEED("Speed"),
    MENSTRUATION("Menstruation"),
}

// ===== Health Connect Int constant → Human-readable string mappers =====

fun bodyPositionToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        0 -> "Unknown"
        1 -> "Standing"
        2 -> "Sitting"
        3 -> "Lying down"
        4 -> "Reclining"
        else -> "Other ($value)"
    }
}

fun specimenSourceToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        0 -> "Unknown"
        1 -> "Interstitial fluid"
        2 -> "Capillary blood"
        3 -> "Plasma"
        4 -> "Serum"
        5 -> "Whole blood"
        else -> "Other ($value)"
    }
}

fun mealTypeToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        0 -> "Unknown"
        1 -> "Fasting"
        2 -> "Before meal"
        3 -> "After meal"
        4 -> "General"
        else -> "Other ($value)"
    }
}

fun sleepStageToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        0 -> "Unknown"
        1 -> "Awake"
        2 -> "Deep sleep"
        3 -> "Light sleep"
        4 -> "REM sleep"
        5 -> "Out of bed"
        else -> "Other ($value)"
    }
}

fun measurementLocationToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        0 -> "Unknown"
        1 -> "Axillary"
        2 -> "Body"
        3 -> "Ear"
        4 -> "Finger"
        5 -> "Forehead"
        6 -> "Mouth"
        7 -> "Rectal"
        8 -> "Toe"
        9 -> "Tympanic"
        10 -> "Wrist"
        11 -> "Temporal artery"
        else -> "Other ($value)"
    }
}

fun menstruationFlowToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        0 -> "Unknown"
        1 -> "Light"
        2 -> "Medium"
        3 -> "Heavy"
        else -> "Other ($value)"
    }
}

fun nutritionMealTypeToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        0 -> "Unknown"
        1 -> "Breakfast"
        2 -> "Lunch"
        3 -> "Dinner"
        4 -> "Snack"
        else -> "Other ($value)"
    }
}

/**
 * Maps a Health Connect exercise type id to a human-readable name.
 *
 * The ids are **sparse** constants of `ExerciseSessionRecord` (0, 2, 4, 5, 8 … 83)
 * and must follow the platform list one-to-one — a dense 0…N table shifted every
 * name after the first gap (a `56 Running` session was exported as "Kite skiing").
 * Unknown ids keep their number so a newer Health Connect cannot turn a real
 * workout into a bare "Unknown".
 */
fun exerciseTypeToString(value: Int?): String? {
    if (value == null) return null
    return when (value) {
        // ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        0 -> "Other workout"
        // EXERCISE_TYPE_BADMINTON
        2 -> "Badminton"
        // EXERCISE_TYPE_BASEBALL
        4 -> "Baseball"
        // EXERCISE_TYPE_BASKETBALL
        5 -> "Basketball"
        // EXERCISE_TYPE_BIKING
        8 -> "Biking"
        // EXERCISE_TYPE_BIKING_STATIONARY
        9 -> "Stationary biking"
        // EXERCISE_TYPE_BOOT_CAMP
        10 -> "Boot camp"
        // EXERCISE_TYPE_BOXING
        11 -> "Boxing"
        // EXERCISE_TYPE_CALISTHENICS
        13 -> "Calisthenics"
        // EXERCISE_TYPE_CRICKET
        14 -> "Cricket"
        // EXERCISE_TYPE_DANCING
        16 -> "Dancing"
        // EXERCISE_TYPE_ELLIPTICAL
        25 -> "Elliptical"
        // EXERCISE_TYPE_EXERCISE_CLASS
        26 -> "Exercise class"
        // EXERCISE_TYPE_FENCING
        27 -> "Fencing"
        // EXERCISE_TYPE_FOOTBALL_AMERICAN
        28 -> "American football"
        // EXERCISE_TYPE_FOOTBALL_AUSTRALIAN
        29 -> "Australian football"
        // EXERCISE_TYPE_FRISBEE_DISC
        31 -> "Frisbee disc"
        // EXERCISE_TYPE_GOLF
        32 -> "Golf"
        // EXERCISE_TYPE_GUIDED_BREATHING
        33 -> "Guided breathing"
        // EXERCISE_TYPE_GYMNASTICS
        34 -> "Gymnastics"
        // EXERCISE_TYPE_HANDBALL
        35 -> "Handball"
        // EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
        36 -> "HIIT"
        // EXERCISE_TYPE_HIKING
        37 -> "Hiking"
        // EXERCISE_TYPE_ICE_HOCKEY
        38 -> "Ice hockey"
        // EXERCISE_TYPE_ICE_SKATING
        39 -> "Ice skating"
        // EXERCISE_TYPE_MARTIAL_ARTS
        44 -> "Martial arts"
        // EXERCISE_TYPE_PADDLING
        46 -> "Paddling"
        // EXERCISE_TYPE_PARAGLIDING
        47 -> "Paragliding"
        // EXERCISE_TYPE_PILATES
        48 -> "Pilates"
        // EXERCISE_TYPE_RACQUETBALL
        50 -> "Racquetball"
        // EXERCISE_TYPE_ROCK_CLIMBING
        51 -> "Rock climbing"
        // EXERCISE_TYPE_ROLLER_HOCKEY
        52 -> "Roller hockey"
        // EXERCISE_TYPE_ROWING
        53 -> "Rowing"
        // EXERCISE_TYPE_ROWING_MACHINE
        54 -> "Rowing machine"
        // EXERCISE_TYPE_RUGBY
        55 -> "Rugby"
        // EXERCISE_TYPE_RUNNING
        56 -> "Running"
        // EXERCISE_TYPE_RUNNING_TREADMILL
        57 -> "Treadmill running"
        // EXERCISE_TYPE_SAILING
        58 -> "Sailing"
        // EXERCISE_TYPE_SCUBA_DIVING
        59 -> "Scuba diving"
        // EXERCISE_TYPE_SKATING
        60 -> "Skating"
        // EXERCISE_TYPE_SKIING
        61 -> "Skiing"
        // EXERCISE_TYPE_SNOWBOARDING
        62 -> "Snowboarding"
        // EXERCISE_TYPE_SNOWSHOEING
        63 -> "Snowshoeing"
        // EXERCISE_TYPE_SOCCER
        64 -> "Soccer"
        // EXERCISE_TYPE_SOFTBALL
        65 -> "Softball"
        // EXERCISE_TYPE_SQUASH
        66 -> "Squash"
        // EXERCISE_TYPE_STAIR_CLIMBING
        68 -> "Stair climbing"
        // EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
        69 -> "Stair climbing machine"
        // EXERCISE_TYPE_STRENGTH_TRAINING
        70 -> "Strength training"
        // EXERCISE_TYPE_STRETCHING
        71 -> "Stretching"
        // EXERCISE_TYPE_SURFING
        72 -> "Surfing"
        // EXERCISE_TYPE_SWIMMING_OPEN_WATER
        73 -> "Open water swimming"
        // EXERCISE_TYPE_SWIMMING_POOL
        74 -> "Pool swimming"
        // EXERCISE_TYPE_TABLE_TENNIS
        75 -> "Table tennis"
        // EXERCISE_TYPE_TENNIS
        76 -> "Tennis"
        // EXERCISE_TYPE_VOLLEYBALL
        78 -> "Volleyball"
        // EXERCISE_TYPE_WALKING
        79 -> "Walking"
        // EXERCISE_TYPE_WATER_POLO
        80 -> "Water polo"
        // EXERCISE_TYPE_WEIGHTLIFTING
        81 -> "Weightlifting"
        // EXERCISE_TYPE_WHEELCHAIR
        82 -> "Wheelchair"
        // EXERCISE_TYPE_YOGA
        83 -> "Yoga"
        else -> "Other ($value)"
    }
}

/**
 * Map of known health data source packages to human-readable display names.
 * Used for UI selection and filtering.
 */
val KNOWN_SOURCE_PACKAGES: Map<String, String> =
    mapOf(
        "com.mi.health" to "Xiaomi Mi Fitness",
        "com.xiaomi.hm.health" to "Xiaomi Wear",
        "com.google.android.apps.fitness" to "Google Fit",
        "com.samsung.samsunghealth" to "Samsung Health",
        "com.samsung.android.wearable.health" to "Samsung Health (Wear)",
        "com.fitbit.FitbitMobile" to "Fitbit",
        "com.mobvoi.companion.at" to "Mobvoi / TicWatch",
        "com.huawei.health" to "Huawei Health",
        "com.hmdm.wearable.health" to "Nokia Health",
        "com.sec.android.app.shealth" to "Samsung S Health",
        "com.htc.fitness" to "HTC Fitness",
        "com.sonymobile.advancedwidget.health" to "Sony Health",
        "com.google.android.wearable.app" to "Google Wear OS",
    )

/**
 * Returns a human-readable name for a source package, or the raw package name if unknown.
 */
fun sourceDisplayName(packageName: String): String = KNOWN_SOURCE_PACKAGES[packageName] ?: packageName

// ===== Export Summary =====

/**
 * Dashboard summary computed after export.
 */
data class ExportSummary(
    val totalSteps: Long = 0,
    val avgHeartRate: Double = 0.0,
    val totalCalories: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val avgSleepMinutes: Long = 0,
    val totalActiveCalories: Double = 0.0,
    val daysCount: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
)

// ===== Helpers =====

fun Instant.toDateString(): String = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(this)

fun Instant.toDateTimeString(): String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(this)
