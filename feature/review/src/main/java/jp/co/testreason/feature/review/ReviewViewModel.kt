package jp.co.testreason.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Question
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val isLoading: Boolean = true,
    val dueQuestions: List<Question> = emptyList()
)

sealed interface ReviewUiEvent {
    data class NavigateToQuiz(val sessionId: String) : ReviewUiEvent
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReviewUiEvent>()
    val events: SharedFlow<ReviewUiEvent> = _events.asSharedFlow()

    init {
        loadReviewQueue()
    }

    fun loadReviewQueue() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            quizRepository.observeDueReviewQuestions().collect { questions ->
                _uiState.value = ReviewUiState(
                    isLoading = false,
                    dueQuestions = questions
                )
            }
        }
    }

    fun startDueReviewSession() {
        viewModelScope.launch {
            val session = quizRepository.createDueReviewSession()
            _events.emit(ReviewUiEvent.NavigateToQuiz(session.id))
        }
    }
}
