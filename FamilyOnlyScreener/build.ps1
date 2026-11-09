# Offline build for FamilyOnly (no Gradle / no network).
# Uses only Android SDK build-tools + android.jar + JDK.
$ErrorActionPreference = "Stop"
$SDK = "C:/Users/yashk/AppData/Local/Android/Sdk"
$BT = "$SDK/build-tools/34.0.0"
$PLATFORM_JAR = "$SDK/platforms/android-34/android.jar"
$ROOT = $PSScriptRoot
Set-Location $ROOT

Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
New-Item -ItemType Directory build/gen, build/obj, build/dex | Out-Null

Write-Output "1/6 aapt2 compile res..."
& "$BT/aapt2.exe" compile --dir res -o build/compiled.zip

Write-Output "2/6 aapt2 link..."
& "$BT/aapt2.exe" link -o build/base.apk `
  -I $PLATFORM_JAR `
  --manifest AndroidManifest.xml `
  --min-sdk-version 29 --target-sdk-version 34 `
  --java build/gen `
  build/compiled.zip
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Output "3/6 javac..."
$javaFiles = Get-ChildItem -Recurse src/*.java | ForEach-Object { $_.FullName }
$genFiles = Get-ChildItem -Recurse build/gen/*.java | ForEach-Object { $_.FullName }
& javac -source 8 -target 8 -bootclasspath $PLATFORM_JAR `
  -d build/obj ($javaFiles + $genFiles)
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Output "4/6 d8..."
& "$BT/d8.bat" --lib $PLATFORM_JAR --min-api 29 --output build/dex `
  (Get-ChildItem -Recurse build/obj/*.class | ForEach-Object { $_.FullName })
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Output "5/6 add dex + zipalign..."
Copy-Item build/base.apk build/unsigned.apk -Force
Push-Location build/dex
& "$BT/aapt.exe" add -k ../unsigned.apk classes.dex
Pop-Location
if ($LASTEXITCODE -ne 0) { throw "aapt add failed" }
& "$BT/zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Output "6/6 sign..."
if (!(Test-Path "$ROOT/debug.keystore")) {
  & keytool -genkeypair -keystore "$ROOT/debug.keystore" -alias debug `
    -keyalg RSA -keysize 2048 -validity 10950 `
    -storepass android -keypass android -dname "CN=FamilyOnlyDebug"
}
& "$BT/apksigner.bat" sign --ks "$ROOT/debug.keystore" `
  --ks-pass pass:android --key-pass pass:android --out build/familyonly.apk build/aligned.apk
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Output "BUILT: $ROOT/build/familyonly.apk"
Get-Item build/familyonly.apk
