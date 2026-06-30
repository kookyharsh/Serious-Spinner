package com.example.data

enum class Difficulty(val min: Int, val max: Int) {
    EASY(1, 5),
    NORMAL(6, 15),
    HARD(16, 30),
    EXTREME(31, 50)
}
