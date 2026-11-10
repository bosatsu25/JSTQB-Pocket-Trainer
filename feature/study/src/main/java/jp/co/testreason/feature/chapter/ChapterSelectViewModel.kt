package jp.co.testreason.feature.chapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.MasteryRepository
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.Question
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

data class ChapterOverview(
    val chapter: Int,
    val title: String,
    val questionCount: Int,
    val unseenCount: Int,
    /** Average mastery of the chapter's answered LOs, or null before any answer. */
    val mastery: Float?
)

data class ChapterSelectUiState(
    val chapters: List<ChapterOverview> = emptyList(),
    val isStarting: Boolean = false
)

sealed interface ChapterSelectEvent {
    data class NavigateToQuiz(val sessionId: String) : ChapterSelectEvent
}

@HiltViewModel
class ChapterSelectViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    masteryRepository: MasteryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterSelectUiState())
    val uiState: StateFlow<ChapterSelectUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChapterSelectEvent>()
    val events: SharedFlow<ChapterSelectEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            quizRepository.syncBundledQuestions()
            combine(
                quizRepository.getAllQuestions(),
                quizRepository.observeAllAttempts(),
                masteryRepository.getAllMasteries(),
                ::chapterOverview
            ).collect { chapters -> _uiState.update { it.copy(chapters = chapters) } }
        }
    }

    /** [count] null = all questions of the chapter. */
    fun startChapter(chapter: Int, count: Int?) {
        if (_uiState.value.isStarting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true) }
            val session = quizRepository.createChapterSession(chapter, count)
            _uiState.update { it.copy(isStarting = false) }
            if (session != null) _events.emit(ChapterSelectEvent.NavigateToQuiz(session.id))
        }
    }
}

fun chapterOverview(questions: List<Question>, attempts: List<Attempt>, masteries: List<Mastery>): List<ChapterOverview> {
    val answered = attempts.map { it.questionId }.toSet()
    val masteryByLo = masteries.associateBy { it.learningObjectiveId }
    return Syllabus.chapters.map { chapter ->
        val chapterQuestions = questions.filter { it.learningObjective?.chapter == chapter.number }
        val answeredMasteries = Syllabus.learningObjectivesOf(chapter.number)
            .mapNotNull { masteryByLo[it.id] }
            .filter { it.totalAttempts > 0 }
        ChapterOverview(
            chapter = chapter.number,
            title = chapter.title,
            questionCount = chapterQuestions.size,
            unseenCount = chapterQuestions.count { it.id !in answered },
            mastery = answeredMasteries.takeIf { it.isNotEmpty() }?.map { it.score }?.average()?.toFloat()
        )
    }
}
