/**
 * IA Call History — Histórico real de chamadas aos 10 agentes via AgentRouter (/api/ia/calls)
 * Fortalecido (Onda 3): uso completo do hook useIaCalls com error state, navegação com state para integração com Console,
 * tipagem IaCallLog e replay que pré-preenche o input real no AgentRouter.
 */
import { Box, Typography, Button } from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useIaCalls } from '../../api/hooks/useIAsk'
import { useNotification } from '../../components/NotificationProvider'
import { useNavigate } from 'react-router-dom'
import type { IaCallLog } from '../../api/types'

export default function IaHistoryPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const navigate = useNavigate()
  // Strengthened real hook usage: full React Query state (error, isError) + typed data
  const { data: calls = [], isLoading, error: callsError, refetch, isError } = useIaCalls(50)

  const handleReplay = (call: IaCallLog) => {
    const question = call.promptPreview || (call as any).question || ''
    const preview = question.length > 90 ? question.slice(0, 87) + '...' : question

    // Real integration: pass state so IaConsolePage prefills input + can seed from prior real /ask result
    showNotification(`Abrindo Console de IA com: ${preview}`, 'info')
    navigate('/ia', {
      state: {
        question,
        // If history had full response we could pass more; for now question drives the real flow
      }
    })
  }

  const columnDefs = [
    {
      headerName: 'Data',
      field: 'timestamp',
      minWidth: 170,
      valueFormatter: (p: { value?: string }) => p.value ? new Date(p.value).toLocaleString('pt-BR') : ''
    },
    {
      headerName: 'Agente(s)',
      field: 'routedAgents',
      minWidth: 180,
      valueFormatter: (p: { value?: string[] | string }) => Array.isArray(p.value) ? p.value.join(', ') : (p.value || '—')
    },
    { headerName: 'Pergunta', field: 'promptPreview', flex: 1 },
    { headerName: 'Resposta (preview)', field: 'responsePreview', flex: 1, minWidth: 200 },
    { headerName: 'Provider', field: 'provider', width: 110 },
    {
      headerName: 'Custo Est.',
      field: 'costEstimate',
      width: 110,
      valueFormatter: (p: { value?: number }) => p.value != null ? `R$ ${Number(p.value).toFixed(4)}` : '—'
    },
    {
      headerName: 'Ações',
      minWidth: 100,
      cellRenderer: (params: { data: IaCallLog }) => (
        <Button size="small" variant="outlined" onClick={() => handleReplay(params.data)}>
          Replay
        </Button>
      ),
    },
  ]

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>Histórico de Chamadas IA</Typography>
      <Typography color="text.secondary" mb={2}>
        Tenant: {tenantName} • Logs reais persistidos pelo AiOrchestrator (AgentRouter /api/ia/ask + /calls)
      </Typography>

      <EnterpriseDataGrid
        title="Chamadas IA (reais do AgentRouter + backend)"
        rowData={calls}
        columnDefs={columnDefs}
        onRefresh={() => refetch()}
        loading={isLoading}
        error={isError ? (callsError ? String((callsError as any).message || callsError) : 'Erro ao carregar histórico de IA') : null}
        emptyMessage="Nenhuma chamada IA registrada ainda. Use o Console de IA ou Ctrl+K para perguntas — os logs reais do AgentRouter (incluindo routed_agents e custo estimado) aparecerão aqui automaticamente via /api/ia/calls."
      />
    </Box>
  )
}
