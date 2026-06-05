/**
 * Fechamento Financeiro Mensal — lock real de período
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, TextField, Button, Alert, Chip,
} from '@mui/material'
import { Lock, RefreshCw } from 'lucide-react'
import { ColDef } from 'ag-grid-community'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import {
  useFechamentosFinanceiros,
  useFecharMesFinanceiro,
  useReabrirMesFinanceiro,
} from '../../api/hooks/useFechamentoFinanceiro'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'

export default function FechamentoFinanceiroPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: fechamentos = [], isLoading, refetch } = useFechamentosFinanceiros()
  const fechar = useFecharMesFinanceiro()
  const reabrir = useReabrirMesFinanceiro()

  const now = new Date()
  const [inicio, setInicio] = useState(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`)
  const [fim, setFim] = useState(now.toISOString().slice(0, 10))

  const columns: ColDef[] = [
    { headerName: 'Início', field: 'dataInicio', width: 110 },
    { headerName: 'Fim', field: 'dataFim', width: 110 },
    {
      headerName: 'Status',
      field: 'status',
      width: 110,
      cellRenderer: (p: any) => (
        <Chip
          label={p.value}
          size="small"
          color={p.value === 'FECHADO' ? 'error' : p.value === 'REABERTO' ? 'warning' : 'default'}
        />
      ),
    },
    {
      headerName: 'Recebimentos',
      field: 'totalRecebimentos',
      valueFormatter: (p) => p.value != null ? `R$ ${Number(p.value).toLocaleString('pt-BR')}` : '—',
    },
    {
      headerName: 'Pagamentos',
      field: 'totalPagamentos',
      valueFormatter: (p) => p.value != null ? `R$ ${Number(p.value).toLocaleString('pt-BR')}` : '—',
    },
    { headerName: 'Fechado em', field: 'dataFechamento', flex: 1 },
    {
      headerName: 'Ações',
      field: 'id',
      width: 120,
      cellRenderer: (p: any) =>
        p.data?.status === 'FECHADO' ? (
          <Button size="small" onClick={() => handleReabrir(p.data)}>Reabrir</Button>
        ) : null,
    },
  ]

  const handleFechar = async () => {
    try {
      await fechar.mutateAsync({ inicio, fim })
      showNotification(`Período ${inicio} a ${fim} fechado. Movimentações bloqueadas.`, 'success')
      refetch()
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao fechar período', 'error')
    }
  }

  const handleReabrir = async (row: any) => {
    try {
      await reabrir.mutateAsync({ inicio: row.dataInicio, fim: row.dataFim })
      showNotification(`Período reaberto.`, 'success')
      refetch()
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao reabrir', 'error')
    }
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Fechamento Financeiro</Typography>
          <Typography color="text.secondary">Tenant: {tenantName} — trava AR/AP/NFS-e no período fechado</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="warning" sx={{ mb: 2 }}>
        Ao fechar um mês, baixas de AR/AP, emissão de NFS-e e cobranças naquele intervalo são bloqueadas até reabertura.
      </Alert>

      <Paper sx={{ p: 2.5, mb: 2, borderRadius: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="flex-end">
          <TextField label="Início" type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} InputLabelProps={{ shrink: true }} />
          <TextField label="Fim" type="date" value={fim} onChange={(e) => setFim(e.target.value)} InputLabelProps={{ shrink: true }} />
          <Button variant="contained" color="error" startIcon={<Lock size={16} />} onClick={handleFechar} disabled={fechar.isPending}>
            Fechar período
          </Button>
        </Stack>
      </Paper>

      <EnterpriseDataGrid
        title="Histórico de fechamentos"
        rowData={fechamentos}
        columnDefs={columns}
        loading={isLoading}
        emptyMessage="Nenhum fechamento registrado."
      />
    </Box>
  )
}
