package jp.co.testreason.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal_questions")
        val DARK_THEME = stringPreferencesKey("dark_theme_config")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            dailyGoalQuestions = prefs[Keys.DAILY_GOAL] ?: 5,
            darkThemeConfig = DarkThemeConfig.valueOf(
                prefs[Keys.DARK_THEME] ?: DarkThemeConfig.FOLLOW_SYSTEM.name
            )
        )
    }

    suspend fun setDailyGoal(questions: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL] = questions
        }
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = config.name
        }
    }
}
