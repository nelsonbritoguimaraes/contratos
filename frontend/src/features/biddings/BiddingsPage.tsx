import { useState } from 'react'
import { Box, Typography, Paper, Stack, Button, Alert, CircularProgress, Chip, TextField } from '@mui/material'
import { Add, Refresh, Search } from '@mui/icons-material'
import BiddingsGrid from './BiddingsGrid'
import BiddingFormDialog from './BiddingFormDialog'
import BiddingDetailDialog from './BiddingDetailDialog'
import { Bidding } from '../../api/types'
import { useBiddings, usePncpSearch } from '../../api/hooks/useBiddings'

export default function BiddingsPage() {
  const { data: biddings = [], isLoading: loading, refetch: fetchBiddings } = useBiddings()
  const [openForm, setOpenForm] = useState(false)
  const [editingBidding, setEditingBidding] = useState<Bidding | null>(null)
  const [openDetail, setOpenDetail] = useState(false)
  const [selectedBidding, setSelectedBidding] = useState<Bidding | null>(null)
  const [pncpTermo, setPncpTermo] = useState('')
  const [pncpQuery, setPncpQuery] = useState('')
  const { data: pncpResult, isFetching: pncpLoading } = usePncpSearch(pncpQuery)

  const handleNew = () => {
    setEditingBidding(null)
    setOpenForm(true)
  }

  const handleEdit = (bidding: Bidding) => {
    setEditingBidding(bidding)
    setOpenForm(true)
  }

  const handleOpenDetail = (bidding: Bidding) => {
    setSelectedBidding(bidding)
    setOpenDetail(true)
  }

  const handleSaved = async () => {
    await fetchBiddings()
  }

  const totalValor = biddings.reduce((sum, b) => sum + (b.valorVencedor || 0), 0)
  const homologadas = biddings.filter((b) => b.status === 'HOMOLOGADA').length

  return (
    <Box>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={{ xs: 2, sm: 0 }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} mb={3}>
        <Box>
          <Typography variant="h5" fontWeight={500} gutterBottom sx={{ fontSize: { xs: '1.4rem', sm: '2rem' } }}>
            Licitações (Biddings)
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Fluxo completo: Licitação → Lotes → Planilha Vencedora → Contrato
          </Typography>
        </Box>

        <Stack direction="row" spacing={1}>
          <Button variant="contained" startIcon={<Add />} onClick={handleNew}>
            Nova Licitação
          </Button>
          <Button variant="outlined" startIcon={<Refresh />} onClick={() => fetchBiddings()} disabled={loading}>
            Atualizar
          </Button>
        </Stack>
      </Stack>

      {/* Radar PNCP */}
      <Paper sx={{ p: 2.5, mb: 3, borderRadius: 3 }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>Radar PNCP — Oportunidades</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          Consulta pública (Lei 14.133). Busque editais abertos para cadastrar como licitação interna.
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          <TextField
            size="small"
            fullWidth
            placeholder="terceirização, vigilância, limpeza..."
            value={pncpTermo}
            onChange={(e) => setPncpTermo(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && setPncpQuery(pncpTermo)}
          />
          <Button
            variant="contained"
            startIcon={<Search />}
            onClick={() => setPncpQuery(pncpTermo)}
            disabled={pncpLoading}
          >
            Buscar PNCP
          </Button>
        </Stack>
        {pncpQuery && (
          <Box sx={{ mt: 2, maxHeight: 200, overflow: 'auto', bgcolor: 'grey.50', p: 1.5, borderRadius: 1 }}>
            {pncpLoading ? (
              <CircularProgress size={20} />
            ) : pncpResult?.erro ? (
              <Alert severity="warning" sx={{ py: 0 }}>{String(pncpResult.erro)}</Alert>
            ) : (
              <Typography component="pre" variant="caption" sx={{ whiteSpace: 'pre-wrap', m: 0 }}>
                {JSON.stringify(pncpResult?.data ?? pncpResult, null, 2)}
              </Typography>
            )}
          </Box>
        )}
      </Paper>

      <Alert severity="info" sx={{ mb: 3, borderRadius: 2 }}>
        Módulo completo <strong>/api/biddings</strong>: propostas, prazos, impugnações, certidões RF/FGTS/CNDT, DRE e import Excel (.xlsx).
      </Alert>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={3}>
        <Paper sx={{ p: 2.5, flex: 1, borderRadius: 3 }}>
          <Typography variant="overline" color="text.secondary">Licitações</Typography>
          <Typography variant="h4" fontWeight={600} sx={{ mt: 0.5 }}>{biddings.length}</Typography>
        </Paper>
        <Paper sx={{ p: 2.5, flex: 1, borderRadius: 3 }}>
          <Typography variant="overline" color="text.secondary">Valor Total Vencedor</Typography>
          <Typography variant="h4" fontWeight={600} color="primary.main" sx={{ mt: 0.5 }}>
            {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(totalValor)}
          </Typography>
        </Paper>
        <Paper sx={{ p: 2.5, flex: 1, borderRadius: 3 }}>
          <Typography variant="overline" color="text.secondary">Homologadas</Typography>
          <Typography variant="h4" fontWeight={600} color="success.main" sx={{ mt: 0.5 }}>
            {homologadas} / {biddings.length}
          </Typography>
          <Stack direction="row" spacing={0.5} mt={0.5} flexWrap="wrap">
            {biddings.slice(0, 5).map((b, i) => (
              <Chip key={i} label={b.status} size="small" />
            ))}
          </Stack>
        </Paper>
      </Stack>

      <Paper sx={{ p: 2, borderRadius: 3, overflow: 'hidden' }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        ) : (
          <BiddingsGrid biddings={biddings} onEdit={handleEdit} onOpenDetail={handleOpenDetail} onRefresh={() => fetchBiddings()} />
        )}
      </Paper>

      <BiddingDetailDialog
        open={openDetail}
        bidding={selectedBidding}
        onClose={() => setOpenDetail(false)}
        onRefresh={() => fetchBiddings()}
      />

      <BiddingFormDialog
        open={openForm}
        bidding={editingBidding}
        onClose={() => setOpenForm(false)}
        onSaved={handleSaved}
      />
    </Box>
  )
}
