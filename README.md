# One Tap

One Tap is an Android timing game built with Kotlin and Jetpack Compose. The player taps at the right moment as the target changes across levels. The app tracks hit and miss states, calculates accuracy, and stores best scores and leaderboard entries locally on the device.

## Current features

- Home, game, result, and leaderboard screens
- Ready, running, hit, and miss game states
- Level-based timing and target adjustments
- Accuracy and best-score tracking
- Local score persistence using Android SharedPreferences
- Generated sound effects and vibration feedback

The leaderboard is local to the device. The repository code currently seeds example leaderboard entries, so these should not be interpreted as real player rankings.

## Tech stack

- Kotlin
- Android SDK
- Jetpack Compose
- AndroidX lifecycle / ViewModel
- SharedPreferences
- Android AudioTrack and vibration APIs

## Run locally

1. Install Android Studio and the Android SDK required by the project.
2. Clone this repository and open it in Android Studio.
3. Allow Gradle sync to complete.
4. Run the app on an emulator or a physical Android device.

You can also build from the project root:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.gradlew.bat assembleDebug
```

## Project structure

- `app/`: Android application module
- `app/src/main/java/`: Kotlin application source
- `gradle/`: Gradle wrapper configuration
- `build.gradle.kts`: root Gradle build configuration
- `settings.gradle.kts`: project settings

## Notes

- Scores are stored locally. No online leaderboard or account sync is implemented in the inspected source.
- The project contains generated build output and a development keystore-related file in the repository history/tree. Do not use a debug keystore for production signing.
- Review the Gradle configuration and replace generic package identifiers before publishing a production app.
- No license file is currently provided. All rights reserved by default unless a license is added.

## Screenshots

Add current screenshots of the running app here.
