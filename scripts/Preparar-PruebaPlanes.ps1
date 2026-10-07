param(
    [ValidateSet('Preparar','Gratis','Premium','Estado')][string]$Action = 'Estado',
    [ValidateRange(1024,65535)][int]$Port = 8083
)
$ErrorActionPreference = 'Stop'
if ($Port -eq 8081) { throw 'Usa el backend aislado de prueba, no el de datos NFL reales.' }
$taskBase = "http://127.0.0.1:$Port"
function Invoke-TaskApi([string]$Path,[string]$Method='GET',$Body=$null,$Headers=@{}) {
    $taskArgs=@{Uri="$taskBase/api/v1/$Path";Method=$Method;Headers=$Headers}
    if ($null -ne $Body) { $taskArgs.ContentType='application/json'; $taskArgs.Body=($Body | ConvertTo-Json -Depth 12 -Compress) }
    Invoke-RestMethod @taskArgs
}
$taskAdminLogin=Invoke-TaskApi 'auth/login' 'POST' @{username='admin-demo';password='admin-demo-change-me'}
$taskAdmin=@{Authorization="Bearer $($taskAdminLogin.accessToken)"}
$taskClientLogin=Invoke-TaskApi 'auth/login' 'POST' @{username='demo';password='demo-change-me'}
$taskClient=@{Authorization="Bearer $($taskClientLogin.accessToken)"}
$taskOverview=Invoke-TaskApi 'admin/mybet/overview' 'GET' $null $taskAdmin
$taskProvider=Invoke-TaskApi 'admin/mybet/provider' 'GET' $null $taskAdmin
if ($taskOverview.billingEnabled -or $taskProvider.provider -notlike 'DEMO*') {
    throw 'Este script solo configura la simulación local con compras deshabilitadas.'
}
$taskUsers=Invoke-TaskApi 'admin/mybet/clients?search=demo' 'GET' $null $taskAdmin
$taskUser=$taskUsers.content | Where-Object {$_.username -eq 'demo'} | Select-Object -First 1
if (-not $taskUser) { throw 'Falta el cliente demo.' }
if ($Action -eq 'Preparar') {
    $taskPlans=Invoke-TaskApi 'admin/mybet/plans' 'GET' $null $taskAdmin
    $taskFree=$taskPlans | Where-Object {$_.code -eq 'FREE'} | Select-Object -First 1
    $taskInput=@{code='FREE';name='Gratis';description='Prueba local: hasta 3 seguimientos activos, props y notificaciones incluidos.';published=$true;playProductId=$null;playBasePlanId=$null;maxActiveTrackers=3;playerProps=$true;pushNotifications=$true;version=$taskFree.version}
    $null=Invoke-TaskApi "admin/mybet/plans/$($taskFree.id)" 'PUT' $taskInput $taskAdmin
    if (-not ($taskPlans | Where-Object {$_.code -eq 'PREMIUM'})) {
        $taskInput=@{code='PREMIUM';name='Premium';description='Prueba local: hasta 25 seguimientos activos, props y notificaciones incluidos. Las alertas personalizadas y análisis aún están pendientes.';published=$true;playProductId=$null;playBasePlanId=$null;maxActiveTrackers=25;playerProps=$true;pushNotifications=$true;version=0}
        $null=Invoke-TaskApi 'admin/mybet/plans' 'POST' $taskInput $taskAdmin
    }
    # El snapshot en juego queda en caché para verificar límites con una cantidad estable de activos.
    $taskDate=[DateTimeOffset]::UtcNow.ToString('yyyy-MM-dd')
    $taskEventsResult=Invoke-TaskApi "tracking/events?sport=NFL&date=$taskDate" 'GET' $null $taskClient
    $taskEvents=@($taskEventsResult)
    if ($taskEvents.Count -ne 1 -or $taskEvents[0].event.status -ne 'LIVE') {
        throw 'Inicia de nuevo el backend de prueba para capturar un partido en juego.'
    }
}
if ($Action -in @('Gratis','Premium')) {
    $taskCurrent=Invoke-TaskApi 'subscriptions/me' 'GET' $null $taskClient
    foreach ($taskSubscription in $taskCurrent.subscriptions | Where-Object {$_.source -eq 'MANUAL' -and $_.active}) {
        $null=Invoke-TaskApi "admin/mybet/subscriptions/$($taskSubscription.id)" 'DELETE' $null $taskAdmin
    }
    if ($Action -eq 'Premium') {
        $taskPlans=Invoke-TaskApi 'admin/mybet/plans' 'GET' $null $taskAdmin
        $taskPremium=$taskPlans | Where-Object {$_.code -eq 'PREMIUM'} | Select-Object -First 1
        if (-not $taskPremium) { throw 'Ejecuta primero -Action Preparar.' }
        $taskGrant=@{planId=$taskPremium.id;expiresAt=[DateTimeOffset]::UtcNow.AddDays(1).ToString('o');note='Prueba en celular: Premium manual durante 24 horas, sin cobro.'}
        $null=Invoke-TaskApi "admin/mybet/clients/$($taskUser.id)/subscriptions" 'POST' $taskGrant $taskAdmin
    }
}
$taskState=Invoke-TaskApi 'subscriptions/me' 'GET' $null $taskClient
[pscustomobject]@{username='demo';port=$Port;plan=$taskState.plan.code;name=$taskState.plan.name;activeTrackers=$taskState.activeTrackers;limit=$taskState.access.maxActiveTrackers;playerProps=$taskState.access.playerProps;notifications=$taskState.access.pushNotifications;billingEnabled=$taskState.billingEnabled} | ConvertTo-Json -Compress
