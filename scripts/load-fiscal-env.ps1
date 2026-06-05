# Carrega config/fiscal.env para o processo atual (PowerShell)
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root 'config\fiscal.env'
if (-not (Test-Path $envFile)) {
    Write-Warning "Crie $envFile a partir de config\fiscal.env.example"
    return
}
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
    $name, $value = $_ -split '=', 2
    if ($name) { Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim().Trim('"') }
}
$env:CONTRACTOPS_HOME = $root
Write-Host "Fiscal env carregado de $envFile (CONTRACTOPS_HOME=$root)"
