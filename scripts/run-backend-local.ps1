# Backend com perfil local + fiscal production (certificados em config/certs/)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\use-jdk21.ps1"
. "$PSScriptRoot\load-fiscal-env.ps1"
$env:SPRING_PROFILES_ACTIVE = 'local'
Set-Location (Join-Path (Split-Path $PSScriptRoot -Parent) 'backend')
.\gradlew bootRun @args
