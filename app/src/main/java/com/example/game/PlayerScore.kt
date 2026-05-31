package com.example.game

data class PlayerScore(
    val rank: Int = 1,
    val name: String,
    val levelReached: Int,
    val accuracy: Float,
    val date: String,
    val isLocalPlayer: Boolean = false
)
