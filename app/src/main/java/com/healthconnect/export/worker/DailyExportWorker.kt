package com.healthconnect.export.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.healthconnect.export.data.ExportConfig
import com.healthconnect.export.data.ExportFrequency
import com.healthconnect.export.repository.GoogleDriveRepository
import com.healthconnect.export.repository.HealthConnectRepository
import com.healthconnect.export.repository.LocalExportRepository
import com.healthconnect.export.repository.WebhookRepository
import com.healthconnect.export.repository.WebhookResult
import com.healthconnect.export.util.DeliveryLog
import com.healthconnect.export.util.ExportForegroundNotifier
import com.healthconnect.export.util.ExportSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Background worker that exports the last seven completed days of health data and optionally syncs to Drive
 */
class DailyExportWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "daily_health_export"
        const val KEY_CONFIG = "export_config"
        private const val TAG = "DailyExportWorker"
        private const val TRIGGER = "daily"
        private val json = Json { ignoreUnknownKeys = true }

        fun schedule(
            context: Context,
            config: ExportConfig,
            reanchor: Boolean = false,
        ) {
            // The every-2-hours webhook has its own switch: switching the export
            // frequency to Manual must not silently disable it.
            scheduleEvery2HoursWebhook(context, config)

            if (config.frequency == ExportFrequency.MANUAL) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val inputData =
                workDataOf(
                    KEY_CONFIG to json.encodeToString(config),
                )

            val requestBuilder =
                PeriodicWorkRequestBuilder<DailyExportWorker>(
                    config.frequency.hours,
                    TimeUnit.HOURS,
                )
                    .setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)

            // Anchor only a fresh/replaced schedule to the requested clock hour.
            // KEEP preserves the existing anchor on app opens and watchdog checks.
            // WorkManager does not promise exact execution time.
            config.scheduleHour?.let { hour ->
                requestBuilder.setInitialDelay(
                    ScheduleTiming.nextDelayMillis(ZonedDateTime.now(), hour.coerceIn(0, 23)),
                    TimeUnit.MILLISECONDS,
                )
            }

            val request = requestBuilder.build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                if (reanchor) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Every2HoursWebhookWorker.cancel(context)
        }

        /** Cancels only the periodic export, leaving the every-2-hours webhook job alone. */
        fun cancelDaily(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Schedules or cancels the every-2-hours webhook periodic work.
         */
        fun scheduleEvery2HoursWebhook(
            context: Context,
            config: ExportConfig,
        ) {
            if (config.autoSendWebhookEvery2Hours && config.webhookUrl.isNotBlank()) {
                Every2HoursWebhookWorker.schedule(context, config)
            } else {
                Every2HoursWebhookWorker.cancel(context)
            }
        }

        fun cancelEvery2HoursWebhook(context: Context) {
            Every2HoursWebhookWorker.cancel(context)
        }

        fun getStatus(context: Context) = WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME)
    }

    private val healthRepo = HealthConnectRepository(applicationContext)
    private val localRepo = LocalExportRepository(applicationContext)
    private val driveRepo = GoogleDriveRepository(applicationContext)
    private val webhookRepo = WebhookRepository(applicationContext)

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val config = resolveConfig()
            var stage = "start"
            try {
                if (config.frequency == ExportFrequency.MANUAL) return@withContext Result.success()
                DeliveryLog.recordStage(applicationContext, TRIGGER, "started; attempt=$runAttemptCount")
                DeliveryLog.recordStage(applicationContext, TRIGGER, healthRepo.backgroundReadDiagnostic())
                stage = "Health Connect"
                // Best-effort only: Android 12+ refuses the promotion while the app
                // runs in the background, and the background read permission is what
                // makes the read legal there.
                ExportForegroundNotifier.promoteToForeground(this@DailyExportWorker)

                // Export completed days only. This avoids repeatedly sending a partial
                // current-day record from a periodic worker.
                val endDate = LocalDate.now().minusDays(1)
                val startDate =
                    when (config.frequency) {
                        ExportFrequency.DAILY -> endDate.minusDays(6)
                        ExportFrequency.WEEKLY -> endDate.minusDays(6)
                        ExportFrequency.MANUAL -> endDate
                    }

                // Read health data for the period (batch mode: 1 API call per type)
                val records =
                    healthRepo.readPeriodInBatch(
                        startDate = startDate,
                        endDate = endDate,
                        types = config.enabledTypes,
                        selectedSourcePackage = config.selectedSourcePackage,
                    )

                // The repository pre-seeds every day of the window, so "empty" means
                // "no day carries a single record", not "empty list".
                val recordsWithData = records.filter { it.hasAnyData() }
                if (recordsWithData.isEmpty()) {
                    Log.w(TAG, "doWork: no health data returned for period $startDate..$endDate")
                    DeliveryLog.recordIdle(applicationContext, TRIGGER, "no data $startDate..$endDate")
                    return@withContext Result.success()
                }

                DeliveryLog.recordStage(applicationContext, TRIGGER, "Health Connect read complete")
                stage = "local export"
                val files = localRepo.saveRecords(recordsWithData, config)
                DeliveryLog.recordStage(applicationContext, TRIGGER, "local export complete: ${files.size} file(s)")

                // Sync to Drive if enabled and signed in
                stage = "Drive"
                if (config.autoSyncDrive && !driveRepo.isSignedIn()) {
                    return@withContext DeliveryOutcome.failure(applicationContext, TRIGGER, "Drive not signed in", retry = false)
                }
                if (config.autoSyncDrive) {
                    val driveResults =
                        files.map { file ->
                            driveRepo.uploadFile(file, "HealthConnectExport/${file.name}")
                        }
                    if (driveResults.any { it == null }) {
                        return@withContext DeliveryOutcome.failure(
                            applicationContext,
                            TRIGGER,
                            "Drive upload failed: ${driveResults.count { it == null }} of ${files.size} file(s); see Android log",
                            retry = true,
                        )
                    }
                }

                if (config.autoSyncDrive) DeliveryLog.recordStage(applicationContext, TRIGGER, "Drive upload complete")
                stage = "webhook"
                // Send to webhook if enabled
                if (config.autoSendWebhook && config.webhookUrl.isNotBlank()) {
                    when (val result = webhookRepo.sendRecords(config.webhookUrl, recordsWithData, config.webhookAuthToken)) {
                        is WebhookResult.Success ->
                            DeliveryOutcome.delivered(applicationContext, TRIGGER, recordsWithData.map { it.date }, result.statusCode)
                        is WebhookResult.Error ->
                            DeliveryOutcome.failure(
                                applicationContext,
                                TRIGGER,
                                "HTTP ${result.statusCode}: ${result.message}",
                                retry = result.isRetryable,
                            )
                    }
                } else {
                    val destination = if (config.autoSyncDrive) "local + Drive" else "local"
                    DeliveryLog.recordExportSuccess(applicationContext, TRIGGER, "$destination: ${files.size} file(s)")
                    Result.success()
                }
            } catch (e: SecurityException) {
                // Retry only when the background permission is granted; otherwise the
                // run would fail identically forever and never deliver anything.
                DeliveryOutcome.permissionFailure(applicationContext, TRIGGER, healthRepo, e)
            } catch (e: IllegalStateException) {
                DeliveryOutcome.failure(applicationContext, TRIGGER, "$stage: ${e.javaClass.simpleName}", retry = true)
            } catch (e: Exception) {
                DeliveryOutcome.failure(applicationContext, TRIGGER, "$stage: ${e.javaClass.simpleName}", retry = true)
            }
        }

    private fun resolveConfig(): ExportConfig = ExportSettings.loadConfig(applicationContext)
}
