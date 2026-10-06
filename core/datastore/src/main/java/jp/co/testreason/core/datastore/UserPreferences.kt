package jp.co.testreason.core.datastore

data class UserPreferences(
    val dailyGoalQuestions: Int = 5,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM
)

enum class DarkThemeConfig {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}
