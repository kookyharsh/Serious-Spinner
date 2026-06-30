package com.example.playgames

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.tasks.Task

class PlayGamesManager(private val context: Context) {

    fun signIn(activity: Activity) {
        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask: Task<com.google.android.gms.games.AuthenticationResult> ->
            val isAuthenticated = isAuthenticatedTask.isSuccessful &&
                    isAuthenticatedTask.result.isAuthenticated
            if (isAuthenticated) {
                Log.d("PlayGames", "User is already authenticated")
            } else {
                gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful && signInTask.result.isAuthenticated) {
                        Log.d("PlayGames", "Signed in successfully")
                    } else {
                        Log.e("PlayGames", "Sign in failed")
                    }
                }
            }
        }
    }

    fun submitScore(activity: Activity, leaderboardId: String, score: Long) {
        try {
            PlayGames.getLeaderboardsClient(activity).submitScore(leaderboardId, score)
        } catch (e: Exception) {
            Log.e("PlayGames", "Failed to submit score", e)
        }
    }

    fun unlockAchievement(activity: Activity, achievementId: String) {
        try {
            PlayGames.getAchievementsClient(activity).unlock(achievementId)
        } catch (e: Exception) {
            Log.e("PlayGames", "Failed to unlock achievement", e)
        }
    }

    fun showLeaderboard(activity: Activity, leaderboardId: String) {
        try {
            PlayGames.getLeaderboardsClient(activity)
                .getLeaderboardIntent(leaderboardId)
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, 9004)
                }
        } catch (e: Exception) {
            Log.e("PlayGames", "Failed to show leaderboard", e)
        }
    }
}
