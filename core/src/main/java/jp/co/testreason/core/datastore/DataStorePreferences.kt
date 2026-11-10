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
) : UserPreferencesRepository {
    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal_questions")
        val DARK_THEME = stringPreferencesKey("dark_theme_config")
    }

    // A stored value this version does not know (written by another version, or a damaged file) falls back
    // to the default instead of crashing the app at start.
    override val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            dailyGoalQuestions = prefs[Keys.DAILY_GOAL]?.takeIf { it in UserPreferences.DAILY_GOAL_OPTIONS }
                ?: UserPreferences.DEFAULT_DAILY_GOAL,
            darkThemeConfig = DarkThemeConfig.entries.firstOrNull { it.name == prefs[Keys.DARK_THEME] }
                ?: DarkThemeConfig.FOLLOW_SYSTEM
        )
    }

    override suspend fun setDailyGoal(questions: Int) {
        if (questions !in UserPreferences.DAILY_GOAL_OPTIONS) return
        dataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL] = questions
        }
    }

    override suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = config.name
        }
    }
}
