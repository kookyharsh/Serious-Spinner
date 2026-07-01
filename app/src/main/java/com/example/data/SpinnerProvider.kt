package com.example.data

import android.content.Context
import com.example.R
import org.json.JSONArray

data class SpinnerConfig(
    val id: String,
    val name: String,
    val cost: Int,
    val shellResId: Int? = null,
    val stickResId: Int? = null,
    val isCanvasBased: Boolean = false
)

object SpinnerProvider {
    private var initialized = false
    private val hardcodedSpinners = listOf(
        SpinnerConfig(
            id = "DEFAULT",
            name = "Blue 3D Dial",
            cost = 0,
            isCanvasBased = true
        ),
        SpinnerConfig(
            id = "MINIMALIST",
            name = "Minimalist Neon",
            cost = 50,
            isCanvasBased = true
        )
    )

    private val dynamicSpinners = mutableListOf<SpinnerConfig>()

    val spinners: List<SpinnerConfig>
        get() = hardcodedSpinners + dynamicSpinners

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        dynamicSpinners.clear()
        try {
            val json = context.resources.openRawResource(R.raw.spinners_config)
                .bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val shellName = obj.optString("shell", "")
                val stickName = obj.optString("stick", "")
                dynamicSpinners.add(
                    SpinnerConfig(
                        id = id,
                        name = obj.getString("name"),
                        cost = obj.getInt("cost"),
                        shellResId = context.resources.getIdentifier(shellName, "drawable", context.packageName)
                            .takeIf { it != 0 },
                        stickResId = context.resources.getIdentifier(stickName, "drawable", context.packageName)
                            .takeIf { it != 0 }
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    fun getSpinner(id: String): SpinnerConfig {
        val all = spinners
        return all.find { it.id == id } ?: all.first()
    }
}
