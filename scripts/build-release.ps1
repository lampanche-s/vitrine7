$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$releaseRoot = Join-Path $projectRoot "release"
$stagingRoot = Join-Path $releaseRoot "vitrine7"

Push-Location $projectRoot

try {
    & .\scripts\final-audit.ps1

    Push-Location .\backend
    try {
        mvn clean verify
    }
    finally {
        Pop-Location
    }

    Push-Location .\frontend
    try {
        npm ci
        npm run build
        npm run lint
        npm test
    }
    finally {
        Pop-Location
    }

    Push-Location .\pagbank-agent
    try {
        mvn clean verify
    }
    finally {
        Pop-Location
    }

    Push-Location .\printer-agent
    try {
        mvn clean verify
    }
    finally {
        Pop-Location
    }

    if (Test-Path $stagingRoot) {
        Remove-Item $stagingRoot -Recurse -Force
    }

    $backendOutput = Join-Path $stagingRoot "backend"
    $frontendOutput = Join-Path $stagingRoot "frontend"

    New-Item -ItemType Directory -Force $backendOutput, $frontendOutput |
        Out-Null

    $backendJar = Get-ChildItem ".\backend\target\vitrine7-backend-*.jar" |
        Where-Object { $_.Name -notlike "*.original" } |
        Select-Object -First 1

    if (-not $backendJar) {
        throw "JAR do backend nao encontrado."
    }

    Copy-Item $backendJar.FullName (Join-Path $backendOutput "vitrine7-backend.jar")
    Copy-Item ".\frontend\dist\*" $frontendOutput -Recurse
    Copy-Item ".\DEPLOY.md" $stagingRoot

    $archive = Join-Path $releaseRoot "vitrine7-release.zip"

    if (Test-Path $archive) {
        Remove-Item $archive -Force
    }

    Compress-Archive -Path (Join-Path $stagingRoot "*") -DestinationPath $archive

    $printerArchiveRoot = Join-Path $releaseRoot "printer-agent"
    if (Test-Path $printerArchiveRoot) {
        Remove-Item $printerArchiveRoot -Recurse -Force
    }

    New-Item -ItemType Directory -Force $printerArchiveRoot | Out-Null

    $printerJar = Get-Item ".\printer-agent\target\vitrine7-printer-agent.jar"
    Copy-Item $printerJar.FullName (Join-Path $printerArchiveRoot "vitrine7-printer-agent.jar")
    Copy-Item ".\printer-agent\printer-agent.properties.example" (Join-Path $printerArchiveRoot "printer-agent.properties.example")
    Copy-Item ".\printer-agent\list-printers.ps1" (Join-Path $printerArchiveRoot "list-printers.ps1")
    Copy-Item ".\printer-agent\run-printer-agent.cmd" (Join-Path $printerArchiveRoot "run-printer-agent.cmd")
    Copy-Item ".\printer-agent\install-startup.ps1" (Join-Path $printerArchiveRoot "install-startup.ps1")
    Copy-Item ".\printer-agent\uninstall-startup.ps1" (Join-Path $printerArchiveRoot "uninstall-startup.ps1")

    $printerArchive = Join-Path $releaseRoot "vitrine7-printer-agent.zip"
    if (Test-Path $printerArchive) {
        Remove-Item $printerArchive -Force
    }

    Compress-Archive -Path (Join-Path $printerArchiveRoot "*") -DestinationPath $printerArchive

    Write-Host "Release criada em: $archive"
    Write-Host "Agente de impressao criado em: $printerArchive"
}
finally {
    Pop-Location
}
