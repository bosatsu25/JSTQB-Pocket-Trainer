package jp.co.testreason.feature.drill

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.Syllabus
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------- setup

data class DrillChapter(val chapter: Int, val title: String, val questionCount: Int)

data class DrillSetupUiState(
    val chapters: List<DrillChapter> = emptyList()
) {
    val totalCount: Int get() = chapters.sumOf { it.questionCount }
}

fun drillChapters(questions: List<Question>): List<DrillChapter> = Syllabus.chapters.map { chapter ->
    DrillChapter(chapter.number, chapter.title, questions.count { it.learningObjective?.chapter == chapter.number })
}

@HiltViewModel
class DrillSetupViewModel @Inject constructor(
    quizRepository: QuizRepository
) : ViewModel() {

    val uiState: StateFlow<DrillSetupUiState> = quizRepository.getAllQuestions()
        .map { DrillSetupUiState(drillChapters(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DrillSetupUiState(drillChapters(emptyList())))

    init {
        viewModelScope.launch { quizRepository.syncBundledQuestions() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

// ---------------------------------------------------------------- drill

/** How the current question was resolved; null while the learner is still choosing. */
enum class DrillReveal { CORRECT, WRONG, SHOWN }

data class DrillUiState(
    val isLoading: Boolean = true,
    val lap: DrillLap = DrillLap.start(emptyList()),
    val question: Question? = null,
    val selectedChoiceIds: Set<String> = emptySet(),
    val reveal: DrillReveal? = null,
    /** Per-choice reasons ("なぜ？") are shown on request so the default pace stays fast. */
    val showReasons: Boolean = false,
    val elapsedMs: Long = 0L
) {
    val isEmpty: Boolean get() = !isLoading && lap.total == 0
    val isFinished: Boolean get() = !isLoading && lap.total > 0 && lap.isFinished

    /** Average seconds per answer in the finished lap. */
    val secondsPerAnswer: Int
        get() = if (lap.answerCount == 0) 0 else (elapsedMs / MILLIS_PER_SECOND / lap.answerCount).toInt()

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

/**
 * Rapid drill: tap a choice, see the answer at once, move on; missed questions come back until every
 * question of the lap has been answered correctly. Nothing is written to attempts, mastery or the review
 * schedule - answers given from memory would inflate them (docs/ux/LEARNING_LOOP.md, "Rapid Drill").
 */
@HiltViewModel
class DrillViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val timeProvider: TimeProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapter: Int? = savedStateHandle.get<Int>("chapter")?.takeIf { it > 0 }
    private val count: Int? = savedStateHandle.get<Int>("count")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(DrillUiState())
    val uiState: StateFlow<DrillUiState> = _uiState.asStateFlow()

    private var questions: Map<String, Question> = emptyMap()
    private var startedAt: Long = timeProvider.currentTimeMillis()

    /**
     * In this mode a tap on a choice is the answer, and the next question's choices can appear where "次へ"
     * was. Without the guard, the second tap of a double tap on "次へ" would answer the next question.
     */
    private val tapGuard = AccidentalTapGuard(timeProvider)

    init {
        viewModelScope.launch {
            val saved = restoreLap()
            if (saved != null) {
                restore(saved)
            } else {
                val picked = quizRepository.getDrillQuestions(chapter, count)
                questions = picked.associateBy { it.id }
                startedAt = timeProvider.currentTimeMillis()
                show(DrillLap.start(picked.map { it.id }))
            }
        }
    }

    /** Single-answer questions resolve on the tap; multi-answer ones once the required number is chosen. */
    fun select(choiceId: String) {
        val state = _uiState.value
        val question = state.question ?: return
        if (state.reveal != null || tapGuard.blocks()) return
        val selected = when {
            question.selectCount == 1 -> setOf(choiceId)
            choiceId in state.selectedChoiceIds -> state.selectedChoiceIds - choiceId
            else -> state.selectedChoiceIds + choiceId
        }
        val reveal = if (selected.size == question.selectCount) {
            if (question.isCorrectSelection(selected)) DrillReveal.CORRECT else DrillReveal.WRONG
        } else {
            null
        }
        publish(state.copy(selectedChoiceIds = selected, reveal = reveal))
    }

    /** "I don't know": look at the answer right away; the question comes back later in the lap. */
    fun showAnswer() {
        val state = _uiState.value
        if (state.question == null || state.reveal != null || tapGuard.blocks()) return
        publish(state.copy(reveal = DrillReveal.SHOWN))
    }

    fun toggleReasons() = _uiState.update { it.copy(showReasons = !it.showReasons) }

    fun next() {
        val state = _uiState.value
        val reveal = state.reveal ?: return
        tapGuard.arm()
        show(state.lap.answered(correct = reveal == DrillReveal.CORRECT))
    }

    /** Another lap over the same questions, or only over the ones missed in the lap just finished. */
    fun restart(onlyMissed: Boolean) {
        val lap = _uiState.value.lap
        val ids = if (onlyMissed) lap.questionIds.filter { it in lap.missed } else lap.questionIds
        startedAt = timeProvider.currentTimeMillis()
        show(DrillLap.start(ids.shuffled()))
    }

    private fun show(lap: DrillLap) = publish(
        DrillUiState(
            isLoading = false,
            lap = lap,
            question = lap.current?.let { questions[it] },
            elapsedMs = if (lap.isFinished) timeProvider.currentTimeMillis() - startedAt else 0L
        )
    )

    /**
     * Shows [state] and saves what must survive process death: the lap, an answer that is already shown
     * (otherwise a miss would be forgotten and the question could be retried as a first try), and the
     * time of a finished lap (otherwise time spent away after finishing would be added to it).
     */
    private fun publish(state: DrillUiState) {
        _uiState.value = state
        savedStateHandle[KEY_IDS] = ArrayList(state.lap.questionIds)
        savedStateHandle[KEY_QUEUE] = ArrayList(state.lap.queue)
        savedStateHandle[KEY_MISSED] = ArrayList(state.lap.missed)
        savedStateHandle[KEY_ANSWERS] = state.lap.answerCount
        savedStateHandle[KEY_STARTED_AT] = startedAt
        savedStateHandle[KEY_SELECTED] = ArrayList(state.selectedChoiceIds)
        savedStateHandle[KEY_REVEAL] = state.reveal?.name
        savedStateHandle[KEY_ELAPSED] = state.elapsedMs.takeIf { state.lap.isFinished }
    }

    private fun restoreLap(): DrillLap? {
        val ids = savedStateHandle.get<ArrayList<String>>(KEY_IDS) ?: return null
        startedAt = savedStateHandle.get<Long>(KEY_STARTED_AT) ?: startedAt
        return DrillLap(
            questionIds = ids,
            queue = savedStateHandle.get<ArrayList<String>>(KEY_QUEUE).orEmpty(),
            missed = savedStateHandle.get<ArrayList<String>>(KEY_MISSED).orEmpty().toSet(),
            answerCount = savedStateHandle.get<Int>(KEY_ANSWERS) ?: 0
        )
    }

    private suspend fun restore(saved: DrillLap) {
        // Active questions only: one retired by a content update since the lap was saved is dropped.
        // (getQuestionById also returns retired questions, so it cannot be used here.)
        quizRepository.syncBundledQuestions()
        questions = quizRepository.getAllQuestions().first().filter { it.id in saved.questionIds }.associateBy { it.id }
        val lap = saved.copy(
            questionIds = saved.questionIds.filter { it in questions },
            queue = saved.queue.filter { it in questions },
            missed = saved.missed.filter { it in questions }.toSet()
        )
        val isSameQuestion = lap.current == saved.current
        val reveal = savedStateHandle.get<String>(KEY_REVEAL)?.let { name -> DrillReveal.entries.firstOrNull { it.name == name } }
        publish(
            DrillUiState(
                isLoading = false,
                lap = lap,
                question = lap.current?.let { questions[it] },
                selectedChoiceIds = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED).orEmpty().toSet().takeIf { isSameQuestion }.orEmpty(),
                reveal = reveal.takeIf { isSameQuestion },
                elapsedMs = when {
                    !lap.isFinished -> 0L
                    else -> savedStateHandle.get<Long>(KEY_ELAPSED) ?: (timeProvider.currentTimeMillis() - startedAt)
                }
            )
        )
    }

    private companion object {
        const val KEY_IDS = "drill_ids"
        const val KEY_QUEUE = "drill_queue"
        const val KEY_MISSED = "drill_missed"
        const val KEY_ANSWERS = "drill_answers"
        const val KEY_STARTED_AT = "drill_started_at"
        const val KEY_SELECTED = "drill_selected"
        const val KEY_REVEAL = "drill_reveal"
        const val KEY_ELAPSED = "drill_elapsed"
    }
}
