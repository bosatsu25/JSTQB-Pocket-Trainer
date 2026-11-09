package jp.co.testreason.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.MasteryRepository
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.Syllabus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChapterProgress(
    val chapter: Int,
    val title: String,
    val studyMinutes: Int,
    val answeredObjectives: Int,
    val totalObjectives: Int,
    /** Average mastery of the answered LOs of this chapter, or null when none is answered yet. */
    val averageMastery: Float?,
    val objectives: List<ObjectiveStatus> = emptyList()
)

data class AnalyticsUiState(
    val chapters: List<ChapterProgress> = emptyList(),
    val kLevels: List<KLevelAccuracy> = emptyList(),
    val mistakes: List<MistakeCount> = emptyList(),
    val isStarting: Boolean = false
)

sealed interface AnalyticsEvent {
    data class NavigateToQuiz(val sessionId: String) : AnalyticsEvent
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    masteryRepository: MasteryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildDashboard(emptyList(), emptyList(), emptyList()))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AnalyticsEvent>()
    val events: SharedFlow<AnalyticsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                quizRepository.getAllQuestions(),
                quizRepository.observeAllAttempts(),
                masteryRepository.getAllMasteries(),
                ::buildDashboard
            ).collect { dashboard -> _uiState.update { dashboard.copy(isStarting = it.isStarting) } }
        }
    }

    /** Starts a short session on one LO, typically a weak one. */
    fun startObjective(learningObjectiveId: String) {
        if (_uiState.value.isStarting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true) }
            val session = quizRepository.createLearningObjectiveSession(learningObjectiveId, OBJECTIVE_SESSION_SIZE)
            _uiState.update { it.copy(isStarting = false) }
            if (session != null) _events.emit(AnalyticsEvent.NavigateToQuiz(session.id))
        }
    }

    private companion object {
        const val OBJECTIVE_SESSION_SIZE = 5
    }
}

fun chapterProgress(masteries: List<Mastery>): List<ChapterProgress> {
    val byLo = masteries.associateBy { it.learningObjectiveId }
    return Syllabus.chapters.map { chapter ->
        val los = Syllabus.learningObjectivesOf(chapter.number)
        val answered = los.mapNotNull { byLo[it.id] }.filter { it.totalAttempts > 0 }
        ChapterProgress(
            chapter = chapter.number,
            title = chapter.title,
            studyMinutes = chapter.studyMinutes,
            answeredObjectives = answered.size,
            totalObjectives = los.size,
            averageMastery = answered.takeIf { it.isNotEmpty() }?.map { it.score }?.average()?.toFloat()
        )
    }
}
