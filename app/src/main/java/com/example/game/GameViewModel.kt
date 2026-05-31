package com.example.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class Screen {
    Home, Game, Result, Leaderboard
}

enum class PlayState {
    Ready,     // Waiting for tap to spin
    Running,   // Spin in progress
    Hit,       // Success animation / pause
    Miss       // Miss animation / pause
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("one_tap_prefs", Context.MODE_PRIVATE)

    // Screen State
    private val _currentScreen = MutableStateFlow(Screen.Home)
    val currentScreen = _currentScreen.asStateFlow()

    // Game States
    private val _playState = MutableStateFlow(PlayState.Ready)
    val playState = _playState.asStateFlow()

    private val _currentLevel = MutableStateFlow(1)
    val currentLevel = _currentLevel.asStateFlow()

    private val _targetCenterAngle = MutableStateFlow(180f)
    val targetCenterAngle = _targetCenterAngle.asStateFlow()

    private val _targetWidth = MutableStateFlow(45f)
    val targetWidth = _targetWidth.asStateFlow()

    // Accuracy and Stats
    private val _lastAccuracy = MutableStateFlow(0f)
    val lastAccuracy = _lastAccuracy.asStateFlow()

    private val _bestLevel = MutableStateFlow(1)
    val bestLevel = _bestLevel.asStateFlow()

    private val _bestAccuracy = MutableStateFlow(0f)
    val bestAccuracy = _bestAccuracy.asStateFlow()

    // High Score Submission Name
    private val _showNamePrompt = MutableStateFlow(false)
    val showNamePrompt = _showNamePrompt.asStateFlow()

    // Leaderboard
    private val _leaderboard = MutableStateFlow<List<PlayerScore>>(emptyList())
    val leaderboard = _leaderboard.asStateFlow()

    init {
        loadBestScores()
        loadLeaderboard()
    }

    private fun loadBestScores() {
        _bestLevel.value = prefs.getInt("best_level", 1)
        _bestAccuracy.value = prefs.getFloat("best_accuracy", 0f)
    }

    fun saveBestScores(level: Int, accuracy: Float) {
        val currentBestLevel = _bestLevel.value
        val currentBestAccuracy = _bestAccuracy.value

        var isNewBest = false
        if (level > currentBestLevel) {
            _bestLevel.value = level
            prefs.edit().putInt("best_level", level).apply()
            isNewBest = true
        }

        // Keep local best accuracy for highest level reached
        if (level >= currentBestLevel && accuracy > currentBestAccuracy) {
            _bestAccuracy.value = accuracy
            prefs.edit().putFloat("best_accuracy", accuracy).apply()
            isNewBest = true
        }

        if (isNewBest) {
            // Check if player dynamic entry should go into the leaderboard
            checkLeaderboardEligibility(level, accuracy)
        }
    }

    private fun loadLeaderboard() {
        val rawList = prefs.getStringSet("leaderboard_records", null)
        if (rawList == null) {
            // Create default neon arcade pro players
            val defaultScores = listOf(
                PlayerScore(1, "ZEN_MASTER", 15, 98.4f, "2026-05-30"),
                PlayerScore(2, "NEON_TAP", 12, 95.8f, "2026-05-31"),
                PlayerScore(3, "FOCUS_HYPER", 10, 94.2f, "2026-05-29"),
                PlayerScore(4, "SLIDER", 8, 91.5f, "2026-05-31"),
                PlayerScore(5, "NANO_TUBE", 7, 88.0f, "2026-05-28"),
                PlayerScore(6, "DOT_DODGE", 5, 87.2f, "2026-05-30"),
                PlayerScore(7, "REBOUND", 4, 85.9f, "2026-05-27"),
                PlayerScore(8, "ARCADE_KID", 3, 82.1f, "2026-05-31"),
                PlayerScore(9, "QUICK_TAP", 2, 79.4f, "2026-05-29"),
                PlayerScore(10, "STATION", 1, 75.0f, "2026-05-31")
            )
            saveAndPublishLeaderboard(defaultScores)
        } else {
            val loaded = rawList.map { serialized ->
                val tokens = serialized.split("|")
                PlayerScore(
                    rank = tokens.getOrNull(0)?.toIntOrNull() ?: 1,
                    name = tokens.getOrNull(1) ?: "PLAYER",
                    levelReached = tokens.getOrNull(2)?.toIntOrNull() ?: 1,
                    accuracy = tokens.getOrNull(3)?.toFloatOrNull() ?: 0f,
                    date = tokens.getOrNull(4) ?: "2026-05-31",
                    isLocalPlayer = tokens.getOrNull(5)?.toBoolean() ?: false
                )
            }.sortedByDescending { it.levelReached }
             .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

            _leaderboard.value = loaded
        }
    }

