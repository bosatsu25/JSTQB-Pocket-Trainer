package jp.co.testreason.feature.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
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

data class QuizUiState(
    val session: QuizSession? = null,
    val currentQuestion: Question? = null,
    val selectedChoiceId: String? = null,
    val confidence: ConfidenceLevel? = null,
    val isSubmitting: Boolean = false
)

sealed interface QuizUiEvent {
    data class NavigateToExplanation(val sessionId: String, val questionId: String, val attemptId: String) : QuizUiEvent
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuizUiEvent>()
    val events: SharedFlow<QuizUiEvent> = _events.asSharedFlow()

    private var questionStartTime: Long = System.currentTimeMillis()

    init {
        loadSession()
    }

    fun loadSession() {
        viewModelScope.launch {
            val session = quizRepository.getSessionById(sessionId)
            if (session != null && session.currentQuestionIndex < session.questionIds.size) {
                val qId = session.questionIds[session.currentQuestionIndex]
                val question = quizRepository.getQuestionById(qId)
                questionStartTime = System.currentTimeMillis()
                _uiState.value = QuizUiState(
                    session = session,
                    currentQuestion = question,
                    selectedChoiceId = null,
                    confidence = null
                )
            }
        }
    }

    fun selectChoice(choiceId: String) {
        _uiState.value = _uiState.value.copy(selectedChoiceId = choiceId)
    }

    fun selectConfidence(confidence: ConfidenceLevel) {
        _uiState.value = _uiState.value.copy(confidence = confidence)
    }

    fun submitAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val selectedChoiceId = state.selectedChoiceId ?: return
        if (state.isSubmitting) return

        _uiState.value = state.copy(isSubmitting = true)

        viewModelScope.launch {
            val isCorrect = question.choices.find { it.id == selectedChoiceId }?.isCorrect == true
            val attemptId = "attempt_" + System.currentTimeMillis()
            val timeSpent = System.currentTimeMillis() - questionStartTime

            val attempt = Attempt(
                id = attemptId,
                sessionId = sessionId,
                questionId = question.id,
                selectedChoiceId = selectedChoiceId,
                isCorrect = isCorrect,
                confidence = state.confidence,
                mistakeReason = null,
                timeSpentMs = timeSpent,
                timestamp = System.currentTimeMillis()
            )

            quizRepository.finalizeAttempt(attempt)
            _events.emit(
                QuizUiEvent.NavigateToExplanation(
                    sessionId = sessionId,
                    questionId = question.id,
                    attemptId = attemptId
                )
            )
            _uiState.value = _uiState.value.copy(isSubmitting = false)
        }
    }
}
