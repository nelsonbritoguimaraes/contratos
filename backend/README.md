# ContractOps API (Backend)

**Kotlin + Spring Boot 3.4 + Java 21** — ERP SaaS para contratos de mão de obra (SPEC v1.0).

## Status (Maio 2026)

Monólito modular com **23+ controllers REST**, Flyway V1–V29, JWT (Keycloak-ready), fluxo operacional completo.

### Módulos principais

| Área | Prefixo API |
|------|-------------|
| Contratos / Aditivos | `/api/contracts` |
| Licitações | `/api/biddings` |
| Colaboradores | `/api/employees` |
| Ponto | `/api/time-punches` (+ `/export-aej`) |
| Glosas | `/api/glosas` |
| Medições | `/api/measurements` (+ `POST /{id}/approve`) |
| Financeiro | `/api/financeiro` |
| Contabilidade | `/api/contabilidade` |
| RH / Folha | `/api/rh/payslips`, `/api/rh/rubrics` |
| IA | `/api/ia` |

### Integração fiscal (`contractops.fiscal.mode`)

| Modo | Comportamento |
|------|----------------|
| `stub` (default) | Protocolo/recibo simulado |
| `sandbox` | Valida XML localmente |
| `production` | HTTP + certificado ICP-Brasil |

Endpoints: `GET /api/fiscal/status`, `POST /api/fiscal/sped/ecd/transmit`, `POST /api/rh/esocial/transmit/{eventId}`.

Configure certificados em `application.yml` → `contractops.fiscal.esocial.certificate-path`, `nfse.certificate-path`.

## Como rodar

```powershell
# Na raiz do projeto
docker compose up -d

cd backend
gradle wrapper --gradle-version 8.10   # primeira vez
./gradlew bootRun
```

API: **http://localhost:8080**

## Testes

Requer **JDK 17+** no `JAVA_HOME` (toolchain do projeto: **Java 21**). O Java 8 do PATH não funciona.

```powershell
# Exemplo: JDK do JetBrains (ajuste o caminho) ou instale Temurin 21
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot"
./gradlew test
```

Inclui `FiscalGatewayTest`, `GlosaEngineTest`, `MeasurementServiceTest`, `AejExportServiceTest`.

## Documentação

- [spec.md](../spec.md)
- [IMPLEMENTATION_STATUS.md](../IMPLEMENTATION_STATUS.md)
