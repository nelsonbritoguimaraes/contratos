# Frontend com .env (AG Grid license + demo fallback)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $root 'frontend\.env'
if (-not (Test-Path $envFile)) {
    Copy-Item (Join-Path $root 'frontend\.env.example') $envFile
    Write-Warning "Criado frontend/.env — defina VITE_AG_GRID_LICENSE_KEY antes do Pivot enterprise."
}
Set-Location (Join-Path $root 'frontend')
npm run dev @args
