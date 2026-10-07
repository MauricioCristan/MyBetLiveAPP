param(
    [ValidateRange(1024,65535)][int]$Port = 8083,
    [string]$BackendRoot = (Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'seed\seed-backend')
)
$ErrorActionPreference = 'Stop'
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "El puerto $Port está ocupado."
}
$env:JAVA_HOME = Join-Path $env:USERPROFILE 'jdk-25'
Set-Location -LiteralPath $backendRoot
Write-Host "Prueba local de planes: http://127.0.0.1:$Port. Datos deportivos simulados y compras deshabilitadas."
# Mantiene un snapshot en juego durante esta prueba de permisos, sin consultar API-Sports.
& .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo' "-Dspring-boot.run.arguments=--server.port=$Port --server.address=127.0.0.1 --mybet.polling.enabled=false --mybet.events.cache-seconds=86400 --spring.devtools.restart.enabled=false"
if ($LASTEXITCODE -ne 0) { throw "El backend terminó con código $LASTEXITCODE." }
