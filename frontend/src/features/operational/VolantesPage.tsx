/**
 * VolantesPage — workflow completo detect → assign → confirm (SPEC §11)
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, FormControl, InputLabel, Select, MenuItem, TextField, Chip,
} from '@mui/material'
import { RefreshCw, Search, CheckCircle } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useContracts } from '../../api/hooks/useContracts'
import {
  useVolanteAssignments, useDetectVolantes, useConfirmVolante, useVolanteCoverage,
} from '../../api/hooks/usePhase78'

export default function VolantesPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()
  const [contractId, setContractId] = useState('')
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))

  const { data: assignments = [], isLoading, refetch } = useVolanteAssignments(contractId || undefined, date)
  const { data: coverage } = useVolanteCoverage(contractId || undefined, date)
  const detect = useDetectVolantes()
  const confirm = useConfirmVolante()

  const handleDetect = async () => {
    if (!contractId) return showNotification('Selecione um contrato', 'warning')
    try {
      const found = await detect.mutateAsync({ contractId, date })
      showNotification(`${(found as any[])?.length ?? 0} falta(s) detectada(s)`, 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro na detecção', 'error')
    }
  }

  const handleConfirm = async (id: string) => {
    try {
      await confirm.mutateAsync(id)
      showNotification('Acionamento confirmado', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro', 'error')
    }
  }

  const coveragePct = coverage?.coverage_percent ?? coverage?.percent

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Volantes & Substituições</Typography>
          <Typography color="text.secondary">Tenant: {tenantName}</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Workflow: detectar falta → atribuir volante → supervisor confirma → evidência reduz glosa.
        {coveragePct != null && ` Cobertura hoje: ${Number(coveragePct).toFixed(1)}%.`}
      </Alert>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2} alignItems="center">
        <FormControl sx={{ minWidth: 280 }}>
          <InputLabel>Contrato</InputLabel>
          <Select value={contractId} label="Contrato" onChange={(e) => setContractId(e.target.value)}>
            <MenuItem value="">Selecione</MenuItem>
            {contracts.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.numero}</MenuItem>)}
          </Select>
        </FormControl>
        <TextField type="date" label="Data" value={date} onChange={(e) => setDate(e.target.value)} InputLabelProps={{ shrink: true }} />
        <Button variant="contained" startIcon={<Search size={16} />} onClick={handleDetect} disabled={!contractId || detect.isPending}>
          Detectar faltas
        </Button>
      </Stack>

      <Paper sx={{ borderRadius: 3 }}>
        <EnterpriseDataGrid
          title={`Acionamentos (${assignments.length})`}
          rowData={assignments}
          columnDefs={[
            { headerName: 'Posto', field: 'postId', flex: 1 },
            { headerName: 'Ausente', field: 'absentEmployeeId', flex: 1 },
            { headerName: 'Volante', field: 'volanteEmployeeId', flex: 1 },
            { headerName: 'Status', field: 'workflowStatus', width: 140,
              cellRenderer: (p: any) => <Chip size="small" label={p.value} color={p.value === 'CONCLUIDO' ? 'success' : 'default'} /> },
            {
              headerName: 'Ações', width: 120,
              cellRenderer: (p: any) =>
                p.data?.workflowStatus === 'VOLANTE_ATRIBUIDO' ? (
                  <Button size="small" startIcon={<CheckCircle size={14} />} onClick={() => handleConfirm(p.data.id)}>
                    Confirmar
                  </Button>
                ) : null,
            },
          ]}
          loading={isLoading}
          height="420px"
          emptyMessage="Clique em Detectar faltas para iniciar o workflow."
        />
      </Paper>
    </Box>
  )
}
