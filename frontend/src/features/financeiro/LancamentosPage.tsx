import { useMemo, useState } from 'react'
import { Box, Typography, Paper, Stack, Button, TextField, Tabs, Tab, Alert, MenuItem, CircularProgress } from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import { useFornecedores } from '../../api/hooks/useFornecedores'
import { useCriarLancamento, useLancamentos } from '../../api/hooks/useLancamentos'
import { useContasPagar } from '../../api/hooks/useContasPagar'

export default function LancamentosPage() {
  const { showNotification } = useNotification()
  const [tab, setTab] = useState(0)

  const { data: fornecedores = [] } = useFornecedores()
  const { data: compras = [], isLoading: loadingCompras, refetch: refetchCompras } = useLancamentos('COMPRA')
  const { data: despesas = [], isLoading: loadingDespesas, refetch: refetchDespesas } = useLancamentos('DESPESA')
  const { data: pagamentosPendentes = [] } = useContasPagar({ status: 'ABERTO' })
  const criarLancamento = useCriarLancamento()

  const [novaCompra, setNovaCompra] = useState({ fornecedorId: '', descricao: '', valor: '' })
  const [novaDespesa, setNovaDespesa] = useState({ descricao: '', categoria: '', valor: '' })

  const comprasGrid = useMemo(() => compras.map(c => {
    const f = fornecedores.find(x => x.id === c.fornecedorId)
    return {
      ...c,
      fornecedor: f?.razaoSocial ?? '—',
      data: c.data,
    }
  }), [compras, fornecedores])

  const handleLancarCompra = async () => {
    if (!novaCompra.fornecedorId || !novaCompra.descricao || !novaCompra.valor) {
      showNotification('Preencha fornecedor, descrição e valor da compra', 'error')
      return
    }
    try {
      await criarLancamento.mutateAsync({
        tipo: 'COMPRA',
        fornecedorId: novaCompra.fornecedorId,
        descricao: novaCompra.descricao,
        valor: parseFloat(novaCompra.valor),
        status: 'LANCADO',
      })
      setNovaCompra({ fornecedorId: '', descricao: '', valor: '' })
      showNotification('Compra lançada (API)', 'success')
      refetchCompras()
    } catch {
      showNotification('Erro ao lançar compra', 'error')
    }
  }

  const handleLancarDespesa = async () => {
    if (!novaDespesa.descricao || !novaDespesa.valor) {
      showNotification('Preencha descrição e valor da despesa', 'error')
      return
    }
    try {
      await criarLancamento.mutateAsync({
        tipo: 'DESPESA',
        descricao: novaDespesa.descricao,
        categoria: novaDespesa.categoria || 'Geral',
        valor: parseFloat(novaDespesa.valor),
        status: 'LANCADO',
      })
      setNovaDespesa({ descricao: '', categoria: '', valor: '' })
      showNotification('Despesa lançada (API)', 'success')
      refetchDespesas()
    } catch {
      showNotification('Erro ao lançar despesa', 'error')
    }
  }

  const pagamentosGrid = useMemo(() =>
    (pagamentosPendentes as any[]).map(p => ({
      id: p.id,
      descricao: p.observacoes || p.origem || 'Conta a pagar',
      valor: Number(p.valor),
      vencimento: p.vencimento,
      status: p.status,
    })),
  [pagamentosPendentes])

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>Lançamentos Financeiros</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
        Compras e despesas via API • Pagamentos pendentes do módulo Contas a Pagar
      </Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Compras" />
        <Tab label="Despesas" />
        <Tab label="Pagamentos Pendentes" />
      </Tabs>

      {tab === 0 && (
        <Paper sx={{ p: { xs: 2, sm: 2.5 }, mb: 3 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2}>
            <TextField
              select
              label="Fornecedor *"
              size="small"
              sx={{ minWidth: 240 }}
              value={novaCompra.fornecedorId}
              onChange={(e) => setNovaCompra({ ...novaCompra, fornecedorId: e.target.value })}
            >
              <MenuItem value="">Selecione...</MenuItem>
              {fornecedores.map(f => (
                <MenuItem key={f.id} value={f.id}>{f.razaoSocial}</MenuItem>
              ))}
            </TextField>
            <TextField label="Descrição *" size="small" sx={{ flex: 1 }} value={novaCompra.descricao}
              onChange={(e) => setNovaCompra({ ...novaCompra, descricao: e.target.value })} />
            <TextField label="Valor (R$)" type="number" size="small" sx={{ width: 140 }} value={novaCompra.valor}
              onChange={(e) => setNovaCompra({ ...novaCompra, valor: e.target.value })} />
            <Button variant="contained" onClick={handleLancarCompra} disabled={criarLancamento.isPending}>
              {criarLancamento.isPending ? <CircularProgress size={22} /> : 'Lançar Compra'}
            </Button>
          </Stack>
          <EnterpriseDataGrid
            title="Compras Lançadas"
            rowData={comprasGrid}
            loading={loadingCompras}
            columnDefs={[
              { headerName: 'Data', field: 'data' },
              { headerName: 'Fornecedor', field: 'fornecedor', flex: 1.5 },
              { headerName: 'Descrição', field: 'descricao', flex: 2 },
              { headerName: 'Valor', field: 'valor', valueFormatter: (p: any) => `R$ ${Number(p.value).toLocaleString('pt-BR')}` },
              { headerName: 'Status', field: 'status' },
            ]}
            onRefresh={() => refetchCompras()}
            height="calc(100dvh - 420px)"
          />
        </Paper>
      )}

      {tab === 1 && (
        <Paper sx={{ p: { xs: 2, sm: 2.5 } }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2}>
            <TextField label="Descrição *" size="small" sx={{ flex: 1 }} value={novaDespesa.descricao}
              onChange={(e) => setNovaDespesa({ ...novaDespesa, descricao: e.target.value })} />
            <TextField select label="Categoria" size="small" sx={{ width: 200 }} value={novaDespesa.categoria}
              onChange={(e) => setNovaDespesa({ ...novaDespesa, categoria: e.target.value })}>
              <MenuItem value="">Selecione...</MenuItem>
              <MenuItem value="Operacional">Operacional</MenuItem>
              <MenuItem value="Administrativo">Administrativo</MenuItem>
              <MenuItem value="Manutenção">Manutenção</MenuItem>
              <MenuItem value="Geral">Geral</MenuItem>
            </TextField>
            <TextField label="Valor (R$)" type="number" size="small" sx={{ width: 140 }} value={novaDespesa.valor}
              onChange={(e) => setNovaDespesa({ ...novaDespesa, valor: e.target.value })} />
            <Button variant="contained" onClick={handleLancarDespesa} disabled={criarLancamento.isPending}>
              Lançar Despesa
            </Button>
          </Stack>
          <EnterpriseDataGrid
            title="Despesas Lançadas"
            rowData={despesas}
            loading={loadingDespesas}
            columnDefs={[
              { headerName: 'Data', field: 'data' },
              { headerName: 'Descrição', field: 'descricao', flex: 2 },
              { headerName: 'Categoria', field: 'categoria' },
              { headerName: 'Valor', field: 'valor', valueFormatter: (p: any) => `R$ ${Number(p.value).toLocaleString('pt-BR')}` },
            ]}
            onRefresh={() => refetchDespesas()}
            height="calc(100dvh - 420px)"
          />
        </Paper>
      )}

      {tab === 2 && (
        <Paper sx={{ p: { xs: 2, sm: 2.5 } }}>
          <Alert severity="info" sx={{ mb: 2 }}>Contas a pagar em aberto (backend).</Alert>
          <EnterpriseDataGrid
            title="Pagamentos Pendentes"
            rowData={pagamentosGrid}
            columnDefs={[
              { headerName: 'Descrição', field: 'descricao', flex: 2 },
              { headerName: 'Valor', field: 'valor', valueFormatter: (p: any) => `R$ ${Number(p.value).toLocaleString('pt-BR')}` },
              { headerName: 'Vencimento', field: 'vencimento' },
              { headerName: 'Status', field: 'status' },
            ]}
            height="calc(100dvh - 420px)"
          />
        </Paper>
      )}
    </Box>
  )
}
