package com.takji.metronow.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class WidgetRefreshScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodic(requestedMinutes: Int) {
        if (requestedMinutes <= 0) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }
        val effectiveMinutes = requestedMinutes.coerceAtLeast(15)
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(effectiveMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun refreshNow(appWidgetId: Int? = null) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(WidgetRefreshWorker.KEY_WIDGET_ID to (appWidgetId ?: -1)))
            .build()
        workManager.enqueue(request)
    }

    private fun networkConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val PERIODIC_WORK_NAME = "metro_now_widget_periodic_refresh"
    }
}
