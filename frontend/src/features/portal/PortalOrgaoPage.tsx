/**
 * PortalOrgaoPage — portal read-only para órgão contratante
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, FormControl, InputLabel, Select, MenuItem, Tabs, Tab,
} from '@mui/material'
import { RefreshCw, Building2 } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useTenant } from '../../api/hooks/useTenant'
import { useContracts } from '../../api/hooks/useContracts'
import { usePortalDocuments, usePortalMeasurements } from '../../api/hooks/usePhase78'

export default function PortalOrgaoPage() {
  const { tenantName } = useTenant()
  const { data: contracts = [] } = useContracts()
  const [contractId, setContractId] = useState('')
  const [tab, setTab] = useState(0)

  const { data: docs = [], refetch: refetchD } = usePortalDocuments(contractId || undefined)
  const { data: medicoes = [], refetch: refetchM } = usePortalMeasurements(contractId || undefined)

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            <Building2 size={28} style={{ verticalAlign: 'middle', marginRight: 8 }} />
            Portal do Órgão
          </Typography>
          <Typography color="text.secondary">Acesso restrito ROLE_PORTAL_ORGAO — Tenant: {tenantName}</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => { refetchD(); refetchM() }} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>Visualização read-only de documentos e medições do contrato para o fiscal do órgão.</Alert>

      <FormControl sx={{ minWidth: 320, mb: 2 }}>
        <InputLabel>Contrato</InputLabel>
        <Select value={contractId} label="Contrato" onChange={(e) => setContractId(e.target.value)}>
          <MenuItem value="">Selecione</MenuItem>
          {contracts.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.numero} — {c.orgao}</MenuItem>)}
        </Select>
      </FormControl>

      <Paper sx={{ p: 2, borderRadius: 3 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
          <Tab label="Documentos" />
          <Tab label="Medições" />
        </Tabs>
        {tab === 0 && (
          <EnterpriseDataGrid title="Documentos do contrato" rowData={docs} columnDefs={[
            { headerName: 'Título', field: 'title', flex: 1 },
            { headerName: 'Tipo', field: 'mimeType', width: 140 },
            { headerName: 'Status', field: 'status', width: 100 },
          ]} height="360px" emptyMessage="Selecione um contrato." />
        )}
        {tab === 1 && (
          <EnterpriseDataGrid title="Medições" rowData={medicoes} columnDefs={[
            { headerName: 'Período', field: 'period', width: 120 },
            { headerName: 'Valor final', field: 'finalAmount', valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
            { headerName: 'Glosas', field: 'glosaTotal', valueFormatter: (p: any) => `R$ ${Number(p.value || 0).toLocaleString('pt-BR')}` },
            { headerName: 'Status', field: 'status', width: 110 },
          ]} height="360px" emptyMessage="Selecione um contrato." />
        )}
      </Paper>
    </Box>
  )
}
