/**
 * UniformesPage — catálogo e alocações (SPEC §14)
 */
import { Box, Typography, Paper, Stack, Button, Alert } from '@mui/material'
import { RefreshCw } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useUniformItems } from '../../api/hooks/usePhase78'

const columns = [
  { headerName: 'Nome', field: 'name', flex: 1 },
  { headerName: 'Tipo', field: 'type', width: 120 },
  { headerName: 'Tamanho', field: 'size', width: 90 },
  { headerName: 'Custo', field: 'cost', valueFormatter: (p: any) => p.value ? `R$ ${Number(p.value).toLocaleString('pt-BR')}` : '-' },
  { headerName: 'Ativo', field: 'active', width: 80 },
]

export default function UniformesPage() {
  const { tenantName } = useTenant()
  const { data: items = [], isLoading, refetch } = useUniformItems()

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Uniformes & EPIs</Typography>
          <Typography color="text.secondary">Tenant: {tenantName}</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>
      <Alert severity="info" sx={{ mb: 2 }}>Catálogo de uniformes, alocação por colaborador/posto e controle de reposição.</Alert>
      <Paper sx={{ borderRadius: 3 }}>
        <EnterpriseDataGrid title="Itens de uniforme" rowData={items} columnDefs={columns} loading={isLoading} height="480px" emptyMessage="Cadastre itens via API /uniformes/items." />
      </Paper>
    </Box>
  )
}
