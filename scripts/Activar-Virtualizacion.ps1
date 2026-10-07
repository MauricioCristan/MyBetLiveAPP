$ErrorActionPreference = 'Stop'
$windowsIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
$windowsPrincipal = [Security.Principal.WindowsPrincipal]::new($windowsIdentity)
if (-not $windowsPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Windows requiere permisos de administrador. Haz clic derecho en Activar-Virtualizacion.cmd y elige Ejecutar como administrador.'
}

Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All -NoRestart -ErrorAction Stop | Format-List FeatureName,RestartNeeded
Write-Host 'Guarda tu trabajo y reinicia Windows para activar el hipervisor. Este script no reinicia la computadora.'
Write-Host 'Despues del reinicio, abre Iniciar-MyBetLive.cmd.'
