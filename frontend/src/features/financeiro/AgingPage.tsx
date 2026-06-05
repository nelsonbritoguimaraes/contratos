/**
 * AgingPage — Relatório de Aging (AR e AP)
 * Muito importante para o CFO acompanhar risco de recebimento e pagamento.
 */
import { useState } from 'react'
import { Box, Typography, Paper, Tabs, Tab, Alert } from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { ColDef } from 'ag-grid-community'
import { useAgingReport } from '../../api/hooks/useAgingReport'

const agingColumns: ColDef[] = [
  { headerName: 'Faixa', field: 'faixa', minWidth: 120 },
  { headerName: 'Quantidade', field: 'quantidade', minWidth: 110 },
  { headerName: 'Valor', field: 'valor', valueFormatter: p => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
  { headerName: '% do Total', field: 'percentual' },
]

export default function AgingPage() {
  const [tab, setTab] = useState(0)

  // Dados reais do backend (substitui arrays mockados)
  const { agingAR, agingAP, isLoading, isError, error, refetch } = useAgingReport()

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>
        Relatório de Aging
      </Typography>
      <Typography color="text.secondary" mb={3}>
        Visão de risco de recebimentos e pagamentos por faixa de atraso.
      </Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Contas a Receber (AR)" />
        <Tab label="Contas a Pagar (AP)" />
      </Tabs>

      <Paper sx={{ p: 2, borderRadius: 3 }}>
        <EnterpriseDataGrid
          title={tab === 0 ? "Aging - Contas a Receber" : "Aging - Contas a Pagar"}
          rowData={tab === 0 ? agingAR : agingAP}
          columnDefs={agingColumns}
          height={380}
          loading={isLoading}
          emptyMessage={
            tab === 0
              ? 'Nenhum dado de aging para Contas a Receber. Cadastre contas a receber com vencimento ou verifique se o backend está retornando dados de contas abertas.'
              : 'Nenhum dado de aging para Contas a Pagar. Cadastre contas a pagar ou verifique a disponibilidade dos dados no backend.'
          }
          error={isError ? (error?.message || 'Erro ao carregar relatório de aging do backend.') : null}
          onRefresh={refetch}
        />
      </Paper>

      <Alert severity="info" sx={{ mt: 3 }}>
        Dados reais vindos de <code>/api/financeiro/relatorios/aging/ar</code> e <code>/ap</code> (via useAgingReport + TanStack Query).
        Use para priorizar cobranças e pagamentos por faixa de risco. O botão "Atualizar" força refetch.
      </Alert>
    </Box>
  )
}
