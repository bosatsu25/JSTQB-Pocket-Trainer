package jp.co.testreason.feature.quiz

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.ui.components.ResponsiveTwoPane
import jp.co.testreason.core.ui.components.ScenarioText
import jp.co.testreason.core.ui.components.label

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
                is QuizUiEvent.NavigateToExplanation ->
                    onAnswerSubmitted(event.sessionId, event.questionId, event.attemptId)
            }
        }
    }

    QuizContent(
        uiState = uiState,
        onSelectChoice = viewModel::selectChoice,
        onSelectConfidence = viewModel::selectConfidence,
        onSubmit = viewModel::submitAnswer,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizContent(
    uiState: QuizUiState,
    onSelectChoice: (String) -> Unit,
    onSelectConfidence: (ConfidenceLevel) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.session
    val question = uiState.currentQuestion

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (session != null) "問題 ${session.currentQuestionIndex + 1} / ${session.questionIds.size}" else "クイズ")
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (question == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ResponsiveTwoPane(
                contentPadding = padding,
                primary = { QuestionCard(question) },
                secondary = {
                    ChoiceList(question, uiState.selectedChoiceIds, onSelectChoice)
                    ConfidenceSelector(uiState.confidence, onSelectConfidence)
                    Button(
                        onClick = onSubmit,
                        enabled = uiState.canSubmit,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("解答を確定する")
                    }
                }
            )
        }
    }
}

@Composable
private fun QuestionCard(question: Question) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val lo = question.learningObjective
            Text(
                text = if (lo != null) "${lo.id}（${lo.kLevel}）${lo.summary}" else question.learningObjectiveId,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            question.scenario?.let { scenario ->
                Spacer(modifier = Modifier.padding(top = 8.dp))
                ScenarioText(text = scenario)
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(text = question.stem, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ChoiceList(question: Question, selected: Set<String>, onSelectChoice: (String) -> Unit) {
    val isMultiSelect = question.selectCount > 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (isMultiSelect) "選択肢（${question.selectCount}つ選択）" else "選択肢",
            style = MaterialTheme.typography.titleMedium
        )
        question.choices.forEach { choice ->
            ChoiceCard(choice, isSelected = choice.id in selected, isMultiSelect = isMultiSelect) {
                onSelectChoice(choice.id)
            }
        }
    }
}

@Composable
private fun ChoiceCard(choice: Choice, isSelected: Boolean, isMultiSelect: Boolean, onClick: () -> Unit) {
    val selectionModifier = if (isMultiSelect) {
        Modifier.toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
    }
    Card(
        modifier = Modifier.fillMaxWidth().then(selectionModifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The whole card is the touch target; the control only shows the state.
            if (isMultiSelect) Checkbox(checked = isSelected, onCheckedChange = null) else RadioButton(selected = isSelected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "${choice.id}. ${choice.text}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfidenceSelector(confidence: ConfidenceLevel?, onSelectConfidence: (ConfidenceLevel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("確信度（任意）", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "選ばない場合は「未選択」として、復習が早めに来ます。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConfidenceLevel.entries.forEach { level ->
                FilterChip(
                    selected = confidence == level,
                    onClick = { onSelectConfidence(level) },
                    label = { Text(level.label) },
                    // FilterChip is 32dp tall by default; keep the touch target at 48dp.
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}
