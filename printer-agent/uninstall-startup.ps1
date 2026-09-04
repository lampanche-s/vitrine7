$ErrorActionPreference = "Stop"
$taskName = "Vitrine7 Printer Agent"

$task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($task) {
    Stop-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
}

Write-Host "Inicializacao automatica removida."
