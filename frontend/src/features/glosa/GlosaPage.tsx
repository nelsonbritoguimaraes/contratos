/**
 * Glosa Management — integrado com GlosaEngine + AttendanceDay + provisão financeira
 */
import { useMemo, useState } from 'react'
import { Box, Typography, Alert, Button, Stack, FormControl, InputLabel, Select, MenuItem, Tabs, Tab } from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useGlosas, useUpdateGlosa } from '../../api/hooks/useGlosas'
import { useContracts } from '../../api/hooks/useContracts'
import { useMeasurements } from '../../api/hooks/useMeasurements'
import { useProvisionarGlosa } from '../../api/hooks/useProvisionarGlosa'
import GlosaRulesTab from './GlosaRulesTab'

const columns = [
  { headerName: 'Período', field: 'measurementPeriod', minWidth: 110 },
  { headerName: 'Tipo', field: 'glosaType', minWidth: 140 },
  { headerName: 'Descrição', field: 'description', flex: 1 },
  { headerName: 'Valor', field: 'glosaAmount', valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
  { headerName: 'Status', field: 'status', minWidth: 120 },
]

export default function GlosaPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()
  const [contractId, setContractId] = useState('')
  const [period, setPeriod] = useState('2025-06-01')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [pageTab, setPageTab] = useState(0)

  const { data: glosas = [], isLoading, refetch } = useGlosas(contractId || undefined, period)
  const { data: measurements = [] } = useMeasurements(contractId || undefined)
  const updateGlosa = useUpdateGlosa()
  const provisionarGlosa = useProvisionarGlosa()

  const selectedGlosa = useMemo(
    () => glosas.find((g) => g.id === selectedId),
    [glosas, selectedId]
  )

  const measurementForPeriod = useMemo(
    () => measurements.find((m) => m.period?.startsWith(period.slice(0, 7))),
    [measurements, period]
  )

  const handleContest = async () => {
    if (!selectedId) {
      showNotification('Selecione uma glosa na grade', 'warning')
      return
    }
    try {
      await updateGlosa.mutateAsync({
        id: selectedId,
        status: 'CONTESTADA',
        description: 'Contestação registrada pelo gestor',
      })
      showNotification('Glosa contestada', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro ao contestar', 'error')
    }
  }

  const handleRecovery = async () => {
    if (!selectedId) {
      showNotification('Selecione uma glosa na grade', 'warning')
      return
    }
    try {
      await updateGlosa.mutateAsync({
        id: selectedId,
        status: 'RECUPERADA',
        description: 'Recuperação de valor registrada',
      })
      showNotification('Recuperação registrada', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro ao registrar recuperação', 'error')
    }
  }

  const handleProvisionar = async () => {
    if (!selectedGlosa) {
      showNotification('Selecione uma glosa na grade', 'warning')
      return
    }
    const measurementId = measurementForPeriod?.id
    if (!measurementId) {
      showNotification('Nenhuma medição encontrada para a competência. Calcule/aprove uma medição primeiro.', 'warning')
      return
    }
    try {
      await provisionarGlosa.mutateAsync({
        measurementId,
        valorGlosa: Number(selectedGlosa.glosaAmount || 0),
      })
      showNotification(
        `Provisão de glosa R$ ${Number(selectedGlosa.glosaAmount || 0).toLocaleString('pt-BR')} registrada na Conta a Receber.`,
        'success'
      )
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao provisionar glosa no financeiro', 'error')
    }
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>Gestão de Glosas</Typography>
      <Typography color="text.secondary" mb={2}>Tenant: {tenantName}</Typography>

      <Tabs value={pageTab} onChange={(_, v) => setPageTab(v)} sx={{ mb: 2 }}>
        <Tab label="Glosas apuradas" />
        <Tab label="Regras configuráveis" />
      </Tabs>

      {pageTab === 1 ? (
        <GlosaRulesTab />
      ) : (
      <>
      <Alert severity="info" sx={{ mb: 3 }}>
        Glosas calculadas com base em apuração de ponto (AttendanceDay). Selecione contrato e competência.
        Use &quot;Provisionar no Financeiro&quot; para refletir a glosa na Conta a Receber da medição (POST /financeiro/provisao-glosa).
      </Alert>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2}>
        <FormControl sx={{ minWidth: 280 }}>
          <InputLabel>Contrato</InputLabel>
          <Select value={contractId} label="Contrato" onChange={(e) => setContractId(e.target.value)}>
            <MenuItem value="">Selecione</MenuItem>
            {contracts.map((c: any) => (
              <MenuItem key={c.id} value={c.id}>{c.numero} — {c.orgao}</MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl sx={{ minWidth: 160 }}>
          <InputLabel>Competência</InputLabel>
          <Select value={period} label="Competência" onChange={(e) => setPeriod(e.target.value)}>
            <MenuItem value="2025-05-01">05/2025</MenuItem>
            <MenuItem value="2025-06-01">06/2025</MenuItem>
          </Select>
        </FormControl>
      </Stack>

      <Stack direction="row" spacing={2} mb={2} flexWrap="wrap">
        <Button variant="outlined" onClick={handleContest} disabled={updateGlosa.isPending}>
          Contestar Glosa Selecionada
        </Button>
        <Button variant="contained" onClick={handleRecovery} disabled={updateGlosa.isPending}>
          Registrar Recuperação
        </Button>
        <Button
          variant="contained"
          color="warning"
          onClick={handleProvisionar}
          disabled={provisionarGlosa.isPending || !selectedGlosa}
        >
          Provisionar no Financeiro
        </Button>
      </Stack>

      <EnterpriseDataGrid
        title="Glosas por Contrato / Período"
        rowData={glosas}
        columnDefs={columns}
        loading={isLoading}
        compact
        height="420px"
        emptyMessage="Nenhuma glosa no período. Calcule uma medição para gerar glosas automaticamente."
        onRowClicked={(e) => setSelectedId(e.data?.id ?? null)}
      />
      </>
      )}
    </Box>
  )
}
