package jp.co.testreason.feature.glossary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.GlossaryTerm
import jp.co.testreason.core.model.Syllabus
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

// ---------------------------------------------------------------- list

@Composable
fun GlossaryScreen(
    viewModel: GlossaryViewModel,
    onOpenCards: (chapter: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    GlossaryContent(uiState = uiState, onSelectChapter = viewModel::selectChapter, onOpenCards = onOpenCards, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlossaryContent(
    uiState: GlossaryUiState,
    onSelectChapter: (Int?) -> Unit,
    onOpenCards: (chapter: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { TopAppBar(title = { Text("用語ひとこと") }) }, modifier = modifier) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        ResponsiveScreenColumn(contentPadding = padding) {
            Text(
                text = "シラバス各章のキーワードを1行で確認できます。説明はこのアプリ独自の要約です。" +
                    "正確な定義はシラバスと用語集で確認してください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf<Int?>(null) + Syllabus.chapters.map { it.number }).forEach { chapter ->
                    FilterChip(
                        selected = uiState.chapter == chapter,
                        onClick = { onSelectChapter(chapter) },
                        label = { Text("${chapterLabel(chapter)}（${uiState.countIn(chapter)}）") },
                        // FilterChip is 32dp tall by default; keep the touch target at 48dp.
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
            val visible = uiState.visibleTerms
            Button(
                onClick = { onOpenCards(uiState.chapter) },
                enabled = visible.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("カードで確認する（${visible.size}語）")
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column {
                    visible.forEachIndexed { index, term ->
                        if (index > 0) HorizontalDivider()
                        TermRow(term)
                    }
                }
            }
        }
    }
}

@Composable
private fun TermRow(term: GlossaryTerm) {
    // One node for screen readers: term, then meaning.
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(term.term, style = MaterialTheme.typography.titleSmall)
        Text(term.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun chapterLabel(chapter: Int?) = if (chapter == null) "全章" else "第${chapter}章"

// ---------------------------------------------------------------- cards

data class GlossaryCardsActions(
    val onReveal: () -> Unit = {},
    val onGrade: (remembered: Boolean) -> Unit = {},
    val onRestart: (onlyMissed: Boolean) -> Unit = {},
    val onBackToList: () -> Unit = {},
    val onNavigateHome: () -> Unit = {}
)

@Composable
fun GlossaryCardsScreen(
    viewModel: GlossaryCardsViewModel,
    onBackToList: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    GlossaryCardsContent(
        uiState = uiState,
        actions = GlossaryCardsActions(
            onReveal = viewModel::reveal,
            onGrade = viewModel::grade,
            onRestart = viewModel::restart,
            onBackToList = onBackToList,
            onNavigateHome = onNavigateHome
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryCardsContent(uiState: GlossaryCardsUiState, actions: GlossaryCardsActions, modifier: Modifier = Modifier) {
    val term = uiState.term
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (uiState.lap.total > 0) "用語カード ${uiState.lap.clearedCount} / ${uiState.lap.total}" else "用語カード") })
        },
        modifier = modifier
    ) { padding ->
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.isEmpty -> ResponsiveScreenColumn(contentPadding = padding) {
                Text("この範囲の用語はありません。", style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(onClick = actions.onBackToList, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("用語一覧へ戻る") }
            }
            uiState.isFinished || term == null -> ResponsiveScreenColumn(contentPadding = padding) { CardsSummary(uiState, actions) }
            // Keyed by the answer count: every card starts at the top, also when the same term comes back.
            else -> key(uiState.lap.answerCount) {
                ResponsiveScreenColumn(contentPadding = padding) { TermCard(term, uiState.isRevealed, actions) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TermCard(term: GlossaryTerm, isRevealed: Boolean, actions: GlossaryCardsActions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(term.chapters.joinToString("・") { "第${it}章" }, style = MaterialTheme.typography.labelMedium)
            Text(term.term, style = MaterialTheme.typography.headlineSmall)
            if (isRevealed) {
                HorizontalDivider()
                Text(term.summary, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("意味を思い出してから、カードをめくりましょう。", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (isRevealed) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { actions.onGrade(false) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("まだ") }
            Button(onClick = { actions.onGrade(true) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("覚えた") }
        }
    } else {
        Button(onClick = actions.onReveal, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
            Text("意味を見る")
        }
    }
}

@Composable
private fun CardsSummary(uiState: GlossaryCardsUiState, actions: GlossaryCardsActions) {
    val lap = uiState.lap
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("1周クリア", style = MaterialTheme.typography.titleLarge)
            Text("${lap.total}語中、1回目で覚えていた語 ${lap.firstTryCorrectCount}語", style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (lap.missed.isNotEmpty()) {
        Button(onClick = { actions.onRestart(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
            Text("「まだ」の${lap.missed.size}語だけもう1周")
        }
    }
    OutlinedButton(onClick = { actions.onRestart(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("同じ語でもう1周") }
    OutlinedButton(onClick = actions.onBackToList, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("用語一覧へ戻る") }
    OutlinedButton(onClick = actions.onNavigateHome, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("ホームへ戻る") }
}
