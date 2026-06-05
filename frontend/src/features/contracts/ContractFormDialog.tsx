import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Stack,
  Paper,
  FormControlLabel,
  Checkbox,
  Typography,
  Box
} from '@mui/material'
import { Contract, CreateContractRequest, UpdateContractRequest } from '../../api/types'
import { apiPost, apiPut } from '../../api/client'
import { useNotification } from '../../components/NotificationProvider'
import { useBiddings } from '../../api/hooks/useBiddings'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { contractSchema, type ContractForm } from '../../schemas/contractSchema'
import { maskCurrency, unmask } from '../../utils/masks'

interface ContractFormDialogProps {
  open: boolean
  contract?: Contract | null
  onClose: () => void
  onSaved: () => void
  initialData?: Partial<{
    biddingId: string
    orgao: string
    objeto: string
    winningSpreadsheetId: string
  }>
  plannedPostsFromBidding?: any[]   // Postos planejados vindos da licitação
}

export default function ContractFormDialog({ 
  open, 
  contract, 
  onClose, 
  onSaved, 
  initialData, 
  plannedPostsFromBidding = [] 
}: ContractFormDialogProps) {
  const isEdit = !!contract
  const { showNotification } = useNotification()
  const { data: biddings = [] } = useBiddings()

  const { register, handleSubmit, control, reset, getValues, formState: { errors } } = useForm<ContractForm & { valorGlobal?: string; objeto?: string; status?: string; biddingId?: string; winningSpreadsheetId?: string }>({
    resolver: zodResolver(contractSchema) as any,
    defaultValues: {
      numero: '',
      orgao: '',
      vigenciaInicio: '',
      vigenciaFim: '',
      valorMensal: 0,
    },
  })

  // Campos extras não validados pelo schema do contract mas necessários para o formulário
  const [extraFields, setExtraFields] = useState({
    objeto: '',
    valorGlobal: '',
    status: 'ATIVO',
    biddingId: '',
    winningSpreadsheetId: ''
  })

  // Controle de importação de postos da licitação
  const [selectedPostsToImport, setSelectedPostsToImport] = useState<string[]>([])
  const [importPostsAfterCreation, setImportPostsAfterCreation] = useState(true)

  const [submitting, setSubmitting] = useState(false)

  // Preenche o form quando estiver editando ou com dados iniciais (vindo da licitação)
  useEffect(() => {
    if (contract) {
      reset({
        numero: contract.numero || '',
        orgao: contract.orgao || '',
        vigenciaInicio: contract.vigenciaInicio || '',
        vigenciaFim: contract.vigenciaFim || '',
        valorMensal: contract.valorMensal || 0,
      })
      setExtraFields({
        objeto: contract.objeto || '',
        valorGlobal: contract.valorGlobal ? String(contract.valorGlobal) : '',
        status: contract.status || 'ATIVO',
        biddingId: (contract as any).biddingId || '',
        winningSpreadsheetId: contract.winningSpreadsheet?.id || ''
      })
    } else if (initialData) {
      setExtraFields(prev => ({
        ...prev,
        biddingId: initialData.biddingId || '',
        winningSpreadsheetId: initialData.winningSpreadsheetId || '',
      }))
      if (initialData.orgao) {
        reset({ ...getValues(), orgao: initialData.orgao! })
      }

      // Pré-seleciona todos os postos planejados para importação
      if (plannedPostsFromBidding.length > 0) {
        setSelectedPostsToImport(plannedPostsFromBidding.map(p => p.id))
      }
    } else {
      // reset para criação
      reset({ numero: '', orgao: '', vigenciaInicio: '', vigenciaFim: '', valorMensal: 0 })
      setExtraFields({ objeto: '', valorGlobal: '', status: 'ATIVO', biddingId: '', winningSpreadsheetId: '' })
      setSelectedPostsToImport([])
    }
  }, [contract, open, initialData, plannedPostsFromBidding, reset])

  const onSubmit = async (data: ContractForm) => {
    setSubmitting(true)

    const payload: CreateContractRequest | UpdateContractRequest = {
      companyId: (contract as any)?.companyId || '',
      biddingId: extraFields.biddingId || undefined,
      winningSpreadsheetId: extraFields.winningSpreadsheetId || undefined,
      numero: data.numero,
      orgao: data.orgao,
      objeto: extraFields.objeto || undefined,
      vigenciaInicio: data.vigenciaInicio || undefined,
      vigenciaFim: data.vigenciaFim || undefined,
      valorMensal: data.valorMensal,
      valorGlobal: extraFields.valorGlobal ? parseFloat(extraFields.valorGlobal) : undefined,
      status: extraFields.status,
    }

    try {
      let createdContractId = contract?.id

      if (isEdit && contract) {
        await apiPut(`/contracts/${contract.id}`, payload)
      } else {
        const created = await apiPost('/contracts', payload)
        createdContractId = (created as any)?.id
      }

      // Importar postos selecionados da licitação (Item 1 e 3)
      if (!isEdit && createdContractId && importPostsAfterCreation && selectedPostsToImport.length > 0) {
        for (const postoId of selectedPostsToImport) {
          const originalPosto = plannedPostsFromBidding.find((p: any) => p.id === postoId)
          if (originalPosto) {
            try {
              await apiPost(`/contracts/${createdContractId}/posts`, {
                ...originalPosto,
                contratoId: createdContractId,
              })
            } catch (postErr) {
              console.warn('Erro ao importar posto:', postErr)
            }
          }
        }
        showNotification(`${selectedPostsToImport.length} posto(s) importado(s) do planejamento da licitação!`, 'success')
      }

      onSaved()
      onClose()
    } catch (e: any) {
      showNotification(`Erro ao salvar contrato: ${e.message || e}`, 'error')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Editar Contrato' : 'Novo Contrato'}</DialogTitle>
      <DialogContent sx={{ pt: 1.25, pb: 2 }}>
        <Stack spacing={1.75} sx={{ mt: 0.25 }}>
          <TextField label="Número do Contrato *" {...register('numero')} error={!!errors.numero} helperText={errors.numero?.message} fullWidth required />
          <TextField label="Órgão Contratante *" {...register('orgao')} error={!!errors.orgao} helperText={errors.orgao?.message} fullWidth required />
          <TextField label="Objeto" value={extraFields.objeto} onChange={(e) => setExtraFields({ ...extraFields, objeto: e.target.value })} fullWidth multiline rows={2} />

          <Stack direction="row" spacing={2}>
            <TextField label="Vigência Início" type="date" {...register('vigenciaInicio')} InputLabelProps={{ shrink: true }} error={!!errors.vigenciaInicio} helperText={errors.vigenciaInicio?.message} fullWidth />
            <TextField label="Vigência Fim" type="date" {...register('vigenciaFim')} InputLabelProps={{ shrink: true }} error={!!errors.vigenciaFim} helperText={errors.vigenciaFim?.message} fullWidth />
          </Stack>

          <Stack direction="row" spacing={2}>
            <Controller
              name="valorMensal"
              control={control}
              render={({ field }) => (
                <TextField
                  label="Valor Mensal (R$)"
                  value={field.value ? maskCurrency(String(Math.round(field.value * 100))) : ''}
                  onChange={(e) => {
                    const raw = unmask(e.target.value)
                    field.onChange(raw ? parseInt(raw, 10) / 100 : 0)
                  }}
                  fullWidth
                  error={!!errors.valorMensal}
                  helperText={errors.valorMensal?.message}
                />
              )}
            />
            <TextField label="Valor Global (R$)" value={extraFields.valorGlobal} onChange={(e) => setExtraFields({ ...extraFields, valorGlobal: e.target.value })} fullWidth type="number" />
          </Stack>

          {/* Vinculação Licitação + Planilha Vencedora */}
          <Stack direction="row" spacing={2}>
            <TextField
              select
              SelectProps={{ native: true }}
              label="Licitação"
              value={extraFields.biddingId}
              onChange={(e) => setExtraFields({ ...extraFields, biddingId: e.target.value })}
              fullWidth
              helperText="Opcional — vincula ao processo licitatório"
            >
              <option value="">Nenhuma</option>
              {biddings.map((b: any) => (
                <option key={b.id} value={b.id}>
                  {b.editalNumero || b.id} — {b.orgao}
                </option>
              ))}
            </TextField>
            <TextField
              label="ID Planilha Vencedora"
              value={extraFields.winningSpreadsheetId}
              onChange={(e) => setExtraFields({ ...extraFields, winningSpreadsheetId: e.target.value })}
              fullWidth
              helperText="ID da versão vencedora"
            />
          </Stack>

          {/* Importar Postos da Licitação (fluxo Licitação → Contrato) */}
          {plannedPostsFromBidding.length > 0 && (
            <Paper variant="outlined" sx={{ p: 1.75, mt: 0.5 }}>
              <Typography variant="subtitle2" gutterBottom sx={{ fontSize: '0.875rem' }}>
                Importar Postos Planejados da Licitação ({plannedPostsFromBidding.length})
              </Typography>
              <FormControlLabel
                control={
                  <Checkbox 
                    checked={importPostsAfterCreation} 
                    onChange={(e) => setImportPostsAfterCreation(e.target.checked)} 
                  />
                }
                label="Importar postos selecionados após criar o contrato"
              />
              {importPostsAfterCreation && (
                <Box sx={{ mt: 1, maxHeight: 180, overflow: 'auto' }}>
                  {plannedPostsFromBidding.map((posto: any) => (
                    <FormControlLabel
                      key={posto.id}
                      control={
                        <Checkbox
                          checked={selectedPostsToImport.includes(posto.id)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedPostsToImport([...selectedPostsToImport, posto.id])
                            } else {
                              setSelectedPostsToImport(selectedPostsToImport.filter(id => id !== posto.id))
                            }
                          }}
                        />
                      }
                      label={`${posto.nome || posto.funcao} — ${posto.localExecucao || 'Sem local'}`}
                    />
                  ))}
                </Box>
              )}
            </Paper>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit(onSubmit)} disabled={submitting}>
          {submitting ? 'Salvando...' : isEdit ? 'Salvar Alterações' : 'Criar Contrato'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
