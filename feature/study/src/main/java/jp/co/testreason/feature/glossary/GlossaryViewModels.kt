package jp.co.testreason.feature.glossary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.content.GlossarySource
import jp.co.testreason.core.model.GlossaryTerm
import jp.co.testreason.core.model.TimeProvider
import jp.co.testreason.feature.drill.AccidentalTapGuard
import jp.co.testreason.feature.drill.DrillLap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ---------------------------------------------------------------- list

data class GlossaryUiState(
    val isLoading: Boolean = true,
    val terms: List<GlossaryTerm> = emptyList(),
    /** null = every chapter. */
    val chapter: Int? = null
) {
    val visibleTerms: List<GlossaryTerm> get() = terms.inChapter(chapter)
    fun countIn(chapter: Int?): Int = terms.inChapter(chapter).size
}

internal fun List<GlossaryTerm>.inChapter(chapter: Int?): List<GlossaryTerm> =
    if (chapter == null) this else filter { chapter in it.chapters }

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    private val glossarySource: GlossarySource
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlossaryUiState())
    val uiState: StateFlow<GlossaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val terms = withContext(Dispatchers.IO) { glossarySource.load() }
            _uiState.update { it.copy(isLoading = false, terms = terms) }
        }
    }

    fun selectChapter(chapter: Int?) = _uiState.update { it.copy(chapter = chapter) }
}

// ---------------------------------------------------------------- cards

data class GlossaryCardsUiState(
    val isLoading: Boolean = true,
    val lap: DrillLap = DrillLap.start(emptyList()),
    val term: GlossaryTerm? = null,
    val isRevealed: Boolean = false
) {
    val isEmpty: Boolean get() = !isLoading && lap.total == 0
    val isFinished: Boolean get() = !isLoading && lap.total > 0 && lap.isFinished
}

/**
 * Flash cards over the glossary: recall the meaning, turn the card, and say whether you knew it. A term
 * marked "まだ" comes back until it is marked "覚えた". Like the rapid drill, nothing is recorded.
 */
@HiltViewModel
class GlossaryCardsViewModel @Inject constructor(
    private val glossarySource: GlossarySource,
    timeProvider: TimeProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapter: Int? = savedStateHandle.get<Int>("chapter")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(GlossaryCardsUiState())
    val uiState: StateFlow<GlossaryCardsUiState> = _uiState.asStateFlow()

    private var terms: Map<String, GlossaryTerm> = emptyMap()

    /** "意味を見る" is replaced by "まだ / 覚えた" in the same place: a double tap must not grade the card. */
    private val tapGuard = AccidentalTapGuard(timeProvider)

    init {
        viewModelScope.launch {
            val scope = withContext(Dispatchers.IO) { glossarySource.load() }.inChapter(chapter)
            terms = scope.associateBy { it.id }
            val saved = restoreLap()
            show(saved ?: DrillLap.start(scope.map { it.id }.shuffled()))
        }
    }

    fun reveal() {
        if (_uiState.value.term == null || _uiState.value.isRevealed) return
        tapGuard.arm()
        _uiState.update { it.copy(isRevealed = true) }
    }

    fun grade(remembered: Boolean) {
        val state = _uiState.value
        if (!state.isRevealed || tapGuard.blocks()) return
        show(state.lap.answered(correct = remembered))
    }

    /** Another lap over the same terms, or only over the ones marked "まだ" in the lap just finished. */
    fun restart(onlyMissed: Boolean) {
        val lap = _uiState.value.lap
        val ids = if (onlyMissed) lap.questionIds.filter { it in lap.missed } else lap.questionIds
        show(DrillLap.start(ids.shuffled()))
    }

    private fun show(lap: DrillLap) {
        savedStateHandle[KEY_IDS] = ArrayList(lap.questionIds)
        savedStateHandle[KEY_QUEUE] = ArrayList(lap.queue)
        savedStateHandle[KEY_MISSED] = ArrayList(lap.missed)
        savedStateHandle[KEY_ANSWERS] = lap.answerCount
        _uiState.value = GlossaryCardsUiState(isLoading = false, lap = lap, term = lap.current?.let { terms[it] })
    }

    private fun restoreLap(): DrillLap? {
        val ids = savedStateHandle.get<ArrayList<String>>(KEY_IDS)?.filter { it in terms } ?: return null
        return DrillLap(
            questionIds = ids,
            queue = savedStateHandle.get<ArrayList<String>>(KEY_QUEUE).orEmpty().filter { it in terms },
            missed = savedStateHandle.get<ArrayList<String>>(KEY_MISSED).orEmpty().filter { it in terms }.toSet(),
            answerCount = savedStateHandle.get<Int>(KEY_ANSWERS) ?: 0
        )
    }

    private companion object {
        const val KEY_IDS = "cards_ids"
        const val KEY_QUEUE = "cards_queue"
        const val KEY_MISSED = "cards_missed"
        const val KEY_ANSWERS = "cards_answers"
    }
}
