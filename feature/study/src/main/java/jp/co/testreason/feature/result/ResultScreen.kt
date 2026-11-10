package jp.co.testreason.feature.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onNavigateHome: () -> Unit,
    onStartQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ResultUiEvent.NavigateToQuiz -> onStartQuiz(event.sessionId)
            }
        }
    }

    ResultContent(
        uiState = uiState,
        onStartRecommended = viewModel::startRecommendedSession,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultContent(
    uiState: ResultUiState,
    onStartRecommended: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { TopAppBar(title = { Text("セッション結果") }) }, modifier = modifier) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "正解率 ${uiState.accuracyPercentage}%" },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("スコア", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${uiState.accuracyPercentage}%", style = MaterialTheme.typography.titleLarge)
                    Text("${uiState.correctCount} / ${uiState.totalQuestions} 問正解", style = MaterialTheme.typography.bodyMedium)
                }
            }

            SummaryCard(uiState.summary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("おすすめの次の学習", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.recommendedAction.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartRecommended,
                        enabled = !uiState.isStarting,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(uiState.recommendedAction.buttonLabel)
                    }
                }
            }

            OutlinedButton(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ホームへ戻る")
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: SessionSummary) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ふりかえり", style = MaterialTheme.typography.titleMedium)
            Text("所要時間 ${summary.totalTimeText}", style = MaterialTheme.typography.bodyMedium)

            if (summary.confidenceRows.isNotEmpty()) {
                Text("確信度と正誤", style = MaterialTheme.typography.labelLarge)
                summary.confidenceRows.forEach { row ->
                    Text("${row.label}：正解 ${row.correct} ／ 不正解 ${row.incorrect}", style = MaterialTheme.typography.bodyMedium)
                }
                val luckyGuesses = summary.confidenceRows
                    .filter { it.confidence == ConfidenceLevel.GUESS || it.confidence == ConfidenceLevel.LOW }
                    .sumOf { it.correct }
                if (luckyGuesses > 0) {
                    Text(
                        text = "自信なしで正解した $luckyGuesses 問は、理解を固めるため早めに復習に入ります。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ObjectiveList("身についた学習目的", summary.strongObjectives)
            ObjectiveList("復習が必要な学習目的", summary.reviewObjectives)
        }
    }
}

@Composable
private fun ObjectiveList(title: String, objectives: List<ObjectiveResult>) {
    if (objectives.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge)
    objectives.forEach { Text("${it.id} ${it.summary}", style = MaterialTheme.typography.bodyMedium) }
}

