package com.zenvairo.sportssuitability.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zenvairo.sportssuitability.alerts.AlertScheduler
import com.zenvairo.sportssuitability.alerts.DailyAlertWorker
import com.zenvairo.sportssuitability.alerts.WeeklyForecastWorker
import com.zenvairo.sportssuitability.data.local.Spot
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.SettingsRepository
import com.zenvairo.sportssuitability.data.remote.GeocodingResult
import com.zenvairo.sportssuitability.data.repository.SpotConditions
import com.zenvairo.sportssuitability.data.repository.SpotsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 350L
private const val MIN_SEARCH_LENGTH = 2

data class SportUiState(
    val conditions: List<SpotConditions> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val results: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

class SpotsViewModel(
    private val repository: SpotsRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiStateBySport = MutableStateFlow(
        SportType.values().associateWith { SportUiState() }
    )
    val uiStateBySport: StateFlow<Map<SportType, SportUiState>> = _uiStateBySport.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, AppSettings()
    )

    private var searchJob: Job? = null

    init {
        SportType.values().forEach { observeSport(it) }
    }

    private fun observeSport(sport: SportType) {
        viewModelScope.launch {
            repository.observeSpots(sport).collect { spots ->
                _uiStateBySport.update(sport) { it.copy(isLoading = spots.isNotEmpty()) }
                val results = fetchAll(spots)
                _uiStateBySport.update(sport) {
                    it.copy(conditions = results, isLoading = false, isRefreshing = false)
                }
            }
        }
    }

    fun refreshSport(sport: SportType) {
        viewModelScope.launch {
            val spots = _uiStateBySport.value[sport]?.conditions.orEmpty().map { it.spot }
            if (spots.isEmpty()) return@launch
            _uiStateBySport.update(sport) { it.copy(isRefreshing = true) }
            val refreshed = fetchAll(spots)
            _uiStateBySport.update(sport) { it.copy(conditions = refreshed, isRefreshing = false) }
        }
    }

    private suspend fun fetchAll(spots: List<Spot>): List<SpotConditions> = coroutineScope {
        spots.map { spot -> async { repository.fetchConditions(spot) } }.awaitAll()
    }

    // -- Settings ----------------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            // Re-arm from the value update() just wrote: the settings StateFlow may not have
            // caught up with DataStore yet, and a stale read would schedule the old hour.
            val stored = settingsRepository.update(transform)
            AlertScheduler.rescheduleAll(appContext, stored.alerts)
        }
    }

    fun resetSportDefaults(sport: SportType) {
        viewModelScope.launch { settingsRepository.resetSportDefaults(sport) }
    }

    /** Fires an alert immediately so the user can see what it will look like. */
    fun sendTestDailyAlert() {
        WorkManager.getInstance(appContext)
            .enqueue(OneTimeWorkRequestBuilder<DailyAlertWorker>().build())
    }

    fun sendTestWeeklyAlert() {
        WorkManager.getInstance(appContext)
            .enqueue(OneTimeWorkRequestBuilder<WeeklyForecastWorker>().build())
    }

    // -- Location search ---------------------------------------------------

    fun onSearchQueryChanged(query: String) {
        _search.value = _search.value.copy(query = query, error = null)
        searchJob?.cancel()
        if (query.trim().length < MIN_SEARCH_LENGTH) {
            _search.value = _search.value.copy(results = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchNow()
        }
    }

    fun searchLocations() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { searchNow() }
    }

    private suspend fun searchNow() {
        val query = _search.value.query.trim()
        if (query.isEmpty()) return
        _search.value = _search.value.copy(isSearching = true, error = null)
        val outcome = runCatching { repository.searchLocations(query) }
        _search.value = _search.value.copy(
            results = outcome.getOrDefault(emptyList()),
            isSearching = false,
            error = outcome.exceptionOrNull()?.let { "No se pudo buscar: ${it.message}" }
        )
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchUiState()
    }

    // -- Spot mutations ----------------------------------------------------

    fun addSpot(result: GeocodingResult, sportType: SportType) {
        viewModelScope.launch { repository.addSpot(result, sportType) }
    }

    fun removeSpot(spot: Spot) {
        viewModelScope.launch { repository.removeSpot(spot) }
    }

    fun toggleAlerts(spot: Spot, enabled: Boolean) {
        viewModelScope.launch { repository.setAlertsEnabled(spot, enabled) }
    }

    // Room re-emits the spot list after each write, so the observer refetches on its own.
    fun setCoastFacing(spot: Spot, facingDeg: Double?) {
        viewModelScope.launch { repository.setCoastFacing(spot, facingDeg) }
    }

    fun setAltitude(spot: Spot, altitudeM: Double?) {
        viewModelScope.launch { repository.setAltitude(spot, altitudeM) }
    }

    private fun MutableStateFlow<Map<SportType, SportUiState>>.update(
        sport: SportType,
        transform: (SportUiState) -> SportUiState
    ) {
        value = value.toMutableMap().apply { this[sport] = transform(this[sport] ?: SportUiState()) }
    }
}
