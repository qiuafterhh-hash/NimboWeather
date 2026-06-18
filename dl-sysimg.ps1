$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$sdk = "C:\Users\TaKu\AppData\Local\Android\Sdk"
$url = "https://dl.google.com/android/repository/sys-img/google_apis/x86_64-35_r09.zip"
$zip = "C:\gradle\x86_64-35_r09.zip"   # ASCII path

New-Item -ItemType Directory -Force -Path (Split-Path $zip) | Out-Null

Write-Output "Downloading system image (~1.5GB) via BITS (resumable)..."
$ok = $false
try {
    Import-Module BitsTransfer -ErrorAction Stop
    Start-BitsTransfer -Source $url -Destination $zip -Description "android-35 google_apis x86_64"
    $ok = $true
} catch {
    Write-Output "BITS failed: $_  -> falling back to Invoke-WebRequest"
}
if (-not $ok) {
    Invoke-WebRequest -Uri $url -OutFile $zip
}

if (-not (Test-Path $zip)) { Write-Output "DOWNLOAD_FAILED"; exit 1 }
Write-Output ("Downloaded {0:N1} MB" -f ((Get-Item $zip).Length / 1MB))

$dest = Join-Path $sdk "system-images\android-35\google_apis"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
$x86 = Join-Path $dest "x86_64"
if (Test-Path $x86) { Remove-Item $x86 -Recurse -Force }
Expand-Archive -Path $zip -DestinationPath $dest -Force
Write-Output "Extracted to $dest"

$sp = Join-Path $x86 "source.properties"
if (Test-Path $sp) {
    Write-Output "=== source.properties ==="
    Get-Content $sp
} else {
    Write-Output "WARN: source.properties missing"
}
$sysimg = Join-Path $x86 "system.img"
if (Test-Path $sysimg) {
    Write-Output ("system.img OK {0:N1} MB" -f ((Get-Item $sysimg).Length / 1MB))
}
Write-Output "SYSIMG_DONE"
