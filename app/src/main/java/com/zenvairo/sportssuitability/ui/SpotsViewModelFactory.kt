package com.zenvairo.sportssuitability.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenvairo.sportssuitability.data.prefs.SettingsRepository
import com.zenvairo.sportssuitability.data.repository.SpotsRepository

class SpotsViewModelFactory(
    private val repository: SpotsRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SpotsViewModel(repository, settingsRepository, appContext) as T
    }
}
