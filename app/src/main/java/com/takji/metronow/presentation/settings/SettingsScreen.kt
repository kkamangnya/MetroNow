package com.takji.metronow.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.takji.metronow.domain.model.AppTheme
import com.takji.metronow.domain.model.MetroSettings
import com.takji.metronow.domain.model.WidgetAppearance
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    settings: MetroSettings,
    onSaveApiKey: (String) -> Unit,
    onSaveRefresh: (Boolean, Int, Int) -> Unit,
    onSaveAppearance: (WidgetAppearance) -> Unit,
    onSaveTheme: (AppTheme) -> Unit,
    onOpenApiGuide: () -> Unit,
) {
    var showKeyDialog by remember { mutableStateOf(false) }
    var liveRefresh by remember { mutableStateOf(settings.liveAutoRefresh) }
    var appSeconds by remember { mutableIntStateOf(settings.appRefreshSeconds) }
    var widgetMinutes by remember { mutableIntStateOf(settings.widgetRefreshMinutes) }
    var opacity by remember { mutableFloatStateOf(settings.appearance.backgroundOpacity) }

    LaunchedEffect(settings) {
        liveRefresh = settings.liveAutoRefresh
        appSeconds = settings.appRefreshSeconds
        widgetMinutes = settings.widgetRefreshMinutes
        opacity = settings.appearance.backgroundOpacity
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(22.dp))
        Text("설정", style = MaterialTheme.typography.headlineLarge)
        Text("실시간 데이터와 위젯 표현을 조절합니다", color = MaterialTheme.colorScheme.onSurfaceVariant)

        SettingsSection("실시간 데이터")
        SettingRow(
            title = "서울시 API 키",
            subtitle = settings.maskedApiKey(),
            onClick = { showKeyDialog = true },
            trailing = { Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        )
        SettingRow(
            title = "키 발급 방법",
            subtitle = "서울 열린데이터광장",
            onClick = onOpenApiGuide,
            trailing = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
        )
        ToggleRow(
            title = "앱 실시간 자동 새로고침",
            subtitle = "화면을 보고 있을 때만 동작",
            checked = liveRefresh,
            onCheckedChange = {
                liveRefresh = it
                onSaveRefresh(liveRefresh, appSeconds, widgetMinutes)
            },
        )
        if (liveRefresh) {
            OptionRow(
                label = "앱 갱신 간격",
                options = listOf(15 to "15초", 30 to "30초"),
                selected = appSeconds,
                onSelect = {
                    appSeconds = it
                    onSaveRefresh(liveRefresh, appSeconds, widgetMinutes)
                },
            )
        }
        OptionRow(
            label = "위젯 자동 새로고침",
            options = listOf(0 to "OFF", 1 to "1분", 5 to "5분", 15 to "15분", 30 to "30분"),
            selected = widgetMinutes,
            onSelect = {
                widgetMinutes = it
                onSaveRefresh(liveRefresh, appSeconds, widgetMinutes)
            },
        )
        if (widgetMinutes in 1..14) {
            Text(
                "Android 정책상 백그라운드 갱신은 최소 15분 간격입니다. 1분·5분 선택은 앱 사용 중/수동 갱신에 적용되며 백그라운드에서는 15분 이상 걸릴 수 있습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SettingsSection("위젯")
        Text("배경 투명도  ${(opacity * 100).roundToInt()}%", fontWeight = FontWeight.SemiBold)
        Slider(
            value = opacity,
            onValueChange = { opacity = it },
            onValueChangeFinished = { onSaveAppearance(settings.appearance.copy(backgroundOpacity = opacity)) },
            valueRange = 0.65f..1f,
        )
        ToggleRow(
            title = "노선 진행도 표시",
            subtitle = "역과 열차 위치를 가로선으로 표시",
            checked = settings.appearance.showProgress,
            onCheckedChange = { onSaveAppearance(settings.appearance.copy(showProgress = it, backgroundOpacity = opacity)) },
        )
        ToggleRow(
            title = "두 번째 열차 표시",
            subtitle = "중형·대형 위젯에 다음 열차도 표시",
            checked = settings.appearance.showSecondTrain,
            onCheckedChange = { onSaveAppearance(settings.appearance.copy(showSecondTrain = it, backgroundOpacity = opacity)) },
        )
        ToggleRow(
            title = "업데이트 시간 표시",
            subtitle = "데이터 최신성을 하단에 표시",
            checked = settings.appearance.showUpdateTime,
            onCheckedChange = { onSaveAppearance(settings.appearance.copy(showUpdateTime = it, backgroundOpacity = opacity)) },
        )

        SettingsSection("앱 테마")
        OptionRow(
            label = "화면 모드",
            options = listOf(AppTheme.SYSTEM to "시스템", AppTheme.LIGHT to "라이트", AppTheme.DARK to "다크"),
            selected = settings.theme,
            onSelect = onSaveTheme,
        )
        Spacer(Modifier.height(28.dp))
        Text("MetroNow 0.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))
    }

    if (showKeyDialog) {
        ApiKeyDialog(
            initial = settings.apiKey,
            onDismiss = { showKeyDialog = false },
            onSave = { onSaveApiKey(it); showKeyDialog = false },
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Spacer(Modifier.height(28.dp))
    Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        trailing()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> OptionRow(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { (value, text) ->
                val active = value == selected
                Text(
                    text = text,
                    modifier = Modifier
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(9.dp),
                        )
                        .clickable { onSelect(value) }
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("서울시 API 키") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("인증키") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                supportingText = { Text("비워서 저장하면 실시간 조회가 중지됩니다.") },
            )
        },
        confirmButton = { Button(onClick = { onSave(value.trim()) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
