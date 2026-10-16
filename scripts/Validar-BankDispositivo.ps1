param([string]$Adb = 'D:\Android\Sdk\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de pruebas.' }
    # Install updates only. Never uninstall the target application: it holds user data.
    & $Adb install -r app\build\outputs\apk\debug\app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'No se instaló la actualización. Revisa la firma; no desinstales la app.' }
    & $Adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'No se instaló el APK de pruebas.' }
    & $Adb shell am instrument -w -e class com.mybetlive.ui.BankDevicePreviewTest com.mybetlive.test/androidx.test.runner.AndroidJUnitRunner
    if ($LASTEXITCODE -ne 0) { throw 'Revisa el resultado de instrumentación.' }
    Write-Host 'Las capturas están en files/bank-qa del almacenamiento privado de la app.'
    Write-Host 'La vista es local, no consulta cuentas ni realiza operaciones financieras.'
} finally {
    Pop-Location
}
