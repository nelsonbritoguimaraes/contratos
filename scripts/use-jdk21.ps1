# Define JAVA_HOME para Temurin 21 (winget: EclipseAdoptium.Temurin.21.JDK)
$candidates = @(
    'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot',
    'C:\Program Files\Eclipse Adoptium\jdk-21*'
)
foreach ($c in $candidates) {
    $resolved = Get-Item $c -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved) {
        $env:JAVA_HOME = $resolved.FullName
        $env:PATH = "$($env:JAVA_HOME)\bin;" + ($env:PATH -split ';' | Where-Object { $_ -notmatch 'java8path|jre1\.8' }) -join ';'
        Write-Host "JAVA_HOME=$($env:JAVA_HOME)"
        java -version
        return
    }
}
Write-Error 'JDK 21 não encontrado. Instale: winget install EclipseAdoptium.Temurin.21.JDK'
