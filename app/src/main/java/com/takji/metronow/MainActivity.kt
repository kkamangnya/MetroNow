package com.takji.metronow

import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takji.metronow.presentation.home.HomeScreen
import com.takji.metronow.presentation.home.MainViewModel
import com.takji.metronow.presentation.onboarding.OnboardingScreen
import com.takji.metronow.presentation.presets.PresetsScreen
import com.takji.metronow.presentation.settings.SettingsScreen
import com.takji.metronow.presentation.theme.MetroNowTheme
import com.takji.metronow.presentation.theme.MetroNowSystemBars
import com.takji.metronow.widget.MetroNowWidgetReceiver
import com.takji.metronow.widget.MetroNowWidgetConfigurationActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MetroNowApplication
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(app))
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MetroNowTheme(uiState.settings.theme) {
                MetroNowSystemBars(uiState.settings.theme)
                MetroNowRoot(
                    app = app,
                    viewModel = viewModel,
                    onOpenApiGuide = { openApiGuide() },
                    onRequestPinWidget = { requestPinWidget() },
                )
            }
        }
    }

    private fun openApiGuide() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://data.seoul.go.kr/dataList/OA-12764/F/1/datasetView.do")))
    }

    private fun requestPinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            val configureIntent = Intent(this, MetroNowWidgetConfigurationActivity::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            }
            val successCallback = PendingIntent.getActivity(
                this,
                2401,
                configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            manager.requestPinAppWidget(
                ComponentName(this, MetroNowWidgetReceiver::class.java),
                null,
                successCallback,
            )
        }
    }
}

private enum class AppDestination(val label: String) { HOME("홈"), PRESETS("프리셋"), SETTINGS("설정") }

@Composable
private fun MetroNowRoot(
    app: MetroNowApplication,
    viewModel: MainViewModel,
    onOpenApiGuide: () -> Unit,
    onRequestPinWidget: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (!uiState.settings.onboardingComplete) {
        OnboardingScreen(
            catalog = app.stationCatalog,
            onOpenApiGuide = onOpenApiGuide,
            onComplete = viewModel::completeOnboarding,
        )
        return
    }

    var destination by remember { mutableStateOf(AppDestination.HOME) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setForegroundActive(true)
                Lifecycle.Event.ON_STOP -> viewModel.setForegroundActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setForegroundActive(false)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    AppDestination.HOME -> Icons.Outlined.Home
                                    AppDestination.PRESETS -> Icons.Outlined.StarOutline
                                    AppDestination.SETTINGS -> Icons.Outlined.Settings
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (destination) {
                AppDestination.HOME -> HomeScreen(
                    contentPadding = padding,
                    state = uiState,
                    catalog = app.stationCatalog,
                    onSelectPreset = viewModel::selectPreset,
                    onRefresh = viewModel::refreshSelected,
                    onAddPreset = { destination = AppDestination.PRESETS },
                    onRequestPinWidget = onRequestPinWidget,
                )
                AppDestination.PRESETS -> PresetsScreen(
                    contentPadding = padding,
                    settings = uiState.settings,
                    catalog = app.stationCatalog,
                    onSave = viewModel::upsertPreset,
                    onDelete = viewModel::deletePreset,
                )
                AppDestination.SETTINGS -> SettingsScreen(
                    contentPadding = padding,
                    settings = uiState.settings,
                    onSaveApiKey = viewModel::saveApiKey,
                    onSaveRefresh = viewModel::saveRefreshSettings,
                    onSaveAppearance = viewModel::saveAppearance,
                    onSaveTheme = viewModel::saveTheme,
                    onOpenApiGuide = onOpenApiGuide,
                )
            }
        }
    }
}
