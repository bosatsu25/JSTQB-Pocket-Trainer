package jp.co.testreason.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.testreason.core.data.MasteryRepository
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.Syllabus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChapterProgress(
    val chapter: Int,
    val title: String,
    val studyMinutes: Int,
    val answeredObjectives: Int,
    val totalObjectives: Int,
    /** Average mastery of the answered LOs of this chapter, or null when none is answered yet. */
    val averageMastery: Float?
)

data class AnalyticsUiState(
    val chapters: List<ChapterProgress> = emptyList()
)

/** Chapter-level progress. The full LO dashboard (K-levels, heatmap, mistake tags) is issue #18. */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    masteryRepository: MasteryRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = masteryRepository.getAllMasteries()
        .map { AnalyticsUiState(chapterProgress(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AnalyticsUiState(chapterProgress(emptyList())))

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
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
