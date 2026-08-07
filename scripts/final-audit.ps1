$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $projectRoot

try {
    $sourceExtensions = @("*.java", "*.ts", "*.tsx")
    $legacyPatterns = @(
        "BarDirectSale",
        "BarDish",
        "BarSaleItem",
        "BarStockReservation",
        "StockMovement",
        "LAVA_WORK_ORDER",
        "br.com.vitrine7.lava",
        "br.com.vitrine7.finance",
        "SystemSettings",
        "UserPreference",
        "PaymentTerminalSettings",
        "REVERSAL_PENDING",
        "REFUND_REQUESTED",
        "REFUNDED",
        "REVERSE_PAYMENT",
        "TerminalPaymentReversal",
        "terminal-reversal",
        "directSale",
        "saleItems",
        "adminMode",
        "window.print",
        "printWindow.print",
        "createReceiptPrintHtml",
        "receipt/print"
    )

    $sourceRoots = @(
        ".\backend\src\main\java",
        ".\backend\src\test\java",
        ".\frontend\src",
        ".\pagbank-agent\src",
        ".\printer-agent\src"
    )

    $sourceFiles = Get-ChildItem $sourceRoots -Recurse -File -Include $sourceExtensions
    $legacyResults = $sourceFiles | Select-String -Pattern $legacyPatterns

    if ($legacyResults) {
        $legacyResults |
            Select-Object Path, LineNumber, Line |
            Format-Table -Wrap

        throw "Foram encontrados residuos operacionais no codigo-fonte."
    }

    $trackedGenerated = git ls-files |
        Where-Object {
            $_ -match '(^|/)(target|dist|node_modules|test-results|coverage)(/|$)' -or
            $_ -match '\.(dump|patch)$'
        }

    if ($trackedGenerated) {
        $trackedGenerated | ForEach-Object { Write-Host $_ }
        throw "Ainda existem artefatos gerados ou backups versionados."
    }

    if (-not (Test-Path ".\backend\src\main\resources\db\migration\V43__add_print_queue.sql")) {
        throw "A migration V43 nao foi encontrada."
    }

    if (-not (Test-Path ".\printer-agent\pom.xml")) {
        throw "O printer-agent nao foi encontrado."
    }

    Write-Host "Auditoria final aprovada."
}
finally {
    Pop-Location
}
