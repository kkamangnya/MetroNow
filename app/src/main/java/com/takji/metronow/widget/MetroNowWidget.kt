package com.takji.metronow.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
            .getOrElse {
                AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, MetroNowWidgetReceiver::class.java))
                    .singleOrNull()
                    ?: AppWidgetManager.INVALID_APPWIDGET_ID
            }

        var presetId = settings.widgetBindings[platformId]
        if (presetId == null && platformId != AppWidgetManager.INVALID_APPWIDGET_ID && settings.presets.size == 1) {
            presetId = settings.presets.single().id
            app.settingsStore.bindWidget(platformId, presetId)
        }
        if (presetId == null && platformId == AppWidgetManager.INVALID_APPWIDGET_ID && settings.widgetBindings.size == 1) {
            presetId = settings.widgetBindings.values.single()
        }

        val preset = settings.presets.firstOrNull { it.id == presetId }
        val snapshot = preset?.let { settings.snapshots[it.id] }
        val primaryNeighbors = preset?.let { app.stationCatalog.neighbors(it.stationId, it.direction) }
        val oppositeNeighbors = preset?.let { app.stationCatalog.neighbors(it.stationId, it.direction.opposite()) }
        val primaryHint = preset?.let { app.stationCatalog.directionHint(it.stationId, it.direction) }
        val oppositeHint = preset?.let { app.stationCatalog.directionHint(it.stationId, it.direction.opposite()) }
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
                primaryNeighbors = primaryNeighbors,
                oppositeNeighbors = oppositeNeighbors,
                appearance = settings.appearance,
                primaryHint = primaryHint,
                oppositeHint = oppositeHint,
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
    primaryNeighbors: StationNeighbors?,
    oppositeNeighbors: StationNeighbors?,
    appearance: WidgetAppearance,
    primaryHint: String?,
    oppositeHint: String?,
    configureAction: Action,
) {
    val size = LocalSize.current
    val compact = size.width < 230.dp || size.height < 125.dp
    val expanded = size.height >= 190.dp || size.width >= 350.dp
    val opacity = (appearance.backgroundOpacity.coerceIn(0.65f, 1f) * 255).toInt()
    val backgroundArgb = (opacity shl 24) or 0x00090D11
    val background = ColorProvider(Color(backgroundArgb))
    val panel = ColorProvider(Color(0xFF151B20))
    val primaryText = ColorProvider(Color(0xFFF4F7F5))
    val mutedText = ColorProvider(Color(0xFF93A099))

    val rootModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(26.dp)
        .background(background)
        .padding(horizontal = if (compact) 11.dp else 14.dp, vertical = if (compact) 9.dp else 11.dp)
        .let { if (preset == null) it.clickable(configureAction) else it }

    Column(modifier = rootModifier) {
        if (preset == null) {
            Text("MetroNow", style = TextStyle(color = primaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp))
            Spacer(GlanceModifier.height(7.dp))
            Text("탭하여 이 위젯의 프리셋을 선택하세요", style = TextStyle(color = mutedText, fontSize = 11.sp))
            return@Column
        }

        val lineColor = ColorProvider(Color(preset.line.colorHex))
        WidgetHeader(
            appWidgetId = appWidgetId,
            preset = preset,
            snapshot = snapshot,
            lineColor = lineColor,
            primaryText = primaryText,
            mutedText = mutedText,
            compact = compact,
            showUpdateTime = appearance.showUpdateTime,
        )
        Spacer(GlanceModifier.height(if (compact) 5.dp else 7.dp))

        val hasArrivals = snapshot?.primary?.isNotEmpty() == true || snapshot?.opposite?.isNotEmpty() == true
        when {
            snapshot?.isLoading == true && !hasArrivals -> StatusText("양방향 실시간 정보를 불러오는 중…", mutedText)
            snapshot?.apiKeyMissing == true -> StatusText("앱에서 API 키를 설정하세요", mutedText)
            snapshot == null -> StatusText("새로고침하여 양방향 도착정보 확인", mutedText)
            else -> {
                val emptyMessage = snapshot.errorMessage
                    ?.let { if (it.length <= 12) it else "정보를 불러올 수 없음" }
                    ?: "도착정보 없음"
                if (!compact && appearance.showProgress) {
                    DualStationNames(primaryNeighbors, oppositeNeighbors, primaryText, mutedText)
                    Spacer(GlanceModifier.height(2.dp))
                    DualTransitLine(
                        primary = snapshot.primary.firstOrNull(),
                        opposite = snapshot.opposite.firstOrNull(),
                        lineColor = lineColor,
                    )
                    Spacer(GlanceModifier.height(4.dp))
                }

                Row(GlanceModifier.fillMaxWidth()) {
                    DirectionPanel(
                        directionLabel = preset.directionLabel(),
                        hint = primaryHint,
                        arrivals = snapshot.primary,
                        arrow = "←",
                        lineColor = lineColor,
                        panel = panel,
                        primaryText = primaryText,
                        mutedText = mutedText,
                        compact = compact,
                        showSecond = expanded && appearance.showSecondTrain,
                        emptyMessage = emptyMessage,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(GlanceModifier.width(7.dp))
                    DirectionPanel(
                        directionLabel = preset.directionLabel(preset.direction.opposite()),
                        hint = oppositeHint,
                        arrivals = snapshot.opposite,
                        arrow = "→",
                        lineColor = lineColor,
                        panel = panel,
                        primaryText = primaryText,
                        mutedText = mutedText,
                        compact = compact,
                        showSecond = expanded && appearance.showSecondTrain,
                        emptyMessage = emptyMessage,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(
    appWidgetId: Int,
    preset: MetroPreset,
    snapshot: ArrivalSnapshot?,
    lineColor: ColorProvider,
    primaryText: ColorProvider,
    mutedText: ColorProvider,
    compact: Boolean,
    showUpdateTime: Boolean,
) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(if (compact) 27.dp else 31.dp)
                .cornerRadius(16.dp)
                .background(lineColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                preset.line.number,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 14.sp else 16.sp,
                ),
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                preset.stationDisplayName,
                style = TextStyle(color = primaryText, fontWeight = FontWeight.Bold, fontSize = if (compact) 14.sp else 15.sp),
                maxLines = 1,
            )
            if (!compact) {
                Text(
                    "${preset.routeDisplayName()} · 양방향",
                    style = TextStyle(color = mutedText, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
        }
        if (showUpdateTime && !compact) {
            Text(
                snapshot?.ageText() ?: "업데이트 전",
                style = TextStyle(
                    color = if (snapshot?.isStale() == true) ColorProvider(Color(0xFFFFC46B)) else mutedText,
                    fontSize = 9.sp,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(4.dp))
        }
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "양방향 정보 새로고침",
            modifier = GlanceModifier
                .size(if (compact) 22.dp else 24.dp)
                .clickable(
                    actionRunCallback<RefreshWidgetAction>(
                        actionParametersOf(RefreshWidgetAction.WidgetIdKey to appWidgetId),
                    ),
                ),
        )
    }
}

@Composable
private fun DualStationNames(
    primaryNeighbors: StationNeighbors?,
    oppositeNeighbors: StationNeighbors?,
    primaryText: ColorProvider,
    mutedText: ColorProvider,
) {
    Row(GlanceModifier.fillMaxWidth()) {
        Text(
            primaryNeighbors?.previous?.displayName?.removeSuffix("역") ?: "한쪽 전역",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = mutedText, fontSize = 9.sp, textAlign = TextAlign.Start),
            maxLines = 1,
        )
        Text(
            primaryNeighbors?.current?.displayName?.removeSuffix("역") ?: "현재역",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = primaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        Text(
            oppositeNeighbors?.previous?.displayName?.removeSuffix("역") ?: "반대 전역",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = mutedText, fontSize = 9.sp, textAlign = TextAlign.End),
            maxLines = 1,
        )
    }
}

@Composable
private fun DualTransitLine(primary: MetroArrival?, opposite: MetroArrival?, lineColor: ColorProvider) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text("●", style = TextStyle(color = lineColor, fontSize = 8.sp))
        TransitHalf(primary, lineColor, towardCenter = true, modifier = GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier.size(12.dp).cornerRadius(6.dp).background(ColorProvider(Color(0xFFF4F7F5))),
            contentAlignment = Alignment.Center,
        ) {
            Box(GlanceModifier.size(5.dp).cornerRadius(3.dp).background(ColorProvider(Color(0xFF0B0F13)))) {}
        }
        TransitHalf(opposite, lineColor, towardCenter = false, modifier = GlanceModifier.defaultWeight())
        Text("●", style = TextStyle(color = lineColor, fontSize = 8.sp))
    }
}

@Composable
private fun TransitHalf(
    arrival: MetroArrival?,
    lineColor: ColorProvider,
    towardCenter: Boolean,
    modifier: GlanceModifier,
) {
    val progress = arrival?.position?.progress ?: 0.12f
    val nearCurrent = progress >= 0.44f
    val mid = progress >= 0.28f
    val alignment = when {
        towardCenter && nearCurrent -> Alignment.CenterEnd
        towardCenter && mid -> Alignment.Center
        towardCenter -> Alignment.CenterStart
        !towardCenter && nearCurrent -> Alignment.CenterStart
        !towardCenter && mid -> Alignment.Center
        else -> Alignment.CenterEnd
    }
    Box(
        modifier = modifier.height(19.dp),
        contentAlignment = alignment,
    ) {
        Spacer(GlanceModifier.fillMaxWidth().height(2.dp).background(lineColor))
        if (arrival != null) {
            Image(
                provider = ImageProvider(R.drawable.ic_train),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun DirectionPanel(
    directionLabel: String,
    hint: String?,
    arrivals: List<MetroArrival>,
    arrow: String,
    lineColor: ColorProvider,
    panel: ColorProvider,
    primaryText: ColorProvider,
    mutedText: ColorProvider,
    compact: Boolean,
    showSecond: Boolean,
    emptyMessage: String,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier
            .cornerRadius(13.dp)
            .background(panel)
            .padding(horizontal = if (compact) 7.dp else 9.dp, vertical = if (compact) 6.dp else 7.dp),
    ) {
        Text(
            "$arrow $directionLabel",
            style = TextStyle(color = lineColor, fontWeight = FontWeight.Bold, fontSize = if (compact) 9.sp else 10.sp),
            maxLines = 1,
        )
        if (!compact) {
            Text(
                hint.orEmpty().removeSuffix(" 방면"),
                style = TextStyle(color = mutedText, fontSize = 8.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(3.dp))
        }
        if (arrivals.isEmpty()) {
            Text(emptyMessage, style = TextStyle(color = mutedText, fontSize = 9.sp), maxLines = 1)
        } else {
            CompactArrival(arrivals.first(), primaryText, mutedText, compact)
            if (showSecond && arrivals.size > 1) {
                Spacer(GlanceModifier.height(4.dp))
                CompactArrival(arrivals[1], primaryText, mutedText, compact = false)
            }
        }
    }
}

@Composable
private fun CompactArrival(
    arrival: MetroArrival,
    primaryText: ColorProvider,
    mutedText: ColorProvider,
    compact: Boolean,
) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Column(GlanceModifier.defaultWeight()) {
            val service = arrival.serviceLabel()
            Text(
                arrival.statusWithService(),
                style = TextStyle(
                    color = if (service != null) ColorProvider(Color(0xFFFFC46B)) else primaryText,
                    fontSize = if (compact) 8.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            if (!compact) {
                Text(arrival.destination, style = TextStyle(color = mutedText, fontSize = 8.sp), maxLines = 1)
            }
        }
        Spacer(GlanceModifier.width(3.dp))
        Text(
            arrival.etaText(),
            style = TextStyle(color = primaryText, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusText(message: String, mutedText: ColorProvider) {
    Text(message, style = TextStyle(color = mutedText, fontSize = 11.sp), maxLines = 2)
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val requestedId = parameters[WidgetIdKey]
        val appWidgetId = requestedId?.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            ?: runCatching { GlanceAppWidgetManager(context).getAppWidgetId(glanceId) }.getOrNull()
        val app = context.applicationContext as MetroNowApplication
        app.widgetScheduler.refreshNow(appWidgetId)
    }

    companion object {
        val WidgetIdKey = ActionParameters.Key<Int>("metro_now_widget_id")
    }
}
