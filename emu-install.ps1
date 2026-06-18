$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$sdk = "C:\Users\TaKu\AppData\Local\Android\Sdk"
$jdk = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$sdkmanager = Join-Path $sdk "cmdline-tools\latest\bin\sdkmanager.bat"

$img = "system-images;android-35;google_apis;x86_64"
Write-Output "Installing emulator + $img ..."
$y = ("y`r`n" * 50)
$y | & $sdkmanager --sdk_root="$sdk" "emulator" "platform-tools" $img 2>&1 | Select-Object -Last 6

Write-Output "=== installed (filter) ==="
& $sdkmanager --sdk_root="$sdk" --list_installed 2>&1 | Select-String "emulator|system-images|platform-tools"
Write-Output "EMU_INSTALL_DONE exit=$LASTEXITCODE"
