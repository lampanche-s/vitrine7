$ErrorActionPreference = "Stop"

$agentDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $agentDir "vitrine7-printer-agent.jar"
$configPath = Join-Path $agentDir "printer-agent.properties"
$taskName = "Vitrine7 Printer Agent"

if (-not (Test-Path $jarPath)) {
    throw "vitrine7-printer-agent.jar nao encontrado em $agentDir"
}

if (-not (Test-Path $configPath)) {
    throw "printer-agent.properties nao encontrado em $agentDir"
}

$javaw = (Get-Command javaw.exe -ErrorAction Stop).Source
$arguments = "-jar `"$jarPath`" `"$configPath`""

$action = New-ScheduledTaskAction `
    -Execute $javaw `
    -Argument $arguments `
    -WorkingDirectory $agentDir

$trigger = New-ScheduledTaskTrigger -AtLogOn

Register-ScheduledTask `
    -TaskName $taskName `
    -Action $action `
    -Trigger $trigger `
    -Description "Agente local de impressao termica do Vitrine 7" `
    -Force |
    Out-Null

Start-ScheduledTask -TaskName $taskName
Write-Host "Agente configurado para iniciar com o Windows."
