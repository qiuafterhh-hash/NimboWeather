$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$sdk = "C:\Users\TaKu\AppData\Local\Android\Sdk"
$jdk = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

New-Item -ItemType Directory -Force -Path $sdk | Out-Null
$cmdlineParent = Join-Path $sdk "cmdline-tools"
New-Item -ItemType Directory -Force -Path $cmdlineParent | Out-Null

$latest = Join-Path $cmdlineParent "latest"
if (-not (Test-Path (Join-Path $latest "bin\sdkmanager.bat"))) {
    Write-Output "Downloading commandline-tools..."
    $zip = Join-Path $env:TEMP "cmdline-tools.zip"
    Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile $zip
    Write-Output ("Downloaded {0:N1} MB" -f ((Get-Item $zip).Length/1MB))
    $tmp = Join-Path $env:TEMP "cmdline-extract"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    if (Test-Path $latest) { Remove-Item $latest -Recurse -Force }
    Move-Item (Join-Path $tmp "cmdline-tools") $latest
    Write-Output "cmdline-tools installed to $latest"
} else {
    Write-Output "cmdline-tools already present"
}

$sdkmanager = Join-Path $latest "bin\sdkmanager.bat"

Write-Output "Accepting licenses..."
$y = ("y`r`n" * 50)
$y | & $sdkmanager --sdk_root="$sdk" --licenses | Out-Null

Write-Output "Installing platform-tools, platforms;android-35, build-tools;35.0.0..."
& $sdkmanager --sdk_root="$sdk" "platform-tools" "platforms;android-35" "build-tools;35.0.0" 2>&1 | Select-Object -Last 5

Write-Output "=== installed packages ==="
& $sdkmanager --sdk_root="$sdk" --list_installed 2>&1 | Select-Object -First 30
Write-Output "ANDROID_SDK_READY exit=$LASTEXITCODE"
