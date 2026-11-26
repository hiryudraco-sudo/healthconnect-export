package com.healthconnect.export.viewmodel

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleManagerTest {
    @Test
    fun observesScheduleStatusWithinProvidedScope() =
        runTest {
            val source = MutableStateFlow<ScheduleStatus>(ScheduleStatus.NotScheduled)
            var state = ExportUiState()
            val manager = ScheduleManager(mock<Application>(), ScheduleStatusSource { source }) { update ->
                state = state.update()
            }

            manager.observeStatus(backgroundScope)
            runCurrent()
            assertEquals(ScheduleStatus.NotScheduled, state.scheduleStatus)

            val scheduled = ScheduleStatus.Scheduled("Next eligible run: 25/11/2026 05:15")
            source.value = scheduled
            runCurrent()
            assertEquals(scheduled, state.scheduleStatus)

            source.value = ScheduleStatus.Running
            runCurrent()
            assertEquals(ScheduleStatus.Running, state.scheduleStatus)

            source.value = ScheduleStatus.NotScheduled
            runCurrent()
            assertEquals(ScheduleStatus.NotScheduled, state.scheduleStatus)
        }
}
