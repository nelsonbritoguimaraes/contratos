/**
 * Auditoria Financeira — trilha de ações AR/AP/NFS-e/Open Finance
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, TextField, Chip,
} from '@mui/material'
import { RefreshCw } from 'lucide-react'
import { ColDef } from 'ag-grid-community'
import { useTenant } from '../../api/hooks/useTenant'
import { useAuditoriaFinanceira } from '../../api/hooks/useAuditoriaFinanceira'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'

const ACAO_COLORS: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
  EMITIR: 'success',
  CANCELAR: 'error',
  PROVISAO_GLOSA: 'warning',
  INICIAR_CONSENT: 'info',
  AUTORIZAR: 'success',
  BAIXAR: 'default',
}

export default function AuditoriaFinanceiraPage() {
  const { tenantName } = useTenant()
  const [limit, setLimit] = useState(100)
  const { data: logs = [], isLoading, refetch, isError, error } = useAuditoriaFinanceira(limit)

  const columns: ColDef[] = [
    {
      headerName: 'Data/Hora',
      field: 'createdAt',
      width: 170,
      valueFormatter: (p) =>
        p.value ? new Date(p.value).toLocaleString('pt-BR') : '—',
    },
    { headerName: 'Entidade', field: 'entidadeTipo', width: 130 },
    { headerName: 'ID Entidade', field: 'entidadeId', width: 120, valueFormatter: (p) => p.value?.slice?.(0, 8) ?? '—' },
    {
      headerName: 'Ação',
      field: 'acao',
      width: 140,
      cellRenderer: (p: any) => (
        <Chip
          label={p.value}
          size="small"
          color={ACAO_COLORS[p.value] || 'default'}
          variant="outlined"
        />
      ),
    },
    { headerName: 'Usuário', field: 'usuario', width: 120 },
    { headerName: 'Detalhe', field: 'detalhe', flex: 1 },
  ]

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Auditoria Financeira</Typography>
          <Typography color="text.secondary">
            Tenant: {tenantName} — trilha de auditoria de operações financeiras
          </Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">
          Atualizar
        </Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Registra emissões/cancelamentos de NFS-e, provisões de glosa, consentimentos Open Finance e demais ações críticas do módulo financeiro.
      </Alert>

      <Paper sx={{ p: 2, mb: 2, borderRadius: 3 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <TextField
            label="Limite de registros"
            type="number"
            size="small"
            value={limit}
            onChange={(e) => setLimit(Math.max(10, Math.min(500, Number(e.target.value) || 100)))}
            sx={{ width: 160 }}
          />
          <Typography variant="body2" color="text.secondary">
            {logs.length} registro(s) carregado(s)
          </Typography>
        </Stack>
      </Paper>

      <EnterpriseDataGrid
        title="Log de auditoria financeira"
        rowData={logs}
        columnDefs={columns}
        loading={isLoading}
        emptyMessage="Nenhum registro de auditoria. Emissões de NFS-e, baixas e consentimentos Open Finance geram entradas automaticamente."
        error={isError ? (error as Error)?.message : null}
        height={520}
        onRefresh={refetch}
      />
    </Box>
  )
}
