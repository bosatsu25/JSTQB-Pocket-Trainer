package jp.co.testreason.feature.analytics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class AnalyticsUiState(
    val title: String = "学習分析"
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState
}
