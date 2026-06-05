/**
 * AuditoriaGlobalPage — log imutável cross-módulo (SPEC §24)
 */
import { useState } from 'react'
import { Box, Typography, Paper, Stack, Button, FormControl, InputLabel, Select, MenuItem } from '@mui/material'
import { RefreshCw } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useGlobalAudit } from '../../api/hooks/usePhase78'

const ENTITY_TYPES = ['', 'IMPLANTATION', 'GLOSA', 'NFS_E', 'PAYSLIP', 'JOURNAL', 'CONTRACT']

export default function AuditoriaGlobalPage() {
  const { tenantName } = useTenant()
  const [entityType, setEntityType] = useState('')
  const { data: events = [], isLoading, refetch } = useGlobalAudit(entityType || undefined)

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Auditoria Global</Typography>
          <Typography color="text.secondary">Tenant: {tenantName} — trilha imutável cross-módulo</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>

      <Paper sx={{ p: 2, mb: 2, borderRadius: 3 }}>
        <FormControl size="small" sx={{ minWidth: 240 }}>
          <InputLabel>Filtrar entidade</InputLabel>
          <Select value={entityType} label="Filtrar entidade" onChange={(e) => setEntityType(e.target.value)}>
            <MenuItem value="">Todas</MenuItem>
            {ENTITY_TYPES.filter(Boolean).map((t) => (
              <MenuItem key={t} value={t}>{t}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Paper>

      <EnterpriseDataGrid
        title={`Eventos (${events.length})`}
        rowData={events}
        columnDefs={[
          { headerName: 'Quando', field: 'occurredAt', width: 180 },
          { headerName: 'Entidade', field: 'entityType', width: 120 },
          { headerName: 'Ação', field: 'action', width: 120 },
          { headerName: 'Ator', field: 'actor', width: 120 },
          { headerName: 'Detalhes', field: 'details', flex: 1 },
        ]}
        loading={isLoading}
        height="480px"
        emptyMessage="Nenhum evento registrado."
      />
    </Box>
  )
}
