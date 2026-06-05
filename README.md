# ContractOps AI

**ERP SaaS de Gestão de Contratos Públicos com Mão de Obra Exclusiva, Ponto, Glosas, Folha, Contabilidade e IA**

Especificação: SPEC v1.0 (completa em `/docs/SPEC-v1.0.md` se adicionada posteriormente)

Status atual: **Scaffold v0.0.1** — Estrutura inicial + ambiente de desenvolvimento local funcional seguindo a stack recomendada na SPEC.

---

## Visão (resumo da SPEC v1.0)

Plataforma multiempresa/multifilial para empresas que executam contratos públicos de dedicação exclusiva de mão de obra (licitações → proposta vencedora → contrato → postos → colaboradores → ponto → cobertura → glosas/IMR → medição → NFS-e → folha → encargos → contabilidade).

Diferencial: automação por agentes de IA + rastreabilidade total + conformidade com Portaria 671/2021, eSocial, FGTS Digital, DCTFWeb, NFS-e Nacional, Lei 14.133/2021 e IN 05/2017.

Nome do produto (conforme decisão): **ContractOps AI**

---

## Stack (decisão confirmada — adesão estrita à SPEC)

| Camada              | Tecnologia                                      | Status no scaffold |
|---------------------|--------------------------------------------------|--------------------|
| Backend principal   | Kotlin 2.x + Spring Boot 3.4+ + Java 21/25 LTS   | Estrutura vazia pronta |
| Banco principal     | PostgreSQL 16+                                   | Docker + seed demo |
| Cache / Filas       | Redis                                            | Docker |
| Auth / SSO          | Keycloak (futuro: RBAC + ABAC)                   | Docker (stub) |
| Frontend            | React 18 + TypeScript + Vite + MUI (MD3) + AG Grid | Demo funcional |
| Data Grid           | AG Grid Enterprise (Quartz) — licença necessária | Pacotes instalados + nota |
| Containers          | Docker + Docker Compose                          | Funcional |
| Multi-tenant        | tenant_id em todas as tabelas + RLS (futuro)     | Preparado no seed |

**Compromisso**: Backend em Kotlin/Spring Boot desde o dia 1 (conforme sua escolha). Não usamos atalhos de stack leve para o núcleo.

---

## Como rodar o ambiente local (Windows / PowerShell)

### Pré-requisitos
- Docker Desktop (Windows) com WSL2 backend
- Node.js 20+ (LTS recomendado)
- Java 21+ JDK (você confirmou que irá configurar)
- Git

### Passo a passo

```powershell
# 1. Clonar / entrar na pasta do projeto (você já está aqui)
cd "C:\Users\nelso\OneDrive\Área de Trabalho\Contratos"

# 2. Subir a infraestrutura (Postgres + Redis + Keycloak)
docker compose up -d

# 3. (Opcional) Verificar os containers
docker compose ps

# 4. Instalar dependências do frontend
cd frontend
npm install

# 5. Rodar o frontend (demo com grid)
npm run dev
```

Abra **http://localhost:5173**

Você verá:
- App bar "ContractOps AI"
- KPIs resumo
- Grid de Contratos com **AG Grid** (agrupamento por Órgão + Status)
- Dados seed: 3 empresas, 5 contratos, 20 postos (realistas, baseados em contratos públicos brasileiros de segurança/limpeza)

### Parar o ambiente

```powershell
# No diretório raiz
docker compose down
```

---

## O que você vê no demo (scaffold)

O grid de contratos demonstra:
- Row Grouping por `orgao` (ex.: "Prefeitura Municipal de Curitiba", "Estado do Paraná")
- Colunas alinhadas com SPEC (número, vigência, valor mensal, cobertura, status)
- Filtros, ordenação e exportação CSV (recursos do AG Grid)
- Tema próximo de Material Design 3 (via MUI customizado)
- Dados que representam o modelo conceitual: Tenant → Empresa/Filial → Contrato → Postos

**Status**: Backend com APIs REST completas (veja `backend/README.md`). Frontend ainda estático (conexão real será feita por último, conforme solicitado).

---

## Estrutura de pastas (scaffold)

