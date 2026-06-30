package com.example.data

import com.example.R

data class SpinnerConfig(
    val id: String,
    val name: String,
    val cost: Int,
    val shellResId: Int? = null,
    val stickResId: Int? = null,
    val isCanvasBased: Boolean = false
)

object SpinnerProvider {
    val spinners = listOf(
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
        ),
        SpinnerConfig(
            id = "RETRO",
            name = "Retro Radar",
            cost = 100,
            shellResId = R.drawable.ic_radar_shell,
            stickResId = R.drawable.ic_radar_stick
        ),
        SpinnerConfig(
            id = "STEEL",
            name = "Brushed Steel",
            cost = 200,
            shellResId = R.drawable.ic_steel_shell,
            stickResId = R.drawable.ic_steel_stick
        )
    )

    fun getSpinner(id: String): SpinnerConfig {
        return spinners.find { it.id == id } ?: spinners.first()
    }
}
