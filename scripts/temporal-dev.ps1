# Sobe Temporal dev server (UI em http://localhost:8233) e orienta variáveis do backend
Write-Host "Iniciando Temporal (dev server)..." -ForegroundColor Cyan
Set-Location -LiteralPath (Join-Path $PSScriptRoot "..")
docker compose up -d temporal

Write-Host ""
Write-Host "Temporal gRPC: localhost:7233" -ForegroundColor Green
Write-Host "Temporal UI:   http://localhost:8233" -ForegroundColor Green
Write-Host ""
Write-Host "Para o backend usar o worker:" -ForegroundColor Yellow
Write-Host '  $env:CONTRACTOPS_TEMPORAL_ENABLED = "true"'
Write-Host '  $env:CONTRACTOPS_TEMPORAL_TARGET = "localhost:7233"'
Write-Host "  .\scripts\run-backend-local.ps1"
Write-Host ""
Write-Host "Testes (JDK 21):" -ForegroundColor Yellow
Write-Host "  .\scripts\use-jdk21.ps1"
Write-Host "  cd backend"
Write-Host "  .\gradlew test --tests com.contractops.api.financeiro.service.BankStatementParserServiceTest --tests com.contractops.api.financeiro.temporal.NfsCobrancaWorkflowTest"
