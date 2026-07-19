package com.takji.metronow.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.takji.metronow.domain.model.AppTheme
import com.takji.metronow.domain.model.ArrivalSnapshot
import com.takji.metronow.domain.model.MetroPreset
import com.takji.metronow.domain.model.MetroSettings
import com.takji.metronow.domain.model.WidgetAppearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.metroNowDataStore by preferencesDataStore(name = "metro_now_settings")

class MetroSettingsStore(
    private val context: Context,
    private val gson: Gson,
) {
    val settingsFlow: Flow<MetroSettings> = context.metroNowDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::fromPreferences)

    suspend fun current(): MetroSettings = settingsFlow.first()

    suspend fun saveApiKey(apiKey: String) {
        context.metroNowDataStore.edit { it[Keys.API_KEY] = apiKey.trim() }
    }

    suspend fun completeOnboarding(apiKey: String, preset: MetroPreset) {
        context.metroNowDataStore.edit { preferences ->
            preferences[Keys.API_KEY] = apiKey.trim()
            preferences[Keys.ONBOARDING_COMPLETE] = true
            preferences[Keys.PRESETS] = gson.toJson(listOf(preset))
            preferences[Keys.SELECTED_PRESET] = preset.id
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.metroNowDataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun upsertPreset(preset: MetroPreset) {
        context.metroNowDataStore.edit { preferences ->
            val current = presets(preferences).toMutableList()
            val index = current.indexOfFirst { it.id == preset.id }
            if (index >= 0) current[index] = preset else current.add(preset)
            preferences[Keys.PRESETS] = gson.toJson(current)
            if (preferences[Keys.SELECTED_PRESET].isNullOrBlank()) {
                preferences[Keys.SELECTED_PRESET] = preset.id
            }
        }
    }

    suspend fun deletePreset(presetId: String) {
        context.metroNowDataStore.edit { preferences ->
            val remaining = presets(preferences).filterNot { it.id == presetId }
            val bindings = widgetBindings(preferences).filterValues { it != presetId }
            val snapshots = snapshots(preferences).toMutableMap().apply { remove(presetId) }
            preferences[Keys.PRESETS] = gson.toJson(remaining)
            preferences[Keys.WIDGET_BINDINGS] = gson.toJson(bindings)
            preferences[Keys.SNAPSHOTS] = gson.toJson(snapshots)
            if (preferences[Keys.SELECTED_PRESET] == presetId) {
                remaining.firstOrNull()?.id?.let { preferences[Keys.SELECTED_PRESET] = it }
                    ?: preferences.remove(Keys.SELECTED_PRESET)
            }
        }
    }

    suspend fun selectPreset(presetId: String) {
        context.metroNowDataStore.edit { it[Keys.SELECTED_PRESET] = presetId }
    }

    suspend fun saveRefreshSettings(live: Boolean, appSeconds: Int, widgetMinutes: Int) {
        context.metroNowDataStore.edit {
            it[Keys.LIVE_AUTO_REFRESH] = live
            it[Keys.APP_REFRESH_SECONDS] = appSeconds.coerceIn(15, 30)
            it[Keys.WIDGET_REFRESH_MINUTES] = widgetMinutes
        }
    }

    suspend fun saveTheme(theme: AppTheme) {
        context.metroNowDataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun saveAppearance(appearance: WidgetAppearance) {
        context.metroNowDataStore.edit {
            it[Keys.BACKGROUND_OPACITY] = appearance.backgroundOpacity.coerceIn(0.65f, 1f)
            it[Keys.SHOW_PROGRESS] = appearance.showProgress
            it[Keys.SHOW_SECOND_TRAIN] = appearance.showSecondTrain
            it[Keys.SHOW_UPDATE_TIME] = appearance.showUpdateTime
        }
    }

    suspend fun bindWidget(appWidgetId: Int, presetId: String) {
        context.metroNowDataStore.edit { preferences ->
            val bindings = widgetBindings(preferences).toMutableMap()
            bindings[appWidgetId] = presetId
            preferences[Keys.WIDGET_BINDINGS] = gson.toJson(bindings)
        }
    }

    suspend fun removeWidget(appWidgetId: Int) {
        context.metroNowDataStore.edit { preferences ->
            val bindings = widgetBindings(preferences).toMutableMap().apply { remove(appWidgetId) }
            preferences[Keys.WIDGET_BINDINGS] = gson.toJson(bindings)
        }
    }

    suspend fun saveSnapshot(snapshot: ArrivalSnapshot) {
        context.metroNowDataStore.edit { preferences ->
            val values = snapshots(preferences).toMutableMap()
            values[snapshot.presetId] = snapshot
            preferences[Keys.SNAPSHOTS] = gson.toJson(values)
        }
    }

    suspend fun markSnapshotLoading(presetId: String) {
        context.metroNowDataStore.edit { preferences ->
            val values = snapshots(preferences).toMutableMap()
            values[presetId] = (values[presetId] ?: ArrivalSnapshot(presetId = presetId)).copy(
                isLoading = true,
                errorMessage = null,
            )
            preferences[Keys.SNAPSHOTS] = gson.toJson(values)
        }
    }

    private fun fromPreferences(preferences: Preferences): MetroSettings = MetroSettings(
        apiKey = preferences[Keys.API_KEY].orEmpty(),
        onboardingComplete = preferences[Keys.ONBOARDING_COMPLETE] ?: false,
        liveAutoRefresh = preferences[Keys.LIVE_AUTO_REFRESH] ?: true,
        appRefreshSeconds = preferences[Keys.APP_REFRESH_SECONDS] ?: 30,
        widgetRefreshMinutes = preferences[Keys.WIDGET_REFRESH_MINUTES] ?: 15,
        theme = runCatching { AppTheme.valueOf(preferences[Keys.THEME] ?: AppTheme.SYSTEM.name) }
            .getOrDefault(AppTheme.SYSTEM),
        appearance = WidgetAppearance(
            backgroundOpacity = preferences[Keys.BACKGROUND_OPACITY] ?: 0.90f,
            showProgress = preferences[Keys.SHOW_PROGRESS] ?: true,
            showSecondTrain = preferences[Keys.SHOW_SECOND_TRAIN] ?: true,
            showUpdateTime = preferences[Keys.SHOW_UPDATE_TIME] ?: true,
        ),
        presets = presets(preferences),
        selectedPresetId = preferences[Keys.SELECTED_PRESET],
        widgetBindings = widgetBindings(preferences),
        snapshots = snapshots(preferences),
    )

    private fun presets(preferences: Preferences): List<MetroPreset> =
        fromJson(preferences[Keys.PRESETS], object : TypeToken<List<MetroPreset>>() {}.type, emptyList())

    private fun widgetBindings(preferences: Preferences): Map<Int, String> =
        fromJson(preferences[Keys.WIDGET_BINDINGS], object : TypeToken<Map<Int, String>>() {}.type, emptyMap())

    private fun snapshots(preferences: Preferences): Map<String, ArrivalSnapshot> =
        fromJson(preferences[Keys.SNAPSHOTS], object : TypeToken<Map<String, ArrivalSnapshot>>() {}.type, emptyMap())

    private fun <T> fromJson(raw: String?, type: java.lang.reflect.Type, fallback: T): T =
        if (raw.isNullOrBlank()) fallback else runCatching { gson.fromJson<T>(raw, type) }.getOrDefault(fallback)

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LIVE_AUTO_REFRESH = booleanPreferencesKey("live_auto_refresh")
        val APP_REFRESH_SECONDS = intPreferencesKey("app_refresh_seconds")
        val WIDGET_REFRESH_MINUTES = intPreferencesKey("widget_refresh_minutes")
        val THEME = stringPreferencesKey("theme")
        val BACKGROUND_OPACITY = floatPreferencesKey("widget_background_opacity")
        val SHOW_PROGRESS = booleanPreferencesKey("widget_show_progress")
        val SHOW_SECOND_TRAIN = booleanPreferencesKey("widget_show_second_train")
        val SHOW_UPDATE_TIME = booleanPreferencesKey("widget_show_update_time")
        val PRESETS = stringPreferencesKey("presets_json")
        val SELECTED_PRESET = stringPreferencesKey("selected_preset_id")
        val WIDGET_BINDINGS = stringPreferencesKey("widget_bindings_json")
        val SNAPSHOTS = stringPreferencesKey("snapshots_json")
    }
}
