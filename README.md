# Country for Android

Native Kotlin and Jetpack Compose port of the Country Delight app in `D:\country`.

The app uses the original catalogue, images, fonts and Lucide icon paths. It does not embed React Native, JavaScript or a WebView.

This is a migration branch. Core shopping flows are implemented; full visual and animation parity has not been signed off. See [migration/PARITY.md](migration/PARITY.md).

## Run

Open this folder in Android Studio, let Gradle sync, choose a Pixel emulator and run the `app` configuration. Java 17 and Android SDK 36 are required. Minimum supported Android version is Android 8.0 / API 26.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Its package is `com.countryd.app.nativepreview`, so it can coexist with the Expo Android app.

An iPhone cannot run this APK. Use the Android emulator on Windows or an Android phone. [Testing instructions](docs/TESTING.md) cover both.

## Code

* `ui`: Compose screens, navigation, brand styling and native animations.
* `CountryViewModel`: shared screen state and user actions.
* `data`: typed models, catalogue loading and DataStore persistence.
* `domain`: pack pricing, offers, schedules, nutrition and feedback eligibility.
* `app/src/test`: calculation and policy tests.
* `app/src/androidTest`: emulator journeys.
* `migration`: source hashes, comparison images and parity notes.

See [the engineering handoff](docs/ENGINEERING_HANDOFF.md) for implementation decisions and review boundaries.

Login, payments, wallet credit, deliveries, support and feedback transport remain local demos, as in the source app. Nothing here performs a real charge or places a real delivery.
