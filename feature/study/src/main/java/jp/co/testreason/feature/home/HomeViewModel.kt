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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val averageMastery: Float = 0f,
    val activeSession: QuizSession? = null,
    val dueReviewCount: Int = 0,
    val weakQuestionCount: Int = 0,
    val isLoading: Boolean = false
) {
    /** A session can be resumed only if it still has unanswered questions. */
    val resumableSession: QuizSession?
        get() = activeSession?.takeIf { !it.isFinalized && it.currentQuestionIndex < it.questionIds.size }
}

sealed interface HomeUiEvent {
    data class NavigateToQuiz(val sessionId: String) : HomeUiEvent
    data class ShowMessage(val message: String) : HomeUiEvent
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
            quizRepository.syncBundledQuestions()
        }
        viewModelScope.launch {
            masteryRepository.getAllMasteries().collect { masteries ->
                val avg = if (masteries.isNotEmpty()) masteries.map { it.score }.average().toFloat() else 0f
                _uiState.update { it.copy(averageMastery = avg) }
            }
        }
        viewModelScope.launch {
            quizRepository.getActiveSession().collect { session ->
                _uiState.update { it.copy(activeSession = session) }
            }
        }
        viewModelScope.launch {
            quizRepository.observeDueReviewQuestions().collect { due ->
                _uiState.update { it.copy(dueReviewCount = due.size) }
            }
        }
        viewModelScope.launch {
            quizRepository.observeWeakQuestions().collect { weak ->
                _uiState.update { it.copy(weakQuestionCount = weak.size) }
            }
        }
    }

    fun startDailyQuiz() = startSession(emptyMessage = "出題できる問題がありません。") {
        quizRepository.createDailySession(DAILY_QUESTION_COUNT)
    }

    fun startWeaknessReview() = startSession(emptyMessage = "弱点の問題はまだありません。まずは今日のおすすめを解きましょう。") {
        quizRepository.createWeaknessSession()
    }

    fun resumeSession() {
        val state = _uiState.value
        val session = state.resumableSession ?: return
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch { _events.emit(HomeUiEvent.NavigateToQuiz(session.id)) }
    }

    /** Unlocks the start buttons when Home is shown (again); see [startSession]. */
    fun onScreenShown() = _uiState.update { it.copy(isLoading = false) }

    private fun startSession(emptyMessage: String, create: suspend () -> QuizSession?) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val session = create()
            if (session != null) {
                // Stay locked while the app navigates away, so a second tap cannot create another session.
                _events.emit(HomeUiEvent.NavigateToQuiz(session.id))
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(HomeUiEvent.ShowMessage(emptyMessage))
            }
        }
    }

    private companion object {
        const val DAILY_QUESTION_COUNT = 5
    }
}
