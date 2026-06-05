/**
 * ComplianceMonitorPage — monitores técnicos (Phase 8)
 */
import { Box, Typography, Paper, Stack, Button, Alert, Chip } from '@mui/material'
import { RefreshCw, ShieldAlert } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useComplianceMonitors } from '../../api/hooks/usePhase78'

const severityColor: Record<string, 'error' | 'warning' | 'info'> = {
  CRITICAL: 'error',
  WARNING: 'warning',
  INFO: 'info',
}

export default function ComplianceMonitorPage() {
  const { tenantName } = useTenant()
  const { data: monitors = [], isLoading, refetch } = useComplianceMonitors()

  const critical = monitors.filter((m: any) => m.severity === 'CRITICAL').length

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Monitores de Compliance</Typography>
          <Typography color="text.secondary">Tenant: {tenantName}</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity={critical > 0 ? 'error' : 'success'} icon={<ShieldAlert size={18} />} sx={{ mb: 2 }}>
        {critical > 0
          ? `${critical} alerta(s) crítico(s): certificados, eSocial rejeitado, relógios offline ou NFS-e com falha.`
          : 'Nenhum alerta crítico no momento.'}
      </Alert>

      <Paper sx={{ borderRadius: 3 }}>
        <EnterpriseDataGrid
          title="Alertas ativos"
          rowData={monitors}
          columnDefs={[
            { headerName: 'Categoria', field: 'category', width: 140 },
            {
              headerName: 'Severidade', field: 'severity', width: 110,
              cellRenderer: (p: any) => <Chip label={p.value} size="small" color={severityColor[p.value] || 'default'} />,
            },
            { headerName: 'Título', field: 'title', flex: 1.2 },
            { headerName: 'Descrição', field: 'description', flex: 1.5 },
          ]}
          loading={isLoading}
          height="480px"
          emptyMessage="Nenhum monitor ativo — sistema saudável."
        />
      </Paper>
    </Box>
  )
}
