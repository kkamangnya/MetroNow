package com.takji.metronow.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.takji.metronow.MetroNowApplication

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MetroNowApplication
        val settings = app.settingsStore.current()
        val requestedWidgetId = inputData.getInt(KEY_WIDGET_ID, -1)
        val presetIds = if (requestedWidgetId >= 0) {
            listOfNotNull(settings.widgetBindings[requestedWidgetId])
        } else {
            settings.widgetBindings.values.distinct()
        }
        if (presetIds.isEmpty()) {
            MetroNowWidget().updateAll(app)
            return Result.success()
        }

        presetIds.forEach { presetId ->
            val preset = settings.presets.firstOrNull { it.id == presetId } ?: return@forEach
            val snapshot = app.repository.fetchSnapshot(preset, settings.apiKey)
            app.settingsStore.saveSnapshot(snapshot)
        }
        MetroNowWidget().updateAll(app)
        return Result.success()
    }

    companion object {
        const val KEY_WIDGET_ID = "app_widget_id"
    }
}
