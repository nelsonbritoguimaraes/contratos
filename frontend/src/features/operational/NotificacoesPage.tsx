/**
 * NotificacoesPage — notificações contratuais do órgão (SPEC §15.1)
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, FormControl, InputLabel, Select, MenuItem, TextField,
} from '@mui/material'
import { Plus, RefreshCw } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useContracts } from '../../api/hooks/useContracts'
import { useNotificacoes, useCreateNotificacao } from '../../api/hooks/usePhase78'

const columns = [
  { headerName: 'Nº', field: 'notificationNumber', width: 100 },
  { headerName: 'Órgão', field: 'orgao', flex: 1 },
  { headerName: 'Assunto', field: 'subject', flex: 1.5 },
  { headerName: 'Recebida', field: 'receivedAt', width: 120 },
  { headerName: 'Prazo', field: 'responseDeadline', width: 120 },
  { headerName: 'Status', field: 'status', width: 110 },
]

export default function NotificacoesPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()
  const [contractFilter, setContractFilter] = useState('')
  const { data: notificacoes = [], isLoading, refetch } = useNotificacoes(contractFilter || undefined)
  const create = useCreateNotificacao()

  const [form, setForm] = useState({
    contractId: '', subject: '', orgao: '', receivedAt: new Date().toISOString().slice(0, 10),
    responseDeadline: '', description: '',
  })

  const handleCreate = async () => {
    if (!form.contractId || !form.subject) return showNotification('Preencha contrato e assunto', 'warning')
    try {
      await create.mutateAsync(form)
      showNotification('Notificação registrada', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro', 'error')
    }
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Notificações Contratuais</Typography>
          <Typography color="text.secondary">Tenant: {tenantName}</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="warning" sx={{ mb: 2 }}>
        Alertas de vencimento de prazo de resposta. Notificações não respondidas podem gerar glosa administrativa.
      </Alert>

      <Paper sx={{ p: 2, mb: 2, borderRadius: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="flex-end">
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Filtrar contrato</InputLabel>
            <Select value={contractFilter} label="Filtrar contrato" onChange={(e) => setContractFilter(e.target.value)}>
              <MenuItem value="">Todos</MenuItem>
              {contracts.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.numero}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Novo — Contrato</InputLabel>
            <Select value={form.contractId} label="Novo — Contrato" onChange={(e) => setForm({ ...form, contractId: e.target.value })}>
              {contracts.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.numero}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField size="small" label="Assunto" value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} sx={{ flex: 1 }} />
          <Button variant="contained" startIcon={<Plus size={16} />} onClick={handleCreate} disabled={create.isPending}>Registrar</Button>
        </Stack>
      </Paper>

      <EnterpriseDataGrid
        title="Notificações recebidas"
        rowData={notificacoes}
        columnDefs={columns}
        loading={isLoading}
        height="420px"
        emptyMessage="Nenhuma notificação registrada."
      />
    </Box>
  )
}
