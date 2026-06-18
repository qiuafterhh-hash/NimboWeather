$ErrorActionPreference = "Continue"
$sdk = "C:\Users\TaKu\AppData\Local\Android\Sdk"
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$adb = Join-Path $sdk "platform-tools\adb.exe"
$apk = "D:\塔酷\SDK\AI-cc\anythink project\NimboWeather\app\build\outputs\apk\debug\app-debug.apk"
$shot = "D:\塔酷\SDK\AI-cc\anythink project\NimboWeather\screenshot.png"

& $adb start-server | Out-Null
Write-Output "waiting for device..."
& $adb wait-for-device

$booted = $false
for ($i = 0; $i -lt 150; $i++) {
    $b = (& $adb shell getprop sys.boot_completed 2>$null)
    if ("$b".Trim() -eq "1") { $booted = $true; break }
    Start-Sleep -Seconds 2
}
Write-Output "boot_completed=$booted"
if (-not $booted) { Write-Output "BOOT_TIMEOUT"; exit 1 }

Start-Sleep -Seconds 3
Write-Output "=== install ==="
& $adb install -r "$apk" 2>&1 | Select-Object -Last 3

Write-Output "=== launch ==="
& $adb shell monkey -p com.nimboweather.forecast -c android.intent.category.LAUNCHER 1 2>&1 | Select-Object -Last 2

Start-Sleep -Seconds 10
& $adb shell screencap -p /sdcard/shot.png
& $adb pull /sdcard/shot.png "$shot" 2>&1 | Select-Object -Last 2

if (Test-Path $shot) {
    Write-Output ("SHOT_OK {0:N0} bytes" -f (Get-Item $shot).Length)
} else {
    Write-Output "SHOT_FAILED"
}

Write-Output "=== app process / logcat (ad+weather) ==="
& $adb shell ps -A 2>$null | Select-String "nimbo"
& $adb logcat -d -t 80 2>$null | Select-String "AdMediator|AdMob|MobileAds|OkHttp|openweather|Weather|nimbo" | Select-Object -Last 25
