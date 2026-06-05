# ContractOps AI — Frontend

React 18 + TypeScript + Vite + MUI (MD3) + AG Grid Community/Enterprise + TanStack Query.

## Variáveis de ambiente (`.env`)

Copie `.env.example`:

| Variável | Uso |
|----------|-----|
| `VITE_AG_GRID_LICENSE_KEY` | Licença AG Grid Enterprise (Pivot / SideBar) |
| `VITE_DEMO_FALLBACK` | `false` (default) — não simula dados quando a API falha |

## Como rodar

```powershell
# Backend em http://localhost:8080
cd frontend
npm install
npm run dev
```

UI: **http://localhost:5173** (proxy `/api` → backend)

## Rotas principais

| Rota | Página |
|------|--------|
| `/contracts`, `/biddings` | Operacional |
| `/measurements`, `/glosas`, `/ponto`, `/postos` | Operações + ponto |
| `/financeiro/*` | CFO |
| `/rh/folha`, `/rh/employees`, `/rh/alocacao-por-licitacao` | RH |
| `/contabilidade/*` | Contabilidade (tabs ↔ URL) |
| `/ia` | Console IA |

Atalho global: **Ctrl+K** (Command Palette)

## Hooks API (`src/api/hooks/`)

- `useContracts`, `useMeasurements`, `useApproveMeasurement`
- `useGlosas`, `useRubrics`
- `usePayslip`, `useContabilidade`, `usePonto`, `useFiscalStatus`, `useEsocial`

## Build

```powershell
npm run build
```
