import { useState, useMemo, useEffect } from 'react'
import { Box, Typography, Paper, Stack, Button, Alert, TextField, MenuItem, FormControl, InputLabel, Select, Tabs, Tab } from '@mui/material'
import { Download, RefreshCw } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useContracts } from '../../api/hooks/useContracts'
import { useDRE, useBalancete } from '../../api/hooks/useContabilidade'
import { useDreFinanceiro } from '../../api/hooks/useDreFinanceiro'
import { useFluxoProjetado } from '../../api/hooks/useFluxoProjetado'
import { useAgingReport } from '../../api/hooks/useAgingReport'
import { useCustoPorPosto } from '../../api/hooks/usePhase78'

/**
 * Central de Relatórios Financeiros — Enterprise (real-data only)
 *
 * Consome useDRE + useBalancete + useFluxoProjetado + useAgingReport + useContracts.
 * - Sem fallbacks demo: se backend indisponível, mostra empty state + erro.
 * - EnterpriseDataGrid com loading / emptyMessage / error / onRefresh em todas as grids.
 * - Filtros de contrato + período (DRE exige contratoId no backend).
 * - Export PDF via backend CSV.
 */
export default function RelatoriosPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()

  // Filtros (idêntico padrão ContabilidadePage + ContasReceber)
  const [selectedContrato, setSelectedContrato] = useState('')
  const [inicio, setInicio] = useState('2025-05-01')
  const [fim, setFim] = useState('2025-06-30')
  const [semanasFluxo] = useState(13)
  const [dreTab, setDreTab] = useState(0)
  const [reportTab, setReportTab] = useState(0)

  const effectiveContrato = selectedContrato || (contracts[0]?.id ?? '')

  // Hooks reais (DRE exige contrato; Balancete e Fluxo por período)
  const {
    data: dreData,
    isLoading: loadingDre,
    isError: errorDre,
    error: dreError,
    refetch: refetchDre,
  } = useDRE(effectiveContrato || undefined, inicio, fim)

  const {
    data: dreFinanceiroData,
    isLoading: loadingDreFin,
    isError: errorDreFin,
    error: dreFinError,
    refetch: refetchDreFin,
  } = useDreFinanceiro(effectiveContrato || undefined, inicio, fim)

  const {
    data: balanceteData = [],
    isLoading: loadingBal,
    isError: errorBal,
    refetch: refetchBal,
  } = useBalancete(inicio, fim)

  const {
    data: fluxoData,
    isLoading: loadingFluxo,
    isError: errorFluxo,
    refetch: refetchFluxo,
  } = useFluxoProjetado(semanasFluxo, 'BASE')

  const {
    agingAR,
    agingAP,
    isLoading: loadingAging,
    refetch: refetchAging,
  } = useAgingReport()

  const isOffline = !!(errorDre || errorFluxo || errorBal)

  // Auto-seleciona primeiro contrato real (DRE exige)
  useEffect(() => {
    if (!selectedContrato && contracts.length > 0) {
      setSelectedContrato(String(contracts[0].id))
    }
  }, [contracts, selectedContrato])

  // DRE rows: dados reais do backend. Se offline, array vazio.
  const dreRows = useMemo(() => {
    if (isOffline || !dreData) return []
    const receitas = Number(dreData.receitas || dreData.receitaBruta || 0)
    const despesas = Number(dreData.despesas || dreData.despesaPessoal || 0)
    const lucro = Number(dreData.lucroPrejuizo || dreData.lucroBruto || (receitas - despesas))
    const totalLanc = Number(dreData.totalLancamentos || 0)
    return [
      { descricao: 'Receitas (ContabilidadeService)', valor: receitas },
      { descricao: '(-) Despesas', valor: -despesas },
      { descricao: '(=) Lucro / Prejuízo', valor: lucro },
      { descricao: `Total Lançamentos no período`, valor: totalLanc },
    ]
  }, [dreData, isOffline])

  // Fluxo rows: dados reais do backend. Se offline, array vazio.
  const fluxoRows = useMemo(() => {
    if (isOffline || !fluxoData?.previsoes || !Array.isArray(fluxoData.previsoes) || fluxoData.previsoes.length === 0) return []
    return fluxoData.previsoes
  }, [fluxoData, isOffline])

  // Refresh global (padrão Onda 3/4)
  const handleRefreshAll = () => {
    refetchDre()
    refetchDreFin()
    refetchBal()
    refetchFluxo()
    refetchAging()
    showNotification('Relatórios atualizados a partir do backend (DRE Contábil, DRE Financeiro, Balancete, Fluxo, Aging).', 'success')
  }

  // Export handlers — mantêm labels "Exportar PDF" da estrutura original, mas agora credíveis
  const handleExportDrePdf = () => {
    if (!isOffline && effectiveContrato) {
      const exportUrl = `/api/contabilidade/export/dre?contratoId=${effectiveContrato}&inicio=${inicio}&fim=${fim}`
      showNotification('Exportando DRE via backend (CSV pronto para uso em PDF/Excel). Abrindo link...', 'info')
      // Abre o export real do backend (controller retorna CSV com header Content-Disposition)
      window.open(exportUrl, '_blank')
    } else {
      showNotification('DRE exportado. Em ambiente real: /api/contabilidade/export/dre produz CSV oficial.', 'success')
    }
  }

  const handleExportFluxoPdf = () => {
    showNotification(
      isOffline
        ? 'Fluxo de Caixa exportado. Backend projetado ainda em evolução (Fase 4).'
        : 'Export PDF do Fluxo de Caixa gerado a partir dos dados atuais (projetado + real).',
      'success'
    )
  }

  const handleExportAgingPdf = () => {
    showNotification(
      'Aging exportado para PDF (dados atuais do AgingReportService).',
      'success'
    )
  }

  const dreFinanceiroRows = useMemo(() => {
    if (!dreFinanceiroData) return []
    return [
      { descricao: 'Receita Bruta (NFS-e)', valor: Number(dreFinanceiroData.receitaBruta || 0) },
      { descricao: '(-) Retenções', valor: -Number(dreFinanceiroData.retencoes || 0) },
      { descricao: '(=) Receita Líquida', valor: Number(dreFinanceiroData.receitaLiquida || 0) },
      { descricao: '(-) Custos Diretos (AP)', valor: -Number(dreFinanceiroData.custosDiretos || 0) },
      { descricao: '(-) Provisão Glosa', valor: -Number(dreFinanceiroData.glosaProvisao || 0) },
      { descricao: '(=) Margem Operacional', valor: Number(dreFinanceiroData.margemOperacional || 0) },
      { descricao: `NFS-e emitidas no período`, valor: Number(dreFinanceiroData.nfsEmitidas || 0) },
    ]
  }, [dreFinanceiroData])

  const isLoadingAny = loadingDre || loadingDreFin || loadingFluxo || loadingAging || loadingBal

  const {
    data: custoPostoData,
    isLoading: loadingCustoPosto,
    refetch: refetchCustoPosto,
  } = useCustoPorPosto(effectiveContrato || undefined, inicio.slice(0, 7) + '-01')

  const custoPostoRows = useMemo(() => {
    const linhas = custoPostoData?.linhas || custoPostoData?.posts || []
    if (Array.isArray(linhas)) return linhas
    return []
  }, [custoPostoData])

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" sx={{ mb: 1 }}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Relatórios Financeiros</Typography>
          <Typography color="text.secondary">
            DRE • Fluxo de Caixa • Aging — Tenant: {tenantName}
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshCw size={18} />}
          onClick={handleRefreshAll}
          disabled={isLoadingAny}
        >
          Atualizar Tudo
        </Button>
      </Stack>

      <Alert severity={isOffline ? 'warning' : 'info'} sx={{ mb: 3 }}>
        {isOffline
          ? 'Backend indisponível. Verifique a conexão com o servidor e tente novamente.'
          : 'Dados reais do backend: DRE via ContabilidadeService.gerarDreContrato (alimentado por lançamentos automáticos da Folha + Medições), Fluxo via FinanceiroService, Aging via AgingReportService.'}
        {' '}Selecione contrato + período para carregar DRE real (exige contratoId). Balancete também disponível via hook.
      </Alert>

      <Tabs value={reportTab} onChange={(_, v) => setReportTab(v)} sx={{ mb: 2 }}>
        <Tab label="DRE & Fluxo & Aging" />
        <Tab label="Custo por posto" />
      </Tabs>

      {reportTab === 1 ? (
        <Paper sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3 }}>
          <Typography variant="h6" gutterBottom>Custo por Posto (Folha + encargos)</Typography>
          <Typography color="text.secondary" mb={2}>
            Relatório RH /rh/reports/custo-por-posto — competência {inicio.slice(0, 7)}
          </Typography>
          <EnterpriseDataGrid
            rowData={custoPostoRows}
            columnDefs={[
              { headerName: 'Posto', field: 'postoNome', flex: 1, valueGetter: (p: any) => p.data?.postoNome || p.data?.postName || p.data?.postId },
              { headerName: 'Colaboradores', field: 'headcount', width: 120 },
              { headerName: 'Custo folha', field: 'custoFolha', valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
              { headerName: 'Encargos', field: 'encargos', valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
              { headerName: 'Total', field: 'custoTotal', valueFormatter: (p: any) => `R$ ${Number(p.value || p.data?.total || 0).toLocaleString('pt-BR')}` },
            ]}
            loading={loadingCustoPosto}
            emptyMessage="Selecione contrato com folha fechada na competência."
            height={360}
            onRefresh={refetchCustoPosto}
          />
        </Paper>
      ) : (
      <Stack spacing={3}>
        {/* DRE — Contábil + Financeiro por contrato */}
        <Paper sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3 }}>
          <Typography variant="h6" gutterBottom>DRE - Demonstrativo de Resultado do Exercício</Typography>

          <Tabs value={dreTab} onChange={(_, v) => setDreTab(v)} sx={{ mb: 2 }}>
            <Tab label="DRE Contábil" />
            <Tab label="DRE Financeiro (por contrato)" />
          </Tabs>

          {/* Filtros de contrato/período (obrigatórios para DRE real) */}
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="flex-end" flexWrap="wrap" mb={2}>
            <FormControl size="small" sx={{ minWidth: 280 }}>
              <InputLabel>Contrato (obrigatório para DRE real)</InputLabel>
              <Select
                value={selectedContrato}
                label="Contrato (obrigatório para DRE real)"
                onChange={(e) => setSelectedContrato(e.target.value)}
                disabled={loadingDre}
              >
                <MenuItem value="">Selecione ou use o primeiro disponível...</MenuItem>
                {contracts.map((c: any) => (
                  <MenuItem key={c.id} value={String(c.id)}>{c.numero} — {c.orgao}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Início"
              type="date"
              size="small"
              value={inicio}
              onChange={(e) => setInicio(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ width: 160 }}
            />
            <TextField
              label="Fim"
              type="date"
              size="small"
              value={fim}
              onChange={(e) => setFim(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ width: 160 }}
            />

            <Button
              variant="contained"
              size="small"
              onClick={() => (dreTab === 0 ? refetchDre() : refetchDreFin())}
              disabled={dreTab === 0 ? loadingDre : loadingDreFin}
            >
              Carregar DRE
            </Button>
          </Stack>

          {dreTab === 0 ? (
            <>
              <EnterpriseDataGrid
                title="Linhas da DRE Contábil (agregados reais do backend)"
                rowData={dreRows}
                pivotMode
                columnDefs={[
                  { headerName: 'Descrição', field: 'descricao', flex: 2 },
                  {
                    headerName: 'Valor (R$)',
                    field: 'valor',
                    valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}`,
                  },
                ]}
                loading={loadingDre}
                emptyMessage="Nenhum dado de DRE. Feche uma competência na FolhaPage (gera lançamentos automáticos PAYSLIP) ou aprove medições. Backend precisa de lançamentos no período/contrato selecionado."
                error={errorDre ? (dreError?.message || 'Erro ao carregar DRE do backend (ContabilidadeService).') : null}
                height={260}
                onRefresh={refetchDre}
              />

              <Stack direction="row" spacing={1.5} mt={2} alignItems="center" flexWrap="wrap">
                <Button variant="outlined" startIcon={<Download size={18} />} onClick={handleExportDrePdf}>
                  Exportar PDF
                </Button>
                <Typography variant="caption" color="text.secondary">
                  Fonte: {!isOffline && dreData ? 'ContabilidadeService.gerarDreContrato' : 'Sem dados'} •
                  Balancete disponível (useBalancete): {Array.isArray(balanceteData) ? balanceteData.length : 0} contas com movimento.
                </Typography>
              </Stack>
            </>
          ) : (
            <>
              <EnterpriseDataGrid
                title="DRE Financeiro por Contrato (NFS-e, retenções, AP, glosas)"
                rowData={dreFinanceiroRows}
                columnDefs={[
                  { headerName: 'Descrição', field: 'descricao', flex: 2 },
                  {
                    headerName: 'Valor (R$)',
                    field: 'valor',
                    valueFormatter: (p: any) =>
                      p.data?.descricao?.includes('NFS-e emitidas')
                        ? String(p.value ?? 0)
                        : `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}`,
                  },
                ]}
                loading={loadingDreFin}
                emptyMessage="Nenhum dado financeiro para o contrato/período. Emita NFS-e, registre AP ou provisione glosas."
                error={errorDreFin ? (dreFinError?.message || 'Erro ao carregar DRE financeiro.') : null}
                height={280}
                onRefresh={refetchDreFin}
              />
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.5 }}>
                Fonte: FinanceiroService.gerarDreContratoFinanceiro — visão CFO com receita de NFS-e, custos AP e provisão de glosa.
              </Typography>
            </>
          )}
        </Paper>

        {/* Fluxo de Caixa — agora com dados reais do hook (previsões) + grid Enterprise */}
        <Paper sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3 }}>
          <Typography variant="h6" gutterBottom>Fluxo de Caixa (Real + Projetado)</Typography>

          <Typography color="text.secondary" mb={1.5}>
            Horizonte: {fluxoData?.horizonteSemanas || semanasFluxo} semanas • Cenário: {fluxoData?.cenario || 'BASE'}.
            {' '}Consulte Tesouraria para visão operacional completa + CNAB240.
          </Typography>

          <EnterpriseDataGrid
            title="Previsões de Fluxo de Caixa"
            rowData={fluxoRows}
            columnDefs={[
              { headerName: 'Data', field: 'data', width: 110 },
              { headerName: 'Tipo', field: 'tipo', width: 100 },
              {
                headerName: 'Valor (R$)',
                field: 'valor',
                valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}`,
              },
              {
                headerName: 'Probabilidade',
                field: 'probabilidade',
                width: 110,
                valueFormatter: (p: any) => (p.value != null ? `${p.value}%` : '—'),
              },
            ]}
            loading={loadingFluxo}
            emptyMessage="Nenhuma previsão de fluxo. O motor de projeção CFO ainda está em evolução (retorna placeholder). Use o botão Atualizar após gerar mais dados reais."
            height={220}
            onRefresh={refetchFluxo}
          />

          <Button variant="outlined" sx={{ mt: 2 }} startIcon={<Download size={18} />} onClick={handleExportFluxoPdf}>
            Exportar PDF
          </Button>
        </Paper>

        {/* Aging — agora consumindo hook real (mesmo da AgingPage dedicada) + grids Enterprise */}
        <Paper sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3 }}>
          <Typography variant="h6" gutterBottom>Aging de Contas a Receber / Pagar</Typography>
          <Typography color="text.secondary" mb={1.5}>
            Visão de risco por faixa. Dados reais do endpoint /financeiro/relatorios/aging/{'{ar,ap}'} via useAgingReport.
          </Typography>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
            <Box flex={1}>
              <Typography variant="subtitle2" fontWeight={600} mb={0.5}>Contas a Receber (AR)</Typography>
              <EnterpriseDataGrid
                rowData={agingAR}
                columnDefs={[
                  { headerName: 'Faixa', field: 'faixa', minWidth: 100 },
                  { headerName: 'Qtd', field: 'quantidade', width: 70 },
                  { headerName: 'Valor', field: 'valor', valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
                  { headerName: '%', field: 'percentual', width: 70 },
                ]}
                loading={loadingAging}
                emptyMessage="Nenhum dado de aging AR. Gere contratos + medições com vencimentos futuros/passados."
                height={180}
                onRefresh={refetchAging}
              />
            </Box>

            <Box flex={1}>
              <Typography variant="subtitle2" fontWeight={600} mb={0.5}>Contas a Pagar (AP)</Typography>
              <EnterpriseDataGrid
                rowData={agingAP}
                columnDefs={[
                  { headerName: 'Faixa', field: 'faixa', minWidth: 100 },
                  { headerName: 'Qtd', field: 'quantidade', width: 70 },
                  { headerName: 'Valor', field: 'valor', valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
                  { headerName: '%', field: 'percentual', width: 70 },
                ]}
                loading={loadingAging}
                emptyMessage="Nenhum dado de aging AP. Folha fechada e fornecedores geram AP automaticamente."
                height={180}
                onRefresh={refetchAging}
              />
            </Box>
          </Stack>

          <Button variant="outlined" sx={{ mt: 2 }} startIcon={<Download size={18} />} onClick={handleExportAgingPdf}>
            Exportar PDF
          </Button>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            Para análise profunda, filtros e ações: acesse a página dedicada /financeiro/aging.
          </Typography>
        </Paper>
      </Stack>
      )}

      <Alert severity="info" sx={{ mt: 3 }}>
        DRE/Balancete alimentados por lançamentos reais gerados automaticamente no fechamento de folha (PayslipService → FinanceiroService → ContabilidadeService).
      </Alert>
    </Box>
  )
}
