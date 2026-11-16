package com.zenvairo.sportssuitability

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.zenvairo.sportssuitability.data.local.AppDatabase
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.SettingsRepository
import com.zenvairo.sportssuitability.data.repository.SpotsRepository
import com.zenvairo.sportssuitability.ui.SpotsViewModel
import com.zenvairo.sportssuitability.ui.SpotsViewModelFactory
import com.zenvairo.sportssuitability.ui.screens.SettingsScreen
import com.zenvairo.sportssuitability.ui.screens.SportScreen
import com.zenvairo.sportssuitability.ui.theme.SportsSuitabilityTheme
import com.zenvairo.sportssuitability.ui.uiInfo

class MainActivity : ComponentActivity() {

    private val viewModel: SpotsViewModel by viewModels {
        SpotsViewModelFactory(
            SpotsRepository(AppDatabase.get(applicationContext)),
            SettingsRepository(applicationContext),
            applicationContext
        )
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SportsSuitabilityTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(viewModel: SpotsViewModel) {
    var selectedSport by remember { mutableStateOf(SportType.SURF) }
    var showSettings by remember { mutableStateOf(false) }
    val uiStateBySport by viewModel.uiStateBySport.collectAsState()
    val searchState by viewModel.search.collectAsState()
    val settings by viewModel.settings.collectAsState()

    BackHandler(enabled = showSettings) { showSettings = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showSettings) "Ajustes" else selectedSport.uiInfo().label) },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = { viewModel.refreshSport(selectedSport) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar {
                    SportType.values().forEach { sport ->
                        val info = sport.uiInfo()
                        NavigationBarItem(
                            selected = selectedSport == sport,
                            onClick = { selectedSport = sport },
                            icon = { Icon(info.icon, contentDescription = info.label) },
                            label = { Text(info.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (showSettings) {
                SettingsScreen(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    onResetSport = viewModel::resetSportDefaults,
                    onTestDailyAlert = viewModel::sendTestDailyAlert,
                    onTestWeeklyAlert = viewModel::sendTestWeeklyAlert
                )
            } else {
                val state = uiStateBySport[selectedSport]
                SportScreen(
                    sportType = selectedSport,
                    conditions = state?.conditions.orEmpty(),
                    isLoading = state?.isLoading ?: false,
                    isRefreshing = state?.isRefreshing ?: false,
                    searchState = searchState,
                    settings = settings,
                    onQueryChanged = viewModel::onSearchQueryChanged,
                    onSearch = viewModel::searchLocations,
                    onPick = { result -> viewModel.addSpot(result, selectedSport) },
                    onRemove = viewModel::removeSpot,
                    onToggleAlerts = viewModel::toggleAlerts,
                    onSetCoastFacing = viewModel::setCoastFacing,
                    onSetAltitude = viewModel::setAltitude,
                    onRefresh = { viewModel.refreshSport(selectedSport) },
                    onDismissSearch = viewModel::clearSearch
                )
            }
        }
    }
}
