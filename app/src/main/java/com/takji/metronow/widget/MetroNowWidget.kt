package com.takji.metronow.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.takji.metronow.MetroNowApplication
import com.takji.metronow.R
import com.takji.metronow.domain.model.ArrivalSnapshot
import com.takji.metronow.domain.model.MetroArrival
import com.takji.metronow.domain.model.MetroPreset
import com.takji.metronow.domain.model.StationNeighbors
import com.takji.metronow.domain.model.WidgetAppearance

class MetroNowWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 100.dp),
            DpSize(250.dp, 140.dp),
            DpSize(360.dp, 220.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MetroNowApplication
        val settings = app.settingsStore.current()
        val platformId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }
            .getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val presetId = settings.widgetBindings[platformId]
        val preset = settings.presets.firstOrNull { it.id == presetId }
        val snapshot = preset?.let { settings.snapshots[it.id] }
        val neighbors = preset?.let { app.stationCatalog.neighbors(it.stationId, it.direction) }
        val directionHint = preset?.let { app.stationCatalog.directionHint(it.stationId, it.direction) }
        val configureAction = actionStartActivity<MetroNowWidgetConfigurationActivity>(
            actionParametersOf(
                MetroNowWidgetConfigurationActivity.WidgetIdActionKey to platformId,
            ),
        )

        provideContent {
            MetroNowWidgetContent(
                appWidgetId = platformId,
                preset = preset,
                snapshot = snapshot,
                neighbors = neighbors,
                appearance = settings.appearance,
                directionHint = directionHint,
                configureAction = configureAction,
            )
        }
    }
}

