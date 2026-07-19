package com.takji.metronow

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.takji.metronow.data.local.MetroSettingsStore
import com.takji.metronow.data.local.StationCatalog
import com.takji.metronow.data.remote.MetroApiFactory
import com.takji.metronow.data.remote.MetroArrivalMapper
import com.takji.metronow.data.repository.MetroRepository
import com.takji.metronow.data.repository.SeoulMetroRemoteDataSource
import com.takji.metronow.widget.WidgetRefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MetroNowApplication : Application() {
    val gson: Gson by lazy { GsonBuilder().disableHtmlEscaping().create() }
    val settingsStore: MetroSettingsStore by lazy { MetroSettingsStore(this, gson) }
    val stationCatalog: StationCatalog by lazy { StationCatalog(this, gson) }
    val repository: MetroRepository by lazy {
        MetroRepository(
            remote = SeoulMetroRemoteDataSource(MetroApiFactory.create(gson)),
            mapper = MetroArrivalMapper(),
        )
    }
    val widgetScheduler: WidgetRefreshScheduler by lazy { WidgetRefreshScheduler(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            settingsStore.settingsFlow
                .map { it.widgetRefreshMinutes }
                .distinctUntilChanged()
                .collect(widgetScheduler::schedulePeriodic)
        }
    }
}