    private fun saveAndPublishLeaderboard(list: List<PlayerScore>) {
        val sorted = list.sortedWith(
            compareByDescending<PlayerScore> { it.levelReached }
                .thenByDescending { it.accuracy }
        ).take(10)
         .mapIndexed { idx, score -> score.copy(rank = idx + 1) }

        _leaderboard.value = sorted

        val stringSet = sorted.map {
            "${it.rank}|${it.name}|${it.levelReached}|${it.accuracy}|${it.date}|${it.isLocalPlayer}"
        }.toSet()

        prefs.edit().putStringSet("leaderboard_records", stringSet).apply()
    }

    private fun checkLeaderboardEligibility(levelReached: Int, accuracy: Float) {
        val worstInTop10 = _leaderboard.value.lastOrNull()
        if (worstInTop10 == null || levelReached >= worstInTop10.levelReached) {
            _showNamePrompt.value = true
        }
    }

    fun submitLeaderboardScore(playerName: String) {
        _showNamePrompt.value = false
        val cleanName = if (playerName.isNotBlank()) playerName.trim().uppercase() else "PLAYER"
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val newList = _leaderboard.value.toMutableList()
        // Remove any previous local player score to prevent duplicates, or keep them if they represent separate achievements
        newList.removeAll { it.isLocalPlayer }

        val newEntry = PlayerScore(
            name = cleanName,
            levelReached = _bestLevel.value,
            accuracy = _bestAccuracy.value,
            date = dateString,
            isLocalPlayer = true
        )
        newList.add(newEntry)
        saveAndPublishLeaderboard(newList)
    }

    // Navigation and screen management
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.Game) {
            initLevel()
        }
    }

    // Set up variables for current level
    private fun initLevel() {
        val lvl = _currentLevel.value
        _playState.value = PlayState.Ready

        // Level speeds up: Level 1 starts at 140 deg/sec, increases 30+ each level
        // Target shrinks: Level 1 width starts at 45 degrees, shrinks 3-4 degrees each level, minimum cap at 8 degrees
        val initialWidth = 45f
        val calculatedWidth = (initialWidth - (lvl - 1) * 3.5f)
        _targetWidth.value = calculatedWidth.coerceAtLeast(8f)

        // Select a random target center angle, ensuring it is away from the starting top center (0/360 or 270)
        // so the player gets a split second to see the track before hitting it.
        _targetCenterAngle.value = Random.nextInt(45, 315).toFloat()
    }

    // The current rotational speed of the rotating ball for the current level
    fun getCurrentSpeed(): Float {
        val lvl = _currentLevel.value
        // Basespeed is 140 degrees per second. Speed increases dynamically
        val speed = 140f + (lvl - 1) * 35f
        return speed.coerceAtMost(700f) // Soft cap of speed to keep it humanly achievable
    }

    // Assess Tap Action
    fun processTap(tapAngle: Float) {
        if (_playState.value != PlayState.Running) return

        val center = _targetCenterAngle.value
        val halfWidth = _targetWidth.value / 2f

        val dist = angularDistance(tapAngle, center)

        if (dist <= halfWidth) {
            // HIT SUCCESS!
            _playState.value = PlayState.Hit
            
            // Play physical success sound and crisp haptic feedback
            GameEffectsManager.playSuccess()
            GameEffectsManager.triggerSuccessHaptic(getApplication())
            
            // Calculate accuracy: 100% is perfect, absolute edge is 50%
            val normalizedOffset = dist / halfWidth // 0.0 at center, 1.0 at edge
            val acc = 100f - (normalizedOffset * 50f)
            _lastAccuracy.value = acc

            // Update user highscore if higher
            saveBestScores(_currentLevel.value, acc)
        } else {
            // MISS!
            _playState.value = PlayState.Miss
            
            // Play physical miss buzzer sound and strong haptic heartbeat rumble
            GameEffectsManager.playMiss()
            GameEffectsManager.triggerMissHaptic(getApplication())
            
            _lastAccuracy.value = 0f
            // Save state but do not level up
            saveBestScores(_currentLevel.value, 0f)
        }
    }

    fun startSpinning() {
        if (_playState.value == PlayState.Ready) {
            _playState.value = PlayState.Running
            GameEffectsManager.playStart()
        }
    }

    fun proceedToNextLevel() {
        _currentLevel.value += 1
        initLevel()
    }

    fun restartGame() {
        _currentLevel.value = 1
        initLevel()
    }

    fun returnToHome() {
        _currentScreen.value = Screen.Home
    }

    fun angularDistance(a: Float, b: Float): Float {
        val diff = Math.abs(a - b) % 360f
        return if (diff > 180f) 360f - diff else diff
    }
}
