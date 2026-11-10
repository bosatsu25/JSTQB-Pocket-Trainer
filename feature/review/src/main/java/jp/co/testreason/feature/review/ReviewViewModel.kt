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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val isLoading: Boolean = true,
    val dueQuestions: List<Question> = emptyList(),
    val isStarting: Boolean = false
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
            _uiState.update { it.copy(isLoading = true) }
            quizRepository.observeDueReviewQuestions().collect { questions ->
                _uiState.update { it.copy(isLoading = false, dueQuestions = questions) }
            }
        }
    }

    fun startDueReviewSession() {
        if (_uiState.value.isStarting) return
        _uiState.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            val session = quizRepository.createDueReviewSession()
            if (session != null) {
                // Stay locked: this screen is removed from the back stack when the session opens.
                _events.emit(ReviewUiEvent.NavigateToQuiz(session.id))
            } else {
                // The queue emptied since it was displayed; there is nothing to start.
                _uiState.update { it.copy(isStarting = false) }
            }
        }
    }
}
