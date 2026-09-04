$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $projectRoot

try {
    $generatedPaths = @(
        "backend/target",
        "pagbank-agent/target",
        "frontend/node_modules",
        "frontend/dist",
        "frontend/test-results",
        "frontend/coverage",
        "release"
    )

    foreach ($path in $generatedPaths) {
        git rm -r -f --ignore-unmatch -- $path | Out-Null

        if (Test-Path $path) {
            Remove-Item $path -Recurse -Force
        }
    }

    $generatedFiles = @(
        Get-ChildItem . -File -Filter "*.patch" -ErrorAction SilentlyContinue
        Get-ChildItem .\backend -File -Filter "*.dump" -ErrorAction SilentlyContinue
    )

    foreach ($file in $generatedFiles) {
        $relativePath = [System.IO.Path]::GetRelativePath(
            $projectRoot,
            $file.FullName
        )

        git rm -f --ignore-unmatch -- $relativePath | Out-Null

        if (Test-Path $file.FullName) {
            Remove-Item $file.FullName -Force
        }
    }

    Write-Host "Arquivos gerados e backups locais removidos do repositorio."
    Write-Host "Revise com: git status --short"
}
finally {
    Pop-Location
}
