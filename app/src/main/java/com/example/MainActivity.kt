package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.playgames.PlayGamesManager
import com.example.ui.GameScreen
import com.example.ui.GameViewModel
import com.example.ui.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var playGamesManager: PlayGamesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        playGamesManager = PlayGamesManager(this)
        playGamesManager.signIn(this)
        playGamesManager.unlockAchievement(this, "CgkI_mock_daily_login_id")

        setContent {
            val viewModel: GameViewModel = viewModel()
            val themeMode by viewModel.prefs.themeMode.collectAsState(initial = 0)
            
            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val onboardingCompleted by viewModel.prefs.onboardingCompleted.collectAsState(initial = false)

                    NavHost(
                        navController = navController,
                        startDestination = if (onboardingCompleted) "game" else "onboarding"
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onFinishOnboarding = {
                                    navController.navigate("game") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("game") {
                            GameScreen(
                                viewModel = viewModel,
                                onLeaderboardClick = {
                                    playGamesManager.showLeaderboard(this@MainActivity, "CgkI_mock_leaderboard_id")
                                },
                                onShopClick = {
                                    navController.navigate("shop")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onWin = { streak ->
                                    playGamesManager.submitScore(this@MainActivity, "CgkI_mock_leaderboard_id", streak.toLong())
                                    if (streak == 5) playGamesManager.unlockAchievement(this@MainActivity, "CgkI_mock_achievement_5")
                                    if (streak == 10) playGamesManager.unlockAchievement(this@MainActivity, "CgkI_mock_achievement_10")
                                }
                            )
                        }
                        composable("shop") {
                            com.example.ui.ShopScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            com.example.ui.SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
