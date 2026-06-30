package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE id = :id")
    fun getGameState(id: Int): Flow<GameState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(state: GameState)
}
