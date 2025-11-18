# Jarvis - Personal AI Assistant (Kotlin)

This repository contains a full Android project that implements a voice-based personal assistant "Jarvis".
The app listens for the wake phrase ("Hey Jarvis"), records a follow-up command, sends it to the OpenAI Chat API,
and speaks back the response. It can optionally parse action lines to open apps.

IMPORTANT: Do NOT store your OpenAI API key in the repository. The app prompts for the API key on first run and stores it using EncryptedSharedPreferences.

## What is included
- Complete Kotlin source files (MainActivity, VoiceService, ChatGptApi, SecurePrefs)
- Basic layouts and AndroidManifest
- app/build.gradle and top-level Gradle files
- GitHub Actions workflow example is included in `.github/workflows/release.yml` (you still need to provide keystore secrets)

## Build instructions (no Android Studio required)
Option A: Use GitHub Actions (recommended)
1. Create a new GitHub repo and push this project.
2. Add the repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Make sure you have a gradle wrapper or update the workflow to install gradle. (If gradle wrapper is not present, install Gradle on the CI or locally.)
4. Run the workflow and download the artifact.

Option B: Build locally with Gradle (no Android Studio GUI required)
1. Install JDK 11+ and Gradle.
2. Run `gradle assembleRelease` (or `./gradlew assembleRelease` if you add the Gradle wrapper).
3. Sign the produced APK with your keystore.

## Security
- The app uses `EncryptedSharedPreferences` to store the API key locally on the device.
- Do not commit your keystore or API keys.

## Customize
- Change wake phrase detection inside `VoiceService.onPartialResults` (currently checks for "hey jarvis" or "jarvis").
- Add more ACTION handlers in `parseAndExecuteActions` to enable SEND_SMS, OPEN_URL, etc.

## Notes
- This is a starting project. For higher-quality wake-word detection, consider integrating a native wake-word engine (Porcupine, Snowboy, etc.).
- The ChatGptApi uses okhttp and minimal JSON handling. For production, consider Retrofit + proper JSON parsing.

