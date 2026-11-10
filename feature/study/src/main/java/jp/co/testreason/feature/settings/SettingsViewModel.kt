package jp.co.testreason.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.datastore.DarkThemeConfig
import jp.co.testreason.core.datastore.UserPreferences
import jp.co.testreason.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = preferencesRepository.userPreferences
        .map { SettingsUiState(isLoading = false, preferences = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun setTheme(config: DarkThemeConfig) {
        viewModelScope.launch { preferencesRepository.setDarkThemeConfig(config) }
    }

    fun setDailyGoal(questions: Int) {
        viewModelScope.launch { preferencesRepository.setDailyGoal(questions) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
