/**
 * MedicaoPage — Tela de Medição e Faturamento (Fase 1)
 * Permite calcular medições (GlosaEngine + Attendance), visualizar resultado e aprovar.
 * A aprovação no backend dispara NFS-e + Conta a Receber + lançamentos contábeis.
 */
import { useState } from 'react'
import { Box, Typography, Paper, Stack, Button, TextField, Alert, CircularProgress, MenuItem, Select, FormControl, InputLabel } from '@mui/material'
import { useTenant } from '../../api/hooks/useTenant'
import { useMeasurements, useCalculateMeasurement, useApproveMeasurement } from '../../api/hooks/useMeasurements'
import { useContracts } from '../../api/hooks/useContracts'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { ApprovalModal } from '../../components/common/ApprovalModal'
import { useNotification } from '../../components/NotificationProvider'
import { ColDef } from 'ag-grid-community'

const columns: ColDef[] = [
  { headerName: 'Período', field: 'period', minWidth: 120 },
  { headerName: 'Valor Base', field: 'baseValue', valueFormatter: p => `R$ ${Number(p.value).toLocaleString('pt-BR')}` },
  { headerName: 'Glosa', field: 'glosaTotal', valueFormatter: p => `R$ ${Number(p.value).toLocaleString('pt-BR')}` },
  { headerName: 'Valor Final', field: 'finalAmount', valueFormatter: p => `R$ ${Number(p.value).toLocaleString('pt-BR')}` },
  { headerName: 'Status', field: 'status' },
]

export default function MedicaoPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: contracts } = useContracts()
  const [selectedContractId, setSelectedContractId] = useState('')
  const [period, setPeriod] = useState('2025-06-01')
  const [approvalOpen, setApprovalOpen] = useState(false)
  const [selectedMeasurement, setSelectedMeasurement] = useState<any>(null)

  const { data: measurements, isLoading, refetch } = useMeasurements(selectedContractId || undefined)
  const calculateMutation = useCalculateMeasurement()
  const approveMutation = useApproveMeasurement()

  const handleCalculate = async () => {
    if (!selectedContractId) {
      alert('Selecione um contrato')
      return
    }
    await calculateMutation.mutateAsync({ contractId: selectedContractId, period })
    refetch()
  }

  const handleOpenApproval = (measurement: any) => {
    setSelectedMeasurement(measurement)
    setApprovalOpen(true)
  }

  const handleApproveMeasurement = async (nivel: number, justificativa: string) => {
    if (!selectedMeasurement?.id) return
    try {
      await approveMutation.mutateAsync({
        measurementId: selectedMeasurement.id,
        nivel,
        justificativa,
      })
      showNotification(`Medição aprovada (nível ${nivel}). Conta a Receber e NFS-e geradas.`, 'success')
    } catch (e: any) {
      showNotification(e.message || 'Erro ao aprovar medição', 'error')
    }
    setApprovalOpen(false)
    refetch()
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>
        Medição & Faturamento
      </Typography>
      <Typography color="text.secondary" mb={3}>
        Tenant: {tenantName} — Medições disparam NFS-e, Contas a Receber e Contabilidade automaticamente.
      </Typography>

      <Paper sx={{ p: { xs: 2, sm: 2.5 }, mb: 3, borderRadius: 3 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-end">
          <FormControl sx={{ minWidth: 320 }}>
            <InputLabel>Contrato</InputLabel>
            <Select
              value={selectedContractId}
              label="Contrato"
              onChange={(e) => setSelectedContractId(e.target.value)}
            >
              {(contracts || []).map((c: any) => (
                <MenuItem key={c.id} value={c.id}>
                  {c.numero} — {c.orgao}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            label="Competência (YYYY-MM-DD)"
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
          />
          <Button
            variant="contained"
            onClick={handleCalculate}
            disabled={calculateMutation.isPending || !selectedContractId}
          >
            {calculateMutation.isPending ? <CircularProgress size={20} /> : 'Calcular Medição'}
          </Button>
        </Stack>

        {calculateMutation.data && (
          <Alert severity="success" sx={{ mt: 2 }}>
            Medição calculada: Valor Final R$ {calculateMutation.data.finalAmount} | Glosas: R$ {calculateMutation.data.glosaTotal}
          </Alert>
        )}
      </Paper>

      <EnterpriseDataGrid
        title="Medições do Contrato"
        rowData={measurements || []}
        columnDefs={columns}
        onRefresh={() => refetch()}
        loading={isLoading}
      />

      <Stack direction="row" spacing={2} mt={2}>
        <Button
          variant="contained"
          color="success"
          disabled={!selectedContractId || (measurements?.length ?? 0) === 0}
          onClick={() => {
            const latest = measurements?.[0]
            if (latest) handleOpenApproval(latest)
          }}
        >
          Aprovar Última Medição
        </Button>
        <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center' }}>
          Aprovação dispara automaticamente: Conta a Receber → NFS-e → Lançamentos Contábeis.
        </Typography>
      </Stack>

      <ApprovalModal
        open={approvalOpen}
        onClose={() => setApprovalOpen(false)}
        onApprove={handleApproveMeasurement}
        title="Aprovar Medição"
        description={`Medição de ${selectedMeasurement?.period} - Valor Final: R$ ${selectedMeasurement?.finalAmount?.toLocaleString('pt-BR') ?? ''}`}
        valor={selectedMeasurement?.finalAmount}
        loading={false}
      />
    </Box>
  )
}
