# set-java21.ps1
# Script auxiliar para configurar Java 21 no PowerShell atual

Write-Host "Procurando instalação do Java 21..." -ForegroundColor Cyan

$possibleRoots = @(
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Java",
    "C:\Program Files (x86)\Eclipse Adoptium",
    "C:\Program Files (x86)\Java"
)

$jdk21 = $null

foreach ($root in $possibleRoots) {
    if (Test-Path $root) {
        $candidates = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue | 
                      Where-Object { $_.Name -like "*21*" -or $_.Name -like "*jdk-21*" }
        
        if ($candidates) {
            $jdk21 = $candidates | Sort-Object Name -Descending | Select-Object -First 1
            break
        }
    }
}

if ($jdk21) {
    $env:JAVA_HOME = $jdk21.FullName
    # Remove versões antigas do Java do PATH e adiciona a nova
    $newPath = ($env:Path -split ';' | Where-Object { 
        $_ -notlike '*Java\jdk*' -and 
        $_ -notlike '*Adoptium*' -and 
        $_ -notlike '*jre1.8*' 
    }) -join ';'
    
    $env:Path = "$env:JAVA_HOME\bin;$newPath"
    
    Write-Host "`n✅ JAVA_HOME configurado com sucesso!" -ForegroundColor Green
    Write-Host "   Caminho: $env:JAVA_HOME" -ForegroundColor Green
    
    Write-Host "`nVersão atual do Java:" -ForegroundColor Yellow
    java -version
    
    Write-Host "`nAgora você pode compilar o projeto:" -ForegroundColor Cyan
    Write-Host "   cd backend" -ForegroundColor White
    Write-Host "   .\gradlew.bat clean build -x test" -ForegroundColor White
} else {
    Write-Host "`n❌ Java 21 não encontrado nos caminhos padrão." -ForegroundColor Red
    Write-Host "`nPor favor:" -ForegroundColor Yellow
    Write-Host "1. Instale o Java 21 (Temurin recomendado):" -ForegroundColor Yellow
    Write-Host "   winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor White
    Write-Host "`n2. Reabra este PowerShell como Administrador e rode este script novamente." -ForegroundColor Yellow
    Write-Host "`nOu consulte o arquivo JAVA21_SETUP_INSTRUCTIONS.md" -ForegroundColor Cyan
}
