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
            targetValue = Random.nextInt(difficulty.min, difficulty.max + 1),
            maxValue = difficulty.max,
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
}
