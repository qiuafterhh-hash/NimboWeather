# Black-box test runner for an attached device / emulator (adb).
# Usage:  .\scripts\test-device.ps1            (uses latest built debug APK)
#         .\scripts\test-device.ps1 -Apk path\to.apk
#
# Prereqs: adb on PATH, a device shown by `adb devices`, and (for UI flows) the
# Maestro CLI installed (https://maestro.mobile.dev). Monkey needs no extra tool.
param(
    [string]$Apk = "$PSScriptRoot\..\app\build\outputs\apk\debug\app-debug.apk",
    [int]$MonkeyEvents = 1500
)
$ErrorActionPreference = "Stop"
$pkg = "com.nimboweather.forecast"

Write-Host "== Checking device ==" -ForegroundColor Cyan
adb devices

Write-Host "== Installing $Apk ==" -ForegroundColor Cyan
adb install -r "$Apk"

$maestro = Get-Command maestro -ErrorAction SilentlyContinue
if ($maestro) {
    Write-Host "== Maestro UI flows ==" -ForegroundColor Cyan
    maestro test "$PSScriptRoot\..\.maestro"
} else {
    Write-Host "(maestro not found on PATH - skipping UI flows)" -ForegroundColor Yellow
}

Write-Host "== Monkey stability ($MonkeyEvents events) ==" -ForegroundColor Cyan
adb shell monkey -p $pkg --pct-syskeys 0 --throttle 300 -v $MonkeyEvents

Write-Host "== Done ==" -ForegroundColor Green
