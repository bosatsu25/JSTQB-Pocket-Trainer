package jp.co.testreason.feature.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.QuizSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val session: QuizSession? = null,
    val attempts: List<Attempt> = emptyList(),
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val accuracyPercentage: Int = 0,
    val recommendedNextAction: String = "弱点カテゴリを復習する"
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            val session = quizRepository.getSessionById(sessionId)
            quizRepository.getAttemptsForSession(sessionId).collect { attempts ->
                val total = session?.questionIds?.size ?: attempts.size
                val correct = attempts.count { it.isCorrect }
                val accuracy = if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 0

                val recommendation = if (accuracy >= 80) {
                    "素晴らしい成果です！次の章へ進みましょう。"
                } else {
                    "復習セッションで間違えた問題を確認しましょう。"
                }

                _uiState.value = ResultUiState(
                    session = session,
                    attempts = attempts,
                    totalQuestions = total,
                    correctCount = correct,
                    accuracyPercentage = accuracy,
                    recommendedNextAction = recommendation
                )
            }
        }
    }
}
