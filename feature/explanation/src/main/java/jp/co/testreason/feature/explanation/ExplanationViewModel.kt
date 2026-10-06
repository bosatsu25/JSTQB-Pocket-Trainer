package jp.co.testreason.feature.explanation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.QuizSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExplanationUiState(
    val session: QuizSession? = null,
    val question: Question? = null,
    val attempt: Attempt? = null,
    val isLastQuestion: Boolean = false,
    val isProcessing: Boolean = false
)

sealed interface ExplanationUiEvent {
    data class NavigateToNextQuestion(val sessionId: String) : ExplanationUiEvent
    data class NavigateToResult(val sessionId: String) : ExplanationUiEvent
}

@HiltViewModel
class ExplanationViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    private val questionId: String = checkNotNull(savedStateHandle["questionId"])
    private val attemptId: String = checkNotNull(savedStateHandle["attemptId"])

    private val _uiState = MutableStateFlow(ExplanationUiState())
    val uiState: StateFlow<ExplanationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExplanationUiEvent>()
    val events: SharedFlow<ExplanationUiEvent> = _events.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val session = quizRepository.getSessionById(sessionId)
            val question = quizRepository.getQuestionById(questionId)

            if (session != null) {
                quizRepository.getAttemptsForSession(sessionId).collect { attempts ->
                    val attempt = attempts.find { it.questionId == questionId }
                    val isLast = session.currentQuestionIndex + 1 >= session.questionIds.size
                    _uiState.value = ExplanationUiState(
                        session = session,
                        question = question,
                        attempt = attempt,
                        isLastQuestion = isLast
                    )
                }
            }
        }
    }

    fun selectMistakeReason(reason: MistakeReason) {
        viewModelScope.launch {
            quizRepository.updateMistakeReason(sessionId, questionId, reason)
            val currentAttempt = _uiState.value.attempt
            if (currentAttempt != null) {
                _uiState.value = _uiState.value.copy(attempt = currentAttempt.copy(mistakeReason = reason))
            }
        }
    }

    fun proceedToNext() {
        val state = _uiState.value
        val session = state.session ?: return
        val question = state.question ?: return

        if (state.isProcessing) return
        _uiState.value = state.copy(isProcessing = true)

        viewModelScope.launch {
            val updatedSession = quizRepository.completeQuestion(sessionId, questionId)
            val isFinalized = updatedSession?.isFinalized ?: false

            if (isFinalized) {
                _events.emit(ExplanationUiEvent.NavigateToResult(sessionId))
            } else {
                _events.emit(ExplanationUiEvent.NavigateToNextQuestion(sessionId))
            }
            _uiState.value = _uiState.value.copy(isProcessing = false)
        }
    }
}
