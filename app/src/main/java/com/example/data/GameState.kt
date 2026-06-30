package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val targetValue: Int,
    val maxValue: Int,
    val currentAngle: Float,
    val totalRotation: Float,
    val attempts: Int,
    val streak: Int,
    val bestStreak: Int
)
