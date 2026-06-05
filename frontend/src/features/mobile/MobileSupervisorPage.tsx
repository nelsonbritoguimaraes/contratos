/**
 * MobileSupervisorPage — PWA layout responsivo para supervisor de campo
 */
import {
  Box, Typography, Paper, Stack, Button, Card, CardContent, Grid, Chip, Fab,
} from '@mui/material'
import { RefreshCw, MapPin, Users, AlertTriangle } from 'lucide-react'
import { useTenant } from '../../api/hooks/useTenant'
import { useContracts } from '../../api/hooks/useContracts'
import { useDailyCoverage, useVolantes } from '../../api/hooks/usePonto'
import { useComplianceMonitors } from '../../api/hooks/usePhase78'

export default function MobileSupervisorPage() {
  const { tenantName } = useTenant()
  const { data: contracts = [] } = useContracts()
  const contractId = contracts[0]?.id
  const today = new Date().toISOString().slice(0, 10)

  const { data: coverage, refetch: refetchC } = useDailyCoverage(contractId, today)
  const { data: volantes = [], refetch: refetchV } = useVolantes(contractId, today)
  const { data: monitors = [] } = useComplianceMonitors()

  const coveragePct = coverage?.coverage_percent ?? coverage?.coveragePercent ?? 100

  return (
    <Box sx={{ maxWidth: 480, mx: 'auto', pb: 10 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Box>
          <Typography variant="h5" fontWeight={700}>Supervisor</Typography>
          <Typography variant="caption" color="text.secondary">{tenantName}</Typography>
        </Box>
        <Button size="small" startIcon={<RefreshCw size={14} />} onClick={() => { refetchC(); refetchV() }}>Sync</Button>
      </Stack>

      <Grid container spacing={2} mb={2}>
        <Grid item xs={6}>
          <Card sx={{ bgcolor: coveragePct >= 95 ? 'success.light' : 'warning.light' }}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Stack direction="row" alignItems="center" spacing={1}>
                <MapPin size={18} />
                <Box>
                  <Typography variant="caption">Cobertura hoje</Typography>
                  <Typography variant="h5" fontWeight={700}>{coveragePct}%</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={6}>
          <Card>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Stack direction="row" alignItems="center" spacing={1}>
                <Users size={18} />
                <Box>
                  <Typography variant="caption">Volantes ausentes</Typography>
                  <Typography variant="h5" fontWeight={700}>{volantes.length}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {monitors.filter((m: any) => m.severity === 'CRITICAL').slice(0, 3).map((m: any) => (
        <Paper key={m.id} sx={{ p: 1.5, mb: 1, borderRadius: 2, borderLeft: 4, borderColor: 'error.main' }}>
          <Stack direction="row" spacing={1} alignItems="flex-start">
            <AlertTriangle size={16} color="#d32f2f" />
            <Box>
              <Typography variant="body2" fontWeight={600}>{m.title}</Typography>
              <Typography variant="caption" color="text.secondary">{m.category}</Typography>
            </Box>
          </Stack>
        </Paper>
      ))}

      <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>Volantes do dia</Typography>
      {volantes.length === 0 ? (
        <Typography variant="body2" color="text.secondary">Nenhum volante ausente.</Typography>
      ) : (
        volantes.map((v: any, i: number) => (
          <Paper key={i} sx={{ p: 1.5, mb: 1, borderRadius: 2 }}>
            <Typography variant="body2" fontWeight={600}>{v.post_name || v.postId || 'Posto'}</Typography>
            <Typography variant="caption">{v.employee_name || v.reason || 'Ausente'}</Typography>
          </Paper>
        ))
      )}

      <Fab color="primary" variant="extended" sx={{ position: 'fixed', bottom: 16, left: '50%', transform: 'translateX(-50%)' }}>
        <Chip label="PWA Mobile" size="small" sx={{ mr: 1 }} /> Evidência foto
      </Fab>
    </Box>
  )
}
