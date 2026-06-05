/**
 * Contas a Pagar — gestão AP com baixa via API real
 */
import { useState, useMemo } from 'react'
import {
  Box, Typography, Alert, Button, Stack, Paper, TextField, Chip, Grid, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material'
import { ColDef } from 'ag-grid-community'
import { RefreshCw, CreditCard } from 'lucide-react'
import { useTenant } from '../../api/hooks/useTenant'
import { useContasPagar, usePagarConta } from '../../api/hooks/useContasPagar'
import { useContasBancarias } from '../../api/hooks/useTesouraria'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'

export default function ContasPagarPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const [statusFilter, setStatusFilter] = useState('')
  const [origemFilter, setOrigemFilter] = useState('')

  const { data: contas = [], resumo, isLoading, isError, error, refetch } = useContasPagar({
    status: statusFilter || undefined,
    origem: origemFilter || undefined,
  })
  const { data: contasBancarias = [] } = useContasBancarias()
  const pagar = usePagarConta()

  const [baixaModal, setBaixaModal] = useState<{ open: boolean; conta: any | null }>({ open: false, conta: null })
  const [baixaForm, setBaixaForm] = useState({ data: new Date().toISOString().slice(0, 10), contaBancariaId: '', formaPagamento: 'PIX' })

  const columns: ColDef[] = [
    { headerName: 'Origem', field: 'origem', flex: 1 },
    { headerName: 'Vencimento', field: 'vencimento', width: 120 },
    {
      headerName: 'Valor',
      field: 'valor',
      width: 130,
      valueFormatter: (p) => p.value != null ? `R$ ${Number(p.value).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}` : '—',
    },
    {
      headerName: 'Saldo',
      field: 'saldoAberto',
      width: 130,
      valueFormatter: (p) => p.value != null ? `R$ ${Number(p.value).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}` : '—',
    },
    { headerName: 'Status', field: 'status', width: 110 },
    { headerName: 'Aging', field: 'agingBucket', width: 90 },
    {
      headerName: 'Ações',
      field: 'id',
      width: 120,
      cellRenderer: (p: any) =>
        p.data?.status !== 'PAGO' ? (
          <Button size="small" variant="outlined" onClick={() => openBaixa(p.data)}>
            Baixar
          </Button>
        ) : null,
    },
  ]

  const openBaixa = (conta: any) => {
    setBaixaForm({
      data: new Date().toISOString().slice(0, 10),
      contaBancariaId: contasBancarias[0]?.id ? String(contasBancarias[0].id) : '',
      formaPagamento: 'PIX',
    })
    setBaixaModal({ open: true, conta })
  }

  const handleBaixar = async () => {
    const c = baixaModal.conta
    if (!c?.id || !baixaForm.contaBancariaId) {
      showNotification('Selecione conta bancária', 'error')
      return
    }
    try {
      await pagar.mutateAsync({
        contaAPagarId: String(c.id),
        data: baixaForm.data,
        valor: Number(c.saldoAberto ?? c.valor),
        contaBancariaId: baixaForm.contaBancariaId,
        formaPagamento: baixaForm.formaPagamento,
      })
      showNotification('Pagamento registrado com sucesso', 'success')
      setBaixaModal({ open: false, conta: null })
      refetch()
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao registrar pagamento', 'error')
    }
  }

  const kpis = useMemo(() => ({
    total: resumo?.total ?? contas.length,
    aberto: resumo?.valorTotalAberto ?? 0,
    vencido: resumo?.valorTotalVencido ?? 0,
  }), [resumo, contas])

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            Contas a Pagar
          </Typography>
          <Typography color="text.secondary">Tenant: {tenantName} — Folha, fornecedores e tributos</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">
          Atualizar
        </Button>
      </Stack>

      {isError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Backend indisponível: {(error as Error)?.message}
        </Alert>
      )}

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="overline" color="text.secondary">Total contas</Typography>
            <Typography variant="h5">{kpis.total}</Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="overline" color="text.secondary">Em aberto</Typography>
            <Typography variant="h5" color="warning.main">
              R$ {Number(kpis.aberto).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="overline" color="text.secondary">Vencido</Typography>
            <Typography variant="h5" color="error.main">
              R$ {Number(kpis.vencido).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select label="Status" size="small" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} sx={{ minWidth: 140 }}>
          <MenuItem value="">Todos</MenuItem>
          <MenuItem value="ABERTO">Aberto</MenuItem>
          <MenuItem value="APROVADO">Aprovado</MenuItem>
          <MenuItem value="PAGO">Pago</MenuItem>
        </TextField>
        <TextField select label="Origem" size="small" value={origemFilter} onChange={(e) => setOrigemFilter(e.target.value)} sx={{ minWidth: 160 }}>
          <MenuItem value="">Todas</MenuItem>
          <MenuItem value="PAYSLIP">Folha</MenuItem>
          <MenuItem value="FORNECEDOR">Fornecedor</MenuItem>
          <MenuItem value="RETENCAO_TRIBUTARIA">Retenção</MenuItem>
        </TextField>
        <Chip icon={<CreditCard size={14} />} label="Baixa via POST /pagar/{id}/baixar" variant="outlined" />
      </Stack>

      <EnterpriseDataGrid
        rowData={contas}
        columnDefs={columns}
        loading={isLoading}
        emptyMessage="Nenhuma conta a pagar. Feche a folha ou lance despesas de fornecedor."
      />

      <Dialog open={baixaModal.open} onClose={() => setBaixaModal({ open: false, conta: null })} maxWidth="sm" fullWidth>
        <DialogTitle>Baixar Conta a Pagar</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2">{baixaModal.conta?.observacoes || baixaModal.conta?.origem}</Typography>
            <TextField label="Data pagamento" type="date" value={baixaForm.data} onChange={(e) => setBaixaForm({ ...baixaForm, data: e.target.value })} InputLabelProps={{ shrink: true }} fullWidth />
            <TextField select label="Conta bancária" value={baixaForm.contaBancariaId} onChange={(e) => setBaixaForm({ ...baixaForm, contaBancariaId: e.target.value })} fullWidth>
              {contasBancarias.map((cb: any) => (
                <MenuItem key={cb.id} value={String(cb.id)}>{cb.bancoNome} — {cb.agencia}/{cb.conta}</MenuItem>
              ))}
            </TextField>
            <TextField select label="Forma" value={baixaForm.formaPagamento} onChange={(e) => setBaixaForm({ ...baixaForm, formaPagamento: e.target.value })} fullWidth>
              <MenuItem value="PIX">PIX</MenuItem>
              <MenuItem value="TED">TED</MenuItem>
              <MenuItem value="BOLETO">Boleto</MenuItem>
              <MenuItem value="CNAB_RETORNO">CNAB Retorno</MenuItem>
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBaixaModal({ open: false, conta: null })}>Cancelar</Button>
          <Button variant="contained" onClick={handleBaixar} disabled={pagar.isPending}>Confirmar pagamento</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
