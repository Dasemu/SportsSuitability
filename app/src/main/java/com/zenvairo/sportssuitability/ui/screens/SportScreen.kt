package com.zenvairo.sportssuitability.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenvairo.sportssuitability.data.local.Spot
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.remote.GeocodingResult
import com.zenvairo.sportssuitability.data.repository.SpotConditions
import com.zenvairo.sportssuitability.ui.SearchUiState
import com.zenvairo.sportssuitability.ui.components.AddSpotDialog
import com.zenvairo.sportssuitability.ui.components.ConditionsCard
import com.zenvairo.sportssuitability.ui.components.SpotSettingsDialog
import com.zenvairo.sportssuitability.ui.uiInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportScreen(
    sportType: SportType,
    conditions: List<SpotConditions>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    searchState: SearchUiState,
    settings: AppSettings,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (GeocodingResult) -> Unit,
    onRemove: (Spot) -> Unit,
    onToggleAlerts: (Spot, Boolean) -> Unit,
    onSetCoastFacing: (Spot, Double?) -> Unit,
    onSetAltitude: (Spot, Double?) -> Unit,
    onRefresh: () -> Unit,
    onDismissSearch: () -> Unit
) {
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Spot?>(null) }
    val info = sportType.uiInfo()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogOpen = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir sitio")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                conditions.isEmpty() && isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                conditions.isEmpty() ->
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Aún no has añadido sitios de ${info.label}.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Toca + para buscar uno.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(conditions, key = { it.spot.id }) { item ->
                            ConditionsCard(
                                conditions = item,
                                settings = settings,
                                onRemove = { onRemove(item.spot) },
                                onEdit = { editing = item.spot },
                                onToggleAlerts = { enabled -> onToggleAlerts(item.spot, enabled) }
                            )
                        }
                    }
            }
        }
    }

    if (dialogOpen) {
        AddSpotDialog(
            sportLabel = info.label,
            state = searchState,
            onQueryChanged = onQueryChanged,
            onSearch = onSearch,
            onPick = { result ->
                onPick(result)
                dialogOpen = false
                onDismissSearch()
            },
            onDismiss = {
                dialogOpen = false
                onDismissSearch()
            }
        )
    }

    editing?.let { spot ->
        // Re-read the spot from the freshly loaded list so the dialog reflects saved edits.
        val current = conditions.firstOrNull { it.spot.id == spot.id }?.spot ?: spot
        SpotSettingsDialog(
            spot = current,
            onSetCoastFacing = { onSetCoastFacing(current, it) },
            onSetAltitude = { onSetAltitude(current, it) },
            onDismiss = { editing = null }
        )
    }
}
