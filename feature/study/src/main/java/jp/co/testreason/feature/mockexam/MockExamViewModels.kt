package jp.co.testreason.feature.mockexam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.MockExamRepository
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.ExamBlueprint
import jp.co.testreason.core.model.MockExam
import jp.co.testreason.core.model.MockExamQuestion
import jp.co.testreason.core.model.MockExamResult
import jp.co.testreason.core.model.MockExamStart
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "59:07" style remaining time. */
fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    return "${totalSeconds / SECONDS_PER_MINUTE}:${(totalSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

// ---------------------------------------------------------------- intro

data class MockExamIntroUiState(
    val resumableExamId: String? = null,
    val extendedTime: Boolean = false,
    val isStarting: Boolean = false
)

sealed interface MockExamIntroEvent {
    data class NavigateToExam(val sessionId: String) : MockExamIntroEvent
    data class ShowMessage(val message: String) : MockExamIntroEvent
}

@HiltViewModel
class MockExamIntroViewModel @Inject constructor(
    private val mockExamRepository: MockExamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockExamIntroUiState())
    val uiState: StateFlow<MockExamIntroUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MockExamIntroEvent>()
    val events: SharedFlow<MockExamIntroEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            mockExamRepository.observeUnsubmittedExamId().collect { id -> _uiState.update { it.copy(resumableExamId = id) } }
        }
    }

    fun setExtendedTime(enabled: Boolean) = _uiState.update { it.copy(extendedTime = enabled) }

    /** Unlocks the buttons when the intro is shown (again). */
    fun onScreenShown() = _uiState.update { it.copy(isStarting = false) }

    fun resume() {
        val state = _uiState.value
        val id = state.resumableExamId ?: return
        if (state.isStarting) return
        _uiState.update { it.copy(isStarting = true) }
        viewModelScope.launch { _events.emit(MockExamIntroEvent.NavigateToExam(id)) }
    }

    fun start() {
        if (_uiState.value.isStarting) return
        _uiState.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            when (val result = mockExamRepository.startMockExam(_uiState.value.extendedTime)) {
                // Stay locked while navigating away; onScreenShown() unlocks on return.
                is MockExamStart.Started -> _events.emit(MockExamIntroEvent.NavigateToExam(result.sessionId))
                is MockExamStart.NotEnoughQuestions -> {
                    _uiState.update { it.copy(isStarting = false) }
                    _events.emit(
                        MockExamIntroEvent.ShowMessage(
                            "問題が足りない出題範囲があるため、模擬試験を組めません（${result.missingGroups.size}グループ）。"
                        )
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- exam

data class MockExamUiState(
    val exam: MockExam? = null,
    val currentIndex: Int = 0,
    val remainingMs: Long = 0L,
    val isConfirmingSubmit: Boolean = false,
    val isSubmitting: Boolean = false
) {
    val current: MockExamQuestion? get() = exam?.questions?.getOrNull(currentIndex)
    val total: Int get() = exam?.questions?.size ?: 0
    val unansweredCount: Int get() = exam?.let { it.questions.size - it.answeredCount } ?: 0
    val flaggedCount: Int get() = exam?.questions?.count { it.flagged } ?: 0
    val isTimeLow: Boolean get() = remainingMs in 1..LOW_TIME_MS

    private companion object {
        const val LOW_TIME_MS = 5 * 60_000L
    }
}

sealed interface MockExamEvent {
    data class NavigateToResult(val sessionId: String) : MockExamEvent
}

@HiltViewModel
class MockExamViewModel @Inject constructor(
    private val mockExamRepository: MockExamRepository,
    private val timeProvider: TimeProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(MockExamUiState())
    val uiState: StateFlow<MockExamUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MockExamEvent>()
    val events: SharedFlow<MockExamEvent> = _events.asSharedFlow()

    private var navigated = false

    init {
        viewModelScope.launch {
            mockExamRepository.observeExam(sessionId).collect { exam ->
                _uiState.update { it.copy(exam = exam) }
                onTick()
            }
        }
        viewModelScope.launch { secondTicks().collect { onTick() } }
    }

    /** Updates the countdown; submits automatically once the deadline has passed (also after process death). */
    fun onTick() {
        val exam = _uiState.value.exam ?: return
        if (exam.isSubmitted) {
            navigateToResult()
            return
        }
        val remaining = exam.remainingMs(timeProvider.currentTimeMillis())
        _uiState.update { it.copy(remainingMs = remaining) }
        if (remaining == 0L) submit()
    }

    /**
     * The new selection is worked out by the repository from the saved answer, not from this screen's state:
     * the state lags behind a tap by a database round trip, so a second quick tap would drop the first choice.
     */
    fun select(choiceId: String) {
        val item = _uiState.value.current ?: return
        viewModelScope.launch { mockExamRepository.selectChoice(sessionId, item.question.id, choiceId) }
    }

    fun toggleFlag() {
        val item = _uiState.value.current ?: return
        viewModelScope.launch { mockExamRepository.toggleFlag(sessionId, item.question.id) }
    }

    fun goTo(index: Int) = _uiState.update { it.copy(currentIndex = index.coerceIn(0, (it.total - 1).coerceAtLeast(0))) }

    fun next() = goTo(_uiState.value.currentIndex + 1)

    fun previous() = goTo(_uiState.value.currentIndex - 1)

    fun requestSubmit() = _uiState.update { it.copy(isConfirmingSubmit = true) }

    fun dismissSubmit() = _uiState.update { it.copy(isConfirmingSubmit = false) }

    fun submit() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, isConfirmingSubmit = false) }
        viewModelScope.launch {
            mockExamRepository.submit(sessionId)
            navigateToResult()
        }
    }

    private fun navigateToResult() {
        if (navigated) return
        navigated = true
        viewModelScope.launch { _events.emit(MockExamEvent.NavigateToResult(sessionId)) }
    }

    /** Off the main dispatcher, like the repository clock, so it never spins a test scheduler. */
    private fun secondTicks(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MS)
        }
    }.flowOn(Dispatchers.Default)

    private companion object {
        const val TICK_MS = 1_000L
    }
}

// ---------------------------------------------------------------- result

data class MockExamResultUiState(
    val result: MockExamResult? = null,
    val isStarting: Boolean = false
) {
    val passMark: Int get() = ExamBlueprint.PASS_MARK
}

sealed interface MockExamResultEvent {
    data class NavigateToQuiz(val sessionId: String) : MockExamResultEvent
}

@HiltViewModel
class MockExamResultViewModel @Inject constructor(
    private val mockExamRepository: MockExamRepository,
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(MockExamResultUiState())
    val uiState: StateFlow<MockExamResultUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MockExamResultEvent>()
    val events: SharedFlow<MockExamResultEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(result = mockExamRepository.getResult(sessionId)) }
        }
    }

    /** Wrong exam answers are recorded as attempts, so the weakness queue already contains them. */
    fun startWeaknessReview() {
        if (_uiState.value.isStarting) return
        _uiState.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            val session = quizRepository.createWeaknessSession()
            if (session != null) {
                // Stay locked while navigating away; onScreenShown() unlocks on return.
                _events.emit(MockExamResultEvent.NavigateToQuiz(session.id))
            } else {
                _uiState.update { it.copy(isStarting = false) }
            }
        }
    }

    fun onScreenShown() = _uiState.update { it.copy(isStarting = false) }
}
