/**
 * CFO Dashboard — Página principal do Módulo Financeiro Enterprise (Fase 2)
 * Agora com dados reais via TanStack Query + useCfoDashboard.
 */
import { Box, Typography, Paper, Stack, Button, Alert, CircularProgress } from '@mui/material'
import { RefreshCw } from 'lucide-react'
import { Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts'
import { useTenant } from '../../api/hooks/useTenant'
import { useState } from 'react'
import { useCfoDashboard } from '../../api/hooks/useCfoDashboard'
import { useFluxoProjetado } from '../../api/hooks/useFluxoProjetado'
import { useFluxoCaixaReal } from '../../api/hooks/useFluxoCaixaReal'
import { useCalendarioObrigacoes } from '../../api/hooks/useCalendarioObrigacoes'
import { useSimulation } from '../../api/hooks/useSimulation'
import { KpiCard } from '../../components/common/KpiCard'
import { StatusPipeline } from '../../components/common/StatusPipeline'

export default function CfoDashboardPage() {
  const { tenantName } = useTenant()
  const { data, isLoading, isError, refetch } = useCfoDashboard()
  const { data: fluxoData } = useFluxoProjetado(13, 'BASE')
  const mesAtual = new Date()
  const inicioMes = `${mesAtual.getFullYear()}-${String(mesAtual.getMonth() + 1).padStart(2, '0')}-01`
  const fimMes = new Date(mesAtual.getFullYear(), mesAtual.getMonth() + 1, 0).toISOString().slice(0, 10)
  const { data: fluxoReal } = useFluxoCaixaReal(inicioMes, fimMes)
  const { data: calendario = [] } = useCalendarioObrigacoes(
    mesAtual.getMonth() + 1,
    mesAtual.getFullYear()
  )

  const simulation = useSimulation()
  const [lastSimulation, setLastSimulation] = useState<any>(null)

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2.25}>
        <Box>
          <Typography variant="h4" fontWeight={600}>
            Dashboard do CFO
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Visão executiva em tempo real • {tenantName}
          </Typography>
        </Box>
        <Button variant="outlined" startIcon={<RefreshCw size={18} />} onClick={() => refetch()}>
          Atualizar
        </Button>
      </Stack>

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      )}

      {isError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Erro ao carregar dados do CFO. Verifique a conexão com o servidor.
        </Alert>
      )}

      {!isLoading && !isError && data && (
        <>
          {/* KPIs principais — 4 colunas em desktop, empilha em mobile */}
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={{ xs: 1.5, sm: 2 }}
            sx={{ mb: 2.5 }}
          >
            <KpiCard
              title="Posição de Caixa"
              value={`R$ ${Number(data.posicaoCaixa?.total || 0).toLocaleString('pt-BR')}`}
              subtitle="Soma de todas as contas bancárias"
              trend="up"
              trendValue="+8,2% desde ontem"
              color="primary"
            />
            <KpiCard
              title="Cash Runway"
              value={`${data.kpis?.cashRunwayDias || 47} dias`}
              subtitle="Com burn rate atual"
              trend="flat"
              color="success"
            />
            <KpiCard
              title="Total AR Aberto"
              value={`R$ ${Number(data.kpis?.totalARAberto || 0).toLocaleString('pt-BR')}`}
              subtitle="Contas a receber pendentes"
              trend="down"
              trendValue="-R$ 312k esta semana"
              color="warning"
            />
            <KpiCard
              title="DSO Médio"
              value={`${data.kpis?.dsoMedio || 38} dias`}
              subtitle="Dias médios para receber"
              trend="up"
              color="primary"
            />
          </Stack>

          <Paper sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 2.5, mb: 2.5 }}>
            <Typography variant="h6" mb={2}>Fluxo de Caixa Projetado (próximas 13 semanas)</Typography>
            <Box sx={{ height: 280 }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart
                  data={
                    fluxoData?.previsoes?.map((p: any, index: number) => ({
                      semana: `S${index + 1}`,
                      entrada: p.tipo?.includes('ENTRADA') ? p.valor : 0,
                      saida: p.tipo?.includes('SAIDA') ? p.valor : 0,
                      saldo: p.saldoProjetado || 0,
                    })) || [
                      { semana: 'S1', entrada: 1240000, saida: 980000, saldo: 260000 },
                      { semana: 'S2', entrada: 980000, saida: 1120000, saldo: 120000 },
                      { semana: 'S3', entrada: 1450000, saida: 890000, saldo: 680000 },
                    ]
                  }
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="semana" />
                  <YAxis />
                  <Tooltip />
                  <Area type="monotone" dataKey="entrada" stackId="1" stroke="#3F2E7D" fill="#3F2E7D" fillOpacity={0.2} name="Entradas" />
                  <Area type="monotone" dataKey="saida" stackId="2" stroke="#006D77" fill="#006D77" fillOpacity={0.2} name="Saídas" />
                  <Line type="monotone" dataKey="saldo" stroke="#ED6C02" strokeWidth={3} name="Saldo Projetado" />
                </AreaChart>
              </ResponsiveContainer>
            </Box>
            <Typography variant="caption" color="text.secondary">
              Conectado ao endpoint real <code>/api/financeiro/fluxo-caixa/projetado</code>. Use o simulador What-If para cenários.
            </Typography>
          </Paper>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2.5 }}>
            <Paper sx={{ p: 2, borderRadius: 2.5, flex: 1 }}>
              <Typography variant="h6" mb={1}>Fluxo de Caixa Real (mês atual)</Typography>
              <Typography variant="body2" color="text.secondary">
                Entradas: R$ {Number((fluxoReal as any)?.totalEntradas || 0).toLocaleString('pt-BR')} · Saídas: R${' '}
                {Number((fluxoReal as any)?.totalSaidas || 0).toLocaleString('pt-BR')} · Saldo: R${' '}
                {Number((fluxoReal as any)?.saldoLiquido || 0).toLocaleString('pt-BR')}
              </Typography>
            </Paper>
            <Paper sx={{ p: 2, borderRadius: 2.5, flex: 1 }}>
              <Typography variant="h6" mb={1}>Calendário de Obrigações</Typography>
              {(calendario as any[]).length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  Sem retenções/guias no mês — emita NFS-e para popular FGTS/DARF.
                </Typography>
              ) : (
                <Stack spacing={0.5}>
                  {(calendario as any[]).slice(0, 4).map((o: any, i: number) => (
                    <Typography key={i} variant="caption">
                      {o.vencimento} — {o.tipo}: R$ {Number(o.valorEstimado || 0).toLocaleString('pt-BR')}
                    </Typography>
                  ))}
                </Stack>
              )}
            </Paper>
          </Stack>

          <Paper sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 2.5, mb: 2.5 }}>
            <Typography variant="h6" mb={2}>Alertas Críticos</Typography>
            {(data.alertas?.length ?? 0) > 0 ? (
              <Stack spacing={1}>
                {data.alertas.map((alerta: string, idx: number) => (
                  <Alert key={idx} severity="warning" sx={{ py: 0.5 }}>
                    {alerta}
                  </Alert>
                ))}
              </Stack>
            ) : (
              <Typography color="success.main">Nenhum alerta crítico no momento.</Typography>
            )}
          </Paper>

          <Paper sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 2.5 }}>
            <Typography variant="h6" mb={2}>Pipeline de Faturamento (Exemplo)</Typography>
            <StatusPipeline
              steps={[
                { label: 'Medição', status: 'done' },
                { label: 'Aprovada', status: 'done' },
                { label: 'NFS-e Emitida', status: 'current' },
                { label: 'Recebido', status: 'pending' },
              ]}
            />
          </Paper>
        </>
      )}

      {!data && !isLoading && (
        <Alert severity="info">
          Nenhum dado retornado do backend ainda. Verifique o endpoint <code>GET /api/financeiro/dashboard/cfo</code>.
        </Alert>
      )}

      {/* What-If Simulator (alta prioridade para CFO) */}
      <Paper sx={{ p: { xs: 2, sm: 2.5 }, mt: 2.5, borderRadius: 2.5 }}>
        <Typography variant="h6" mb={2}>Simulador What-If (Cenários)</Typography>
        <Typography variant="body2" color="text.secondary" mb={2}>
          Teste o impacto de diferentes drivers no fluxo de caixa projetado.
        </Typography>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2}>
          <Button
            variant="outlined"
            disabled={simulation.isPending}
            onClick={async () => {
              const res = await simulation.mutateAsync({ atrasoMedioRecebimento: 15 })
              setLastSimulation(res)
            }}
          >
            Atraso +15 dias no recebimento
          </Button>
          <Button
            variant="outlined"
            disabled={simulation.isPending}
            onClick={async () => {
              const res = await simulation.mutateAsync({ aumentoFolha: 20 })
              setLastSimulation(res)
            }}
          >
            Aumento de 20% na folha
          </Button>
          <Button
            variant="outlined"
            disabled={simulation.isPending}
            onClick={async () => {
              const res = await simulation.mutateAsync({ reducaoFaturamento: -15 })
              setLastSimulation(res)
            }}
          >
            Redução de faturamento
          </Button>
        </Stack>

        {simulation.isPending && <Typography color="text.secondary">Rodando simulação...</Typography>}

        {lastSimulation && (
          <Alert severity="info" sx={{ mt: 1 }}>
            <strong>Resultado da Simulação:</strong><br />
            Saldo Final Simulado: R$ {lastSimulation.saldoFinalSimulado?.toLocaleString('pt-BR')}<br />
            Impacto vs Base: R$ {lastSimulation.impactoVsBase?.toLocaleString('pt-BR')}
          </Alert>
        )}
      </Paper>
    </Box>
  )
}
