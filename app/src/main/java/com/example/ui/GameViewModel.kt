package com.example.ui

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Difficulty
import com.example.data.GameRepository
import com.example.data.GameState
import com.example.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

data class SubmissionResult(
    val submittedSpins: Int,
    val targetSpins: Int,
    val isWin: Boolean
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = androidx.room.Room.databaseBuilder(
        application,
        AppDatabase::class.java, "game-database"
    ).fallbackToDestructiveMigration().build()
    private val repository = GameRepository(db.gameStateDao())
    val prefs = PreferencesManager(application)

    val gameState: StateFlow<GameState?> = repository.gameState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val currentDifficulty = MutableStateFlow(Difficulty.NORMAL)
    val message = MutableStateFlow<String?>(null)
    val submissionResult = MutableStateFlow<SubmissionResult?>(null)

    init {
        viewModelScope.launch {
            repository.initOrGetState(currentDifficulty.value)
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        currentDifficulty.value = difficulty
        viewModelScope.launch {
            repository.initOrGetState(difficulty)
        }
    }

    fun updateAngle(deltaAngle: Float) {
        val state = gameState.value ?: return
        val newTotalRotation = state.totalRotation + deltaAngle
        
        val currentSpins = abs(state.totalRotation / 360f).toInt()
        val newSpins = abs(newTotalRotation / 360f).toInt()
        
        if (newSpins != currentSpins) {
            triggerTickHaptic()
        }
        
        viewModelScope.launch {
            repository.saveGameState(state.copy(
                currentAngle = (state.currentAngle + deltaAngle + 360f) % 360f,
                totalRotation = newTotalRotation
            ))
        }
    }

    fun clearMessage() {
        message.value = null
    }

    fun clearSubmissionResult() {
        submissionResult.value = null
    }

    fun submit(onWin: (Int) -> Unit, onFail: () -> Unit) {
        val state = gameState.value ?: return
        val value = abs(state.totalRotation / 360f).toInt()

        viewModelScope.launch {
            submissionResult.value = SubmissionResult(value, state.targetValue, value == state.targetValue)
            
            if (value == state.targetValue) {
                // Win
                val newStreak = state.streak + 1
                val best = maxOf(state.bestStreak, newStreak)
                val newState = state.copy(
                    targetValue = Random.nextInt(1, state.maxValue + 1),
                    currentAngle = 0f,
                    totalRotation = 0f,
                    attempts = 0,
                    streak = newStreak,
                    bestStreak = best
                )
                repository.saveGameState(newState)
                vibrate(getApplication(), true)
                message.value = "Perfect!"
                onWin(newStreak)
            } else {
                // Fail
                val newState = state.copy(
                    currentAngle = 0f,
                    totalRotation = 0f,
                    attempts = state.attempts + 1,
                    streak = 0
                )
                repository.saveGameState(newState)
                vibrate(getApplication(), false)
                message.value = "Incorrect. Try again."
                onFail()
            }
        }
    }

    fun triggerTickHaptic() {
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibrator.vibrate(30)
        }
    }

    private fun vibrate(context: Context, success: Boolean) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (success) {
                val timings = longArrayOf(0, 100, 50, 100)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            if (success) vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1) else vibrator.vibrate(300)
        }
    }
}
