package jp.co.testreason.feature.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.QuizSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The next session the result screen offers to start directly. */
enum class RecommendedAction(val description: String, val buttonLabel: String) {
    WEAKNESS("間違えた・自信のなかった問題を弱点セッションで解き直しましょう。", "弱点セッションを開始"),
    DAILY("よくできました。次のおすすめセッションで範囲を広げましょう。", "次のおすすめを開始")
}

data class ResultUiState(
    val session: QuizSession? = null,
    val attempts: List<Attempt> = emptyList(),
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val accuracyPercentage: Int = 0,
    val recommendedAction: RecommendedAction = RecommendedAction.DAILY,
    val isStarting: Boolean = false
)

sealed interface ResultUiEvent {
    data class NavigateToQuiz(val sessionId: String) : ResultUiEvent
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ResultUiEvent>()
    val events: SharedFlow<ResultUiEvent> = _events.asSharedFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            val session = quizRepository.getSessionById(sessionId)
            quizRepository.getAttemptsForSession(sessionId).collect { attempts ->
                val total = session?.questionIds?.size ?: attempts.size
                val correct = attempts.count { it.isCorrect }
                val accuracy = if (total > 0) correct * PERCENT / total else 0
                _uiState.value = _uiState.value.copy(
                    session = session,
                    attempts = attempts,
                    totalQuestions = total,
                    correctCount = correct,
                    accuracyPercentage = accuracy,
                    recommendedAction = if (accuracy >= GOOD_ACCURACY) RecommendedAction.DAILY else RecommendedAction.WEAKNESS
                )
            }
        }
    }

    /** Starts the recommended session; falls back to the daily mix when no weak question exists. */
    fun startRecommendedSession() {
        val state = _uiState.value
        if (state.isStarting) return
        _uiState.value = state.copy(isStarting = true)
        viewModelScope.launch {
            val session = when (state.recommendedAction) {
                RecommendedAction.WEAKNESS -> quizRepository.createWeaknessSession() ?: quizRepository.createDailySession()
                RecommendedAction.DAILY -> quizRepository.createDailySession()
            }
            _uiState.value = _uiState.value.copy(isStarting = false)
            if (session != null) _events.emit(ResultUiEvent.NavigateToQuiz(session.id))
        }
    }

    private companion object {
        const val PERCENT = 100
        const val GOOD_ACCURACY = 80
    }
}
