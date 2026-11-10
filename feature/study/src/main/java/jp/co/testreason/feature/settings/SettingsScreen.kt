package jp.co.testreason.feature.settings

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.datastore.DarkThemeConfig
import jp.co.testreason.core.datastore.UserPreferences
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onOpenAbout: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    SettingsContent(
        uiState = uiState,
        onSelectTheme = viewModel::setTheme,
        onSelectDailyGoal = viewModel::setDailyGoal,
        onOpenAbout = onOpenAbout,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onSelectTheme: (DarkThemeConfig) -> Unit,
    onSelectDailyGoal: (Int) -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { TopAppBar(title = { Text("設定") }) }, modifier = modifier) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        ResponsiveScreenColumn(contentPadding = padding) {
            SettingCard(title = "テーマ", note = "変更はすぐに反映されます。") {
                DarkThemeConfig.entries.forEach { config ->
                    SettingChip(config.label, selected = uiState.preferences.darkThemeConfig == config) { onSelectTheme(config) }
                }
            }
            SettingCard(title = "1日の目標問題数", note = "「今日のおすすめ」で出題する問題数です。") {
                UserPreferences.DAILY_GOAL_OPTIONS.forEach { goal ->
                    SettingChip("${goal}問", selected = uiState.preferences.dailyGoalQuestions == goal) { onSelectDailyGoal(goal) }
                }
            }
            OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("このアプリについて")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingCard(title: String, note: String, options: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options() }
        }
    }
}

@Composable
private fun SettingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        // FilterChip is 32dp tall by default; keep the touch target at 48dp.
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

private val DarkThemeConfig.label: String
    get() = when (this) {
        DarkThemeConfig.FOLLOW_SYSTEM -> "システムに合わせる"
        DarkThemeConfig.LIGHT -> "ライト"
        DarkThemeConfig.DARK -> "ダーク"
    }
