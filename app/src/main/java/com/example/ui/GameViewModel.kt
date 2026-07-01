package com.example.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
    val targetValue: Float,
    val isWin: Boolean,
    val preciseSpins: Float
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = androidx.room.Room.databaseBuilder(
        application,
        AppDatabase::class.java, "game-database"
    ).fallbackToDestructiveMigration().build()
    private val repository = GameRepository(db.gameStateDao())
    val prefs = PreferencesManager(application)

    val currentDifficulty = MutableStateFlow(Difficulty.NORMAL)
    val message = MutableStateFlow<String?>(null)
    val submissionResult = MutableStateFlow<SubmissionResult?>(null)

    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabled.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState

    private var activeJob: kotlinx.coroutines.Job? = null

    private var hapticAngleAccum = 0f

    init {
        loadGameStateForDifficulty(currentDifficulty.value)
    }

    private fun loadGameStateForDifficulty(difficulty: Difficulty) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val state = repository.initOrGetState(difficulty)
            _gameState.value = state
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        currentDifficulty.value = difficulty
        loadGameStateForDifficulty(difficulty)
    }

    fun updateAngle(deltaAngle: Float) {
        val state = _gameState.value ?: return
        val newTotalRotation = state.totalRotation + deltaAngle

        // Speed-dependent fractional haptics
        val speed = abs(deltaAngle)
        hapticAngleAccum += speed
        val threshold = maxOf(6f, 40f / (1f + speed * 10f))
        if (hapticAngleAccum >= threshold && speed > 0.1f) {
            val amp = ((speed / 30f).coerceIn(0f, 1f) * 255).toInt().coerceAtLeast(60)
            triggerTickHaptic(amp)
            hapticAngleAccum = 0f
        }

        // Full-rotation heavy click
        val currentSpins = abs(state.totalRotation / 360f).toInt()
        val newSpins = abs(newTotalRotation / 360f).toInt()
        if (newSpins != currentSpins) {
            triggerTickHaptic(255)
        }

        _gameState.value = state.copy(
            currentAngle = (state.currentAngle + deltaAngle + 360f) % 360f,
            totalRotation = newTotalRotation
        )
    }

    fun saveCurrentStateToDb() {
        val state = _gameState.value ?: return
        viewModelScope.launch {
            repository.saveGameState(state)
        }
    }

    fun clearMessage() {
        message.value = null
    }

    fun clearSubmissionResult() {
        submissionResult.value = null
    }

    fun submit(onWin: (Int) -> Unit, onFail: () -> Unit) {
        val state = _gameState.value ?: return
        val diff = currentDifficulty.value

        viewModelScope.launch {
            val precise = abs(state.totalRotation / 360f)
            val submittedSpins = precise.toInt()

            val tolerance = when (diff) {
                Difficulty.EASY -> 0.3f
                Difficulty.NORMAL -> 0.15f
                Difficulty.HARD -> 0.08f
                Difficulty.EXTREME -> 0.03f
            }
            val isWin = abs(precise - state.targetValue) <= tolerance

            submissionResult.value = SubmissionResult(submittedSpins, state.targetValue, isWin, precise)

            if (isWin) {
                val newStreak = state.streak + 1
                val best = maxOf(state.bestStreak, newStreak)
                val newLevel = state.level + 1
                val newState = state.copy(
                    targetValue = GameRepository.generateTarget(diff, newLevel),
                    currentAngle = 0f,
                    totalRotation = 0f,
                    attempts = 0,
                    streak = newStreak,
                    bestStreak = best,
                    level = newLevel
                )
                _gameState.value = newState
                repository.saveGameState(newState)

                val difficultyMultiplier = when (diff) {
                    Difficulty.EASY -> 0.10f
                    Difficulty.NORMAL -> 0.20f
                    Difficulty.HARD -> 0.30f
                    Difficulty.EXTREME -> 0.50f
                }
                val distance = abs(precise - state.targetValue)
                val accuracy = 1f - (distance / tolerance).coerceIn(0f, 1f)
                val accuracyMultiplier = 0.5f + accuracy * 0.5f
                val basePoints = maxOf(0, state.targetValue.toInt() + state.level - state.attempts)
                val earnedPoints = (basePoints * (1f + state.streak * difficultyMultiplier) * accuracyMultiplier).toInt()
                if (earnedPoints > 0) prefs.addPoints(earnedPoints)

                vibrate(getApplication(), true)
                message.value = "Perfect!" + if (earnedPoints > 0) " +$earnedPoints points" else ""
                onWin(newStreak)
            } else {
                val newState = state.copy(
                    currentAngle = 0f,
                    totalRotation = 0f,
                    attempts = state.attempts + 1,
                    streak = 0
                )
                _gameState.value = newState
                repository.saveGameState(newState)
                vibrate(getApplication(), false)
                message.value = "Incorrect. Try again."
                onFail()
            }
        }
    }

    private fun triggerTickHaptic(amplitude: Int = 255) {
        if (!hapticsEnabled.value) return
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(12, amplitude))
        } else {
            vibrator.vibrate(12)
        }
    }

    private fun vibrate(context: Context, success: Boolean) {
        if (!hapticsEnabled.value) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
