package com.takji.metronow.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.action.ActionParameters
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.takji.metronow.MainActivity
import com.takji.metronow.MetroNowApplication
import com.takji.metronow.domain.model.MetroPreset
import com.takji.metronow.presentation.theme.MetroNowTheme
import com.takji.metronow.presentation.theme.MetroNowSystemBars
import kotlinx.coroutines.launch

class MetroNowWidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            intent?.getIntExtra(ACTION_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val canceled = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_CANCELED, canceled)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as MetroNowApplication
        setContent {
            val settings by app.settingsStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.takji.metronow.domain.model.MetroSettings(),
            )
            MetroNowTheme(settings.theme) {
                MetroNowSystemBars(settings.theme)
                WidgetConfigScreen(
                    presets = settings.presets,
                    initialPresetId = settings.widgetBindings[appWidgetId],
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    onSave = { presetId -> saveConfiguration(app, presetId) },
                )
            }
        }
    }

    private fun saveConfiguration(app: MetroNowApplication, presetId: String) {
        lifecycleScope.launch {
            app.settingsStore.bindWidget(appWidgetId, presetId)
            val glanceId = GlanceAppWidgetManager(this@MetroNowWidgetConfigurationActivity)
                .getGlanceIdBy(appWidgetId)
            MetroNowWidget().update(this@MetroNowWidgetConfigurationActivity, glanceId)
            app.widgetScheduler.refreshNow(appWidgetId)

            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    companion object {
        private const val ACTION_WIDGET_ID = "metro_now_config_widget_id"
        val WidgetIdActionKey = ActionParameters.Key<Int>(ACTION_WIDGET_ID)
    }
}

@Composable
private fun WidgetConfigScreen(
    presets: List<MetroPreset>,
    initialPresetId: String?,
    onOpenApp: () -> Unit,
    onSave: (String) -> Unit,
) {
    var selectedId by remember(initialPresetId, presets) {
        mutableStateOf(initialPresetId?.takeIf { id -> presets.any { it.id == id } } ?: presets.firstOrNull()?.id)
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
        Text("MetroNow", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text("위젯 설정", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text("이 위젯에서 사용할 프리셋을 선택하세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

            if (presets.isEmpty()) {
            Text("저장된 프리셋이 없습니다. MetroNow 앱에서 먼저 프리셋을 만들어주세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenApp) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("MetroNow 열기")
            }
            } else {
            LazyColumn(Modifier.weight(1f)) {
                items(presets, key = { it.id }) { preset ->
                    val selected = preset.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) Color(preset.line.colorHex).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { selectedId = preset.id }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(38.dp).background(Color(preset.line.colorHex), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(preset.line.number, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(preset.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${preset.line.displayName} ${preset.stationDisplayName} · ${preset.direction.label(preset.line)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (selected) Text("선택", color = Color(preset.line.colorHex), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Button(
                onClick = { selectedId?.let(onSave) },
                enabled = selectedId != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
            ) { Text("이 프리셋으로 위젯 추가") }
            }
        }
    }
}
