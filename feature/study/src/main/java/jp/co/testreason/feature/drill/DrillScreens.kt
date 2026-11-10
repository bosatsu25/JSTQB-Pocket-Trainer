package jp.co.testreason.feature.drill

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn
import jp.co.testreason.core.ui.components.ResponsiveTwoPane
import jp.co.testreason.core.ui.components.ScenarioText
import jp.co.testreason.core.ui.theme.CorrectGreen
import jp.co.testreason.core.ui.theme.IncorrectRed

// ---------------------------------------------------------------- setup

@Composable
fun DrillSetupScreen(
    viewModel: DrillSetupViewModel,
    onStartDrill: (chapter: Int?, count: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    DrillSetupContent(uiState = uiState, onStartDrill = onStartDrill, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillSetupContent(
    uiState: DrillSetupUiState,
    onStartDrill: (chapter: Int?, count: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { TopAppBar(title = { Text("周回モード") }) }, modifier = modifier) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("答えをすぐ見て、テンポよく回す", style = MaterialTheme.typography.titleMedium)
                    Text("・選ぶとその場で正解と解説が出ます。分からなければ「答えを見る」で進めます", style = MaterialTheme.typography.bodyMedium)
                    Text("・間違えた問題は同じ周の最後にもう一度出て、全問正解すると1周終了です", style = MaterialTheme.typography.bodyMedium)
                    Text("・「なぜ？」を開くと、各選択肢が正しい・誤りである理由を確認できます", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "周回モードの解答は、習熟度・復習予定・弱点には反映しません（答えを覚えた正解で数値が上がらないようにするためです）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (uiState.totalCount > 0) {
                DrillScopeCard("全範囲から", uiState.totalCount, ALL_CHAPTER_COUNTS, includeAll = false) { count ->
                    onStartDrill(null, count)
                }
            }
            uiState.chapters.forEach { chapter ->
                DrillScopeCard("第${chapter.chapter}章 ${chapter.title}", chapter.questionCount, CHAPTER_COUNTS, includeAll = true) { count ->
                    onStartDrill(chapter.chapter, count)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrillScopeCard(title: String, questionCount: Int, counts: List<Int>, includeAll: Boolean, onStart: (Int?) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (questionCount == 0) {
                Text("この範囲の問題はまだありません。", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("全${questionCount}問", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    counts.filter { it < questionCount }.forEach { count ->
                        OutlinedButton(onClick = { onStart(count) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("${count}問") }
                    }
                    if (includeAll || counts.none { it < questionCount }) {
                        OutlinedButton(onClick = { onStart(null) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("全${questionCount}問") }
                    }
                }
            }
        }
    }
}

private val CHAPTER_COUNTS = listOf(10, 20)
private val ALL_CHAPTER_COUNTS = listOf(20, 40)

// ---------------------------------------------------------------- drill

data class DrillActions(
    val onSelect: (String) -> Unit = {},
    val onShowAnswer: () -> Unit = {},
    val onToggleReasons: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onRestart: (onlyMissed: Boolean) -> Unit = {},
    val onOpenChapters: () -> Unit = {},
    val onNavigateHome: () -> Unit = {}
)

@Composable
fun DrillScreen(
    viewModel: DrillViewModel,
    onOpenChapters: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    DrillContent(
        uiState = uiState,
        actions = DrillActions(
            onSelect = viewModel::select,
            onShowAnswer = viewModel::showAnswer,
            onToggleReasons = viewModel::toggleReasons,
            onNext = viewModel::next,
            onRestart = viewModel::restart,
            onOpenChapters = onOpenChapters,
            onNavigateHome = onNavigateHome
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillContent(uiState: DrillUiState, actions: DrillActions, modifier: Modifier = Modifier) {
    val question = uiState.question
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.lap.total > 0) "周回モード ${uiState.lap.clearedCount} / ${uiState.lap.total} クリア" else "周回モード")
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.isEmpty -> ResponsiveScreenColumn(contentPadding = padding) {
                Text("この範囲の問題はまだありません。", style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(onClick = actions.onNavigateHome, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("ホームへ戻る") }
            }
            uiState.isFinished || question == null -> ResponsiveScreenColumn(contentPadding = padding) { LapSummary(uiState, actions) }
            // Keyed by the answer count: each new question starts scrolled to the top, even when the same
            // question comes back because it is the only one left.
            else -> key(uiState.lap.answerCount) {
                ResponsiveTwoPane(
                    contentPadding = padding,
                    primary = { DrillQuestionCard(question) },
                    secondary = {
                        DrillChoices(question, uiState, actions.onSelect)
                        if (uiState.reveal == null) {
                            TextButton(onClick = actions.onShowAnswer, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("分からないので答えを見る")
                            }
                        } else {
                            RevealPanel(question, uiState, actions)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DrillQuestionCard(question: Question) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(question.learningObjectiveId, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            question.scenario?.let { ScenarioText(text = it) }
            Text(question.stem, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DrillChoices(question: Question, uiState: DrillUiState, onSelect: (String) -> Unit) {
    val isMulti = question.selectCount > 1
    val revealed = uiState.reveal != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isMulti) Text("${question.selectCount}つ選択", style = MaterialTheme.typography.labelLarge)
        question.choices.forEach { choice ->
            val selected = choice.id in uiState.selectedChoiceIds
            val selection = if (isMulti) {
                Modifier.toggleable(value = selected, enabled = !revealed, role = Role.Checkbox, onValueChange = { onSelect(choice.id) })
            } else {
                Modifier.selectable(selected = selected, enabled = !revealed, role = Role.RadioButton, onClick = { onSelect(choice.id) })
            }
            Card(
                modifier = Modifier.fillMaxWidth().then(selection),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = choiceColor(choice, selected, revealed))
            ) {
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isMulti) Checkbox(checked = selected, onCheckedChange = null) else RadioButton(selected = selected, onClick = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        // The verdict is written out, not only coloured.
                        choiceVerdict(choice, selected, revealed)?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
                        Text("${choice.id}. ${choice.text}", style = MaterialTheme.typography.bodyMedium)
                        if (revealed && uiState.showReasons) {
                            Text(
                                text = choice.distractorAnalysis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun choiceColor(choice: Choice, selected: Boolean, revealed: Boolean) = when {
    revealed && choice.isCorrect -> CorrectGreen.copy(alpha = 0.12f)
    revealed && selected -> IncorrectRed.copy(alpha = 0.12f)
    selected -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.surface
}

private fun choiceVerdict(choice: Choice, selected: Boolean, revealed: Boolean): String? = when {
    !revealed -> null
    choice.isCorrect && selected -> "正解（あなたの解答）"
    choice.isCorrect -> "正解"
    selected -> "あなたの解答（不正解）"
    else -> null
}

@Composable
private fun RevealPanel(question: Question, uiState: DrillUiState, actions: DrillActions) {
    val (headline, note) = when (uiState.reveal) {
        DrillReveal.CORRECT -> "正解！" to null
        DrillReveal.WRONG -> "不正解" to "この問題は、この周の最後にもう一度出ます。"
        else -> "答えを確認" to "この問題は、この周の最後にもう一度出ます。"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(headline, style = MaterialTheme.typography.titleMedium)
            Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    Button(onClick = actions.onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
        Text("次へ")
    }
    TextButton(onClick = actions.onToggleReasons, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(if (uiState.showReasons) "各選択肢の理由を閉じる" else "なぜ？（各選択肢の理由を見る）")
    }
}

@Composable
private fun LapSummary(uiState: DrillUiState, actions: DrillActions) {
    val lap = uiState.lap
    val seconds = uiState.elapsedMs / MILLIS_PER_SECOND
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("1周クリア", style = MaterialTheme.typography.titleLarge)
            Text("${lap.total}問中、1回目で正解 ${lap.firstTryCorrectCount}問", style = MaterialTheme.typography.bodyLarge)
            Text("解答回数 ${lap.answerCount}回 ・ ${seconds / SECONDS_PER_MINUTE}分${seconds % SECONDS_PER_MINUTE}秒", style = MaterialTheme.typography.bodyMedium)
            Text("1回あたり 約${uiState.secondsPerAnswer}秒", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Text(
        text = if (lap.missed.isEmpty()) {
            "全問を1回目で正解しました。次は章別学習で、なぜその答えになるのかを確認しましょう。"
        } else {
            "見た瞬間に解ける問題が増えたら、章別学習で「なぜその答えになるのか」を確認しましょう。"
        },
        style = MaterialTheme.typography.bodyMedium
    )
    if (lap.missed.isNotEmpty()) {
        Button(onClick = { actions.onRestart(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
            Text("間違えた${lap.missed.size}問だけもう1周")
        }
    }
    OutlinedButton(onClick = { actions.onRestart(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("同じ問題でもう1周") }
    OutlinedButton(onClick = actions.onOpenChapters, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("章別学習で理由を確認") }
    OutlinedButton(onClick = actions.onNavigateHome, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("ホームへ戻る") }
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
