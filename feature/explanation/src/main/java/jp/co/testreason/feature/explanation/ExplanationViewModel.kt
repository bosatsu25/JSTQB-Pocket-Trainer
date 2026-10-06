package jp.co.testreason.feature.explanation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.MasteryRepository
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
    private val masteryRepository: MasteryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    private val questionId: String = checkNotNull(savedStateHandle["questionId"])
    private val attemptId: String = checkNotNull(savedStateHandle["attemptId"])

    private val _uiState = MutableStateFlow(ExplanationUiState())
    val uiState: StateFlow<ExplanationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExplanationUiEvent>()
    val events: SharedFlow<ExplanationUiEvent> = _events.asSharedFlow()

    private var hasUpdatedMastery = false

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
        val currentAttempt = _uiState.value.attempt ?: return
        val updatedAttempt = currentAttempt.copy(mistakeReason = reason)
        viewModelScope.launch {
            quizRepository.recordAttempt(updatedAttempt)
            _uiState.value = _uiState.value.copy(attempt = updatedAttempt)
        }
    }

    fun proceedToNext() {
        val state = _uiState.value
        val session = state.session ?: return
        val question = state.question ?: return
        val attempt = state.attempt ?: return

        if (state.isProcessing) return
        _uiState.value = state.copy(isProcessing = true)

        viewModelScope.launch {
            // Guarantee mastery is updated at most once per question in a session
            if (!hasUpdatedMastery) {
                masteryRepository.updateMastery(
                    learningObjectiveId = question.learningObjectiveId,
                    isCorrect = attempt.isCorrect
                )
                hasUpdatedMastery = true
            }

            val currentQIndex = session.questionIds.indexOf(questionId)
            val nextIndex = if (currentQIndex >= 0) currentQIndex + 1 else session.currentQuestionIndex + 1
            val isFinalized = nextIndex >= session.questionIds.size

            quizRepository.updateSessionProgress(
                sessionId = sessionId,
                currentIndex = nextIndex,
                isFinalized = isFinalized
            )

            if (isFinalized) {
                _events.emit(ExplanationUiEvent.NavigateToResult(sessionId))
            } else {
                _events.emit(ExplanationUiEvent.NavigateToNextQuestion(sessionId))
            }
        }
    }
}
