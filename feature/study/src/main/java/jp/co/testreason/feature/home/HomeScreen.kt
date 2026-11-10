package jp.co.testreason.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.QuizSession
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

/** Navigation and actions available from Home. */
data class HomeActions(
    val onStartDaily: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onOpenReview: () -> Unit = {},
    val onStartWeakness: () -> Unit = {},
    val onOpenChapters: () -> Unit = {},
    val onOpenDrill: () -> Unit = {},
    val onOpenGlossary: () -> Unit = {},
    val onOpenMockExam: () -> Unit = {},
    val onOpenAnalytics: () -> Unit = {}
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartQuiz: (String) -> Unit,
    onOpenReview: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenDrill: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenMockExam: () -> Unit,
    onOpenAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.NavigateToQuiz -> onStartQuiz(event.sessionId)
                is HomeUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    HomeContent(
        uiState = uiState,
        actions = HomeActions(
            onStartDaily = viewModel::startDailyQuiz,
            onResume = viewModel::resumeSession,
            onOpenReview = onOpenReview,
            onStartWeakness = viewModel::startWeaknessReview,
            onOpenChapters = onOpenChapters,
            onOpenDrill = onOpenDrill,
            onOpenGlossary = onOpenGlossary,
            onOpenMockExam = onOpenMockExam,
            onOpenAnalytics = onOpenAnalytics
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("TestReason") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            MasteryCard(uiState.averageMastery)
            uiState.resumableSession?.let { ResumeCard(it, enabled = !uiState.isLoading, onResume = actions.onResume) }
            DailyCard(isLoading = uiState.isLoading, onStart = actions.onStartDaily)

            Text("学習メニュー", style = MaterialTheme.typography.titleMedium)
            // Wraps to as many rows as the width needs instead of squeezing the buttons.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickAction("今日の復習", "${uiState.dueReviewCount}問", actions.onOpenReview)
                QuickAction("弱点克服", "${uiState.weakQuestionCount}問", actions.onStartWeakness)
                QuickAction("章別学習", "全6章", actions.onOpenChapters)
                QuickAction("周回モード", "答えをすぐ確認", actions.onOpenDrill)
                QuickAction("用語カード", "1語1行で確認", actions.onOpenGlossary)
                QuickAction("模擬試験", "40問・60分", actions.onOpenMockExam)
                QuickAction("学習分析", "学習目的別", actions.onOpenAnalytics)
            }
        }
    }
}

@Composable
private fun MasteryCard(averageMastery: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "全体習熟度 ${(averageMastery * 100).toInt()}%" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("全体習熟度", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${(averageMastery * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ResumeCard(session: QuizSession, enabled: Boolean, onResume: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("学習の続き", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${session.currentQuestionIndex} / ${session.questionIds.size} 問 回答済み",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onResume, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("続きから再開")
            }
        }
    }
}

@Composable
private fun DailyCard(isLoading: Boolean, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("今日のおすすめ学習", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "復習期限・弱点・未出題の順に5問（約3分）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("今日のおすすめを開始")
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, detail: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 132.dp).heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}
