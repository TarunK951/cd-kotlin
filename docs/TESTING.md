# Testing the Android app

## Preview the flow on this computer

An Android emulator is a virtual Android phone running on Windows. Use an Android device or emulator for APK testing.

The development machine has Java 17, the Android SDK at `D:\andriodSdk`, and a `Country_Pixel_API35` virtual device. Its Android Emulator acceleration check passed using WHPX.

1. Open `D:\cd-kotlin` in [Android Studio](https://developer.android.com/studio).
2. Allow Gradle to finish syncing. Use the bundled or installed Java 17 JDK.
3. Open **Tools â†’ Device Manager**. Select `Country_Pixel_API35`, or create a Pixel phone with an Android 15 / API 35 system image.
4. Start the device, select it in the toolbar and run **app**.
5. Walk through welcome, mobile number, demo verification, address, VIP and shopping.

The instrumented tests use local demo verification. For connected testing, use a designated test account.

Android Studio's **Running Devices** panel lets you interact with the virtual phone and capture screenshots. See [Google's emulator guide](https://developer.android.com/studio/run/emulator).

## Install on an Android phone

Enable Developer options and USB debugging on the phone. Connect it by USB and accept its debugging prompt. Select it in Android Studio and run **app**. Alternatively:

```powershell
.\gradlew.bat assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Use **Country Native** on the phone.

## Checks for the engineering review

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

The emulator tests reset the native preview's local session before each test. Use a test emulator rather than a device containing demo activity you want to preserve.

Reports are under `app/build/reports/tests`, `app/build/reports/lint-results-debug.html` and `app/build/reports/androidTests`. Captured journey images are pulled into `app/build/outputs/connected_android_test_additional_output`.

Manual review should include 390dp and narrower layouts, larger text, Back gestures, the keyboard, reduced motion, cancelled checkout, repeated taps, photo attachment, relaunch, and network disconnection.

Live OTP, payment settlement, backend feedback receipt, customer support, delivery tracking and Play Store release are outside these local checks.
