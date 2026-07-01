package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class GameRepository(private val gameStateDao: GameStateDao) {
    fun getGameState(difficultyId: Int): Flow<GameState?> = gameStateDao.getGameState(difficultyId)

    suspend fun saveGameState(state: GameState) {
        gameStateDao.saveGameState(state)
    }

    suspend fun initOrGetState(difficulty: Difficulty): GameState {
        val current = getGameState(difficulty.ordinal).firstOrNull()
        if (current != null) {
            return current
        }
        val newState = GameState(
            id = difficulty.ordinal,
            targetValue = generateTarget(difficulty),
            currentAngle = 0f,
            totalRotation = 0f,
            attempts = 0,
            streak = 0,
            bestStreak = 0,
            level = 1
        )
        saveGameState(newState)
        return newState
    }

    companion object {
        fun generateTarget(difficulty: Difficulty, level: Int = 1): Float {
            val range = difficulty.max - difficulty.min
            val maxShift = range / 2
            val shift = ((level - 1) * maxShift / 20).coerceAtMost(maxShift)
            val adjustedMin = difficulty.min + shift
            if (adjustedMin >= difficulty.max) return difficulty.max.toFloat()
            val base = Random.nextInt(adjustedMin, difficulty.max)
            val decimal = Random.nextInt(0, 100) / 100f
            return base + decimal
        }
    }
}
