/**
 * ContractDashboardPage — visão 360° do contrato
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, FormControl, InputLabel, Select, MenuItem, Grid, Card, CardContent,
} from '@mui/material'
import { RefreshCw } from 'lucide-react'
import { useTenant } from '../../api/hooks/useTenant'
import { useContracts } from '../../api/hooks/useContracts'
import { useContractDashboard } from '../../api/hooks/usePhase78'

function KpiCard({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h5" fontWeight={700}>{value}</Typography>
        {sub && <Typography variant="caption">{sub}</Typography>}
      </CardContent>
    </Card>
  )
}

export default function ContractDashboardPage() {
  const { tenantName } = useTenant()
  const { data: contracts = [] } = useContracts()
  const [contractId, setContractId] = useState('')

  const { data, isLoading, refetch } = useContractDashboard(contractId || undefined)

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Dashboard do Contrato</Typography>
          <Typography color="text.secondary">Tenant: {tenantName} — visão 360°</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined" disabled={!contractId}>Atualizar</Button>
      </Stack>

      <FormControl sx={{ minWidth: 320, mb: 2 }}>
        <InputLabel>Contrato</InputLabel>
        <Select value={contractId} label="Contrato" onChange={(e) => setContractId(e.target.value)}>
          <MenuItem value="">Selecione</MenuItem>
          {contracts.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.numero} — {c.orgao}</MenuItem>)}
        </Select>
      </FormControl>

      {!contractId && <Alert severity="info">Selecione um contrato para carregar o dashboard.</Alert>}

      {data && (
        <>
          <Alert severity="info" sx={{ mb: 2 }}>
            {data.numero} — {data.orgao} | Implantação: {data.implantacaoStatus || 'N/A'}
          </Alert>
          <Grid container spacing={2}>
            <Grid item xs={6} md={3}><KpiCard label="Postos" value={String(data.postsCount ?? 0)} /></Grid>
            <Grid item xs={6} md={3}><KpiCard label="Medições" value={String(data.medicoesCount ?? 0)} /></Grid>
            <Grid item xs={6} md={3}><KpiCard label="Glosas" value={String(data.glosasCount ?? 0)} sub={`R$ ${Number(data.glosaTotal || 0).toLocaleString('pt-BR')}`} /></Grid>
            <Grid item xs={6} md={3}><KpiCard label="Notif. pendentes" value={String(data.notificacoesPendentes ?? 0)} /></Grid>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2, borderRadius: 3 }}>
                <Typography variant="subtitle2" gutterBottom>Faturamento total</Typography>
                <Typography variant="h4" fontWeight={700}>R$ {Number(data.faturamentoTotal || 0).toLocaleString('pt-BR')}</Typography>
              </Paper>
            </Grid>
          </Grid>
        </>
      )}
      {isLoading && contractId && <Typography sx={{ mt: 2 }} color="text.secondary">Carregando...</Typography>}
    </Box>
  )
}
