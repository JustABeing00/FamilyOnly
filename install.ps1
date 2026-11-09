# FamilyOnly installer: build + install + make screening app
# Usage: .\install.ps1 [-Device SERIAL]
#   With a single USB device connected, no args needed.
#   With multiple devices: .\install.ps1 -Device <serial>
#   (or set the ANDROID_SERIAL env var).
param(
  [string]$Device = $env:ANDROID_SERIAL
)
$ErrorActionPreference = "Stop"
$ROOT = $PSScriptRoot
$DeviceArgs = @()
if (-not [string]::IsNullOrWhiteSpace($Device)) { $DeviceArgs = @("-s", $Device) }

Write-Output "Building APK..."
powershell -ExecutionPolicy Bypass -File "$ROOT/FamilyOnlyScreener/build.ps1"

Write-Output "Installing..."
adb @DeviceArgs install -r "$ROOT/FamilyOnlyScreener/build/familyonly.apk"

Write-Output "Switching CALL_SCREENING role to FamilyOnly..."
adb @DeviceArgs shell cmd role add-role-holder android.app.role.CALL_SCREENING com.familyonly.screener

Write-Output "Verifying role..."
adb @DeviceArgs shell dumpsys role | Select-String -Pattern "CALL_SCREENING" -Context 2

Write-Output "Launching FamilyOnly (add your trusted numbers, then tap Enable)..."
adb @DeviceArgs shell am start -n com.familyonly.screener/.MainActivity

Write-Output "Done. Next: in the app add trusted numbers and tap 'Enable call screening'."
