/**
 * Open Finance — consentimentos bancários para importação automática de extrato
 */
import { useState } from 'react'
import {
  Box, Typography, Paper, Stack, Button, Alert, MenuItem,
  FormControl, InputLabel, Select, Chip, Link,
} from '@mui/material'
import { RefreshCw, Plus, CheckCircle, XCircle, ExternalLink } from 'lucide-react'
import { ColDef } from 'ag-grid-community'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useContasBancarias } from '../../api/hooks/useTesouraria'
import {
  useOpenFinanceConsents,
  useIniciarOpenFinanceConsent,
  useConfirmarOpenFinanceConsent,
  useRevogarOpenFinanceConsent,
  OpenFinanceConsent,
} from '../../api/hooks/useOpenFinance'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'

const INSTITUICOES = [
  { id: 'itau', name: 'Itaú Unibanco' },
  { id: 'bradesco', name: 'Bradesco' },
  { id: 'bb', name: 'Banco do Brasil' },
  { id: 'santander', name: 'Santander' },
  { id: 'nubank', name: 'Nubank' },
]

const STATUS_COLOR: Record<string, 'default' | 'success' | 'warning' | 'error'> = {
  PENDING: 'warning',
  AUTHORIZED: 'success',
  REVOKED: 'error',
}

export default function OpenFinancePage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data: consents = [], isLoading, refetch } = useOpenFinanceConsents()
  const { data: contas = [] } = useContasBancarias()
  const iniciar = useIniciarOpenFinanceConsent()
  const confirmar = useConfirmarOpenFinanceConsent()
  const revogar = useRevogarOpenFinanceConsent()

  const [contaBancariaId, setContaBancariaId] = useState('')
  const [institutionId, setInstitutionId] = useState('itau')
  const [selected, setSelected] = useState<OpenFinanceConsent | null>(null)

  const institutionName = INSTITUICOES.find((i) => i.id === institutionId)?.name ?? institutionId

  const handleIniciar = async () => {
    try {
      const consent = await iniciar.mutateAsync({
        contaBancariaId: contaBancariaId || undefined,
        institutionId,
        institutionName,
      })
      showNotification('Consentimento iniciado. Abra a URL de autorização no navegador.', 'success')
      if (consent.authorizationUrl) {
        window.open(consent.authorizationUrl, '_blank')
      }
      refetch()
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao iniciar consentimento', 'error')
    }
  }

  const handleConfirmar = async () => {
    if (!selected?.id) {
      showNotification('Selecione um consentimento pendente', 'warning')
      return
    }
    try {
      await confirmar.mutateAsync(selected.id)
      showNotification('Consentimento autorizado. Webhooks importarão extratos automaticamente.', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao confirmar', 'error')
    }
  }

  const handleRevogar = async () => {
    if (!selected?.id) {
      showNotification('Selecione um consentimento', 'warning')
      return
    }
    try {
      await revogar.mutateAsync(selected.id)
      showNotification('Consentimento revogado.', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao revogar', 'error')
    }
  }

  const columns: ColDef[] = [
    { headerName: 'Instituição', field: 'institutionName', flex: 1 },
    {
      headerName: 'Status',
      field: 'status',
      width: 120,
      cellRenderer: (p: any) => (
        <Chip label={p.value} size="small" color={STATUS_COLOR[p.value] || 'default'} />
      ),
    },
    { headerName: 'Consent ID', field: 'consentId', width: 180 },
    {
      headerName: 'Expira em',
      field: 'expiresAt',
      width: 160,
      valueFormatter: (p) => (p.value ? new Date(p.value).toLocaleString('pt-BR') : '—'),
    },
    {
      headerName: 'URL',
      field: 'authorizationUrl',
      width: 90,
      cellRenderer: (p: any) =>
        p.value ? (
          <Link href={p.value} target="_blank" rel="noopener" underline="hover">
            Abrir
          </Link>
        ) : '—',
    },
  ]

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Open Finance</Typography>
          <Typography color="text.secondary">
            Tenant: {tenantName} — consentimentos para importação automática de extrato bancário
          </Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">
          Atualizar
        </Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Fluxo: iniciar consentimento → autorizar no banco (URL sandbox) → confirmar aqui → webhooks importam transações e disparam conciliação automática.
      </Alert>

      <Paper sx={{ p: 2.5, mb: 2, borderRadius: 3 }}>
        <Typography variant="h6" gutterBottom>Novo consentimento</Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="flex-end">
          <FormControl size="small" sx={{ minWidth: 240 }}>
            <InputLabel>Conta bancária (opcional)</InputLabel>
            <Select
              value={contaBancariaId}
              label="Conta bancária (opcional)"
              onChange={(e) => setContaBancariaId(e.target.value)}
            >
              <MenuItem value="">Nenhuma (definir depois)</MenuItem>
              {contas.map((c: any) => (
                <MenuItem key={c.id} value={c.id}>
                  {c.bancoNome} — Ag {c.agencia} / Cc {c.conta}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Instituição</InputLabel>
            <Select
              value={institutionId}
              label="Instituição"
              onChange={(e) => setInstitutionId(e.target.value)}
            >
              {INSTITUICOES.map((i) => (
                <MenuItem key={i.id} value={i.id}>{i.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <Button
            variant="contained"
            startIcon={<Plus size={16} />}
            onClick={handleIniciar}
            disabled={iniciar.isPending}
          >
            Iniciar consentimento
          </Button>
        </Stack>
      </Paper>

      <Stack direction="row" spacing={2} mb={2}>
        <Button
          variant="outlined"
          color="success"
          startIcon={<CheckCircle size={16} />}
          onClick={handleConfirmar}
          disabled={confirmar.isPending || selected?.status !== 'PENDING'}
        >
          Confirmar selecionado
        </Button>
        <Button
          variant="outlined"
          color="error"
          startIcon={<XCircle size={16} />}
          onClick={handleRevogar}
          disabled={revogar.isPending || !selected?.id || selected?.status === 'REVOKED'}
        >
          Revogar selecionado
        </Button>
        {selected?.authorizationUrl && (
          <Button
            variant="text"
            startIcon={<ExternalLink size={16} />}
            onClick={() => window.open(selected.authorizationUrl!, '_blank')}
          >
            Abrir URL de autorização
          </Button>
        )}
      </Stack>

      <EnterpriseDataGrid
        title="Consentimentos Open Finance"
        rowData={consents}
        columnDefs={columns}
        loading={isLoading}
        emptyMessage="Nenhum consentimento. Inicie um fluxo acima para conectar uma instituição financeira."
        height={400}
        onRefresh={refetch}
        onRowClicked={(e) => setSelected(e.data ?? null)}
      />
    </Box>
  )
}
