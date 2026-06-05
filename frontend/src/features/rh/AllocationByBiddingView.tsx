/**
 * Visão de Alocação por Licitação — usa /biddings/{id}/allocation-summary (sem proxy de contratos)
 */
import { useState, useEffect, useMemo } from 'react'
import MassAllocationDialog from './MassAllocationDialog'
import { Box, Typography, Paper, Stack, Alert, Chip, Button, Grid, LinearProgress } from '@mui/material'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as ReTooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { Download, Users, AlertTriangle } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useBiddings, useBiddingAllocation } from '../../api/hooks/useBiddings'
import { useDailySummary } from '../../api/hooks/usePonto'
import { useNotification } from '../../components/NotificationProvider'
import type { Bidding } from '../../api/types'

export default function AllocationByBiddingView() {
  const { data: biddings = [], isLoading: biddingsLoading } = useBiddings()
  const { showNotification } = useNotification()
  const [selectedBidding, setSelectedBidding] = useState<Bidding | null>(null)
  const [massAllocationOpen, setMassAllocationOpen] = useState(false)

  const { data: allocation, isLoading: allocationLoading, refetch } = useBiddingAllocation(selectedBidding?.id)

  const contractId = allocation?.contractId as string | undefined
  const today = new Date().toISOString().slice(0, 10)
  const { data: dailySummary } = useDailySummary(contractId, today)

  useEffect(() => {
    const handler = () => setMassAllocationOpen(true)
    window.addEventListener('open-mass-allocation', handler)
    return () => window.removeEventListener('open-mass-allocation', handler)
  }, [])

  const currentAllocations = useMemo(() => {
    const linhas = (allocation?.linhas as any[]) || []
    return linhas.map((l) => ({
      id: l.postoPlanejadoId || l.postoExecutadoId || l.nome,
      posto: l.nome || 'Posto sem nome',
      local: l.localExecucao || '—',
      municipio: l.municipioExecucao || '—',
      titular: l.alocacoesAtivas > 0 ? `${l.alocacoesAtivas} alocado(s)` : '—',
      cobertura: l.coberturaPct ?? 0,
      status: l.status || 'DESCOBERTO',
      valor: l.valorMensalPlanejado != null ? Number(l.valorMensalPlanejado) : 0,
      tipo: l.alocacoesAtivas > 0 ? 'TITULAR' : '—',
    }))
  }, [allocation])

  const realPontoCoverage = dailySummary?.coverage_percent != null
    ? Math.round(Number(dailySummary.coverage_percent))
    : null

  const totalPostos = allocation?.postosPlanejados ?? currentAllocations.length
  const coberturaMedia = allocation?.coberturaMediaPct ?? (
    currentAllocations.length > 0
      ? Math.round(currentAllocations.reduce((s, a) => s + a.cobertura, 0) / currentAllocations.length)
      : 0
  )
  const custoPlanejado = currentAllocations.reduce((s, a) => s + (a.valor || 0), 0)
  const gaps = currentAllocations.filter((a) => a.cobertura < 100).length
  const descobertos = currentAllocations.filter((a) => a.cobertura === 0).length

  const coberturaChart = currentAllocations.map((a) => ({
    posto: a.posto.length > 18 ? a.posto.substring(0, 17) + '…' : a.posto,
    cobertura: a.cobertura,
    valor: a.valor,
  }))

  const statusData = [
    { name: 'OK', value: currentAllocations.filter((a) => a.status === 'OK').length, fill: '#2e7d32' },
    { name: 'PARCIAL', value: currentAllocations.filter((a) => a.status === 'PARCIAL').length, fill: '#ed6c02' },
    { name: 'DESCOBERTO', value: currentAllocations.filter((a) => a.status === 'DESCOBERTO').length, fill: '#c62828' },
  ].filter((d) => d.value > 0)

  const handleExportCSV = () => {
    if (!currentAllocations.length) return
    const headers = ['Posto', 'Local', 'Município', 'Alocações', 'Cobertura %', 'Status', 'Valor Mensal']
    const rows = currentAllocations.map((a) => [
      a.posto, a.local, a.municipio, a.titular, a.cobertura, a.status, a.valor || '',
    ])
    const csv = [headers, ...rows].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(';')).join('\n')
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `alocacao_${selectedBidding?.editalNumero || selectedBidding?.processoNumero || 'licitacao'}_${today}.csv`
    link.click()
    URL.revokeObjectURL(url)
    showNotification('Export CSV gerado com sucesso', 'success')
  }

  const loading = biddingsLoading || allocationLoading

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Visão de Alocação por Licitação</Typography>
          <Typography color="text.secondary">
            Postos planejados da licitação × contrato vinculado × alocações ativas (EmployeeAssignment)
          </Typography>
        </Box>
        {selectedBidding && (
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<Download size={16} />} onClick={handleExportCSV}>
              Exportar CSV
            </Button>
            <Button variant="contained" startIcon={<Users size={16} />} onClick={() => setMassAllocationOpen(true)}>
              Abrir Alocação em Massa
            </Button>
          </Stack>
        )}
      </Stack>

      <Stack direction="row" spacing={1.5} mb={1} flexWrap="wrap">
        {biddings.length > 0 ? biddings.map((b) => (
          <Chip
            key={b.id}
            label={`${b.editalNumero || b.processoNumero} • ${b.orgao?.substring(0, 18) || ''}`}
            onClick={() => setSelectedBidding(b)}
            color={selectedBidding?.id === b.id ? 'primary' : 'default'}
            clickable
            sx={{ fontWeight: 500 }}
          />
        )) : (
          <Alert severity="info">Nenhuma licitação encontrada. Cadastre em Licitações para visualizar alocações.</Alert>
        )}
      </Stack>

      {loading && <LinearProgress sx={{ mb: 2, borderRadius: 1 }} />}

      {!selectedBidding && biddings.length > 0 && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Selecione uma licitação acima para carregar o resumo de alocação via <strong>/allocation-summary</strong>.
        </Alert>
      )}

      {selectedBidding && (
        <>
          <Paper sx={{ p: 2.5, mb: 2.5, borderRadius: 3 }}>
            <Typography variant="h6" gutterBottom>
              {selectedBidding.editalNumero || selectedBidding.processoNumero} — {selectedBidding.orgao}
            </Typography>
            {allocation?.contractNumero && (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                Contrato vinculado: <strong>{allocation.contractNumero}</strong>
              </Typography>
            )}

            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={6} sm={3}>
                <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center', borderRadius: 2 }}>
                  <Typography variant="caption" color="text.secondary">Postos Planejados</Typography>
                  <Typography variant="h4" fontWeight={700}>{totalPostos}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center', borderRadius: 2, bgcolor: coberturaMedia >= 90 ? '#e8f5e9' : '#fff3e0' }}>
                  <Typography variant="caption" color="text.secondary">Cobertura Média</Typography>
                  <Typography variant="h4" fontWeight={700} color={coberturaMedia >= 90 ? 'success.main' : 'warning.main'}>{coberturaMedia}%</Typography>
                  {realPontoCoverage !== null && (
                    <Typography variant="caption" sx={{ display: 'block', mt: 0.5 }} color="text.secondary">
                      Ponto hoje: <strong>{realPontoCoverage}%</strong>
                    </Typography>
                  )}
                </Paper>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center', borderRadius: 2 }}>
                  <Typography variant="caption" color="text.secondary">Custo Mensal Planejado</Typography>
                  <Typography variant="h5" fontWeight={700}>R$ {custoPlanejado.toLocaleString('pt-BR')}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center', borderRadius: 2, borderColor: gaps > 0 ? '#ffcdd2' : '#c8e6c9' }}>
                  <Typography variant="caption" color="text.secondary">Gaps</Typography>
                  <Typography variant="h4" fontWeight={700} color={gaps > 0 ? 'error.main' : 'success.main'}>
                    {gaps} <Typography component="span" variant="caption">({descobertos} sem ninguém)</Typography>
                  </Typography>
                </Paper>
              </Grid>
            </Grid>

            {!contractId && !allocationLoading && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                Licitação sem contrato vinculado. Crie o contrato a partir da licitação para materializar postos e alocações.
              </Alert>
            )}
            {contractId && totalPostos === 0 && !allocationLoading && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                Nenhum posto planejado nesta licitação. Cadastre postos no detalhe da licitação ou importe planilha Excel.
              </Alert>
            )}
            {contractId && totalPostos > 0 && (allocation?.alocacoesAtivas ?? 0) === 0 && (
              <Alert severity="info" sx={{ mt: 2 }} icon={<AlertTriangle />}>
                Postos existem, mas nenhuma alocação ativa. Use &quot;Alocação em Massa&quot; para vincular funcionários.
              </Alert>
            )}
          </Paper>

          <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
            <Grid item xs={12} md={7}>
              <Paper sx={{ p: 2, borderRadius: 3, height: 320 }}>
                <Typography variant="subtitle2" gutterBottom>Cobertura por Posto</Typography>
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={coberturaChart}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="posto" tick={{ fontSize: 11 }} />
                    <YAxis domain={[0, 100]} />
                    <ReTooltip />
                    <Bar dataKey="cobertura" fill="#1976d2" radius={4} />
                  </BarChart>
                </ResponsiveContainer>
              </Paper>
            </Grid>
            <Grid item xs={12} md={5}>
              <Paper sx={{ p: 2, borderRadius: 3, height: 320 }}>
                <Typography variant="subtitle2" gutterBottom>Distribuição de Status</Typography>
                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Pie dataKey="value" data={statusData} cx="50%" cy="48%" innerRadius={52} outerRadius={92} label>
                      {statusData.map((entry, idx) => (
                        <Cell key={idx} fill={entry.fill} />
                      ))}
                    </Pie>
                    <ReTooltip />
                  </PieChart>
                </ResponsiveContainer>
                <Stack direction="row" justifyContent="center" spacing={2} sx={{ mt: -1 }}>
                  {statusData.map((s, i) => (
                    <Chip key={i} size="small" label={`${s.name}: ${s.value}`} sx={{ bgcolor: s.fill, color: 'white' }} />
                  ))}
                </Stack>
              </Paper>
            </Grid>
          </Grid>

          <Paper sx={{ p: 2, borderRadius: 3 }}>
            <Typography variant="subtitle1" gutterBottom sx={{ px: 1 }}>
              Detalhamento — Postos da Licitação
            </Typography>
            <EnterpriseDataGrid
              title=""
              rowData={currentAllocations}
              columnDefs={[
                { headerName: 'Posto', field: 'posto', flex: 1.6 },
                { headerName: 'Local', field: 'local', flex: 1.1 },
                { headerName: 'Município', field: 'municipio' },
                { headerName: 'Alocado', field: 'titular', flex: 1 },
                {
                  headerName: 'Cobertura',
                  field: 'cobertura',
                  width: 110,
                  cellRenderer: (p: any) => (
                    <Chip label={`${p.value}%`} size="small" color={p.value === 100 ? 'success' : p.value > 0 ? 'warning' : 'error'} />
                  ),
                },
                {
                  headerName: 'Status',
                  field: 'status',
                  width: 130,
                  cellRenderer: (p: any) => (
                    <Chip label={p.value} size="small" color={p.value === 'OK' ? 'success' : p.value === 'PARCIAL' ? 'warning' : 'error'} />
                  ),
                },
                { headerName: 'Valor', field: 'valor', valueFormatter: (p: any) => (p.value ? `R$ ${p.value.toLocaleString('pt-BR')}` : '—') },
              ]}
              height={340}
            />
          </Paper>
        </>
      )}

      <MassAllocationDialog
        open={massAllocationOpen}
        onClose={() => setMassAllocationOpen(false)}
        biddingId={selectedBidding?.id}
        onSuccess={() => {
          setMassAllocationOpen(false)
          refetch()
          showNotification('Alocações atualizadas', 'success')
        }}
      />
    </Box>
  )
}
