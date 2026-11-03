param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath) {
    $localConfig = Join-Path $repoRoot 'local.properties'
    if (Test-Path -LiteralPath $localConfig) {
        $sdkLine = Get-Content -LiteralPath $localConfig | Where-Object { $_ -like 'sdk.dir=*' } | Select-Object -First 1
        $sdkPath = $sdkLine.Substring(8).Replace('\:', ':').Replace('\\', '\')
    }
}
if (-not $sdkPath) { throw 'Set ANDROID_HOME or open the project in Android Studio to configure the SDK.' }
$adbPath = Join-Path $sdkPath 'platform-tools\adb.exe'
$emulatorPath = Join-Path $sdkPath 'emulator\emulator.exe'
$avdName = 'Country_Pixel_API35'
if (-not (Test-Path -LiteralPath $emulatorPath)) { throw 'Install Android Emulator and an Android 15 virtual device through Android Studio Device Manager.' }

$connected = & $adbPath devices
$deviceLine = $connected | Select-String '^emulator-\d+\s+device' | Select-Object -First 1
$serial = if ($deviceLine) { $deviceLine.ToString().Split("`t")[0] } else { $null }
if ($serial) {
    $name = (& $adbPath -s $serial emu avd name | Select-Object -First 1).Trim()
    $hiddenEmulator = Get-CimInstance Win32_Process | Where-Object { $_.Name -like 'qemu-system-x86_64*' -and $_.CommandLine -like '*-no-window*' -and $_.CommandLine -like "*$avdName*" }
    if ($name -eq $avdName -and $hiddenEmulator) {
        & $adbPath -s $serial emu kill | Out-Null
        Start-Sleep -Seconds 3
        $serial = $null
    }
}
if (-not $serial) {
    # This is an interactive preview window requested by the user.
    Start-Process -FilePath $emulatorPath -ArgumentList '-avd', $avdName, '-gpu', 'auto', '-memory', '2048'
    & $adbPath wait-for-device
}
Write-Host 'Waiting for the Android phone to finish starting...'
$deadline = (Get-Date).AddMinutes(3)
while ((& $adbPath shell getprop sys.boot_completed).Trim() -ne '1') {
    if ((Get-Date) -gt $deadline) { throw 'The emulator did not finish starting. Open Device Manager to inspect it.' }
    Start-Sleep -Seconds 2
}
if (-not $SkipBuild) {
    & (Join-Path $repoRoot 'gradlew.bat') -p $repoRoot assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'The Android build failed. See the build output above.' }
}
& $adbPath install -r (Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'The APK could not be installed.' }
& $adbPath shell am start -n 'com.countryd.app.nativepreview/com.countryd.app.MainActivity'
Write-Host 'Country Native is open. Use the emulator window to try the app.'
