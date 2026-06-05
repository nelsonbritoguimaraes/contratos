/**
 * Perfil Fiscal do Tenant — CNPJ prestador, Simples, desoneração, INSS retenção
 */
import { useEffect, useState } from 'react'
import {
  Box, Typography, Paper, Stack, TextField, Button, FormControlLabel, Switch, Alert, Grid,
} from '@mui/material'
import { Save, RefreshCw } from 'lucide-react'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import { useTenantFiscalProfile, useUpdateTenantFiscalProfile } from '../../api/hooks/useTenantFiscalProfile'

export default function PerfilFiscalPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const { data, isLoading, refetch } = useTenantFiscalProfile()
  const update = useUpdateTenantFiscalProfile()

  const [form, setForm] = useState({
    cnpjPrestador: '',
    municipioIbgePadrao: '3550308',
    aliquotaInssRetencao: '0.1100',
    desoneracaoFolha: false,
    simplesNacional: false,
  })

  useEffect(() => {
    if (data) {
      setForm({
        cnpjPrestador: data.cnpjPrestador || '',
        municipioIbgePadrao: data.municipioIbgePadrao || '3550308',
        aliquotaInssRetencao: String(data.aliquotaInssRetencao ?? 0.11),
        desoneracaoFolha: !!data.desoneracaoFolha,
        simplesNacional: !!data.simplesNacional,
      })
    }
  }, [data])

  const handleSave = async () => {
    try {
      await update.mutateAsync({
        cnpjPrestador: form.cnpjPrestador.replace(/\D/g, ''),
        municipioIbgePadrao: form.municipioIbgePadrao,
        aliquotaInssRetencao: Number(form.aliquotaInssRetencao),
        desoneracaoFolha: form.desoneracaoFolha,
        simplesNacional: form.simplesNacional,
      })
      showNotification('Perfil fiscal salvo. CNPJ será usado em NFS-e, Reinf, FGTS e DCTFWeb.', 'success')
      refetch()
    } catch (e: any) {
      showNotification(e?.message || 'Erro ao salvar perfil fiscal', 'error')
    }
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={2}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>Perfil Fiscal</Typography>
          <Typography color="text.secondary">Tenant: {tenantName} — parametrização tributária da empresa prestadora</Typography>
        </Box>
        <Button startIcon={<RefreshCw size={16} />} onClick={() => refetch()} variant="outlined">Atualizar</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Configure o CNPJ do prestador para eliminar placeholders em NFS-e, EFD-Reinf, FGTS Digital e DCTFWeb.
        Simples Nacional suprime retenções federais; desoneração substitui INSS 11% por CPRB.
      </Alert>

      <Paper sx={{ p: 3, borderRadius: 3 }}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              label="CNPJ Prestador (14 dígitos)"
              fullWidth
              value={form.cnpjPrestador}
              onChange={(e) => setForm({ ...form, cnpjPrestador: e.target.value })}
              placeholder="12345678000190"
              disabled={isLoading}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Município IBGE padrão (ISS)"
              fullWidth
              value={form.municipioIbgePadrao}
              onChange={(e) => setForm({ ...form, municipioIbgePadrao: e.target.value })}
              disabled={isLoading}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Alíquota INSS retenção (decimal)"
              fullWidth
              type="number"
              inputProps={{ step: 0.0001, min: 0, max: 1 }}
              value={form.aliquotaInssRetencao}
              onChange={(e) => setForm({ ...form, aliquotaInssRetencao: e.target.value })}
              helperText="Padrão terceirização: 0.1100 (11%)"
              disabled={isLoading || form.desoneracaoFolha}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <Stack spacing={1} sx={{ pt: 1 }}>
              <FormControlLabel
                control={<Switch checked={form.simplesNacional} onChange={(e) => setForm({ ...form, simplesNacional: e.target.checked })} />}
                label="Simples Nacional (sem retenções federais IRRF/PIS/COFINS/CSLL)"
              />
              <FormControlLabel
                control={<Switch checked={form.desoneracaoFolha} onChange={(e) => setForm({ ...form, desoneracaoFolha: e.target.checked })} />}
                label="Desoneração folha (CPRB no lugar do INSS 11%)"
              />
            </Stack>
          </Grid>
        </Grid>

        <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
          <Button variant="contained" startIcon={<Save size={16} />} onClick={handleSave} disabled={update.isPending || isLoading}>
            Salvar perfil fiscal
          </Button>
          {data?.updatedAt && (
            <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center' }}>
              Última atualização: {data.updatedAt}
            </Typography>
          )}
        </Stack>
      </Paper>
    </Box>
  )
}
