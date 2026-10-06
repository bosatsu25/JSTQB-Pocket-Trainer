package jp.co.testreason.feature.explanation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.ui.theme.CorrectGreen
import jp.co.testreason.core.ui.theme.IncorrectRed

@OptIn(ExperimentalMaterial3Api::class)
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

    val question = uiState.question
    val attempt = uiState.attempt

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("解答・解説") }
            )
        },
        modifier = modifier
    ) { padding ->
        if (question == null || attempt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Correct / Incorrect Banner
                val isCorrect = attempt.isCorrect
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = if (isCorrect) "正解です" else "不正解です" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) CorrectGreen.copy(alpha = 0.15f) else IncorrectRed.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCorrect) "正解！" else "不正解",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isCorrect) CorrectGreen else IncorrectRed
                        )
                    }
                }

                // Question & General Explanation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "解説 (${question.learningObjectiveId})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Distractor Analysis per choice
                Text(
                    text = "各選択肢の解説（なぜ正しい/間違っているか）",
                    style = MaterialTheme.typography.titleMedium
                )

                question.choices.forEach { choice ->
                    val isSelected = choice.id == attempt.selectedChoiceId
                    val isChoiceCorrect = choice.isCorrect

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isChoiceCorrect -> CorrectGreen.copy(alpha = 0.1f)
                                isSelected -> IncorrectRed.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${choice.id}. ${choice.text}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isChoiceCorrect) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("正解") }
                                    )
                                } else if (isSelected) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("あなたの解答") }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = choice.distractorAnalysis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Optional Mistake Tagging (Visible if incorrect)
                if (!attempt.isCorrect) {
                    Text(
                        text = "間違えた理由タグ（任意）",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MistakeReason.entries.forEach { reason ->
                            val isSelected = attempt.mistakeReason == reason
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectMistakeReason(reason) },
                                label = { Text(reason.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Next Button
                Button(
                    onClick = { viewModel.proceedToNext() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .semantics { contentDescription = if (uiState.isLastQuestion) "結果を見る" else "次の問題へ" },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (uiState.isLastQuestion) "結果を見る" else "次の問題へ")
                }
            }
        }
    }
}
