# Setup produção — Fiscal + AG Grid + JDK 21

## 1. JDK 21

Instalado neste ambiente via:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Uso diário:

```powershell
.\scripts\use-jdk21.ps1          # define JAVA_HOME na sessão
.\scripts\run-tests.ps1          # todos os testes
.\scripts\run-tests.ps1 --tests "com.contractops.api.fiscal.FiscalGatewayTest"  # só fiscal
```

> **Dica:** defina `JAVA_HOME` permanentemente no Windows apontando para  
> `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` (versão pode variar).

## 2. Certificados ICP-Brasil

1. Copie `config/fiscal.env.example` → `config/fiscal.env`
2. Coloque os `.pfx` em `config/certs/` (veja `config/certs/README.md`)
3. Preencha senhas e CNPJs em `config/fiscal.env`

## 3. Backend (fiscal `production`)

```powershell
.\scripts\load-fiscal-env.ps1
$env:SPRING_PROFILES_ACTIVE = 'local'   # ou 'prod' em servidor
.\scripts\run-backend-local.ps1
```

Verifique: `GET http://localhost:8080/api/fiscal/status` → `mode: production`, `certificateConfigured: true`.

## 4. Frontend (AG Grid Pivot)

```powershell
copy frontend\.env.example frontend\.env
# Edite frontend\.env e cole sua licença Trial/Enterprise:
# VITE_AG_GRID_LICENSE_KEY=CompanyName_LicenseKey_...
.\scripts\run-frontend-local.ps1
```

Pivot e SideBar enterprise só aparecem com licença válida.
