package jp.co.testreason

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import jp.co.testreason.core.datastore.DarkThemeConfig
import jp.co.testreason.core.datastore.UserPreferencesRepository
import jp.co.testreason.core.ui.theme.TestReasonTheme
import jp.co.testreason.navigation.TestReasonNavHost
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Nothing is drawn until the settings are read (a few milliseconds): drawing first and switching
            // afterwards would flash the wrong theme at every start.
            val preferences by preferencesRepository.userPreferences.collectAsState(initial = null)
            val darkTheme = when (preferences?.darkThemeConfig ?: return@setContent) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }
            TestReasonTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestReasonNavHost()
                }
            }
        }
    }
}
