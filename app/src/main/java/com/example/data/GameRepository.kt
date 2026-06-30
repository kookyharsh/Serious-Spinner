package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class GameRepository(private val gameStateDao: GameStateDao) {
    val gameState: Flow<GameState?> = gameStateDao.getGameState()

    suspend fun saveGameState(state: GameState) {
        gameStateDao.saveGameState(state)
    }

    suspend fun initOrGetState(difficulty: Difficulty): GameState {
        val current = gameState.firstOrNull()
        if (current != null && current.maxValue == difficulty.max) {
            return current
        }
        val newState = GameState(
            id = 1,
            targetValue = Random.nextInt(1, difficulty.max + 1),
            maxValue = difficulty.max,
            currentAngle = 0f,
            totalRotation = 0f,
            attempts = 0,
            streak = 0,
            bestStreak = current?.bestStreak ?: 0
        )
        saveGameState(newState)
        return newState
    }
}
