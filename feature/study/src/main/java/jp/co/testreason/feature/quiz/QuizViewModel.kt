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
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizUiState(
    val session: QuizSession? = null,
    val currentQuestion: Question? = null,
    val selectedChoiceIds: Set<String> = emptySet(),
    val confidence: ConfidenceLevel? = null,
    val isSubmitting: Boolean = false,
    /** The answer has been recorded; the screen is leaving, so further taps must not submit again. */
    val isSubmitted: Boolean = false
) {
    val requiredSelectionCount: Int get() = currentQuestion?.selectCount ?: 1

    val canSubmit: Boolean
        get() = currentQuestion != null && selectedChoiceIds.size == requiredSelectionCount && !isSubmitting && !isSubmitted
}

sealed interface QuizUiEvent {
    data class NavigateToExplanation(val sessionId: String, val questionId: String, val attemptId: String) : QuizUiEvent
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val timeProvider: TimeProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuizUiEvent>()
    val events: SharedFlow<QuizUiEvent> = _events.asSharedFlow()

    private var questionStartTime: Long = timeProvider.currentTimeMillis()

    init {
        loadSession()
    }

    fun loadSession() {
        viewModelScope.launch {
            val session = quizRepository.getSessionById(sessionId)
            if (session != null && session.currentQuestionIndex < session.questionIds.size) {
                val qId = session.questionIds[session.currentQuestionIndex]
                val question = quizRepository.getQuestionById(qId)
                questionStartTime = timeProvider.currentTimeMillis()
                // Input made before process death is restored, but only for the question it was made on.
                val isSameQuestion = savedStateHandle.get<String>(KEY_QUESTION) == qId
                _uiState.value = QuizUiState(
                    session = session,
                    currentQuestion = question,
                    selectedChoiceIds = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED).orEmpty().toSet().takeIf { isSameQuestion }.orEmpty(),
                    confidence = savedStateHandle.get<String>(KEY_CONFIDENCE)
                        ?.let { name -> ConfidenceLevel.entries.firstOrNull { it.name == name } }
                        .takeIf { isSameQuestion }
                )
            }
        }
    }

    /**
     * Single-answer questions replace the selection; multi-answer questions toggle the choice
     * and ignore taps beyond the required count.
     */
    fun selectChoice(choiceId: String) {
        _uiState.update { state ->
            if (state.isSubmitting || state.isSubmitted) return@update state
            val required = state.requiredSelectionCount
            val current = state.selectedChoiceIds
            val updated = when {
                required == 1 -> setOf(choiceId)
                choiceId in current -> current - choiceId
                current.size < required -> current + choiceId
                else -> current
            }
            state.copy(selectedChoiceIds = updated)
        }
        saveInput()
    }

    fun selectConfidence(confidence: ConfidenceLevel) {
        _uiState.update { it.copy(confidence = confidence) }
        saveInput()
    }

    private fun saveInput() {
        val state = _uiState.value
        savedStateHandle[KEY_QUESTION] = state.currentQuestion?.id
        savedStateHandle[KEY_SELECTED] = ArrayList(state.selectedChoiceIds)
        savedStateHandle[KEY_CONFIDENCE] = state.confidence?.name
    }

    fun submitAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (!state.canSubmit) return
        val selectedChoiceIds = state.selectedChoiceIds.sorted()

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val isCorrect = question.isCorrectSelection(selectedChoiceIds)
            val now = timeProvider.currentTimeMillis()
            val attemptId = "attempt_$now"
            val timeSpent = now - questionStartTime

            val attempt = Attempt(
                id = attemptId,
                sessionId = sessionId,
                questionId = question.id,
                selectedChoiceIds = selectedChoiceIds,
                isCorrect = isCorrect,
                confidence = state.confidence,
                mistakeReason = null,
                timeSpentMs = timeSpent,
                timestamp = now
            )

            quizRepository.finalizeAttempt(attempt)
            _events.emit(
                QuizUiEvent.NavigateToExplanation(
                    sessionId = sessionId,
                    questionId = question.id,
                    attemptId = attemptId
                )
            )
            // Not reset to "can submit": a tap after this point would record and navigate a second time.
            _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
        }
    }

    private companion object {
        const val KEY_QUESTION = "quiz_question"
        const val KEY_SELECTED = "quiz_selected"
        const val KEY_CONFIDENCE = "quiz_confidence"
    }
}
