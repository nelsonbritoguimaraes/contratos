# Configuração do Java 21 (Obrigatório para Build)

O projeto **ContractOps AI** exige **Java 21** (ou superior). Atualmente seu ambiente tem apenas Java 8.

## Passo a passo para Windows (PowerShell)

### 1. Baixe o Java 21 (recomendado: Eclipse Temurin)

- Acesse: https://adoptium.net/temurin/releases/?version=21
- Baixe a versão **JDK 21** para Windows x64 (arquivo .msi ou .zip)
- Recomendado: `Eclipse Temurin 21.0.x` (LTS)

Ou use o comando abaixo no PowerShell (como Administrador):

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

### 2. Instale o JDK

- Execute o instalador `.msi`
- Marque a opção **"Set JAVA_HOME variable"** durante a instalação (importante!)

### 3. Verifique a instalação

Abra um **novo** PowerShell e execute:

```powershell
java -version
```

Você deve ver algo como:
```
openjdk version "21.0.3" 2024-...
```

### 4. Configure JAVA_HOME manualmente (se necessário)

Se o `java -version` ainda mostrar Java 8:

```powershell
# Exemplo de caminho comum após instalação via winget
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

# Verifique
java -version
```

Para tornar permanente, adicione essas variáveis no "Variáveis de Ambiente do Sistema".

### 5. Teste o build do projeto

```powershell
cd "C:\Users\nelso\OneDrive\Área de Trabalho\Contratos\backend"

# Limpa e compila (sem testes)
.\gradlew.bat clean build -x test

# Se quiser rodar os testes também:
.\gradlew.bat clean build
```

---

## Script auxiliar (opcional)

Crie um arquivo `set-java21.ps1` na raiz do projeto com o seguinte conteúdo e execute quando precisar:

```powershell
# set-java21.ps1
$possiblePaths = @(
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*",
    "C:\Program Files (x86)\Eclipse Adoptium\jdk-21*"
)

$jdk = $possiblePaths | ForEach-Object { Get-ChildItem $_ -ErrorAction SilentlyContinue } | Select-Object -First 1

if ($jdk) {
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = "$env:JAVA_HOME\bin;" + ($env:Path -split ';' | Where-Object { $_ -notlike '*Java\jdk*' -and $_ -notlike '*Adoptium*' } | ForEach-Object { $_ } -join ';')
    Write-Host "JAVA_HOME configurado para: $env:JAVA_HOME" -ForegroundColor Green
    java -version
} else {
    Write-Host "Java 21 não encontrado nos caminhos padrão." -ForegroundColor Red
    Write-Host "Instale via: winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Yellow
}
```

Execute com:
```powershell
.\set-java21.ps1
```

---

## Problemas comuns

- **Gradle ainda usa Java 8**: Feche completamente o terminal/PowerShell e abra um novo após configurar JAVA_HOME.
- **Erro "Daemon JVM"**: O Gradle armazena em cache o Java antigo. Rode `.\gradlew.bat --stop` depois de configurar o Java 21.
- **Permissões**: Execute o PowerShell como Administrador na primeira configuração.

Depois de configurar o Java 21, o build deve funcionar normalmente e você poderá rodar os novos testes criados.
