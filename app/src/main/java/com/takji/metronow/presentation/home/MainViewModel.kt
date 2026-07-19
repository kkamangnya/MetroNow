package com.takji.metronow.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.takji.metronow.MetroNowApplication
import com.takji.metronow.domain.model.AppTheme
import com.takji.metronow.domain.model.ArrivalSnapshot
import com.takji.metronow.domain.model.Direction
import com.takji.metronow.domain.model.MetroLine
import com.takji.metronow.domain.model.MetroPreset
import com.takji.metronow.domain.model.MetroSettings
import com.takji.metronow.domain.model.Station
import com.takji.metronow.domain.model.WidgetAppearance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: MetroSettings = MetroSettings(),
    val selectedPreset: MetroPreset? = null,
    val snapshot: ArrivalSnapshot? = null,
)

class MainViewModel(
    private val app: MetroNowApplication,
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = app.settingsStore.settingsFlow
        .map { settings ->
            val preset = settings.selectedPreset
            MainUiState(
                settings = settings,
                selectedPreset = preset,
                snapshot = preset?.let { settings.snapshots[it.id] },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    private var autoRefreshJob: Job? = null

    fun setForegroundActive(active: Boolean) {
        autoRefreshJob?.cancel()
        if (!active) return
        autoRefreshJob = viewModelScope.launch {
            refreshSelected()
            while (isActive) {
                val settings = app.settingsStore.current()
                if (!settings.liveAutoRefresh) break
                delay(settings.appRefreshSeconds.coerceIn(15, 30) * 1_000L)
                refreshSelected()
            }
        }
    }

    fun refreshSelected() {
        viewModelScope.launch {
            val settings = app.settingsStore.current()
            settings.selectedPreset?.let { refresh(it, settings.apiKey) }
        }
    }

    fun refreshPreset(preset: MetroPreset) {
        viewModelScope.launch {
            val settings = app.settingsStore.current()
            refresh(preset, settings.apiKey)
        }
    }

    private suspend fun refresh(preset: MetroPreset, apiKey: String) {
        app.settingsStore.markSnapshotLoading(preset.id)
        val snapshot = app.repository.fetchSnapshot(preset, apiKey)
        app.settingsStore.saveSnapshot(snapshot)
        runCatching { com.takji.metronow.widget.MetroNowWidget().updateAll(app) }
    }

    fun completeOnboarding(apiKey: String, line: MetroLine, station: Station, direction: Direction) {
        viewModelScope.launch {
            val preset = MetroPreset(
                name = "기본",
                line = line,
                stationId = station.id,
                stationDisplayName = station.displayName,
                stationApiName = station.apiName,
                direction = direction,
            )
            app.settingsStore.completeOnboarding(apiKey, preset)
            refresh(preset, apiKey)
        }
    }

    fun upsertPreset(preset: MetroPreset) {
        viewModelScope.launch {
            app.settingsStore.upsertPreset(preset)
            app.settingsStore.selectPreset(preset.id)
            refreshPreset(preset)
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch { app.settingsStore.deletePreset(id) }
    }

    fun selectPreset(id: String) {
        viewModelScope.launch { app.settingsStore.selectPreset(id) }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            app.settingsStore.saveApiKey(key)
            refreshSelected()
        }
    }

    fun saveRefreshSettings(live: Boolean, appSeconds: Int, widgetMinutes: Int) {
        viewModelScope.launch {
            app.settingsStore.saveRefreshSettings(live, appSeconds, widgetMinutes)
            setForegroundActive(live)
        }
    }

    fun saveTheme(theme: AppTheme) {
        viewModelScope.launch { app.settingsStore.saveTheme(theme) }
    }

    fun saveAppearance(appearance: WidgetAppearance) {
        viewModelScope.launch {
            app.settingsStore.saveAppearance(appearance)
            runCatching { com.takji.metronow.widget.MetroNowWidget().updateAll(app) }
        }
    }

    class Factory(private val app: MetroNowApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(app) as T
    }
}
