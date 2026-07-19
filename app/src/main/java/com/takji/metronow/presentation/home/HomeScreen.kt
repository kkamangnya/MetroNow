package com.takji.metronow.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.takji.metronow.data.local.StationCatalog
import com.takji.metronow.presentation.components.MetroWidgetPreview

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    state: MainUiState,
    catalog: StationCatalog,
    onSelectPreset: (String) -> Unit,
    onRefresh: () -> Unit,
    onAddPreset: () -> Unit,
    onRequestPinWidget: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(22.dp))
        Text("MetroNow", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text("지하철 위젯", style = MaterialTheme.typography.headlineLarge)
        Text("필요한 역의 흐름만 빠르게 확인하세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.settings.presets.forEach { preset ->
                val selected = preset.id == state.selectedPreset?.id
                Text(
                    text = preset.name,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelectPreset(preset.id) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddPreset)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(3.dp))
                Text("추가", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(16.dp))
        val preset = state.selectedPreset
        if (preset != null) {
            MetroWidgetPreview(
                preset = preset,
                snapshot = state.snapshot,
                neighbors = catalog.neighbors(preset.stationId, preset.direction),
                appearance = state.settings.appearance,
                directionHint = catalog.directionHint(preset.stationId, preset.direction),
                modifier = Modifier.fillMaxWidth(),
                onRefresh = onRefresh,
            )
            Spacer(Modifier.height(18.dp))
            Text("현재 설정", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth()) {
                Detail("노선", preset.line.displayName, Modifier.weight(1f))
                Detail("역", preset.stationDisplayName, Modifier.weight(1f))
                Detail("방향", preset.direction.label(preset.line), Modifier.weight(1f))
            }
        } else {
            Text("프리셋을 추가해 첫 위젯을 만들어보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onRequestPinWidget,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.settings.presets.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Icon(Icons.Outlined.AddToHomeScreen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("홈 화면에 MetroNow 위젯 추가")
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun Detail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
