package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CyberNavyDarkScheme = darkColorScheme(
  primary = ElectricBlue,
  secondary = ElectricPurple,
  tertiary = NeonGreen,
  background = DeepNavy,
  surface = SpaceCard,
  error = NeonRed,
  onPrimary = DeepNavy,
  onSecondary = GhostWhite,
  onBackground = GhostWhite,
  onSurface = GhostWhite,
  onTertiary = DeepNavy,
  surfaceVariant = SpaceCard,
  onSurfaceVariant = MutedSlate
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark cyber vibe for timing accuracy gameplay
  dynamicColor: Boolean = false, // Use our handcrafted custom neon colors instead of dynamic muddy wallpaper colors
  content: @Composable () -> Unit,
) {
  // Always use CyberNavyDarkScheme to maintain the intended professional arcade console atmosphere
  val colorScheme = CyberNavyDarkScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
