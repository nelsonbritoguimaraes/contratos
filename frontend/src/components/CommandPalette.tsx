/**
 * CommandPalette — Paleta Universal (Cmd/Ctrl + K) — Fase 0
 * Funcionalidade principal: navegação rápida + "Perguntar à IA" (/api/ia/ask)
 * Ainda em evolução — versão inicial já funcional.
 */
import { useState, useEffect } from 'react'
import { Dialog, DialogContent, TextField, List, ListItem, ListItemButton, ListItemText, Typography, Box } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { useTenant } from '../api/hooks/useTenant'
import { apiPost } from '../api/client'
import { useNotification } from './NotificationProvider'

interface CommandPaletteProps {
  open: boolean
  onClose: () => void
}

interface CommandItem {
  label: string
  action: () => void
  section: string
}

export default function CommandPalette({ open, onClose }: CommandPaletteProps) {
  const [query, setQuery] = useState('')
  const [iaLoading, setIaLoading] = useState(false)
  const navigate = useNavigate()
  const { tenantId } = useTenant()
  const { showNotification } = useNotification()

  const commands: CommandItem[] = [
    { label: 'Ir para Dashboard CFO', action: () => navigate('/financeiro/dashboard'), section: 'Navegação' },
    { label: 'Ir para Console de IA', action: () => navigate('/ia'), section: 'Navegação' },
    { label: 'Ir para Histórico de IA', action: () => navigate('/ia/calls'), section: 'Navegação' },
    { label: 'Ir para Folha / Holerites', action: () => navigate('/rh/folha'), section: 'Navegação' },
    { label: 'Ir para eSocial', action: () => navigate('/rh/esocial'), section: 'Navegação' },
    { label: 'Ir para Ponto & Cobertura', action: () => navigate('/ponto'), section: 'Navegação' },
    { label: 'Ir para Contas a Receber', action: () => navigate('/financeiro/receber'), section: 'Navegação' },
    { label: 'Ir para Tesouraria', action: () => navigate('/financeiro/tesouraria'), section: 'Navegação' },
    { label: 'Nova Medição', action: () => navigate('/measurements'), section: 'Ações' },
    { label: 'Ver Contratos', action: () => navigate('/contracts'), section: 'Ações' },
    { label: 'Upload de CCT + Impacto', action: () => navigate('/cct'), section: 'Ações' },
    { label: 'Gestão de Glosas', action: () => navigate('/glosas'), section: 'Ações' },
    { label: 'Colaboradores', action: () => navigate('/rh/employees'), section: 'Navegação' },
    // Onda 2 additions (dentro de páginas existentes)
    { label: 'Uniformes & Alocações por Posto', action: () => navigate('/postos'), section: 'Navegação' },
    { label: 'Ponto Completo (AFD + Documentos)', action: () => navigate('/ponto'), section: 'Navegação' },
    { label: 'Ver Impacto Ponto → Folha', action: () => navigate('/rh/folha'), section: 'Ações' },
  ]

  const filtered = commands.filter((c) =>
    c.label.toLowerCase().includes(query.toLowerCase())
  )

  const handleAskIA = async () => {
    if (!query.trim()) return

    setIaLoading(true)
    try {
      const res = await apiPost<any>('/ia/ask', {
        question: query,
        tenantId,
      })
      onClose()
      setQuery('')
      navigate('/ia', {
        state: {
          question: query,
          iaResult: res,
        },
      })
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao consultar IA', 'error')
    } finally {
      setIaLoading(false)
    }
  }

  const handleClose = () => {
    setQuery('')
    onClose()
  }

  // Atalho de teclado global (já tratado em App.tsx, mas reforçamos aqui)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        // O controle de abertura é feito no App.tsx
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      fullWidth
      maxWidth="sm"
      PaperProps={{
        sx: { borderRadius: 3, minHeight: 420 },
      }}
    >
      <DialogContent sx={{ p: 0 }}>
        <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
          <TextField
            autoFocus
            fullWidth
            placeholder="Digite um comando ou pergunte algo para a IA (ex: qual o impacto de atraso de 15 dias?)"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && query.trim().length > 3) {
                handleAskIA()
              }
            }}
            variant="outlined"
            size="small"
          />
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            Pressione Enter para perguntar à IA • Esc para fechar
          </Typography>
        </Box>

        <List dense sx={{ maxHeight: 320, overflow: 'auto' }}>
          {filtered.length > 0 ? (
            filtered.map((cmd, index) => (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  onClick={() => {
                    cmd.action()
                    onClose()
                  }}
                >
                  <ListItemText primary={cmd.label} secondary={cmd.section} />
                </ListItemButton>
              </ListItem>
            ))
          ) : (
            <ListItem>
              <ListItemText
                primary={iaLoading ? 'Consultando IA...' : 'Nenhum comando encontrado'}
                secondary="Digite uma pergunta em linguagem natural para consultar a IA"
              />
            </ListItem>
          )}
        </List>

        {query.length > 3 && (
          <Box sx={{ p: 2, borderTop: '1px solid', borderColor: 'divider', bgcolor: 'action.hover' }}>
            <Typography variant="body2" color="primary" sx={{ cursor: 'pointer' }} onClick={handleAskIA}>
              {iaLoading ? 'Consultando IA...' : `Perguntar à IA: "${query}"`}
            </Typography>
          </Box>
        )}
      </DialogContent>
    </Dialog>
  )
}
