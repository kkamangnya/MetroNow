package com.takji.metronow.presentation.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.takji.metronow.data.local.StationCatalog
import com.takji.metronow.domain.model.Direction
import com.takji.metronow.domain.model.MetroLine
import com.takji.metronow.domain.model.MetroPreset
import com.takji.metronow.domain.model.MetroSettings
import com.takji.metronow.domain.model.Station

@Composable
fun PresetsScreen(
    contentPadding: PaddingValues,
    settings: MetroSettings,
    catalog: StationCatalog,
    onSave: (MetroPreset) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<MetroPreset?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MetroPreset?>(null) }

    if (editing != null || creating) {
        PresetEditor(
            contentPadding = contentPadding,
            initial = editing,
            catalog = catalog,
            onClose = { editing = null; creating = false },
            onSave = {
                onSave(it)
                editing = null
                creating = false
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(22.dp))
        Text("프리셋", style = MaterialTheme.typography.headlineLarge)
        Text("장소별 역과 방향을 저장하세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(settings.presets, key = { it.id }) { preset ->
                PresetRow(
                    preset = preset,
                    widgetCount = settings.widgetBindings.count { it.value == preset.id },
                    onEdit = { editing = preset },
                    onDelete = { pendingDelete = preset },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            if (settings.presets.isEmpty()) {
                item {
                    Text("아직 저장된 프리셋이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
                }
            }
        }
        Button(
            onClick = { creating = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("새 프리셋")
        }
        Spacer(Modifier.height(18.dp))
    }

    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("${preset.name} 삭제") },
            text = { Text("이 프리셋을 사용하는 위젯 연결도 함께 해제됩니다.") },
            confirmButton = {
                TextButton(onClick = { onDelete(preset.id); pendingDelete = null }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun PresetRow(preset: MetroPreset, widgetCount: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(preset.line.colorHex), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(preset.line.number, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(preset.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${preset.routeDisplayName()} ${preset.stationDisplayName} · ${preset.directionLabel()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (widgetCount > 0) Text("위젯 ${widgetCount}개에서 사용 중", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "수정") }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "삭제") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetEditor(
    contentPadding: PaddingValues,
    initial: MetroPreset?,
    catalog: StationCatalog,
    onClose: () -> Unit,
    onSave: (MetroPreset) -> Unit,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var line by remember(initial?.id) { mutableStateOf(initial?.line ?: MetroLine.LINE_2) }
    var station by remember(initial?.id) {
        mutableStateOf(initial?.let { catalog.station(it.stationId) } ?: catalog.stationsFor(MetroLine.LINE_2).first { it.apiName == "강남" })
    }
    var direction by remember(initial?.id) { mutableStateOf(initial?.direction ?: Direction.OUTER) }
    var stationQuery by remember { mutableStateOf("") }
    var searchingStation by remember { mutableStateOf(false) }

    LaunchedEffect(line) {
        if (station.line != line) {
            station = catalog.stationsFor(line).first()
            direction = catalog.directionsFor(station.id).first()
        }
    }

    LaunchedEffect(station.id) {
        val options = catalog.directionsFor(station.id)
        if (direction !in options) direction = options.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (initial == null) "새 프리셋" else "프리셋 수정", style = MaterialTheme.typography.headlineMedium)
                Text("위젯에 표시할 역을 설정합니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "닫기") }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("프리셋 이름") },
            placeholder = { Text("예: 집에서") },
            singleLine = true,
        )
        Spacer(Modifier.height(18.dp))
        Text("노선", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            MetroLine.entries.forEach { item ->
                Choice(
                    label = item.displayName,
                    selected = line == item,
                    color = Color(item.colorHex),
                    onClick = { line = item },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("역", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { searchingStation = !searchingStation }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(station.selectionLabel, modifier = Modifier.weight(1f))
            Text(if (searchingStation) "닫기" else "변경")
        }
        if (searchingStation) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = stationQuery,
                onValueChange = { stationQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("역 검색") },
                singleLine = true,
            )
            LazyColumn(Modifier.height(180.dp)) {
                items(catalog.stationsFor(line, stationQuery), key = { it.id }) { item ->
                    Text(
                        item.selectionLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { station = item; searchingStation = false; stationQuery = "" }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        fontWeight = if (item.id == station.id) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("방향", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            catalog.directionDescriptions(station.id).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (direction == item.direction) Color(line.colorHex).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { direction = item.direction }
                        .padding(13.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (direction == item.direction) Text("선택", color = Color(line.colorHex), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                onSave(
                    MetroPreset(
                        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        line = line,
                        stationId = station.id,
                        stationDisplayName = station.displayName,
                        stationApiName = station.apiName,
                        direction = direction,
                        createdAtMillis = initial?.createdAtMillis ?: System.currentTimeMillis(),
                    ),
                )
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) { Text("저장") }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) color else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}
