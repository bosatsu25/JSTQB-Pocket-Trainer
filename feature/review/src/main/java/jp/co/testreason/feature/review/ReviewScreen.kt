package jp.co.testreason.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onStartReviewSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReviewUiEvent.NavigateToQuiz -> onStartReviewSession(event.sessionId)
            }
        }
    }

    ReviewContent(uiState = uiState, onStartReview = viewModel::startDueReviewSession, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewContent(
    uiState: ReviewUiState,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { TopAppBar(title = { Text("今日の復習") }) }, modifier = modifier) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        ResponsiveScreenColumn(contentPadding = padding) {
            if (uiState.dueQuestions.isEmpty()) {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("復習対象はありません", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "今日が期限の問題はありません。ホームの「今日のおすすめ」で新しい問題に進みましょう。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Button(
                    onClick = onStartReview,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("復習を開始（${uiState.dueQuestions.size}問）")
                }
                Text("期限が来ている問題", style = MaterialTheme.typography.titleMedium)
                uiState.dueQuestions.forEach { DueQuestionCard(it) }
            }
        }
    }
}

@Composable
private fun DueQuestionCard(question: Question) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(question.learningObjectiveId, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(question.stem, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
