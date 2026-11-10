package jp.co.testreason.feature.mockexam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.ExamBlueprint
import jp.co.testreason.core.model.MockExamQuestion
import jp.co.testreason.core.model.MockExamResultItem
import jp.co.testreason.core.model.ScoreLine
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn
import jp.co.testreason.core.ui.components.ResponsiveTwoPane
import jp.co.testreason.core.ui.components.ScenarioText

// ---------------------------------------------------------------- intro

@Composable
fun MockExamIntroScreen(viewModel: MockExamIntroViewModel, onOpenExam: (String) -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
        viewModel.events.collect { event ->
            when (event) {
                is MockExamIntroEvent.NavigateToExam -> onOpenExam(event.sessionId)
                is MockExamIntroEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }
    MockExamIntroContent(
        uiState = uiState,
        onExtendedTimeChange = viewModel::setExtendedTime,
        onStart = viewModel::start,
        onResume = viewModel::resume,
        snackbarHostState = snackbar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamIntroContent(
    uiState: MockExamIntroUiState,
    onExtendedTimeChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("模擬試験") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("本番と同じ形式", style = MaterialTheme.typography.titleMedium)
                    Text("・${ExamBlueprint.TOTAL_QUESTIONS}問・${ExamBlueprint.DURATION_MINUTES}分", style = MaterialTheme.typography.bodyMedium)
                    Text("・${ExamBlueprint.PASS_MARK}問以上の正解（65%）で合格", style = MaterialTheme.typography.bodyMedium)
                    Text("・章と知識レベル（K1 8問 / K2 24問 / K3 8問）の配分は公式の出題構成どおり", style = MaterialTheme.typography.bodyMedium)
                    Text("・解答中は解説を表示しません。提出までは解答を変更できます", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "出典: ${ExamBlueprint.SOURCE}、JSTQB 受験案内",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (uiState.resumableExamId != null) {
                // One exam at a time: an open exam must be submitted before a new one can start.
                Text(
                    text = "提出していない模擬試験があります。再開して提出すると、新しい模擬試験を始められます。" +
                        "制限時間を過ぎている場合は、再開した時点で自動的に提出されます。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onResume,
                    enabled = !uiState.isStarting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("途中の模擬試験を再開")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(value = uiState.extendedTime, role = Role.Switch, onValueChange = onExtendedTimeChange),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("時間延長（${ExamBlueprint.EXTENDED_DURATION_MINUTES}分）", style = MaterialTheme.typography.titleSmall)
                        Text("日本語を母語としない受験者向けの延長と同じ時間です", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = uiState.extendedTime, onCheckedChange = null)
                }
                Button(
                    onClick = onStart,
                    enabled = !uiState.isStarting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("模擬試験を開始")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- exam

@Composable
fun MockExamScreen(viewModel: MockExamViewModel, onShowResult: (String) -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MockExamEvent.NavigateToResult -> onShowResult(event.sessionId)
            }
        }
    }
    MockExamContent(
        uiState = uiState,
        actions = MockExamActions(
            onSelect = viewModel::select,
            onToggleFlag = viewModel::toggleFlag,
            onGoTo = viewModel::goTo,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onRequestSubmit = viewModel::requestSubmit,
            onDismissSubmit = viewModel::dismissSubmit,
            onConfirmSubmit = viewModel::submit
        ),
        modifier = modifier
    )
}

data class MockExamActions(
    val onSelect: (String) -> Unit = {},
    val onToggleFlag: () -> Unit = {},
    val onGoTo: (Int) -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onRequestSubmit: () -> Unit = {},
    val onDismissSubmit: () -> Unit = {},
    val onConfirmSubmit: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamContent(uiState: MockExamUiState, actions: MockExamActions, modifier: Modifier = Modifier) {
    val current = uiState.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "残り ${formatRemaining(uiState.remainingMs)}" + if (uiState.isTimeLow) "（残りわずか）" else "",
                        color = if (uiState.isTimeLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        ResponsiveTwoPane(
            contentPadding = padding,
            primary = {
                Text("問題 ${uiState.currentIndex + 1} / ${uiState.total}", style = MaterialTheme.typography.titleMedium)
                ExamQuestion(current, actions.onSelect)
                FlagAndPaging(uiState, current, actions)
            },
            secondary = {
                Text(
                    text = "回答済み ${uiState.total - uiState.unansweredCount} / ${uiState.total} ・ 見直し ${uiState.flaggedCount}",
                    style = MaterialTheme.typography.titleSmall
                )
                Navigator(uiState, actions.onGoTo)
                Button(
                    onClick = actions.onRequestSubmit,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("提出する")
                }
            }
        )
        if (uiState.isConfirmingSubmit) {
            AlertDialog(
                onDismissRequest = actions.onDismissSubmit,
                title = { Text("提出しますか？") },
                text = {
                    Text(
                        if (uiState.unansweredCount > 0) "未回答が ${uiState.unansweredCount} 問あります。未回答は不正解として採点します。"
                        else "提出後は解答を変更できません。"
                    )
                },
                confirmButton = { TextButton(onClick = actions.onConfirmSubmit) { Text("提出する") } },
                dismissButton = { TextButton(onClick = actions.onDismissSubmit) { Text("戻る") } }
            )
        }
    }
}

@Composable
private fun ExamQuestion(item: MockExamQuestion, onSelect: (String) -> Unit) {
    val question = item.question
    val isMulti = question.selectCount > 1
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            question.scenario?.let {
                ScenarioText(text = it)
                Spacer(modifier = Modifier.padding(top = 8.dp))
            }
            Text(question.stem, style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (isMulti) Text("${question.selectCount}つ選択", style = MaterialTheme.typography.labelLarge)
    question.choices.forEach { choice ->
        val selected = choice.id in item.selectedChoiceIds
        val selection = if (isMulti) {
            Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onSelect(choice.id) })
        } else {
            Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(choice.id) })
        }
        Card(
            modifier = Modifier.fillMaxWidth().then(selection),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isMulti) Checkbox(checked = selected, onCheckedChange = null) else RadioButton(selected = selected, onClick = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("${choice.id}. ${choice.text}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagAndPaging(uiState: MockExamUiState, current: MockExamQuestion, actions: MockExamActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = actions.onPrevious, enabled = uiState.currentIndex > 0, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("前へ")
        }
        FilledTonalButton(onClick = actions.onToggleFlag, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (current.flagged) "見直し解除" else "見直しマーク")
        }
        OutlinedButton(onClick = actions.onNext, enabled = uiState.currentIndex < uiState.total - 1, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("次へ")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Navigator(uiState: MockExamUiState, onGoTo: (Int) -> Unit) {
    val questions = uiState.exam?.questions.orEmpty()
    Text("問題一覧（✓=回答済み、?=見直し）", style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        questions.forEachIndexed { index, item ->
            val marks = (if (item.isAnswered) "✓" else "") + (if (item.flagged) "?" else "")
            val state = listOfNotNull(
                if (item.isAnswered) "回答済み" else "未回答",
                if (item.flagged) "見直し" else null,
                if (index == uiState.currentIndex) "表示中" else null
            ).joinToString("、")
            val label = "${index + 1}$marks"
            if (index == uiState.currentIndex) {
                Button(
                    onClick = { onGoTo(index) },
                    modifier = Modifier.widthIn(min = 56.dp).heightIn(min = 48.dp).semantics { contentDescription = "問題${index + 1} $state" }
                ) { Text(label) }
            } else {
                OutlinedButton(
                    onClick = { onGoTo(index) },
                    modifier = Modifier.widthIn(min = 56.dp).heightIn(min = 48.dp).semantics { contentDescription = "問題${index + 1} $state" }
                ) { Text(label) }
            }
        }
    }
}

// ---------------------------------------------------------------- result

@Composable
fun MockExamResultScreen(
    viewModel: MockExamResultViewModel,
    onStartQuiz: (String) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
        viewModel.events.collect { event ->
            when (event) {
                is MockExamResultEvent.NavigateToQuiz -> onStartQuiz(event.sessionId)
            }
        }
    }
    MockExamResultContent(uiState, viewModel::startWeaknessReview, onNavigateHome, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamResultContent(
    uiState: MockExamResultUiState,
    onStartWeakness: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val result = uiState.result
    Scaffold(topBar = { TopAppBar(title = { Text("模擬試験の結果") }) }, modifier = modifier) { padding ->
        if (result == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        ResponsiveScreenColumn(contentPadding = padding) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.passed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(if (result.passed) "合格ライン到達" else "合格ライン未達", style = MaterialTheme.typography.titleLarge)
                    Text("${result.correct} / ${result.total} 問正解（合格ライン ${uiState.passMark} 問）", style = MaterialTheme.typography.bodyLarge)
                    if (result.unanswered > 0) Text("未回答 ${result.unanswered} 問", style = MaterialTheme.typography.bodyMedium)
                }
            }
            ScoreCard("章別", result.byChapter)
            ScoreCard("知識レベル別", result.byKLevel)
            Button(
                onClick = onStartWeakness,
                enabled = !uiState.isStarting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("間違えた問題を弱点克服で復習") }
            OutlinedButton(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("ホームへ戻る") }
            Text("問題ごとの結果（タップで解説）", style = MaterialTheme.typography.titleMedium)
            result.items.forEach { ResultItemRow(it) }
        }
    }
}

@Composable
private fun ScoreCard(title: String, lines: List<ScoreLine>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            lines.filter { it.total > 0 }.forEach { Text("${it.label}：${it.correct} / ${it.total}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ResultItemRow(item: MockExamResultItem) {
    var expanded by rememberSaveable(item.number) { mutableStateOf(false) }
    val verdict = when {
        item.selectedChoiceIds.isEmpty() -> "未回答"
        item.isCorrect -> "正解"
        else -> "不正解"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .heightIn(min = 48.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("問${item.number} ${item.question.learningObjectiveId} ・ $verdict", style = MaterialTheme.typography.labelLarge)
            if (expanded) {
                Text(item.question.stem, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "正解: ${item.question.correctChoiceIds.sorted().joinToString()} ／ あなた: " +
                        item.selectedChoiceIds.sorted().joinToString().ifEmpty { "未回答" },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(item.question.explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
