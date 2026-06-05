/**
 * ApprovalModal — Modal genérico de aprovação multi-nível (Fase 0/3)
 * Componente crítico para o plano (pagamentos, medições, holerites, etc.).
 * Suporta 1 ou 2 níveis de aprovação conforme valor.
 */
import { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  Stack,
  Alert,
  Chip,
} from '@mui/material'

interface ApprovalModalProps {
  open: boolean
  onClose: () => void
  onApprove: (nivel: number, justificativa: string) => Promise<void> | void
  title: string
  description?: string
  valor?: number
  currentNivel?: number
  maxNivel?: number
  loading?: boolean
}

export function ApprovalModal({
  open,
  onClose,
  onApprove,
  title,
  description,
  valor,
  currentNivel = 0,
  loading = false,
}: ApprovalModalProps) {
  const [justificativa, setJustificativa] = useState('')
  const [error, setError] = useState('')

  const nextNivel = currentNivel + 1
  const requiresSecondLevel = valor != null && valor > 50000

  const handleApprove = async () => {
    if (!justificativa.trim()) {
      setError('Justificativa é obrigatória.')
      return
    }
    setError('')

    try {
      await onApprove(nextNivel, justificativa.trim())
      setJustificativa('')
      onClose()
    } catch (e: any) {
      setError(e?.message || 'Erro ao aprovar. Tente novamente.')
    }
  }

  const handleClose = () => {
    setJustificativa('')
    setError('')
    onClose()
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent sx={{ pt: 1.5 }}>
        <Stack spacing={1.75}>
          {description && <Typography color="text.secondary">{description}</Typography>}

          {valor != null && (
            <Typography variant="h6">
              Valor: <strong>R$ {valor.toLocaleString('pt-BR')}</strong>
            </Typography>
          )}

          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="body2">Nível de aprovação:</Typography>
            <Chip label={`Nível ${nextNivel}`} color="primary" />
            {requiresSecondLevel && <Chip label="Requer 2º nível" color="warning" />}
          </Stack>

          <TextField
            label="Justificativa / Observação"
            multiline
            minRows={3}
            fullWidth
            value={justificativa}
            onChange={(e) => setJustificativa(e.target.value)}
            placeholder="Ex: Aprovação após conferência de medição e documentação fiscal..."
            required
          />

          {error && <Alert severity="error">{error}</Alert>}

          {currentNivel >= 1 && (
            <Alert severity="info">
              Esta aprovação será registrada no histórico de aprovações do sistema.
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Cancelar
        </Button>
        <Button
          onClick={handleApprove}
          variant="contained"
          disabled={loading || !justificativa.trim()}
        >
          {loading ? 'Aprovando...' : `Aprovar no Nível ${nextNivel}`}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
