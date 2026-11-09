package jp.co.testreason.feature.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    AnalyticsContent(uiState = uiState, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsContent(uiState: AnalyticsUiState, modifier: Modifier = Modifier) {
    Scaffold(topBar = { TopAppBar(title = { Text("学習分析") }) }, modifier = modifier) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            uiState.chapters.forEach { chapter -> ChapterCard(chapter) }
        }
    }
}

@Composable
private fun ChapterCard(progress: ChapterProgress) {
    val masteryText = progress.averageMastery?.let { "習熟度 ${(it * 100).toInt()}%" } ?: "未着手"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "第${progress.chapter}章 ${progress.title}、$masteryText、" +
                    "学習目的 ${progress.answeredObjectives} / ${progress.totalObjectives} に回答済み"
            },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("第${progress.chapter}章 ${progress.title}", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "シラバスの学習時間 ${progress.studyMinutes}分",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.averageMastery ?: 0f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$masteryText ・ 学習目的 ${progress.answeredObjectives} / ${progress.totalObjectives} に回答済み",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
