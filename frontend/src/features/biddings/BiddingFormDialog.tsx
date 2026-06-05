import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Stack
} from '@mui/material'
import { Bidding, CreateBiddingRequest } from '../../api/types'
import { apiPost, apiPut } from '../../api/client'
import { useNotification } from '../../components/NotificationProvider'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { biddingSchema, type BiddingForm } from '../../schemas/biddingSchema'

interface Props {
  open: boolean
  bidding?: Bidding | null
  onClose: () => void
  onSaved: () => void
}

export default function BiddingFormDialog({ open, bidding, onClose, onSaved }: Props) {
  const isEdit = !!bidding
  const { showNotification } = useNotification()

  const { register, handleSubmit, reset, formState: { errors } } = useForm<BiddingForm & Record<string, any>>({
    resolver: zodResolver(biddingSchema) as any,
    defaultValues: {
      orgao: '',
      editalNumero: '',
      modalidade: 'PREGAO_ELETRONICO',
      dataAbertura: '',
    },
  })

  // Campos extras não validados pelo schema do bidding mas necessários para o formulário
  const [extraFields, setExtraFields] = useState({
    processoNumero: '',
    objeto: '',
    valorVencedor: '',
    status: 'HOMOLOGADA',
  })
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (bidding) {
      reset({
        orgao: bidding.orgao || '',
        editalNumero: bidding.editalNumero || '',
        modalidade: (bidding as any).modalidade || 'PREGAO_ELETRONICO',
        dataAbertura: (bidding as any).dataAbertura || '',
      })
      setExtraFields({
        processoNumero: (bidding as any).processoNumero || '',
        objeto: (bidding as any).objeto || '',
        valorVencedor: (bidding as any).valorVencedor ? String((bidding as any).valorVencedor) : '',
        status: (bidding as any).status || 'HOMOLOGADA',
      })
    } else {
      reset({ orgao: '', editalNumero: '', modalidade: 'PREGAO_ELETRONICO', dataAbertura: '' })
      setExtraFields({ processoNumero: '', objeto: '', valorVencedor: '', status: 'HOMOLOGADA' })
    }
  }, [bidding, open, reset])

  const onSubmit = async (data: BiddingForm) => {
    setSubmitting(true)
    const payload: CreateBiddingRequest = {
      editalNumero: data.editalNumero,
      processoNumero: extraFields.processoNumero || undefined,
      orgao: data.orgao,
      objeto: extraFields.objeto || undefined,
      modalidade: data.modalidade,
      valorVencedor: extraFields.valorVencedor ? parseFloat(extraFields.valorVencedor) : undefined,
      status: extraFields.status,
    }

    try {
      if (isEdit && bidding) {
        await apiPut(`/biddings/${bidding.id}`, payload)
      } else {
        await apiPost('/biddings', payload)
      }
      onSaved()
      onClose()
      showNotification(isEdit ? 'Licitação atualizada' : 'Licitação criada com sucesso')
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro desconhecido'
      showNotification(`Erro: ${message}`, 'error')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Editar Licitação' : 'Nova Licitação'}</DialogTitle>
      <DialogContent sx={{ pt: 1.5 }}>
        <Stack spacing={2} sx={{ mt: 0.5 }}>
          <TextField label="Número do Edital *" {...register('editalNumero')} error={!!errors.editalNumero} helperText={errors.editalNumero?.message} fullWidth required />
          <TextField label="Processo" value={extraFields.processoNumero} onChange={(e) => setExtraFields({ ...extraFields, processoNumero: e.target.value })} fullWidth />
          <TextField label="Órgão *" {...register('orgao')} error={!!errors.orgao} helperText={errors.orgao?.message} fullWidth required />
          <TextField label="Objeto" value={extraFields.objeto} onChange={(e) => setExtraFields({ ...extraFields, objeto: e.target.value })} fullWidth multiline rows={2} />
          <Stack direction="row" spacing={2}>
            <TextField label="Modalidade" {...register('modalidade')} error={!!errors.modalidade} helperText={errors.modalidade?.message} fullWidth />
            <TextField label="Data de Abertura *" type="date" {...register('dataAbertura')} InputLabelProps={{ shrink: true }} error={!!errors.dataAbertura} helperText={errors.dataAbertura?.message} fullWidth />
          </Stack>
          <TextField label="Valor Vencedor (R$)" value={extraFields.valorVencedor} onChange={(e) => setExtraFields({ ...extraFields, valorVencedor: e.target.value })} fullWidth type="number" />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit(onSubmit)} disabled={submitting}>
          {submitting ? 'Salvando...' : isEdit ? 'Salvar' : 'Criar Licitação'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
