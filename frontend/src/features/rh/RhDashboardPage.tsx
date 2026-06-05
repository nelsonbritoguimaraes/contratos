import { useState } from 'react'
import {
  Box, Typography, Paper, Grid, TextField, Alert, Chip, Stack, Table, TableBody, TableCell,
  TableHead, TableRow, Button
} from '@mui/material'
import {
  useRhDashboard, useRhEncargos, useRhComplianceCalendario, useRhDivergencias, useFecharCompetenciaFiscal
} from '../../api/hooks/useRhDashboard'
import { useNotification } from '../../components/NotificationProvider'

const severidadeColor = (s: string) => {
  if (s === 'CRITICO') return 'error'
  if (s === 'ALERTA') return 'warning'
  return 'success'
}

export default function RhDashboardPage() {
  const { showNotification } = useNotification()
  const [competence, setCompetence] = useState(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
  })

  const { data: dashboard, isLoading } = useRhDashboard(competence)
  const { data: encargos } = useRhEncargos(competence)
  const { data: calendario = [] } = useRhComplianceCalendario(competence)
  const { data: divergencias, refetch: refetchDiv } = useRhDivergencias(competence)
  const fecharFiscal = useFecharCompetenciaFiscal()

  const folha = dashboard?.resumoFolha ?? {}
  const esocial = dashboard?.eSocial ?? {}

  const handleFecharFiscal = async () => {
    try {
      const res = await fecharFiscal.mutateAsync({ competencia: competence, transmitir: false })
      showNotification(`Competência fiscal fechada: ${JSON.stringify(res).slice(0, 120)}…`, 'success')
      refetchDiv()
    } catch (e: any) {
      showNotification(e.message || 'Erro ao fechar competência fiscal', 'error')
    }
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>Dashboard RH / DP</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Folha, eSocial, encargos, divergências folha×eSocial×FGTS e calendário fiscal
      </Typography>

      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
        <TextField
          label="Competência"
          type="date"
          size="small"
          value={competence}
          onChange={(e) => setCompetence(e.target.value)}
          InputLabelProps={{ shrink: true }}
        />
        <Button variant="contained" onClick={handleFecharFiscal} disabled={fecharFiscal.isPending}>
          Fechar competência fiscal
        </Button>
      </Stack>

      {isLoading && <Alert severity="info">Carregando...</Alert>}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="overline">Holerites</Typography>
            <Typography variant="h5">{folha.totalHolerites ?? 0}</Typography>
            <Typography variant="body2">Líquido: R$ {Number(folha.totalLiquido ?? 0).toLocaleString('pt-BR')}</Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="overline">eSocial</Typography>
            <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap">
              <Chip size="small" label={`Gerados: ${esocial.gerados ?? 0}`} />
              <Chip size="small" color="warning" label={`Pendentes: ${esocial.pendentes ?? 0}`} />
              <Chip size="small" color="success" label={`Enviados: ${esocial.enviados ?? 0}`} />
            </Stack>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="overline">Encargos (estimativa)</Typography>
            <Typography variant="body2">INSS: R$ {Number(encargos?.encargos?.INSS ?? 0).toLocaleString('pt-BR')}</Typography>
            <Typography variant="body2">FGTS: R$ {Number(encargos?.encargos?.FGTS ?? 0).toLocaleString('pt-BR')}</Typography>
          </Paper>
        </Grid>
      </Grid>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
          <Typography variant="h6">Divergências folha × eSocial × FGTS</Typography>
          {divergencias?.statusGeral && (
            <Chip
              label={divergencias.statusGeral}
              color={severidadeColor(divergencias.statusGeral) as any}
              size="small"
            />
          )}
        </Stack>
        {divergencias?.resumo && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Holerites: {divergencias.resumo.holerites} · S-1200: {divergencias.resumo.eventosS1200} ·
            Críticos: {divergencias.resumo.criticos} · Alertas: {divergencias.resumo.alertas} ·
            S-1299: {divergencias.resumo.s1299Presente ? 'sim' : 'não'}
          </Typography>
        )}
        {!divergencias?.linhas?.length ? (
          <Alert severity="info">Nenhuma divergência calculada para a competência.</Alert>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Colaborador</TableCell>
                <TableCell>Indicador</TableCell>
                <TableCell align="right">Folha</TableCell>
                <TableCell align="right">eSocial</TableCell>
                <TableCell align="right">Diferença</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(divergencias.linhas as any[]).slice(0, 50).map((l, i) => (
                <TableRow key={i}>
                  <TableCell>{l.colaborador || '—'}</TableCell>
                  <TableCell>{l.indicador}</TableCell>
                  <TableCell align="right">{Number(l.valorFolha ?? 0).toLocaleString('pt-BR')}</TableCell>
                  <TableCell align="right">
                    {l.valorEsocial != null ? Number(l.valorEsocial).toLocaleString('pt-BR') : '—'}
                  </TableCell>
                  <TableCell align="right">{Number(l.diferenca ?? 0).toLocaleString('pt-BR')}</TableCell>
                  <TableCell>
                    <Chip size="small" label={l.severidade} color={severidadeColor(l.severidade) as any} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>Calendário de obrigações</Typography>
        {calendario.length === 0 ? (
          <Alert severity="info">Nenhuma obrigação carregada.</Alert>
        ) : (
          calendario.map((o: any, i: number) => (
            <Box key={i} sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Typography fontWeight={600}>{o.tipo} — {o.descricao}</Typography>
              <Typography variant="body2" color="text.secondary">
                Vencimento: {o.vencimento} · {o.integracao}
              </Typography>
            </Box>
          ))
        )}
      </Paper>
    </Box>
  )
}
