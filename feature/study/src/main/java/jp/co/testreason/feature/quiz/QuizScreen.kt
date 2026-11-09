package jp.co.testreason.feature.quiz

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.ui.components.ScenarioText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onAnswerSubmitted: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuizUiEvent.NavigateToExplanation -> {
                    onAnswerSubmitted(event.sessionId, event.questionId, event.attemptId)
                }
            }
        }
    }

    val question = uiState.currentQuestion
    val session = uiState.session

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (session != null) {
                        Text("問題 ${session.currentQuestionIndex + 1} / ${session.questionIds.size}")
                    } else {
                        Text("クイズ")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (question == null) {
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
                // LO Tag & Stem
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val lo = question.learningObjective
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(if (lo != null) "${lo.id} (${lo.kLevel}) ${lo.summary}" else question.learningObjectiveId)
                            }
                        )
                        question.scenario?.let { scenario ->
                            Spacer(modifier = Modifier.height(8.dp))
                            ScenarioText(text = scenario)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.stem,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Choices
                val isMultiSelect = question.selectCount > 1
                Text(
                    text = if (isMultiSelect) "選択肢（${question.selectCount}つ選択）" else "選択肢",
                    style = MaterialTheme.typography.titleMedium
                )
                question.choices.forEach { choice ->
                    val isSelected = choice.id in uiState.selectedChoiceIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "選択肢 ${choice.id}: ${choice.text}" }
                            .clickable { viewModel.selectChoice(choice.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMultiSelect) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.selectChoice(choice.id) }
                                )
                            } else {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectChoice(choice.id) }
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${choice.id}. ${choice.text}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Confidence Selector
                Text(
                    text = "確信度（任意）",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfidenceLevel.entries.forEach { level ->
                        val isSelected = uiState.confidence == level
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectConfidence(level) },
                            label = { Text(level.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = { viewModel.submitAnswer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .semantics { contentDescription = "解答を確定する" },
                    enabled = uiState.canSubmit,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("解答を確定する")
                }
            }
        }
    }
}
