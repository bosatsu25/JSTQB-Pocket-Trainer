package jp.co.testreason.core.datastore

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val dailyGoalQuestions: Int = DEFAULT_DAILY_GOAL,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM
) {
    companion object {
        /** Sizes offered for the daily session ("今日のおすすめ"). */
        val DAILY_GOAL_OPTIONS: List<Int> = listOf(5, 10, 20)
        const val DEFAULT_DAILY_GOAL = 5
    }
}

enum class DarkThemeConfig {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

/** The learner's settings (DataStore; see docs/architecture/DATA_MODEL.md). */
interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    /** Values outside [UserPreferences.DAILY_GOAL_OPTIONS] are ignored. */
    suspend fun setDailyGoal(questions: Int)

    suspend fun setDarkThemeConfig(config: DarkThemeConfig)
}
