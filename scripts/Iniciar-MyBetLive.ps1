param(
    [switch]$Software,
    [int]$EmulatorPort = 5554,
    [int]$BootTimeoutSeconds = 300,
    [string]$SdkRoot = '',
    [string]$BackendRoot = (Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'seed\seed-backend'),
    [string]$BackendJava = (Join-Path $env:USERPROFILE 'jdk-25')
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Path $PSScriptRoot -Parent
if (-not $SdkRoot) { $SdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $SdkRoot) { $SdkRoot = $env:ANDROID_HOME }
if (-not $SdkRoot) { $SdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$emulatorPath = Join-Path $sdkRoot 'emulator\emulator.exe'
$adbPath = Join-Path $sdkRoot 'platform-tools\adb.exe'
$avdName = 'MyBetLive_API_36'
$deviceSerial = "emulator-$EmulatorPort"
$apkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
$runtimeRoot = Join-Path $projectRoot '.local\runtime'

foreach ($requiredFile in @($emulatorPath, $adbPath, $apkPath)) {
    if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
        throw "Falta $requiredFile. Revisa el SDK o compila assembleDebug."
    }
}
if ($EmulatorPort -lt 5554 -or $EmulatorPort -gt 5682 -or $EmulatorPort % 2 -ne 0) {
    throw 'El puerto del emulador debe ser par, entre 5554 y 5682.'
}
if (@(& $emulatorPath -list-avds) -notcontains $avdName) {
    throw "No existe el dispositivo virtual $avdName."
}

$devices = @(& $adbPath devices)
$isOnline = [bool]($devices -match "^$deviceSerial\s+device$")
if (-not $isOnline -and $devices -match "^$deviceSerial\s+offline$") {
    throw "El emulador $deviceSerial ya esta iniciando o no responde. Espera a que termine o cierra su ventana antes de volver a ejecutar el lanzador."
}
if ($isOnline) {
    $runningAvd = @(& $adbPath -s $deviceSerial emu avd name)[0]
    if ($runningAvd -ne $avdName) {
        throw "El puerto $EmulatorPort pertenece a otro emulador. Usa -EmulatorPort 5556."
    }
} elseif (-not $Software) {
    $acceleration = @(& $emulatorPath -accel-check 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Windows no tiene aceleracion disponible. Ejecuta Activar-Virtualizacion.cmd como administrador y reinicia Windows. Para una prueba lenta, usa -Software. Detalle: $($acceleration -join ' ')"
    }
}

$null = New-Item -Path $runtimeRoot -ItemType Directory -Force
if (-not (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)) {
    if (-not (Test-Path -LiteralPath (Join-Path $backendJava 'bin\java.exe'))) {
        throw "No se encuentra JDK 25 en $backendJava."
    }
    $previousJavaHome = $env:JAVA_HOME
    try {
        $env:JAVA_HOME = $backendJava
        $backendProcess = Start-Process -FilePath (Join-Path $backendRoot 'mvnw.cmd') -ArgumentList @('spring-boot:run', '-Dspring-boot.run.profiles=demo') -WorkingDirectory $backendRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimeRoot 'backend.stdout.log') -RedirectStandardError (Join-Path $runtimeRoot 'backend.stderr.log') -PassThru
        Write-Host "Backend demo iniciando (proceso $($backendProcess.Id))."
    } finally {
        $env:JAVA_HOME = $previousJavaHome
    }
}

if (-not $isOnline) {
    $gpuMode = if ($Software) { 'swiftshader' } else { 'auto' }
    $emulatorArgs = @('-avd', $avdName, '-port', $EmulatorPort, '-memory', '2048', '-cores', '2', '-gpu', $gpuMode, '-no-boot-anim', '-camera-back', 'none', '-camera-front', 'none', '-no-audio')
    if ($Software) {
        $emulatorArgs += @('-accel', 'off', '-no-snapshot')
        Write-Warning 'Modo software: el arranque y la app pueden ser lentos. Activa la aceleracion de Windows para uso habitual.'
    }
    $emulatorProcess = Start-Process -FilePath $emulatorPath -ArgumentList $emulatorArgs -WorkingDirectory $projectRoot -WindowStyle Normal -RedirectStandardOutput (Join-Path $runtimeRoot 'emulator.stdout.log') -RedirectStandardError (Join-Path $runtimeRoot 'emulator.stderr.log') -PassThru
    Write-Host "Emulador iniciando (proceso $($emulatorProcess.Id))."
}

$deadline = [DateTime]::UtcNow.AddSeconds($BootTimeoutSeconds)
$booted = $false
while ([DateTime]::UtcNow -lt $deadline) {
    if ($emulatorProcess -and $emulatorProcess.HasExited) {
        throw "El emulador se cerro. Revisa $runtimeRoot\emulator.stderr.log."
    }
    $devices = @(& $adbPath devices)
    if ($devices -match "^$deviceSerial\s+device$") {
        $bootCompleted = @(& $adbPath -s $deviceSerial shell getprop sys.boot_completed 2>$null)
        if ($bootCompleted -contains '1') { $booted = $true; break }
    }
    Start-Sleep -Seconds 3
}
if (-not $booted) {
    throw "El emulador no termino de arrancar. Revisa $runtimeRoot\emulator.stderr.log. Si usaste -Software, activa la aceleracion de Windows."
}

$backendDeadline = [DateTime]::UtcNow.AddSeconds(90)
while (-not (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)) {
    if ([DateTime]::UtcNow -gt $backendDeadline) {
        throw "El backend no abrio el puerto 8080. Revisa $runtimeRoot\backend.stdout.log."
    }
    Start-Sleep -Seconds 2
}

& $adbPath -s $deviceSerial install -r $apkPath
if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar MyBetLive en el emulador.' }
& $adbPath -s $deviceSerial shell am start -W -n com.mybetlive/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir MyBetLive.' }
Write-Host 'MyBetLive abierta. Backend: http://10.0.2.2:8080. Cuenta de demo: demo / demo-change-me.'
