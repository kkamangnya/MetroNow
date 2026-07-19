package com.takji.metronow.domain.model

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class WidgetAppearance(
    val backgroundOpacity: Float = 0.90f,
    val showProgress: Boolean = true,
    val showSecondTrain: Boolean = true,
    val showUpdateTime: Boolean = true,
)

data class MetroSettings(
    val apiKey: String = "",
    val onboardingComplete: Boolean = false,
    val liveAutoRefresh: Boolean = true,
    val appRefreshSeconds: Int = 30,
    val widgetRefreshMinutes: Int = 15,
    val theme: AppTheme = AppTheme.SYSTEM,
    val appearance: WidgetAppearance = WidgetAppearance(),
    val presets: List<MetroPreset> = emptyList(),
    val selectedPresetId: String? = null,
    val widgetBindings: Map<Int, String> = emptyMap(),
    val snapshots: Map<String, ArrivalSnapshot> = emptyMap(),
) {
    val selectedPreset: MetroPreset?
        get() = presets.firstOrNull { it.id == selectedPresetId } ?: presets.firstOrNull()

    fun maskedApiKey(): String {
        val key = apiKey.trim()
        return when {
            key.isEmpty() -> "설정되지 않음"
            key.length <= 6 -> "••••••"
            else -> "${key.take(4)}••••••••${key.takeLast(3)}"
        }
    }
}
