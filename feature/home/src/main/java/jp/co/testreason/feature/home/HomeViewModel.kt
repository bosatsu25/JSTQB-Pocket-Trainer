package jp.co.testreason.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.MasteryRepository
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.QuizSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val averageMastery: Float = 0f,
    val activeSession: QuizSession? = null,
    val isLoading: Boolean = false
)

sealed interface HomeUiEvent {
    data class NavigateToQuiz(val sessionId: String) : HomeUiEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val masteryRepository: MasteryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            quizRepository.seedInitialQuestionsIfEmpty()
        }
        viewModelScope.launch {
            masteryRepository.getAllMasteries().collect { masteries ->
                val avg = if (masteries.isNotEmpty()) {
                    masteries.map { it.score }.average().toFloat()
                } else 0f
                _uiState.value = _uiState.value.copy(averageMastery = avg)
            }
        }
        viewModelScope.launch {
            quizRepository.getActiveSession().collect { session ->
                _uiState.value = _uiState.value.copy(activeSession = session)
            }
        }
    }

    fun startDailyQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val session = quizRepository.createDailySession(5)
            _uiState.value = _uiState.value.copy(isLoading = false)
            _events.emit(HomeUiEvent.NavigateToQuiz(session.id))
        }
    }
}
