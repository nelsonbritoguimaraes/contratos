/**
 * ImplantationPage — checklist de implantação contratual (SPEC §12)
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, FormControl, InputLabel, Select, MenuItem, Chip, LinearProgress,
} from '@mui/material'
import { RefreshCw, PlayCircle } from 'lucide-react'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useContracts } from '../../api/hooks/useContracts'
import {
  useImplantationByContract, useImplantationChecklist, useStartImplantation, useCompleteChecklistItem,
} from '../../api/hooks/usePhase78'

export default function ImplantationPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()
  const [contractId, setContractId] = useState('')

  const { data: implantation, refetch, isLoading } = useImplantationByContract(contractId || undefined)
  const { data: checklist = [], refetch: refetchChecklist } = useImplantationChecklist(implantation?.id)
  const start = useStartImplantation()
  const complete = useCompleteChecklistItem()

  const done = checklist.filter((i: any) => i.completed).length
  const total = checklist.length || 1
  const progress = Math.round((done / total) * 100)

  const handleStart = async () => {
    if (!contractId) return showNotification('Selecione um contrato', 'warning')
    try {
      await start.mutateAsync(contractId)
      showNotification('Implantação iniciada com checklist padrão', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro ao iniciar', 'error')
    }
  }

  const handleComplete = async (itemId: string) => {
    try {
      await complete.mutateAsync(itemId)
      refetchChecklist()
      refetch()
    } catch (e: any) {
      showNotification(e.message || 'Erro', 'error')
    }
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Implantação Contratual</Typography>
          <Typography color="text.secondary">Tenant: {tenantName} — PLANEJAMENTO → EM_ANDAMENTO → PRONTO</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => { refetch(); refetchChecklist() }} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Checklist configurável por contrato: assinatura, postos, uniformes, equipamentos, ponto e ata de início.
      </Alert>

      <Paper sx={{ p: 3, borderRadius: 3, mb: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
          <FormControl sx={{ minWidth: 320 }}>
            <InputLabel>Contrato</InputLabel>
            <Select value={contractId} label="Contrato" onChange={(e) => setContractId(e.target.value)}>
              <MenuItem value="">Selecione</MenuItem>
              {contracts.map((c: any) => (
                <MenuItem key={c.id} value={c.id}>{c.numero} — {c.orgao}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button variant="contained" startIcon={<PlayCircle size={16} />} onClick={handleStart} disabled={!contractId || start.isPending}>
            Iniciar implantação
          </Button>
        </Stack>
      </Paper>

      {implantation && (
        <Paper sx={{ p: 3, borderRadius: 3 }}>
          <Stack direction="row" spacing={2} alignItems="center" mb={2}>
            <Chip label={implantation.status} color={implantation.status === 'PRONTO' ? 'success' : 'primary'} />
            <Typography variant="body2" color="text.secondary">{done}/{total} itens concluídos</Typography>
          </Stack>
          <LinearProgress variant="determinate" value={progress} sx={{ mb: 3, height: 8, borderRadius: 1 }} />
          <Stack spacing={1}>
            {checklist.map((item: any) => (
              <Stack key={item.id} direction="row" justifyContent="space-between" alignItems="center" sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
                <Box>
                  <Typography fontWeight={600}>{item.description}</Typography>
                  <Typography variant="caption" color="text.secondary">{item.code}</Typography>
                </Box>
                {item.completed ? (
                  <Chip label="Concluído" size="small" color="success" />
                ) : (
                  <Button size="small" variant="outlined" onClick={() => handleComplete(item.id)} disabled={complete.isPending}>
                    Concluir
                  </Button>
                )}
              </Stack>
            ))}
          </Stack>
        </Paper>
      )}
      {isLoading && <Typography color="text.secondary">Carregando...</Typography>}
    </Box>
  )
}