@Composable
private fun MetroNowWidgetContent(
    appWidgetId: Int,
    preset: MetroPreset?,
    snapshot: ArrivalSnapshot?,
    neighbors: StationNeighbors?,
    appearance: WidgetAppearance,
    directionHint: String?,
    configureAction: Action,
) {
    val size = LocalSize.current
    val small = size.width < 230.dp || size.height < 125.dp
    val large = size.height >= 205.dp || size.width >= 350.dp
    val opacity = (appearance.backgroundOpacity.coerceIn(0.65f, 1f) * 255).toInt()
    val backgroundArgb = (opacity shl 24) or 0x000B0E12
    val background = ColorProvider(Color(backgroundArgb))
    val primaryText = ColorProvider(Color(0xFFF3F7F4))
    val mutedText = ColorProvider(Color(0xFF9AA59E))

    val rootModifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(background)
            .padding(if (small) 14.dp else 18.dp)
            .let { if (preset == null) it.clickable(configureAction) else it }
    Column(modifier = rootModifier) {
        if (preset == null) {
            Text("MetroNow", style = TextStyle(color = primaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp))
            Spacer(GlanceModifier.height(8.dp))
            Text("앱에서 위젯 프리셋을 선택하세요", style = TextStyle(color = mutedText, fontSize = 12.sp))
            return@Column
        }

        val lineColor = ColorProvider(Color(preset.line.colorHex))
        Header(preset, neighbors, directionHint, lineColor, primaryText, mutedText, small)
        Spacer(GlanceModifier.height(if (small) 8.dp else 12.dp))

        if (!small && appearance.showProgress) {
            StationNames(neighbors, primaryText, mutedText)
            Spacer(GlanceModifier.height(3.dp))
            TransitLine(snapshot?.primary?.firstOrNull(), lineColor)
            Spacer(GlanceModifier.height(8.dp))
        }

        when {
            snapshot?.isLoading == true && snapshot.primary.isEmpty() -> StatusText("불러오는 중…", mutedText)
            snapshot?.apiKeyMissing == true -> StatusText("앱에서 API 키를 설정하세요", mutedText)
            snapshot?.errorMessage != null && snapshot.primary.isEmpty() -> StatusText(snapshot.errorMessage, mutedText)
            snapshot == null || snapshot.primary.isEmpty() -> StatusText("새로고침하여 도착정보 확인", mutedText)
            else -> {
                val count = when {
                    small -> 1
                    large -> 3
                    appearance.showSecondTrain -> 2
                    else -> 1
                }
                ArrivalRows(snapshot.primary.take(count), primaryText, mutedText)
                if (large && snapshot.opposite.isNotEmpty()) {
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        preset.direction.opposite().label(preset.line),
                        style = TextStyle(color = lineColor, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    ArrivalRows(snapshot.opposite.take(2), primaryText, mutedText)
                }
            }
        }

        if (!small && appearance.showUpdateTime) {
            Spacer(GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    snapshot?.ageText()?.let { "$it 업데이트" } ?: "업데이트 전",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = if (snapshot?.isStale() == true) ColorProvider(Color(0xFFFFC46B)) else mutedText,
                        fontSize = 10.sp,
                    ),
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = "새로고침",
                    modifier = GlanceModifier
                        .size(26.dp)
                        .clickable(
                            actionRunCallback<RefreshWidgetAction>(
                                actionParametersOf(RefreshWidgetAction.WidgetIdKey to appWidgetId),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun Header(
    preset: MetroPreset,
    neighbors: StationNeighbors?,
    directionHint: String?,
    lineColor: ColorProvider,
    primaryText: ColorProvider,
    mutedText: ColorProvider,
    small: Boolean,
) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(if (small) 30.dp else 36.dp)
                .cornerRadius(18.dp)
                .background(lineColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                preset.line.number,
                style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = if (small) 15.sp else 18.sp),
            )
        }
        Spacer(GlanceModifier.width(9.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                if (small) preset.stationDisplayName else preset.line.displayName,
                style = TextStyle(color = primaryText, fontWeight = FontWeight.Bold, fontSize = if (small) 15.sp else 14.sp),
                maxLines = 1,
            )
            Text(
                "${preset.direction.label(preset.line)} · ${directionHint ?: "${neighbors?.next?.displayName?.removeSuffix("역") ?: ""} 방면"}",
                style = TextStyle(color = mutedText, fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StationNames(neighbors: StationNeighbors?, primaryText: ColorProvider, mutedText: ColorProvider) {
    Row(GlanceModifier.fillMaxWidth()) {
        Text(
            neighbors?.previous?.displayName?.removeSuffix("역") ?: "전역",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = mutedText, fontSize = 10.sp, textAlign = TextAlign.Start),
            maxLines = 1,
        )
        Text(
            neighbors?.current?.displayName?.removeSuffix("역") ?: "현재역",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = primaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        Text(
            neighbors?.next?.displayName?.removeSuffix("역") ?: "다음역",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = mutedText, fontSize = 10.sp, textAlign = TextAlign.End),
            maxLines = 1,
        )
    }
}

@Composable
private fun TransitLine(arrival: MetroArrival?, lineColor: ColorProvider) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text("●", style = TextStyle(color = lineColor, fontSize = 9.sp))
        val align = when {
            (arrival?.position?.progress ?: 0.39f) < 0.34f -> Alignment.CenterStart
            (arrival?.position?.progress ?: 0.39f) < 0.61f -> Alignment.Center
            else -> Alignment.CenterEnd
        }
        Box(
            modifier = GlanceModifier.defaultWeight().height(22.dp),
            contentAlignment = align,
        ) {
            Spacer(GlanceModifier.fillMaxWidth().height(2.dp).background(lineColor))
            Image(
                provider = ImageProvider(R.drawable.ic_train),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(20.dp),
            )
        }
        Text("●", style = TextStyle(color = lineColor, fontSize = 9.sp))
        Spacer(GlanceModifier.width(4.dp))
        Text("●", style = TextStyle(color = lineColor, fontSize = 9.sp))
    }
}

@Composable
private fun ArrivalRows(arrivals: List<MetroArrival>, primaryText: ColorProvider, mutedText: ColorProvider) {
    arrivals.forEachIndexed { index, arrival ->
        if (index > 0) Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(arrival.statusText, style = TextStyle(color = primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(arrival.destination, style = TextStyle(color = mutedText, fontSize = 9.sp), maxLines = 1)
            }
            Text(arrival.etaText(), style = TextStyle(color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun StatusText(message: String, mutedText: ColorProvider) {
    Text(message, style = TextStyle(color = mutedText, fontSize = 11.sp), maxLines = 2)
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appWidgetId = parameters[WidgetIdKey]
        val app = context.applicationContext as MetroNowApplication
        app.widgetScheduler.refreshNow(appWidgetId)
    }

    companion object {
        val WidgetIdKey = ActionParameters.Key<Int>("metro_now_widget_id")
    }
}
