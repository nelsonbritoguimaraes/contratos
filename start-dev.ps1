# ContractOps AI - Start All (Dev Mode)
# This script starts the full stack with ONE command

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   ContractOps AI - Starting Dev Stack" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Start infrastructure + containers (Postgres, Redis, Keycloak, Backend, Frontend)
Write-Host "[1/3] Starting Docker infrastructure (Postgres + Redis + Keycloak + Backend + Frontend)..." -ForegroundColor Yellow
docker compose up -d --build

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to start Docker services." -ForegroundColor Red
    exit 1
}

Write-Host "Docker services started successfully." -ForegroundColor Green
Write-Host ""

# 2. Start Frontend in development mode with hot reload (outside Docker for best DX)
Write-Host "[2/3] Starting Frontend dev server (Vite with hot reload)..." -ForegroundColor Yellow

$frontendPath = Join-Path $PSScriptRoot "frontend"

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; npm run dev" -WindowStyle Normal

Write-Host "Frontend dev server starting in a new window..." -ForegroundColor Green
Write-Host ""

# 3. Instructions for Backend
Write-Host "[3/3] Backend status:" -ForegroundColor Yellow
Write-Host ""
Write-Host "   The backend container is starting (http://localhost:8080)." -ForegroundColor White
Write-Host "   If you want to run backend with hot reload (recommended for development):" -ForegroundColor White
Write-Host ""
Write-Host "   1. Make sure you have Java 21 installed and JAVA_HOME set" -ForegroundColor Cyan
Write-Host "   2. Run this in another terminal:" -ForegroundColor Cyan
Write-Host ""
Write-Host "      cd backend" -ForegroundColor White
Write-Host "      .\gradlew bootRun" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Green
Write-Host "   Stack is starting!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Access points:" -ForegroundColor Cyan
Write-Host "   - Frontend (dev):   http://localhost:5173" -ForegroundColor White
Write-Host "   - Frontend (prod):  http://localhost:5173 (via Docker)" -ForegroundColor White
Write-Host "   - Backend API:      http://localhost:8080" -ForegroundColor White
Write-Host "   - Keycloak:         http://localhost:8081" -ForegroundColor White
Write-Host ""
Write-Host "Tip: Run this script again anytime with:  .\start-dev.ps1" -ForegroundColor Yellow
Write-Host ""