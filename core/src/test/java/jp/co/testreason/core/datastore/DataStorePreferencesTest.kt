package jp.co.testreason.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The real DataStore on a real file: what is written must still be there after the app restarts. */
class DataStorePreferencesTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "user_preferences.preferences_pb") }

    /** Opens the file, runs [block], and closes it again like a process ending. */
    private fun <T> withStore(block: suspend (DataStore<Preferences>) -> T): T = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            block(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        } finally {
            scope.coroutineContext.job.cancelAndJoin()
        }
    }

    @Test
    fun defaults_whenNothingIsStored() {
        val prefs = withStore { DataStorePreferences(it).userPreferences.first() }
        assertEquals(UserPreferences(dailyGoalQuestions = 5, darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM), prefs)
    }

    @Test
    fun settings_surviveARestart() {
        withStore {
            val preferences = DataStorePreferences(it)
            preferences.setDailyGoal(20)
            preferences.setDarkThemeConfig(DarkThemeConfig.DARK)
        }

        val afterRestart = withStore { DataStorePreferences(it).userPreferences.first() }

        assertEquals(UserPreferences(dailyGoalQuestions = 20, darkThemeConfig = DarkThemeConfig.DARK), afterRestart)
    }

    @Test
    fun dailyGoal_outsideTheOfferedSizes_isIgnored() {
        val prefs = withStore {
            val preferences = DataStorePreferences(it)
            preferences.setDailyGoal(10)
            preferences.setDailyGoal(7)
            preferences.setDailyGoal(0)
            preferences.userPreferences.first()
        }
        assertEquals(10, prefs.dailyGoalQuestions)
    }

    @Test
    fun unknownStoredValues_fallBackToTheDefaults() {
        val prefs = withStore {
            it.edit { stored ->
                stored[intPreferencesKey("daily_goal_questions")] = 999
                stored[stringPreferencesKey("dark_theme_config")] = "SEPIA"
            }
            DataStorePreferences(it).userPreferences.first()
        }
        assertEquals(UserPreferences(), prefs)
    }
}
