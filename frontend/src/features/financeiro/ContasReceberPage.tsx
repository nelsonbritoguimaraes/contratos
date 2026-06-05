/**
 * Contas a Receber — Backend-driven (Terceirizadora)
 *
 * - Usa useContasReceber (contas + resumo do backend)
 * - Emitir NFS-e tenta endpoint real /nfs/emitir quando measurementId existe
 * - Baixar via mutation
 * - KPIs mantidos + EnterpriseDataGrid com loading/empty/error states
 * - Filtros + tenant awareness preservados
 */
import { useState, useMemo, useEffect } from 'react'
import { Box, Typography, Alert, Button, Stack, Paper, TextField, Chip, Grid, MenuItem } from '@mui/material'
import { useTenant } from '../../api/hooks/useTenant'
import { useContasReceber, useRegistrarRecebimento, emitirNfsReal } from '../../api/hooks/useContasReceber'
import { useGerarCobranca } from '../../api/hooks/useContasPagar'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import { useContracts } from '../../api/hooks/useContracts'
import { ColDef } from 'ag-grid-community'
import { RefreshCw, QrCode } from 'lucide-react'
import { useCalcularRetencoes, useGerarDarf } from '../../api/hooks/useRetencoes'
import { Dialog, DialogTitle, DialogContent, DialogActions } from '@mui/material'

