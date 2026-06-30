package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class PreferencesManager(private val context: Context) {
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val POINTS = intPreferencesKey("points")
    private val UNLOCKED_SPINNERS = stringSetPreferencesKey("unlocked_spinners")
    private val CURRENT_SPINNER = stringPreferencesKey("current_spinner")

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }
    
    val points: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POINTS] ?: 0
    }
    
    val unlockedSpinners: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[UNLOCKED_SPINNERS] ?: setOf("DEFAULT")
    }
    
    val currentSpinner: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CURRENT_SPINNER] ?: "DEFAULT"
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun addPoints(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[POINTS] ?: 0
            prefs[POINTS] = current + amount
        }
    }

    suspend fun unlockSpinner(id: String, cost: Int): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val currentPoints = prefs[POINTS] ?: 0
            val unlocked = prefs[UNLOCKED_SPINNERS] ?: setOf("DEFAULT")
            if (currentPoints >= cost && !unlocked.contains(id)) {
                prefs[POINTS] = currentPoints - cost
                prefs[UNLOCKED_SPINNERS] = unlocked + id
                success = true
            }
        }
        return success
    }

    suspend fun setCurrentSpinner(id: String) {
        context.dataStore.edit { prefs ->
            val unlocked = prefs[UNLOCKED_SPINNERS] ?: setOf("DEFAULT")
            if (unlocked.contains(id)) {
                prefs[CURRENT_SPINNER] = id
            }
        }
    }
}
