$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$proj = "D:\塔酷\SDK\AI-cc\anythink project\NimboWeather"
$jdk = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
$sdk = "C:\Users\TaKu\AppData\Local\Android\Sdk"
$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

$gradleHome = "C:\gradle"
$gradleVer = "8.9"
$gradleBin = Join-Path $gradleHome "gradle-$gradleVer\bin\gradle.bat"

if (-not (Test-Path $gradleBin)) {
    Write-Output "Downloading Gradle $gradleVer..."
    New-Item -ItemType Directory -Force -Path $gradleHome | Out-Null
    $zip = Join-Path $env:TEMP "gradle-$gradleVer-bin.zip"
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVer-bin.zip" -OutFile $zip
    Write-Output ("Downloaded {0:N1} MB" -f ((Get-Item $zip).Length/1MB))
    Expand-Archive -Path $zip -DestinationPath $gradleHome -Force
}
Write-Output "Gradle at $gradleBin"

Set-Location $proj

# Generate the wrapper (gradlew + gradle-wrapper.jar) if missing.
if (-not (Test-Path (Join-Path $proj "gradle\wrapper\gradle-wrapper.jar"))) {
    Write-Output "Generating wrapper..."
    & $gradleBin wrapper --gradle-version $gradleVer --no-daemon 2>&1 | Select-Object -Last 5
}

Write-Output "=== assembleDebug ==="
& $gradleBin --no-daemon :app:assembleDebug --stacktrace 2>&1 | Select-Object -Last 60

$apk = Join-Path $proj "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    Write-Output ("BUILD_OK apk={0} size={1:N1}MB" -f $apk, ((Get-Item $apk).Length/1MB))
} else {
    Write-Output "BUILD_FAILED no apk"
}
