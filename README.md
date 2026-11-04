# Country for Android

Country Delight Android app built with Kotlin and Jetpack Compose. Browse products, manage deliveries and subscriptions, and review delivered items.

## Run

Open this folder in Android Studio, let Gradle sync, choose a Pixel emulator and run the `app` configuration. Java 17 and Android SDK 36 are required. Minimum supported Android version is Android 8.0 / API 26.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Its package is `com.countryd.app.nativepreview`.

An iPhone cannot run this APK. Use the Android emulator on Windows or an Android phone. [Testing instructions](docs/TESTING.md) cover both.

## Code

* `ui`: Compose screens, navigation, brand styling and native animations.
* `CountryViewModel`: shared screen state and user actions.
* `data`: typed models, catalogue loading and DataStore persistence.
* `domain`: pack pricing, offers, schedules, nutrition and feedback eligibility.
* `app/src/test`: calculation and policy tests.
* `app/src/androidTest`: emulator journeys.

See [the engineering handoff](docs/ENGINEERING_HANDOFF.md) for implementation decisions and review boundaries.
