/**
 * GlosaRulesTab — regras configuráveis por contrato (tab em GlosaPage)
 */
import { useState } from 'react'
import {
  Box, Stack, Button, FormControl, InputLabel, Select, MenuItem, TextField, Alert,
} from '@mui/material'
import { Plus } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import { useContracts } from '../../api/hooks/useContracts'
import { useGlosaRules, useSaveGlosaRule } from '../../api/hooks/usePhase78'

const columns = [
  { headerName: 'Tipo', field: 'ruleType', width: 160 },
  { headerName: 'Descrição', field: 'description', flex: 1 },
  { headerName: 'Fator', field: 'factor', width: 80 },
  { headerName: 'Tolerância (min)', field: 'toleranceMinutes', width: 130 },
  { headerName: 'Prioridade', field: 'priority', width: 100 },
]

export default function GlosaRulesTab() {
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()
  const [contractId, setContractId] = useState('')
  const { data: rules = [], isLoading, refetch } = useGlosaRules(contractId || undefined)
  const save = useSaveGlosaRule()

  const [form, setForm] = useState({ ruleType: 'FALTA', description: '', factor: '1', priority: '10' })

  const handleAdd = async () => {
    if (!contractId) return showNotification('Selecione contrato', 'warning')
    try {
      await save.mutateAsync({
        contractId,
        ruleType: form.ruleType,
        description: form.description,
        factor: Number(form.factor),
        priority: Number(form.priority),
      })
      showNotification('Regra criada', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro', 'error')
    }
  }

  return (
    <Box>
      <Alert severity="info" sx={{ mb: 2 }}>
        Motor de regras configurável: dias-base, fator, tolerância, prioridade anti-dupla-penalização.
      </Alert>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} mb={2} alignItems="flex-end">
        <FormControl size="small" sx={{ minWidth: 260 }}>
          <InputLabel>Contrato</InputLabel>
          <Select value={contractId} label="Contrato" onChange={(e) => setContractId(e.target.value)}>
            {contracts.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.numero}</MenuItem>)}
          </Select>
        </FormControl>
        <TextField size="small" label="Tipo" value={form.ruleType} onChange={(e) => setForm({ ...form, ruleType: e.target.value })} />
        <TextField size="small" label="Descrição" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} sx={{ flex: 1 }} />
        <Button variant="contained" startIcon={<Plus size={16} />} onClick={handleAdd} disabled={save.isPending}>Adicionar regra</Button>
      </Stack>
      <EnterpriseDataGrid title="Regras de glosa" rowData={rules} columnDefs={columns} loading={isLoading} height="320px" emptyMessage="Selecione contrato." />
    </Box>
  )
}
