package jp.co.testreason.feature.explanation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.ui.components.ResponsiveTwoPane
import jp.co.testreason.core.ui.components.ScenarioText
import jp.co.testreason.core.ui.components.label
import jp.co.testreason.core.ui.theme.CorrectGreen
import jp.co.testreason.core.ui.theme.IncorrectRed

@Composable
fun ExplanationScreen(
    viewModel: ExplanationViewModel,
    onNextQuestion: (String) -> Unit,
    onShowResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplanationUiEvent.NavigateToNextQuestion -> onNextQuestion(event.sessionId)
                is ExplanationUiEvent.NavigateToResult -> onShowResult(event.sessionId)
            }
        }
    }

    ExplanationContent(
        uiState = uiState,
        onSelectMistakeReason = viewModel::selectMistakeReason,
        onNext = viewModel::proceedToNext,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationContent(
    uiState: ExplanationUiState,
    onSelectMistakeReason: (MistakeReason) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.question
    val attempt = uiState.attempt

    Scaffold(topBar = { TopAppBar(title = { Text("解答・解説") }) }, modifier = modifier) { padding ->
        if (question == null || attempt == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ResponsiveTwoPane(
                contentPadding = padding,
                primary = {
                    ResultBanner(attempt.isCorrect)
                    ExplanationCard(question)
                    if (!attempt.isCorrect) MistakeReasonSelector(attempt.mistakeReason, onSelectMistakeReason)
                    Button(
                        onClick = onNext,
                        enabled = !uiState.isProcessing && !uiState.isCompleted,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (uiState.isLastQuestion) "結果を見る" else "次の問題へ")
                    }
                },
                secondary = {
                    Text("各選択肢の解説（なぜ正しい/間違っているか）", style = MaterialTheme.typography.titleMedium)
                    question.choices.forEach { choice ->
                        ChoiceAnalysisCard(choice, isSelected = choice.id in attempt.selectedChoiceIds)
                    }
                }
            )
        }
    }
}

@Composable
private fun ResultBanner(isCorrect: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = if (isCorrect) "正解です" else "不正解です" },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) CorrectGreen.copy(alpha = 0.15f) else IncorrectRed.copy(alpha = 0.15f)
        )
    ) {
        Text(
            text = if (isCorrect) "正解！" else "不正解",
            style = MaterialTheme.typography.titleLarge,
            color = if (isCorrect) CorrectGreen else IncorrectRed,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ExplanationCard(question: Question) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "解説（${question.learningObjectiveId} / シラバス ${question.learningObjectiveId.removePrefix("FL-")}）",
                style = MaterialTheme.typography.titleMedium
            )
            question.scenario?.let { scenario ->
                Spacer(modifier = Modifier.padding(top = 8.dp))
                ScenarioText(text = scenario)
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(text = question.explanation, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ChoiceAnalysisCard(choice: Choice, isSelected: Boolean) {
    // Correctness is spelled out in text, not only by colour.
    val status = when {
        choice.isCorrect && isSelected -> "正解（あなたの解答）"
        choice.isCorrect -> "正解"
        isSelected -> "あなたの解答（不正解）"
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                choice.isCorrect -> CorrectGreen.copy(alpha = 0.1f)
                isSelected -> IncorrectRed.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            status?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
            Text(text = "${choice.id}. ${choice.text}", style = MaterialTheme.typography.titleSmall)
            Text(
                text = choice.distractorAnalysis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MistakeReasonSelector(selected: MistakeReason?, onSelect: (MistakeReason) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("間違えた理由（任意）", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "選んだ理由によって次の復習日が変わります。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MistakeReason.entries.forEach { reason ->
                FilterChip(
                    selected = selected == reason,
                    onClick = { onSelect(reason) },
                    label = { Text(reason.label) },
                    // FilterChip is 32dp tall by default; keep the touch target at 48dp.
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}
