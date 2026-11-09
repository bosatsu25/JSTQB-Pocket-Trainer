package jp.co.testreason.feature.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.KLevel
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn
import jp.co.testreason.core.ui.components.label

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onStartQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AnalyticsEvent.NavigateToQuiz -> onStartQuiz(event.sessionId)
            }
        }
    }

    AnalyticsContent(uiState = uiState, onStartObjective = viewModel::startObjective, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    modifier: Modifier = Modifier,
    onStartObjective: (String) -> Unit = {}
) {
    Scaffold(topBar = { TopAppBar(title = { Text("学習分析") }) }, modifier = modifier) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            KLevelCard(uiState.kLevels)
            if (uiState.mistakes.isNotEmpty()) MistakeCard(uiState.mistakes)
            Text(
                text = "学習目的をタップすると、その学習目的の問題（最大5問）を始めます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.chapters.forEach { chapter ->
                ChapterCard(chapter, enabled = !uiState.isStarting, onStartObjective = onStartObjective)
            }
        }
    }
}

private val KLevel.description: String
    get() = when (this) {
        KLevel.K1 -> "K1（記憶）"
        KLevel.K2 -> "K2（理解）"
        KLevel.K3 -> "K3（適用）"
    }

@Composable
private fun KLevelCard(kLevels: List<KLevelAccuracy>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("知識レベル別の正答率", style = MaterialTheme.typography.titleSmall)
            kLevels.forEach { level ->
                val value = level.percent?.let { "$it%（${level.correct}/${level.answered}問）" } ?: "未回答"
                Text("${level.kLevel.description}：$value", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "K3は計算・適用問題です。正答率が低い場合は第4・5章の章別学習が効果的です。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MistakeCard(mistakes: List<MistakeCount>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("間違えた理由の内訳", style = MaterialTheme.typography.titleSmall)
            mistakes.forEach { mistake ->
                Text("${mistake.reason?.label ?: "理由未選択"}：${mistake.count}回", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ChapterCard(progress: ChapterProgress, enabled: Boolean, onStartObjective: (String) -> Unit) {
    val masteryText = progress.averageMastery?.let { "習熟度 ${(it * 100).toInt()}%" } ?: "未着手"
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "第${progress.chapter}章 $masteryText" }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$masteryText ・ 学習目的 ${progress.answeredObjectives} / ${progress.totalObjectives} に回答済み",
                style = MaterialTheme.typography.bodySmall
            )
            progress.objectives.forEach { objective ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ObjectiveRow(objective, enabled = enabled && objective.questionCount > 0) { onStartObjective(objective.id) }
            }
        }
    }
}

@Composable
private fun ObjectiveRow(objective: ObjectiveStatus, enabled: Boolean, onClick: () -> Unit) {
    // The state is written out as text so it does not rely on colour.
    val score = objective.score?.let { " ${(it * 100).toInt()}%" }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${objective.id}（${objective.kLevel}）", style = MaterialTheme.typography.labelMedium)
            Text(objective.summary, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = objective.state.label + score,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (objective.state == ObjectiveState.WEAK) FontWeight.Bold else FontWeight.Normal,
            color = if (objective.state == ObjectiveState.WEAK) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
