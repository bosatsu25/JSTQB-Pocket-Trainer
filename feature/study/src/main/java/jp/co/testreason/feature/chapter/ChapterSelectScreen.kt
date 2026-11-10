package jp.co.testreason.feature.chapter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

@Composable
fun ChapterSelectScreen(
    viewModel: ChapterSelectViewModel,
    onStartQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
        viewModel.events.collect { event ->
            when (event) {
                is ChapterSelectEvent.NavigateToQuiz -> onStartQuiz(event.sessionId)
            }
        }
    }

    ChapterSelectContent(uiState = uiState, onStartChapter = viewModel::startChapter, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterSelectContent(
    uiState: ChapterSelectUiState,
    onStartChapter: (chapter: Int, count: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { TopAppBar(title = { Text("章別学習") }) }, modifier = modifier) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            Text(
                text = "未出題の問題から優先して出題します。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.chapters.forEach { chapter ->
                ChapterCard(chapter, enabled = !uiState.isStarting, onStart = { count -> onStartChapter(chapter.chapter, count) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterCard(chapter: ChapterOverview, enabled: Boolean, onStart: (Int?) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("第${chapter.chapter}章 ${chapter.title}", style = MaterialTheme.typography.titleSmall)
            val mastery = chapter.mastery?.let { "習熟度 ${(it * 100).toInt()}%" } ?: "未着手"
            Text(
                text = "全${chapter.questionCount}問 ・ 未出題 ${chapter.unseenCount}問 ・ $mastery",
                style = MaterialTheme.typography.bodySmall
            )
            if (chapter.questionCount == 0) {
                Text("この章の問題はまだありません。", style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QUICK_COUNTS.filter { it < chapter.questionCount }.forEach { count ->
                        CountButton("${count}問", enabled) { onStart(count) }
                    }
                    CountButton("全${chapter.questionCount}問", enabled) { onStart(null) }
                }
            }
        }
    }
}

@Composable
private fun CountButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) { Text(label) }
}

private val QUICK_COUNTS = listOf(5, 10)
