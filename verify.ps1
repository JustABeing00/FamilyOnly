# Verify FamilyOnly screening + forwarding
# Usage: .\verify.ps1 [-Device SERIAL] (or set ANDROID_SERIAL).
param(
  [string]$Device = $env:ANDROID_SERIAL
)
$DeviceArgs = @()
if (-not [string]::IsNullOrWhiteSpace($Device)) { $DeviceArgs = @("-s", $Device) }
Write-Output "=== ROLE ==="
adb @DeviceArgs shell dumpsys role | Select-String -Pattern "CALL_SCREENING" -Context 2

Write-Output "`n=== LAST SCREENING DECISIONS ==="
adb @DeviceArgs shell dumpsys telecom | Select-String -Pattern "SCREENING_COMPLETED|FILTERING_COMPLETED" | Select-Object -Last 10

Write-Output "`n=== QUERY busy-forward status ==="
Write-Output "Opening dialer with *#67# - press CALL on phone, read result, then return here."
adb @DeviceArgs shell am start -a android.intent.action.DIAL -d "tel:*%2367%23"

Write-Output "`n=== HOW TO TEST ==="
Write-Output "1. Add trusted numbers in FamilyOnly, keep app screening enabled."
Write-Output "2. From another phone, call your number."
Write-Output "3. Trusted number -> should ring. Unknown -> rejected."
Write-Output "4. Check logcat: adb logcat -s FamilyOnly:*"
