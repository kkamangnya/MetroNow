package com.takji.metronow.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takji.metronow.domain.model.ArrivalSnapshot
import com.takji.metronow.domain.model.MetroArrival
import com.takji.metronow.domain.model.MetroPreset
import com.takji.metronow.domain.model.StationNeighbors
import com.takji.metronow.domain.model.WidgetAppearance

private val WidgetText = Color(0xFFF3F7F4)
private val WidgetMuted = Color(0xFF98A39C)

@Composable
fun MetroWidgetPreview(
    preset: MetroPreset,
    snapshot: ArrivalSnapshot?,
    neighbors: StationNeighbors?,
    oppositeNeighbors: StationNeighbors? = null,
    appearance: WidgetAppearance,
    directionHint: String? = null,
    oppositeDirectionHint: String? = null,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
) {
    val lineColor = Color(preset.line.colorHex)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF0B0E12).copy(alpha = appearance.backgroundOpacity))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(lineColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(preset.line.number, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    preset.stationDisplayName,
                    color = WidgetText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${preset.routeDisplayName()} · 양방향",
                    color = WidgetMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (snapshot?.isStale() == true) {
                Text("오래된 정보", color = Color(0xFFFFC46B), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(18.dp))
        if (appearance.showProgress) {
            StationLabels(neighbors, oppositeNeighbors)
            Spacer(Modifier.height(7.dp))
            MetroProgressLine(
                color = lineColor,
                primaryProgress = snapshot?.primary?.firstOrNull()?.position?.progress,
                oppositeProgress = snapshot?.opposite?.firstOrNull()?.position?.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            )
            Spacer(Modifier.height(13.dp))
        }

        WidgetBody(
            snapshot = snapshot,
            preset = preset,
            directionHint = directionHint,
            oppositeDirectionHint = oppositeDirectionHint,
            showSecond = appearance.showSecondTrain,
        )

        if (appearance.showUpdateTime) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snapshot?.ageText()?.let { "$it 업데이트" } ?: "업데이트 전",
                    modifier = Modifier.weight(1f),
                    color = WidgetMuted,
                    fontSize = 11.sp,
                )
                if (onRefresh != null) {
                    androidx.compose.material3.IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "새로고침", tint = WidgetText, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = WidgetMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StationLabels(neighbors: StationNeighbors?, oppositeNeighbors: StationNeighbors?) {
    val names = listOf(
        neighbors?.previous?.displayName?.removeSuffix("역") ?: "전역",
        neighbors?.current?.displayName?.removeSuffix("역") ?: "현재역",
        oppositeNeighbors?.previous?.displayName?.removeSuffix("역") ?: "반대 전역",
    )
    Row(Modifier.fillMaxWidth()) {
        names.forEachIndexed { index, name ->
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                color = if (index == 1) WidgetText else WidgetMuted,
                fontSize = 11.sp,
                fontWeight = if (index == 1) FontWeight.Bold else FontWeight.Normal,
                textAlign = when (index) {
                    0 -> TextAlign.Start
                    1 -> TextAlign.Center
                    else -> TextAlign.End
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MetroProgressLine(
    color: Color,
    primaryProgress: Float?,
    oppositeProgress: Float?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val centerY = size.height / 2f
        val start = 6.dp.toPx()
        val end = size.width - 6.dp.toPx()
        val center = size.width / 2f
        drawLine(
            color = color.copy(alpha = 0.52f),
            start = Offset(start, centerY),
            end = Offset(end, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        listOf(start, size.width / 2f, end).forEachIndexed { index, x ->
            drawCircle(
                color = if (index == 1) WidgetText else color,
                radius = if (index == 1) 5.dp.toPx() else 4.dp.toPx(),
                center = Offset(x, centerY),
            )
            if (index == 1) drawCircle(color = Color(0xFF0B0E12), radius = 2.2.dp.toPx(), center = Offset(x, centerY))
        }
        primaryProgress?.let { progress ->
            drawTrain(
                center = Offset(start + (center - start) * (progress * 2f).coerceIn(0.06f, 0.94f), centerY),
                color = color,
            )
        }
        oppositeProgress?.let { progress ->
            drawTrain(
                center = Offset(end - (end - center) * (progress * 2f).coerceIn(0.06f, 0.94f), centerY),
                color = color,
            )
        }
    }
}

private fun DrawScope.drawTrain(center: Offset, color: Color) {
    val width = 22.dp.toPx()
    val height = 16.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - width / 2, center.y - height / 2),
        size = Size(width, height),
        cornerRadius = CornerRadius(5.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF0B0E12),
        topLeft = Offset(center.x - width * 0.28f, center.y - height * 0.24f),
        size = Size(width * 0.56f, height * 0.28f),
        cornerRadius = CornerRadius(1.5.dp.toPx()),
    )
    drawCircle(Color(0xFFF3F7F4), 1.4.dp.toPx(), Offset(center.x - width * 0.25f, center.y + height * 0.22f))
    drawCircle(Color(0xFFF3F7F4), 1.4.dp.toPx(), Offset(center.x + width * 0.25f, center.y + height * 0.22f))
}

@Composable
private fun WidgetBody(
    snapshot: ArrivalSnapshot?,
    preset: MetroPreset,
    directionHint: String?,
    oppositeDirectionHint: String?,
    showSecond: Boolean,
) {
    val hasArrivals = snapshot?.primary?.isNotEmpty() == true || snapshot?.opposite?.isNotEmpty() == true
    when {
        snapshot?.isLoading == true && !hasArrivals -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = WidgetMuted)
                Spacer(Modifier.width(9.dp))
                Text("양방향 실시간 정보를 불러오는 중", color = WidgetMuted, fontSize = 13.sp)
            }
        }
        snapshot?.apiKeyMissing == true -> StatusMessage("앱에서 API 키를 설정하세요")
        snapshot == null -> StatusMessage("양방향 도착정보를 새로고침하세요")
        else -> {
            val emptyMessage = snapshot.errorMessage
                ?.let { if (it.length <= 12) it else "정보를 불러올 수 없음" }
                ?: "도착정보 없음"
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DirectionCard(
                    directionLabel = preset.directionLabel(),
                    lineColor = Color(preset.line.colorHex),
                    hint = directionHint,
                    arrow = "←",
                    arrivals = snapshot.primary,
                    showSecond = showSecond,
                    emptyMessage = emptyMessage,
                    modifier = Modifier.weight(1f),
                )
                DirectionCard(
                    directionLabel = preset.directionLabel(preset.direction.opposite()),
                    lineColor = Color(preset.line.colorHex),
                    hint = oppositeDirectionHint,
                    arrow = "→",
                    arrivals = snapshot.opposite,
                    showSecond = showSecond,
                    emptyMessage = emptyMessage,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DirectionCard(
    directionLabel: String,
    lineColor: Color,
    hint: String?,
    arrow: String,
    arrivals: List<MetroArrival>,
    showSecond: Boolean,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            "$arrow $directionLabel",
            color = lineColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            hint.orEmpty().removeSuffix(" 방면"),
            color = WidgetMuted,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(7.dp))
        if (arrivals.isEmpty()) {
            Text(emptyMessage, color = WidgetMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            arrivals.take(if (showSecond) 2 else 1).forEachIndexed { index, arrival ->
                if (index > 0) Spacer(Modifier.height(7.dp))
                ArrivalRow(arrival)
            }
        }
    }
}

@Composable
private fun ArrivalRow(arrival: MetroArrival) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                arrival.statusWithService(),
                color = if (arrival.serviceLabel() != null) Color(0xFFFFC46B) else WidgetText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(arrival.destination, color = WidgetMuted, fontSize = 9.sp, maxLines = 1)
        }
        Text(
            arrival.etaText(),
            color = WidgetText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusMessage(message: String) {
    Text(message, color = WidgetMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
}
