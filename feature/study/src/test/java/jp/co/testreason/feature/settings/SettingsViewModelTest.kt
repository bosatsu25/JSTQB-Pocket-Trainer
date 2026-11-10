package jp.co.testreason.feature.settings

import jp.co.testreason.core.datastore.DarkThemeConfig
import jp.co.testreason.core.datastore.UserPreferences
import jp.co.testreason.feature.testing.FakeUserPreferencesRepository
import jp.co.testreason.feature.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun showsTheStoredSettings_andWritesChangesBack() = runTest {
        val preferences = FakeUserPreferencesRepository(UserPreferences(dailyGoalQuestions = 10, darkThemeConfig = DarkThemeConfig.LIGHT))
        val vm = SettingsViewModel(preferences)
        assertEquals(UserPreferences(10, DarkThemeConfig.LIGHT), vm.uiState.first { !it.isLoading }.preferences)

        vm.setDailyGoal(20)
        vm.setTheme(DarkThemeConfig.DARK)

        assertEquals(UserPreferences(20, DarkThemeConfig.DARK), preferences.userPreferences.first())
        assertEquals(UserPreferences(20, DarkThemeConfig.DARK), vm.uiState.first { it.preferences.dailyGoalQuestions == 20 && it.preferences.darkThemeConfig == DarkThemeConfig.DARK }.preferences)
    }

    @Test
    fun aGoalThatIsNotOffered_changesNothing() = runTest {
        val preferences = FakeUserPreferencesRepository()
        SettingsViewModel(preferences).setDailyGoal(7)
        assertEquals(UserPreferences(), preferences.userPreferences.first())
    }
}
