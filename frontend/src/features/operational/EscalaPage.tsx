/**
 * EscalaPage — templates de turno e escalas por posto (SPEC §4.8)
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, FormControl, InputLabel, Select, MenuItem, Tabs, Tab,
} from '@mui/material'
import { RefreshCw } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useContracts } from '../../api/hooks/useContracts'
import { useShiftTemplates, usePostSchedules, useRosters } from '../../api/hooks/usePhase78'

export default function EscalaPage() {
  const { tenantName } = useTenant()
  const { data: contracts = [] } = useContracts()
  const [contractId, setContractId] = useState('')
  const [tab, setTab] = useState(0)

  const { data: templates = [], refetch: refetchT } = useShiftTemplates(contractId || undefined)
  const { data: schedules = [], refetch: refetchS } = usePostSchedules(contractId || undefined)
  const { data: rosters = [], refetch: refetchR } = useRosters(contractId || undefined)

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Escala & Turnos</Typography>
          <Typography color="text.secondary">Tenant: {tenantName} — 12x36, plantão, conflitos</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => { refetchT(); refetchS(); refetchR() }} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>Templates de turno, escalas por posto e roster de colaboradores.</Alert>

      <FormControl sx={{ minWidth: 320, mb: 2 }}>
        <InputLabel>Contrato</InputLabel>
        <Select value={contractId} label="Contrato" onChange={(e) => setContractId(e.target.value)}>
          <MenuItem value="">Selecione</MenuItem>
          {contracts.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.numero}</MenuItem>)}
        </Select>
      </FormControl>

      <Paper sx={{ p: 2, borderRadius: 3 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
          <Tab label="Templates" />
          <Tab label="Escalas por posto" />
          <Tab label="Roster colaboradores" />
        </Tabs>
        {tab === 0 && (
          <EnterpriseDataGrid title="Templates de turno" rowData={templates} columnDefs={[
            { headerName: 'Nome', field: 'name', flex: 1 },
            { headerName: 'Tipo', field: 'shiftType', width: 120 },
            { headerName: 'Entrada', field: 'entryTime', width: 100 },
            { headerName: 'Saída', field: 'exitTime', width: 100 },
          ]} height="360px" emptyMessage="Nenhum template." />
        )}
        {tab === 1 && (
          <EnterpriseDataGrid title="Escalas por posto" rowData={schedules} columnDefs={[
            { headerName: 'Posto', field: 'postId', flex: 1 },
            { headerName: 'Tipo', field: 'scheduleType', width: 120 },
            { headerName: 'Início', field: 'effectiveFrom', width: 120 },
            { headerName: 'Status', field: 'status', width: 100 },
          ]} height="360px" emptyMessage="Selecione contrato." />
        )}
        {tab === 2 && (
          <EnterpriseDataGrid title="Roster" rowData={rosters} columnDefs={[
            { headerName: 'Colaborador', field: 'employeeId', flex: 1 },
            { headerName: 'Posto', field: 'postId', flex: 1 },
            { headerName: 'Papel', field: 'role', width: 100 },
            { headerName: 'Desde', field: 'effectiveFrom', width: 120 },
          ]} height="360px" emptyMessage="Selecione contrato." />
        )}
      </Paper>
    </Box>
  )
}
