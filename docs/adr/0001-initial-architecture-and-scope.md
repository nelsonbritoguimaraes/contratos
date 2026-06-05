# ADR 0001: Arquitetura Inicial e Escopo do Scaffold — ContractOps AI

**Data**: 2026-04 (data do scaffold)  
**Status**: Aceito  
**Decisor(es)**: Usuário + Grok (implementador)  
**Contexto**: SPEC v1.0 completa para ERP SaaS de contratos públicos de mão de obra exclusiva.

## Contexto

A SPEC v1.0 descreve um sistema extremamente amplo (33 seções, 50+ entidades, integrações governamentais complexas, motor de IA, 6 fases de MVP).

Decisões iniciais de stack foram dadas na SPEC (Kotlin + Spring Boot, PostgreSQL, React + Material Design 3 + AG Grid Enterprise).

O usuário confirmou explicitamente em 2026-04:
- Escopo do primeiro deliverable = **Scaffold apenas** (estrutura + ambiente de dev funcional + demo grid, **sem features de negócio completas**).
- Adesão **estrita** ao backend Kotlin + Spring Boot + Java 21/25 desde o dia 1.
- Nome do produto: **ContractOps AI**.
- AG Grid Enterprise (usuário fornecerá licença quando necessário).
- Demo local deve subir via `docker compose up` + frontend Vite e mostrar grid de contratos com grouping por órgão/status (dados seed: 3 empresas, 5 contratos, 20 postos).

## Decisão

### 1. Estrutura de Repositório
- **Monorepo** na fase de scaffold e MVP 1–2.
  - `backend/` — Kotlin/Spring Boot (domínios modulares)
  - `frontend/` — Vite + React + TS
  - `infra/` — Docker, seeds, Keycloak realms, Terraform futuro
  - `docs/` — SPEC, ADRs, runbooks

Justificativa: Facilita shared types, documentação única, CI/CD inicial e iteração rápida entre frontend e contratos. Evoluiremos para multi-repo quando o módulo de folha/contabilidade ou IA virar serviço independente.

### 2. Stack Técnica (confirmada)
| Camada              | Escolha                                      | Motivo (alinhado à SPEC) |
|---------------------|----------------------------------------------|--------------------------|
| Backend             | Kotlin 2.x + Spring Boot 3.4+ + Java 21/25   | Estabilidade, transações, tipagem, ecossistema fiscal/trabalhista (eSocial, NFS-e, SPED) |
| Banco               | PostgreSQL 16+                               | RLS, JSONB, extensões, maturidade para ERP |
| Cache/Filas         | Redis + (futuro Kafka/Redpanda)              | SPEC |
| Auth                | Keycloak (inicial) → WorkOS/Auth0 enterprise | RBAC + ABAC + SSO por cliente |
| Frontend            | React 18 + TS + Vite + MUI v6 (MD3 theme)    | Material Design 3 conforme SPEC |
| Data Grid           | AG Grid Enterprise (Quartz)                  | Row Grouping + Pivot conforme SPEC seção 3.3 |
| Workflow            | (futuro) Temporal.io                         | SPEC |
| Observabilidade     | (futuro) OpenTelemetry + Grafana             | SPEC |
| IA                  | AI Orchestrator + múltiplos provedores       | SPEC seção 23 |

### 3. Multi-tenancy
- Toda tabela terá `tenant_id` (UUID ou bigint).
- Row Level Security (RLS) será ativado no Postgres assim que o backend tiver queries consistentes.
- Opção enterprise futura: schema ou database isolado por tenant grande (holding).

### 4. AG Grid
- Pacotes enterprise já referenciados no `package.json`.
- Para o scaffold usamos a distribuição community (executável imediatamente).
- Upgrade path documentado: adicionar `LicenseManager.setLicenseKey(...)` + trocar imports quando a licença for obtida.
- O grid demo já usa `rowGroup` por `orgao` e `status` para demonstrar o padrão exigido na SPEC (telas de contratos, postos, glosas, folha).

### 5. Escopo do Scaffold (o que NÃO está incluído)
- Nenhum motor de cálculo de folha, glosa ou IMR.
- Nenhuma integração real com eSocial, NFS-e Nacional, relógios de ponto ou bancos.
- Backend Kotlin é apenas estrutura + Application class + Dockerfile (vazio de lógica de domínio).
- Sem autenticação real no frontend (stub).
- Sem agentes de IA.
- Sem portal do fiscal/órgão.

Isso está **100% alinhado** à resposta do usuário: "Scaffold only: ... empty Gradle + Vite projects ... (no real features yet)".

### 6. Caminho para MVP 1 (próximos passos)
Conforme seção 14 da SPEC:
1. Backend: entidades JPA (Tenant, Company, Branch, Contract, ContractLot, ServicePost) + Flyway + basic REST.
2. Frontend: CRUD + grid real (substituir seed estático por chamadas à API).
3. Importação de planilha vencedora (Excel → preview + versionamento stub).
4. Mapa de cobertura simples.

## Consequências

**Positivas**
- Ambiente de desenvolvimento local sobe em < 5 minutos.
- Stack idêntica à recomendada na SPEC desde o primeiro commit.
- Demo visual do coração do produto (contrato → postos com grouping) já existe e pode ser mostrado a stakeholders.
- ADR deixa explícito o que é temporário vs permanente.

**Negativas / Riscos**
- Velocidade inicial menor que um protótipo Next.js puro (aceito pelo usuário).
- AG Grid Enterprise exigirá licença antes de usar Pivot/Server-side completo em produção.
- Keycloak em Docker é bom para dev, mas exige configuração cuidadosa de realms em produção multi-tenant.

**Mitigações**
- Todo código de frontend/backend terá comentários `// TODO SPEC:<seção>` mapeando de volta para a especificação.
- O seed do Postgres já usa nomes de colunas próximos do modelo real da seção 25.
- Futuro refactor para microserviços já previsto na SPEC ("modular monolith bem separado").

## Referências

- SPEC v1.0 seções 2, 3, 4, 14, 25, 26, 30, 31
- Respostas explícitas do usuário em 2026-04 (stack, nome, escopo do scaffold, AG Grid Enterprise)
- Documentação oficial: Spring Boot 3.4, Kotlin 2.0, AG Grid 33, MUI v6, Keycloak 26, PostgreSQL 16

---

**Assinatura**: Aceito como base para todo o desenvolvimento subsequente do ContractOps AI.