```
.
├── README.md
├── docker-compose.yml
├── .env.example
├── .gitignore
├── docs/
│   └── adr/
│       └── 0001-initial-architecture-and-scope.md
├── infra/
│   └── db/
│       └── init/
│           ├── 01-schema-demo.sql
│           └── 02-seed-demo.sql
├── backend/                  # Kotlin + Spring Boot 3.4 (vazio por enquanto)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── src/
│   │   └── main/kotlin/com/contractops/api/
│   │       └── ContractOpsApplication.kt
│   └── ...
└── frontend/                 # Vite + React + TS + MUI + AG Grid
    ├── package.json
    ├── vite.config.ts
    ├── src/
    │   ├── App.tsx
    │   ├── main.tsx
    │   ├── theme/
    │   │   └── contractops-theme.ts   # Material 3 tokens
    │   ├── features/contracts/
    │   │   └── ContractsGrid.tsx
    │   └── data/
    │       └── seed-contracts.ts
    └── ...
```

---

## Decisões de arquitetura (resumo — ver ADR completo)

- **Monorepo** na fase inicial (facilita shared types, docs e CI). Pode evoluir para repos separados quando os módulos de folha/contabilidade virarem serviços.
- **Modular monolith** no backend (pacotes por domínio: contracts, posts, payroll, glosas, accounting, ia, etc.). Só quebrar em microserviços quando o volume justificar (Temporal + Kafka já previstos).
- **AG Grid Enterprise**: Adotado conforme SPEC. Você fornecerá a license key quando for para produção / testes avançados (pivot, server-side row model completo, etc.). Atualmente o scaffold usa a versão community para ser imediatamente executável.
- **Multi-tenant**: Toda tabela terá `tenant_id`. Row Level Security será ativado no Postgres quando o backend estiver maduro.
- **Keycloak** como provedor de identidade desde o início (suporte futuro a SSO por cliente enterprise + portal do órgão/fiscal).
- **Sem features reais no backend ainda**: Apenas estrutura + Dockerfile + config básica. O foco do scaffold é ter o dev environment rodando + o grid demonstrando o coração do produto (contrato → postos).

---

## Próximos passos recomendados (alinhado com SPEC e seu feedback)

**Backend + APIs**: Concluído o núcleo da Fase 1 (Contracts + Biddings + Postos + Lotes + Winning Spreadsheets + Flyway + seed).

**Ordem solicitada**:
1. **Backend e APIs primeiro** (atual) — pronto para uso e testes.
2. **Frontend por último** — conexão com as APIs reais (substituir seed estático).

Próximos itens de backend (após validação):
- Spring Security + Keycloak Resource Server + tenant a partir do JWT
- Importação de planilha vencedora (Excel)
- Validações de negócio + testes

Depois:
- Clock Bridge Agent + conectores de ponto
- Motor de glosas/IMR
- Agentes de IA (RAG sobre editais/contratos)

Tudo documentado na SPEC v1.0 seções 14, 25, 26, 30 (roadmap).

---

## Licenças e custos importantes

- **AG Grid Enterprise**: Você confirmou que possui ou obterá a licença. O código já referencia os pacotes enterprise.
- **Keycloak**: Open source (ótimo para SaaS multi-tenant).
- **Spring Boot / Kotlin**: Sem custo.
- Futuro: OpenSearch, ClickHouse, Temporal, S3-compatible — todos têm tiers gratuitos ou self-hosted.

---

## Contribuição / Desenvolvimento

Este repositório segue a SPEC v1.0 de forma rigorosa.  
Toda nova feature deve mapear para uma seção da especificação.

Antes de implementar código real (além do scaffold), abra uma issue ou discussão descrevendo:
- Qual fatia da SPEC (ex.: "Seção 6.1 + 7.1 — Cadastro de Contrato e Posto")
- Impacto em multi-tenant e auditoria
- Se envolve IA, ponto, folha ou contabilidade (alta complexidade)

---

## Contato / Responsável

Implementação iniciada via Grok (xAI) seguindo exatamente as respostas do usuário sobre escopo e stack.

**Data do scaffold**: Abril 2026

---

**Próximo comando sugerido após `npm run dev`**:

Abra o navegador em http://localhost:5173 e explore o grid de contratos.  
Em seguida, vamos evoluir o backend Kotlin para expor os mesmos dados via API real.

Bem-vindo ao ContractOps AI.
