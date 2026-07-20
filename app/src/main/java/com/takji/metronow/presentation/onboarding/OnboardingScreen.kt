package com.takji.metronow.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.takji.metronow.data.local.StationCatalog
import com.takji.metronow.domain.model.Direction
import com.takji.metronow.domain.model.MetroLine
import com.takji.metronow.domain.model.MetroPreset
import com.takji.metronow.domain.model.PreviewData
import com.takji.metronow.domain.model.Station
import com.takji.metronow.presentation.components.MetroWidgetPreview

@Composable
fun OnboardingScreen(
    catalog: StationCatalog,
    onOpenApiGuide: () -> Unit,
    onComplete: (String, MetroLine, Station, Direction) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf("") }
    var line by remember { mutableStateOf(MetroLine.LINE_2) }
    var station by remember { mutableStateOf(catalog.stationsFor(MetroLine.LINE_2).first { it.apiName == "강남" }) }
    var direction by remember { mutableStateOf(Direction.OUTER) }

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) {
                IconButton(onClick = { step-- }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "이전")
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("${step + 1} / 6", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
            Spacer(Modifier.height(20.dp))
            Text("MetroNow", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Column(Modifier.weight(1f)) {
                when (step) {
                    0 -> ApiKeyStep(apiKey, { apiKey = it }, onOpenApiGuide)
                    1 -> LineStep(line, { line = it })
                    2 -> StationStep(catalog, line, station, { station = it })
                    3 -> DirectionStep(catalog, station, direction, { direction = it })
                    4 -> ReviewStep(catalog, apiKey, line, station, direction)
                    else -> WidgetGuideStep()
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (step < 5) step++ else onComplete(apiKey, line, station, direction)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
            ) {
                Text(if (step == 5) "MetroNow 시작" else "다음")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ApiKeyStep(value: String, onValueChange: (String) -> Unit, onOpenApiGuide: () -> Unit) {
    Text("서울시 API 키", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("실시간 도착정보를 받기 위해 필요합니다. 키는 기기에만 저장됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("인증키") },
        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        supportingText = { Text("지금 비워두어도 미리보기와 설정은 사용할 수 있습니다.") },
    )
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onOpenApiGuide) { Text("API 키 발급 방법 보기") }
}

@Composable
private fun ColumnScope.LineStep(selected: MetroLine, onSelect: (MetroLine) -> Unit) {
    Text("노선 선택", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("위젯에서 가장 먼저 확인할 노선을 고르세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(MetroLine.entries) { line ->
            SelectionRow(
                title = line.displayName,
                subtitle = "서울 지하철 ${line.number}호선",
                selected = line == selected,
                color = Color(line.colorHex),
                onClick = { onSelect(line) },
            )
        }
    }
}

@Composable
private fun ColumnScope.StationStep(catalog: StationCatalog, line: MetroLine, selected: Station, onSelect: (Station) -> Unit) {
    var query by remember(line) { mutableStateOf("") }
    Text("역 선택", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("${line.displayName} 역 목록에서 검색할 수 있습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("역 검색") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
    )
    Spacer(Modifier.height(10.dp))
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(catalog.stationsFor(line, query), key = { it.id }) { station ->
            SelectionRow(
                title = station.selectionLabel,
                subtitle = station.apiName.takeIf { it != station.displayName.removeSuffix("역") }?.let { "API: $it" },
                selected = station.id == selected.id,
                color = Color(line.colorHex),
                onClick = { onSelect(station) },
            )
        }
    }
}

@Composable
private fun DirectionStep(catalog: StationCatalog, station: Station, selected: Direction, onSelect: (Direction) -> Unit) {
    Text("방향 선택", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("${station.displayName}에서 어느 방향 열차를 볼까요?", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        catalog.directionDescriptions(station.id).forEach { item ->
            SelectionRow(
                title = item.title,
                subtitle = item.subtitle,
                selected = item.direction == selected,
                color = Color(station.line.colorHex),
                onClick = { onSelect(item.direction) },
            )
        }
    }
}

@Composable
private fun ReviewStep(catalog: StationCatalog, apiKey: String, line: MetroLine, station: Station, direction: Direction) {
    Text("설정 확인", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        if (apiKey.isBlank()) "API 키는 나중에 설정할 수 있어요." else "API 키와 첫 프리셋을 저장할 준비가 됐습니다.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(18.dp))
    val preset = MetroPreset(
        id = "onboarding-preview",
        name = "기본",
        line = line,
        stationId = station.id,
        stationDisplayName = station.displayName,
        stationApiName = station.apiName,
        direction = direction,
    )
    val previewSnapshot = PreviewData.snapshot.copy(presetId = preset.id)
    MetroWidgetPreview(
        preset = preset,
        snapshot = previewSnapshot,
        neighbors = catalog.neighbors(station.id, direction),
        oppositeNeighbors = catalog.neighbors(station.id, direction.opposite()),
        appearance = com.takji.metronow.domain.model.WidgetAppearance(),
        directionHint = catalog.directionHint(station.id, direction),
        oppositeDirectionHint = catalog.directionHint(station.id, direction.opposite()),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WidgetGuideStep() {
    Text("홈 화면에서 바로", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("설정을 마치면 홈에서 ‘위젯 추가’를 눌러 프리셋을 고를 수 있습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(28.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(14.dp))
            Text("위젯마다 다른 프리셋", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("강남역과 건대입구역을 각각 둘 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) color.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) Text("선택", style = MaterialTheme.typography.labelMedium, color = color)
    }
}
