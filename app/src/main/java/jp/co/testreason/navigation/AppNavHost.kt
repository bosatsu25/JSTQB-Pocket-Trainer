package jp.co.testreason.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import jp.co.testreason.feature.analytics.AnalyticsScreen
import jp.co.testreason.feature.analytics.AnalyticsViewModel
import jp.co.testreason.feature.chapter.ChapterSelectScreen
import jp.co.testreason.feature.chapter.ChapterSelectViewModel
import jp.co.testreason.feature.explanation.ExplanationScreen
import jp.co.testreason.feature.explanation.ExplanationViewModel
import jp.co.testreason.feature.home.HomeScreen
import jp.co.testreason.feature.home.HomeViewModel
import jp.co.testreason.feature.mockexam.MockExamIntroScreen
import jp.co.testreason.feature.mockexam.MockExamIntroViewModel
import jp.co.testreason.feature.mockexam.MockExamResultScreen
import jp.co.testreason.feature.mockexam.MockExamResultViewModel
import jp.co.testreason.feature.mockexam.MockExamScreen
import jp.co.testreason.feature.mockexam.MockExamViewModel
import jp.co.testreason.feature.quiz.QuizScreen
import jp.co.testreason.feature.quiz.QuizViewModel
import jp.co.testreason.feature.result.ResultScreen
import jp.co.testreason.feature.result.ResultViewModel
import jp.co.testreason.feature.review.ReviewScreen
import jp.co.testreason.feature.review.ReviewViewModel
import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    object HomeRoute : Screen

    @Serializable
    data class QuizRoute(val sessionId: String) : Screen

    @Serializable
    data class ExplanationRoute(
        val sessionId: String,
        val questionId: String,
        val attemptId: String
    ) : Screen

    @Serializable
    data class ResultRoute(val sessionId: String) : Screen

    @Serializable
    object ReviewRoute : Screen

    @Serializable
    object AnalyticsRoute : Screen

    @Serializable
    object ChapterSelectRoute : Screen

    @Serializable
    object MockExamIntroRoute : Screen

    @Serializable
    data class MockExamRoute(val sessionId: String) : Screen

    @Serializable
    data class MockExamResultRoute(val sessionId: String) : Screen
}

@Composable
fun TestReasonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.HomeRoute,
        modifier = modifier
    ) {
        composable<Screen.HomeRoute> {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onStartQuiz = { sessionId ->
                    navController.navigate(Screen.QuizRoute(sessionId))
                },
                onOpenReview = { navController.navigate(Screen.ReviewRoute) },
                onOpenChapters = { navController.navigate(Screen.ChapterSelectRoute) },
                onOpenMockExam = { navController.navigate(Screen.MockExamIntroRoute) },
                onOpenAnalytics = { navController.navigate(Screen.AnalyticsRoute) }
            )
        }

        composable<Screen.ReviewRoute> {
            val viewModel: ReviewViewModel = hiltViewModel()
            ReviewScreen(
                viewModel = viewModel,
                onStartReviewSession = { sessionId ->
                    navController.navigate(Screen.QuizRoute(sessionId)) {
                        popUpTo<Screen.ReviewRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.ChapterSelectRoute> {
            val viewModel: ChapterSelectViewModel = hiltViewModel()
            ChapterSelectScreen(
                viewModel = viewModel,
                onStartQuiz = { sessionId -> navController.navigate(Screen.QuizRoute(sessionId)) }
            )
        }

        composable<Screen.MockExamIntroRoute> {
            val viewModel: MockExamIntroViewModel = hiltViewModel()
            MockExamIntroScreen(
                viewModel = viewModel,
                onOpenExam = { sessionId -> navController.navigate(Screen.MockExamRoute(sessionId)) }
            )
        }

        composable<Screen.MockExamRoute> {
            val viewModel: MockExamViewModel = hiltViewModel()
            MockExamScreen(
                viewModel = viewModel,
                onShowResult = { sessionId ->
                    navController.navigate(Screen.MockExamResultRoute(sessionId)) {
                        popUpTo<Screen.MockExamIntroRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.MockExamResultRoute> {
            val viewModel: MockExamResultViewModel = hiltViewModel()
            MockExamResultScreen(
                viewModel = viewModel,
                onStartQuiz = { sessionId -> navController.navigate(Screen.QuizRoute(sessionId)) },
                onNavigateHome = {
                    navController.navigate(Screen.HomeRoute) {
                        popUpTo<Screen.HomeRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.AnalyticsRoute> {
            val viewModel: AnalyticsViewModel = hiltViewModel()
            AnalyticsScreen(
                viewModel = viewModel,
                onStartQuiz = { sessionId -> navController.navigate(Screen.QuizRoute(sessionId)) }
            )
        }

        composable<Screen.QuizRoute> { backStackEntry ->
            val route: Screen.QuizRoute = backStackEntry.toRoute()
            val viewModel: QuizViewModel = hiltViewModel()
            QuizScreen(
                viewModel = viewModel,
                onAnswerSubmitted = { sessionId, questionId, attemptId ->
                    navController.navigate(
                        Screen.ExplanationRoute(sessionId, questionId, attemptId)
                    ) {
                        popUpTo<Screen.QuizRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.ExplanationRoute> { backStackEntry ->
            val route: Screen.ExplanationRoute = backStackEntry.toRoute()
            val viewModel: ExplanationViewModel = hiltViewModel()
            ExplanationScreen(
                viewModel = viewModel,
                onNextQuestion = { sessionId ->
                    navController.navigate(Screen.QuizRoute(sessionId)) {
                        popUpTo<Screen.ExplanationRoute> { inclusive = true }
                    }
                },
                onShowResult = { sessionId ->
                    navController.navigate(Screen.ResultRoute(sessionId)) {
                        popUpTo<Screen.ExplanationRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.ResultRoute> { backStackEntry ->
            val route: Screen.ResultRoute = backStackEntry.toRoute()
            val viewModel: ResultViewModel = hiltViewModel()
            ResultScreen(
                viewModel = viewModel,
                onNavigateHome = {
                    navController.navigate(Screen.HomeRoute) {
                        popUpTo<Screen.HomeRoute> { inclusive = true }
                    }
                },
                onStartQuiz = { sessionId ->
                    navController.navigate(Screen.QuizRoute(sessionId)) {
                        popUpTo<Screen.ResultRoute> { inclusive = true }
                    }
                }
            )
        }
    }
}
