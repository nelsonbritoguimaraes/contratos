import { useState, useCallback, useEffect } from 'react'
import { Box, Typography, Paper, Stack, Chip, Button, Alert, CircularProgress } from '@mui/material'
import { Download, Refresh, Info, Add } from '@mui/icons-material'
import ContractsGrid from './ContractsGrid'
import ContractDetailDialog from './ContractDetailDialog'
import ContractFormDialog from './ContractFormDialog'
import { Contract } from '../../api/types'
import { apiGet } from '../../api/client'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'

/**
 * ContractsPage — Página principal de contratos (Scaffold)
 *
 * Demonstra o padrão exigido pela SPEC v1.0 seção 3.3:
 * - AG Grid com Row Grouping (por órgão e status)
 * - Visão executiva de contratos
 * - KPIs de alto nível
 *
 * Dados: seed estático (20+ postos distribuídos em 5 contratos)
 * Futuro: substituir por chamada real ao backend Kotlin /api/contracts
 */
export default function ContractsPage() {
  const { tenantId } = useTenant()
  const { showNotification } = useNotification()
  const [contracts, setContracts] = useState<any[]>([])
  const [loadingContracts, setLoadingContracts] = useState(false)
  const [openForm, setOpenForm] = useState(false)
  const [editingContract, setEditingContract] = useState<any>(null)
  const [openDetail, setOpenDetail] = useState(false)
  const [selectedContract, setSelectedContract] = useState<any>(null)

  // Busca contratos reais do backend (via proxy)
  const fetchContracts = useCallback(async () => {
    if (!tenantId) return
    setLoadingContracts(true)
    try {
      const data = await apiGet<Contract[]>(`/contracts?tenantId=${tenantId}`)
      setContracts(data ?? [])
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro ao carregar contratos'
      showNotification(message, 'error')
    } finally {
      setLoadingContracts(false)
    }
  }, [tenantId])

  useEffect(() => {
    if (tenantId) fetchContracts()
  }, [fetchContracts, tenantId])

  const handleNewContract = () => {
    setEditingContract(null)
    setOpenForm(true)
  }

  const handleEditContract = (contract: Contract) => {
    setEditingContract(contract)
    setOpenForm(true)
  }

  const handleContractSaved = async () => {
    await fetchContracts()
  }

  const handleOpenContractDetail = useCallback((contract: Contract) => {
    setSelectedContract(contract)
    setOpenDetail(true)
  }, [])

  const displayContracts = contracts
  const totalMensal = displayContracts.reduce((sum, c) => sum + (c.valorMensal || 0), 0)
  const totalPostos = displayContracts.reduce((sum, c) => sum + (c.qtdPostosContratados || 0), 0)
  const ativos = displayContracts.filter((c) => c.status === 'ATIVO').length

  const refreshContracts = () => fetchContracts()

  return (
    <Box>
      {/* Header da página */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={500} gutterBottom>
            Mapa de Contratos
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Visão executiva • Tenant: Grupo Segurança Brasil • Fase 1 — Contrato Vivo
          </Typography>
        </Box>

        <Stack direction="row" spacing={1}>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={handleNewContract}
          >
            Novo Contrato
          </Button>
          <Button 
            variant="outlined" 
            startIcon={<Refresh />} 
            onClick={refreshContracts}
            disabled={loadingContracts}
          >
            Atualizar
          </Button>
          <Button variant="outlined" startIcon={<Download />} disabled>
            Exportar Excel
          </Button>
        </Stack>
      </Stack>

      {/* Alerta de conexão com backend */}
      <Alert
        severity="success"
        icon={<Info />}
        sx={{ mb: 3, borderRadius: 2 }}
      >
        <strong>Fase 1 — Contrato Vivo</strong> — Dados vindos do backend Kotlin em tempo real. 
        Criar contrato ou posto atualiza a interface automaticamente (sem reload).
        <br />
        Clique em <strong>"Ver Postos"</strong> em qualquer linha para entrar no master-detail (contrato + seus postos).
      </Alert>

      {/* KPIs Executivos (alinhados ao Dashboard da SPEC 27.1) */}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={3}>
        <Paper sx={{ p: 2.5, flex: 1, borderRadius: 3 }}>
          <Typography variant="overline" color="text.secondary">Valor mensal contratado</Typography>
          <Typography variant="h4" fontWeight={600} color="primary.main" sx={{ mt: 0.5 }}>
            {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(totalMensal)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {displayContracts.length} contratos
          </Typography>
        </Paper>

        <Paper sx={{ p: 2.5, flex: 1, borderRadius: 3 }}>
          <Typography variant="overline" color="text.secondary">Postos contratados</Typography>
          <Typography variant="h4" fontWeight={600} sx={{ mt: 0.5 }}>
            {totalPostos}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Distribuídos em {displayContracts.length} contratos
          </Typography>
        </Paper>

        <Paper sx={{ p: 2.5, flex: 1, borderRadius: 3 }}>
          <Typography variant="overline" color="text.secondary">Contratos ativos</Typography>
          <Typography variant="h4" fontWeight={600} color="success.main" sx={{ mt: 0.5 }}>
            {ativos} / {displayContracts.length}
          </Typography>
          <Stack direction="row" spacing={0.5} mt={0.5} flexWrap="wrap">
            {displayContracts.slice(0, 6).map((c: any, idx: number) => (
              <Chip
                key={idx}
                label={c.status}
                size="small"
                color={c.status === 'ATIVO' ? 'success' : c.status === 'SUSPENSO' ? 'error' : 'warning'}
              />
            ))}
          </Stack>
        </Paper>
      </Stack>

      {/* Grid principal — coração da demonstração */}
      <Paper sx={{ p: 2, borderRadius: 3, overflow: 'hidden' }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2} px={1}>
          <Typography variant="h6" fontWeight={500}>
            Contratos e Postos (Row Grouping por Órgão + Status)
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Clique nas setas para expandir/colapsar grupos • SPEC seção 3.3 e 6
          </Typography>
        </Stack>

        {loadingContracts ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        ) : (
          <ContractsGrid 
            contracts={displayContracts} 
            onViewPosts={handleOpenContractDetail} 
            onEditContract={handleEditContract}
            onRefresh={refreshContracts}
          />
        )}
      </Paper>

      {/* Rodapé de referência */}
      <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block', textAlign: 'right' }}>
        Dados fictícios baseados em contratos reais de vigilância e limpeza (Lei 14.133/2021). Modelo alinhado à seção 25 da SPEC v1.0.
      </Typography>

      {/* Dialog reutilizável de Criar/Editar Contrato */}
      <ContractFormDialog
        open={openForm}
        contract={editingContract}
        onClose={() => setOpenForm(false)}
        onSaved={handleContractSaved}
      />

      {/* Dialog Master-Detail: Contrato + Postos */}
      <ContractDetailDialog
        open={openDetail}
        contract={selectedContract}
        onClose={() => setOpenDetail(false)}
        onPostCreated={() => {
          // Opcional: refetch se necessário
        }}
      />
    </Box>
  )
}