export default function ContasReceberPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()

  // Hook real (agora retorna { data: ContaReceberRow[], resumo, isLoading, isError, error, refetch })
  const {
    data: backendReceivables = [],
    resumo,
    isLoading,
    isError,
    error,
    refetch,
  } = useContasReceber()

  const registrar = useRegistrarRecebimento()
  const gerarCobranca = useGerarCobranca()
  const calcularRetencoes = useCalcularRetencoes()
  const gerarDarf = useGerarDarf()

  const [cobrancaModal, setCobrancaModal] = useState<{ open: boolean; data: any | null }>({ open: false, data: null })

  const [valorRetencaoSim, setValorRetencaoSim] = useState('100000')
  const [retencoesPreview, setRetencoesPreview] = useState<any[]>([])

  // Detecção de offline (padrão ConciliacaoPage)
  const isOffline = !!error

  // Contratos map para enriquecer contratoNumero nos dados reais do backend (que só traz contratoId)
  const contractMap = useMemo(() => {
    const map: Record<string, string> = {}
    contracts.forEach((c: any) => {
      if (c.id) map[String(c.id)] = c.numero || String(c.id).slice(0, 8)
      if (c.numero) map[c.numero] = c.numero
    })
    return map
  }, [contracts])

  // Dados efetivos: reais normalizados (com enriquecimento)
  const baseReceivables = useMemo(() => {
    if (isOffline) return []
    // Mapeia/enriquece dados reais
    return backendReceivables.map((r: any) => {
      const enrichedContrato = r.contratoNumero ||
        (r.contratoId ? (contractMap[r.contratoId] || `Contrato ${String(r.contratoId).slice(0, 8)}`) : '—')
      // Normaliza status para os valores esperados pela UI (ABERTO → PENDENTE etc)
      let displayStatus = r.status
      if (displayStatus === 'ABERTO' || displayStatus === 'FATURADO') displayStatus = 'PENDENTE'
      if (displayStatus === 'PAGO') displayStatus = 'RECEBIDO_TOTAL'
      if (displayStatus === 'VENCIDO') displayStatus = 'INADIMPLENTE'

      return {
        ...r,
        contratoNumero: enrichedContrato,
        status: displayStatus,
        // Garante campos para grid e ações
        glosaTotal: r.glosaTotal ?? 0,
      }
    })
  }, [backendReceivables, isOffline, contractMap])

  // All receivables from backend
  const allReceivables = useMemo(() => {
    return baseReceivables
  }, [baseReceivables])

  const [statusFilter, setStatusFilter] = useState<string>('')
  const [contractFilter, setContractFilter] = useState<string>('')

  const filteredData = useMemo(() => {
    return allReceivables.filter((r: any) => {
      const matchStatus = !statusFilter || r.status === statusFilter
      const matchContract = !contractFilter || r.contratoNumero === contractFilter
      return matchStatus && matchContract
    })
  }, [allReceivables, statusFilter, contractFilter])

  // KPIs: preferir resumo do backend quando real; senão calcular localmente (excelente visual preservado)
  const kpis = useMemo(() => {
    const useBackendKpis = !isOffline && resumo && (resumo.valorTotalAberto != null)

    if (useBackendKpis) {
      // Aproximação: inadimplentes não vem direto do resumo, calculamos do filtered
      const inadimplentes = filteredData.filter((r: any) => (r.diasAtraso || 0) > 30).length
      return {
        totalAberto: Number(resumo!.valorTotalAberto || 0),
        totalRecebido: Math.max(0, (filteredData.reduce((s: number, r: any) => s + (r.valorLiquido || 0), 0) - Number(resumo!.valorTotalAberto || 0))),
        inadimplentes,
      }
    }

    // Fallback de cálculo (sem resumo do backend)
    const totalAberto = filteredData
      .filter((r: any) => !['RECEBIDO_TOTAL', 'PAGO'].includes(r.status))
      .reduce((sum: number, r: any) => sum + (r.valorLiquido || 0), 0)

    const totalRecebido = filteredData
      .filter((r: any) => ['RECEBIDO_TOTAL', 'PAGO'].includes(r.status))
      .reduce((sum: number, r: any) => sum + (r.valorLiquido || 0), 0)

    const inadimplentes = filteredData.filter((r: any) => (r.diasAtraso || 0) > 30).length

    return { totalAberto, totalRecebido, inadimplentes }
  }, [filteredData, isOffline, resumo])

  // Limpa filtros ao trocar de tenant
  useEffect(() => {
    setStatusFilter('')
    setContractFilter('')
  }, [tenantName])

  // ====================== AÇÕES RESILIENTES (real + optimistic fallback) ======================

  const handleRefreshAll = () => {
    refetch()
  }

  const handleEmitirNfse = async (item: any) => {
    if (item.measurementId && !isOffline) {
      try {
        await emitirNfsReal({
          measurementId: item.measurementId,
          contratoId: item.contratoId || '',
          valorServicos: item.valorLiquido || item.valorBruto,
        })
        showNotification(`NFS-e emitida no backend para medição ${item.measurementId}.`, 'success')
        refetch()
        return
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Falha ao emitir NFS-e'
        showNotification(message, 'error')
      }
      return
    }

    showNotification('NFS-e requer medição aprovada vinculada. Aprove uma medição no módulo Medições primeiro.', 'warning')
  }

  const handleBaixar = async (item: any) => {
    try {
      await registrar.mutateAsync({
        contaAReceberId: item.id,
        valor: item.valorLiquido,
        data: new Date().toISOString().slice(0, 10),
        observacao: 'Baixa via UI - Módulo Financeiro',
      })

      showNotification(
        `Recebimento de R$ ${item.valorLiquido.toLocaleString('pt-BR')} registrado no backend para ${item.contratoNumero}.`,
        'success'
      )
      refetch()
    } catch (e: any) {
      showNotification(`Falha ao registrar baixa: ${e?.message || 'Erro desconhecido'}`, 'error')
    }
  }

  const handleGerarCobranca = async (item: any, tipo: 'PIX' | 'BOLETO') => {
    if (!item?.id || String(item.id).startsWith('ar-local') || String(item.id).startsWith('ar-00')) {
      showNotification('Cobrança disponível apenas para contas reais do backend', 'warning')
      return
    }
    try {
      const res: any = await gerarCobranca.mutateAsync({ contaAReceberId: String(item.id), tipo })
      setCobrancaModal({ open: true, data: res })
      showNotification(`Cobrança ${tipo} emitida`, 'success')
    } catch (e: any) {
      showNotification(e?.message || `Erro ao emitir cobrança ${tipo}`, 'error')
    }
  }

  // Colunas robustas (funcionam com dados reais)
  const columns: ColDef[] = [
    { headerName: 'Contrato', field: 'contratoNumero', minWidth: 130 },
    { headerName: 'Período', field: 'periodo', minWidth: 100 },
    { headerName: 'Vencimento', field: 'vencimento', minWidth: 120 },
    {
      headerName: 'Valor Bruto',
      field: 'valorBruto',
      valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}`,
    },
    {
      headerName: 'Glosas',
      field: 'glosaTotal',
      valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}`,
      cellRenderer: (p: any) => (
        <span style={{ color: (p.value || 0) > 0 ? '#c62828' : '#2e7d32' }}>
          {(p.value || 0) > 0 ? `- R$ ${p.value.toLocaleString('pt-BR')}` : '—'}
        </span>
      ),
    },
    {
      headerName: 'Valor Líquido',
      field: 'valorLiquido',
      valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}`,
      cellStyle: { fontWeight: 600 },
    },
    {
      headerName: 'Status',
      field: 'status',
      minWidth: 140,
      cellRenderer: (p: any) => {
        const colors: any = {
          PENDENTE: 'default',
          NFSE_EMITIDA: 'info',
          RECEBIDO_PARCIAL: 'warning',
          RECEBIDO_TOTAL: 'success',
          INADIMPLENTE: 'error',
          ABERTO: 'default',
          VENCIDO: 'error',
        }
        return <Chip label={p.value} color={colors[p.value] || 'default'} size="small" />
      },
    },
    {
      headerName: 'Atraso',
      field: 'diasAtraso',
      minWidth: 90,
      cellRenderer: (p: any) => {
        const d = Number(p.value || 0)
        if (d > 30) return <Chip label={`${d} dias`} color="error" size="small" />
        if (d > 0) return <Chip label={`${d} dias`} color="warning" size="small" />
        return <span style={{ color: '#2e7d32' }}>{Math.abs(d)} dias (futuro)</span>
      },
    },
    {
      headerName: 'NFS-e',
      field: 'nfseNumero',
      minWidth: 140,
      cellRenderer: (p: any) => p.value || '—',
    },
    {
      headerName: 'Ações',
      minWidth: 320,
      cellRenderer: (params: any) => {
        const r = params.data
        const st = r.status
        const isReal = r.id && !String(r.id).startsWith('ar-local') && !String(r.id).startsWith('ar-00')
        return (
          <Stack direction="row" spacing={0.5} flexWrap="wrap">
            {(st === 'PENDENTE' || st === 'ABERTO') && (
              <Button size="small" variant="outlined" onClick={() => handleEmitirNfse(r)}>
                Emitir NFS-e
              </Button>
            )}
            {!['RECEBIDO_TOTAL', 'PAGO'].includes(st) && (
              <Button size="small" variant="contained" onClick={() => handleBaixar(r)}>
                Baixar
              </Button>
            )}
            {isReal && !['RECEBIDO_TOTAL', 'PAGO'].includes(st) && (
              <>
                <Button size="small" variant="outlined" startIcon={<QrCode size={14} />} onClick={() => handleGerarCobranca(r, 'PIX')}>
                  PIX
                </Button>
                <Button size="small" variant="outlined" onClick={() => handleGerarCobranca(r, 'BOLETO')}>
                  Boleto
                </Button>
              </>
            )}
          </Stack>
        )
      },
    },
  ]

  const isLoadingGrid = isLoading

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" sx={{ mb: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Contas a Receber</Typography>
          <Typography color="text.secondary">
            Faturamento • NFS-e • Baixas • Impacto de Glosas — Tenant: {tenantName}
          </Typography>
        </Box>
      </Stack>

      <Alert severity={isOffline ? 'warning' : 'info'} sx={{ mb: 2.5 }}>
        {isOffline
          ? 'Backend offline ou sem contas a receber. Verifique a conexão com o servidor.'
          : 'Dados vindos do backend (GET /financeiro/receber). A geração real de Contas a Receber acontece automaticamente quando você aprova medições no módulo Medições.'}
      </Alert>

      {/* KPIs Executivos — mantidos exatamente excelentes como antes */}
      <Grid container spacing={{ xs: 1.5, sm: 2 }} sx={{ mb: 2.25 }}>
        <Grid item xs={12} sm={4}>
          <Paper sx={{ p: 2, borderRadius: 2.5, bgcolor: '#e3f2fd', height: '100%' }}>
            <Typography variant="overline" sx={{ fontSize: '0.68rem', fontWeight: 600 }}>Total em Aberto</Typography>
            <Typography sx={{ mt: 0.5, fontSize: '1.45rem', fontWeight: 700, color: 'primary.main' }}>
              R$ {kpis.totalAberto.toLocaleString('pt-BR')}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Paper sx={{ p: 2, borderRadius: 2.5, bgcolor: '#e8f5e9', height: '100%' }}>
            <Typography variant="overline" sx={{ fontSize: '0.68rem', fontWeight: 600 }}>Já Recebido (período)</Typography>
            <Typography sx={{ mt: 0.5, fontSize: '1.45rem', fontWeight: 700, color: 'success.main' }}>
              R$ {kpis.totalRecebido.toLocaleString('pt-BR')}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Paper sx={{ p: 2, borderRadius: 2.5, bgcolor: kpis.inadimplentes > 0 ? '#ffebee' : '#f5f5f5', height: '100%' }}>
            <Typography variant="overline" sx={{ fontSize: '0.68rem', fontWeight: 600 }}>Títulos Inadimplentes (&gt;30 dias)</Typography>
            <Typography sx={{ mt: 0.5, fontSize: '1.45rem', fontWeight: 700, color: kpis.inadimplentes > 0 ? 'error.main' : 'text.primary' }}>
              {kpis.inadimplentes}
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Painel retenções / DARF (SPEC §16 compliance) */}
      <Paper sx={{ p: 2, mb: 2, borderRadius: 2.5, border: '1px solid', borderColor: 'divider' }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>Retenções e DARF</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          Simule retenções federais/municipais sobre o valor de serviço e gere prévia DARF por retenção persistida.
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} sx={{ mb: 2 }}>
          <TextField
            label="Valor do serviço (R$)"
            type="number"
            size="small"
            value={valorRetencaoSim}
            onChange={(e) => setValorRetencaoSim(e.target.value)}
            sx={{ width: 200 }}
          />
          <Button
            variant="outlined"
            disabled={calcularRetencoes.isPending}
            onClick={async () => {
              try {
                const rows = await calcularRetencoes.mutateAsync({ valorServico: Number(valorRetencaoSim) })
                setRetencoesPreview(rows)
                showNotification(`${rows.length} retenções calculadas`, 'success')
              } catch {
                showNotification('Falha ao calcular retenções', 'error')
              }
            }}
          >
            Calcular retenções
          </Button>
        </Stack>
        {retencoesPreview.length > 0 && (
          <Stack spacing={1}>
            {retencoesPreview.map((r: any, i: number) => (
              <Stack key={i} direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                <Chip size="small" label={r.tipo} color="primary" variant="outlined" />
                <Typography variant="body2">
                  Alíquota {Number(r.aliquota || 0).toFixed(2)}% — R$ {Number(r.valorRetido || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </Typography>
                {r.id && (
                  <Button
                    size="small"
                    variant="text"
                    disabled={gerarDarf.isPending}
                    onClick={async () => {
                      try {
                        const darf = await gerarDarf.mutateAsync(r.id)
                        showNotification(darf?.preview || darf?.message || 'DARF gerado', 'success')
                      } catch {
                        showNotification('Informe uma retenção já persistida (após emitir NFS-e)', 'info')
                      }
                    }}
                  >
                    Gerar DARF
                  </Button>
                )}
              </Stack>
            ))}
          </Stack>
        )}
      </Paper>

      {/* Filtros (preservados + limpos) */}
      <Stack direction="row" spacing={2} sx={{ mb: 2 }} alignItems="center">
        <TextField
          select
          label="Status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          sx={{ minWidth: 180 }}
        >
          <MenuItem value="">Todos</MenuItem>
          <MenuItem value="PENDENTE">PENDENTE</MenuItem>
          <MenuItem value="NFSE_EMITIDA">NFSE_EMITIDA</MenuItem>
          <MenuItem value="RECEBIDO_TOTAL">RECEBIDO_TOTAL</MenuItem>
          <MenuItem value="INADIMPLENTE">INADIMPLENTE</MenuItem>
        </TextField>

        <TextField
          select
          label="Contrato"
          value={contractFilter}
          onChange={(e) => setContractFilter(e.target.value)}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">Todos</MenuItem>
          {contracts.map((c: any) => (
            <MenuItem key={c.id} value={c.numero}>{c.numero} — {c.orgao}</MenuItem>
          ))}
        </TextField>

        <Button variant="outlined" onClick={() => { setStatusFilter(''); setContractFilter('') }}>
          Limpar Filtros
        </Button>

        <Button variant="outlined" startIcon={<RefreshCw size={16} />} onClick={handleRefreshAll} disabled={isLoading}>
          Atualizar
        </Button>
      </Stack>

      {/* Grid com loading/empty/error states melhorados (EnterpriseDataGrid) */}
      <EnterpriseDataGrid
        title="Contas a Receber — Faturamento de Serviços de Terceirização"
        rowData={filteredData}
        columnDefs={columns}
        onRefresh={handleRefreshAll}
        loading={isLoadingGrid}
        error={isError ? (error?.message || 'Erro ao carregar contas a receber do backend.') : null}
        emptyMessage="Nenhuma conta a receber. Aprove medições no módulo Medições (dispara geração automática no backend)."
        height={520}
      />

      <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
        Fluxo real: aprovação de medição → AR + NFS-e automáticos. Cobrança PIX/Boleto via POST /receber/{'{id}'}/cobranca.
      </Typography>

      <Dialog open={cobrancaModal.open} onClose={() => setCobrancaModal({ open: false, data: null })} maxWidth="sm" fullWidth>
        <DialogTitle>Cobrança {cobrancaModal.data?.tipo || ''} emitida</DialogTitle>
        <DialogContent>
          {cobrancaModal.data?.tipo === 'PIX' && (
            <TextField
              fullWidth
              multiline
              rows={4}
              label="PIX Copia e Cola"
              value={cobrancaModal.data?.codigoPix || cobrancaModal.data?.qrCodePayload || ''}
              InputProps={{ readOnly: true }}
              sx={{ mt: 1 }}
            />
          )}
          {cobrancaModal.data?.tipo === 'BOLETO' && (
            <TextField
              fullWidth
              label="Linha digitável"
              value={cobrancaModal.data?.linhaDigitavel || ''}
              InputProps={{ readOnly: true }}
              sx={{ mt: 1 }}
            />
          )}
          <Typography variant="body2" sx={{ mt: 2 }}>
            Valor: R$ {Number(cobrancaModal.data?.valor || 0).toLocaleString('pt-BR')} • Vencimento: {cobrancaModal.data?.vencimento || '—'}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCobrancaModal({ open: false, data: null })}>Fechar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
