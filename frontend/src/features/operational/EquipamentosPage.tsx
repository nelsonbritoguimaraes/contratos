/**
 * EquipamentosPage — patrimônio e alocação (SPEC §13)
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, TextField, Grid,
} from '@mui/material'
import { Plus, RefreshCw } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useEquipamentos, useEquipamentoAllocations, useCreateEquipamento } from '../../api/hooks/usePhase78'

const itemCols = [
  { headerName: 'Nome', field: 'name', flex: 1 },
  { headerName: 'Serial', field: 'serialNumber', width: 130 },
  { headerName: 'Categoria', field: 'category', width: 140 },
  { headerName: 'Status', field: 'status', width: 110 },
  { headerName: 'Custo', field: 'acquisitionCost', valueFormatter: (p: any) => p.value ? `R$ ${Number(p.value).toLocaleString('pt-BR')}` : '-' },
]

export default function EquipamentosPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: items = [], isLoading, refetch } = useEquipamentos()
  const { data: allocations = [] } = useEquipamentoAllocations()
  const create = useCreateEquipamento()

  const [form, setForm] = useState({ name: '', serialNumber: '', category: 'EPI', acquisitionCost: '' })

  const handleCreate = async () => {
    if (!form.name) return showNotification('Informe o nome', 'warning')
    try {
      await create.mutateAsync({ ...form, acquisitionCost: form.acquisitionCost ? Number(form.acquisitionCost) : null })
      showNotification('Equipamento cadastrado', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro', 'error')
    }
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Equipamentos & Patrimônio</Typography>
          <Typography color="text.secondary">Tenant: {tenantName}</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>Patrimônio, alocação a posto/colaborador e manutenção preventiva.</Alert>

      <Paper sx={{ p: 3, mb: 2, borderRadius: 3 }}>
        <Typography variant="h6" gutterBottom>Novo equipamento</Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} md={4}><TextField fullWidth label="Nome" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth label="Serial" value={form.serialNumber} onChange={(e) => setForm({ ...form, serialNumber: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth label="Categoria" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} /></Grid>
          <Grid item xs={12} md={2}>
            <Button fullWidth variant="contained" startIcon={<Plus size={16} />} onClick={handleCreate} sx={{ height: 56 }} disabled={create.isPending}>Cadastrar</Button>
          </Grid>
        </Grid>
      </Paper>

      <EnterpriseDataGrid title="Catálogo de equipamentos" rowData={items} columnDefs={itemCols} loading={isLoading} height="320px" emptyMessage="Nenhum equipamento." />
      <Box mt={2}>
        <EnterpriseDataGrid title={`Alocações (${allocations.length})`} rowData={allocations} columnDefs={[
          { headerName: 'Equipamento', field: 'equipmentId', flex: 1 },
          { headerName: 'Posto', field: 'postId', flex: 1 },
          { headerName: 'Status', field: 'status', width: 100 },
        ]} height="240px" emptyMessage="Sem alocações." />
      </Box>
    </Box>
  )
}
