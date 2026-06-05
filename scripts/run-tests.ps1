# Executa testes do backend com JDK 21
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\use-jdk21.ps1"
Set-Location (Join-Path (Split-Path $PSScriptRoot -Parent) 'backend')
.\gradlew test @args
