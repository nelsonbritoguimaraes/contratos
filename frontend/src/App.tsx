import { useState, useEffect } from 'react'
import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { Box, Typography, Button } from '@mui/material'
import { LogOut } from 'lucide-react'
import AppShell from './components/layout/AppShell'
import TenantSwitcher from './components/TenantSwitcher'
import CommandPalette from './components/CommandPalette'

import { useAuth } from './api/hooks/useAuth'
import RoleGuard from './components/RoleGuard'
import LoginPage from './features/auth/LoginPage'
import UnauthorizedPage from './features/auth/UnauthorizedPage'

import ContractsPage from './features/contracts/ContractsPage'
import BiddingsPage from './features/biddings/BiddingsPage'

// Placeholders das grandes áreas (serão implementadas nas próximas fases)
import CfoDashboardPage from './features/financeiro/CfoDashboardPage'
import IaConsolePage from './features/ia/IaConsolePage'
import FinanceiroReceberPage from './features/financeiro/ContasReceberPage'
import RhFolhaPage from './features/rh/FolhaPage'
import EmployeesPage from './features/rh/EmployeesPage'
import AllocationByBiddingView from './features/rh/AllocationByBiddingView'
import MedicaoPage from './features/measurement/MedicaoPage'
import ConciliacaoPage from './features/financeiro/ConciliacaoPage'
import AgingPage from './features/financeiro/AgingPage'
import RepactuacaoPage from './features/financeiro/RepactuacaoPage'
import FornecedoresPage from './features/financeiro/FornecedoresPage'
import LancamentosPage from './features/financeiro/LancamentosPage'
import RelatoriosPage from './features/financeiro/RelatoriosPage'
import PontoPage from './features/ponto/PontoPage'
import EsocialPage from './features/rh/EsocialPage'
import RhDashboardPage from './features/rh/RhDashboardPage'
import TesourariaPage from './features/financeiro/TesourariaPage'
import ContasPagarPage from './features/financeiro/ContasPagarPage'
import PerfilFiscalPage from './features/financeiro/PerfilFiscalPage'
import FechamentoFinanceiroPage from './features/financeiro/FechamentoFinanceiroPage'
import AuditoriaFinanceiraPage from './features/financeiro/AuditoriaFinanceiraPage'
import OpenFinancePage from './features/financeiro/OpenFinancePage'
import ContabilidadePage from './features/contabilidade/ContabilidadePage'
import IaHistoryPage from './features/ia/IaHistoryPage'
import CctUploadPage from './features/cct/CctUploadPage'
import GlosaPage from './features/glosa/GlosaPage'
import PostosPage from './features/operational/PostosPage'
import ImplantationPage from './features/contracts/ImplantationPage'
import NotificacoesPage from './features/operational/NotificacoesPage'
import EquipamentosPage from './features/operational/EquipamentosPage'
import UniformesPage from './features/operational/UniformesPage'
import EscalaPage from './features/operational/EscalaPage'
import VolantesPage from './features/operational/VolantesPage'
import ContractDashboardPage from './features/contracts/ContractDashboardPage'
import ComplianceMonitorPage from './features/compliance/ComplianceMonitorPage'
import AuditoriaGlobalPage from './features/compliance/AuditoriaGlobalPage'
import PortalOrgaoPage from './features/portal/PortalOrgaoPage'
import MobileSupervisorPage from './features/mobile/MobileSupervisorPage'

function ProtectedShell({
  children,
  onOpenCommandPalette,
}: {
  children: React.ReactNode
  onOpenCommandPalette: () => void
}) {
  const { isAuthenticated, logout, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return (
    <AppShell onOpenCommandPalette={onOpenCommandPalette}>
      {/* 
        Container da página:
        - overflow auto = scroll da página inteiro
        - p responsivo (menor no mobile)
        - flex column para que grids internos possam usar flex corretamente
      */}
      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          overflow: 'auto',
          bgcolor: 'background.default',
          p: { xs: 2, sm: 2.5, md: 3 },
        }}
      >
        {/* Barra superior da página (tenant + usuário + logout) */}
        <Box
          sx={{
            mb: 2,
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            flexWrap: 'wrap',
          }}
        >
          <TenantSwitcher />
          <Typography variant="body2" color="text.secondary" sx={{ ml: 0.5 }}>
            {user?.name || 'Usuário'}
          </Typography>
          <Button
            variant="outlined"
            size="small"
            startIcon={<LogOut size={15} />}
            onClick={logout}
            sx={{ ml: 'auto' }}
          >
            Sair
          </Button>
        </Box>

        {children}
      </Box>
    </AppShell>
  )
}

function App() {
  const [commandOpen, setCommandOpen] = useState(false)

  // Atalho global Cmd/Ctrl + K
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setCommandOpen(true)
      }
      if (e.key === 'Escape') {
        setCommandOpen(false)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  return (
    <>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* Protected application */}
        <Route
          path="/*"
          element={
            <ProtectedShell onOpenCommandPalette={() => setCommandOpen(true)}>
              <Routes>
                <Route path="/" element={<Navigate to="/financeiro/dashboard" replace />} />

                {/* Core Operacional */}
                <Route path="/contracts" element={<RoleGuard requiredRoles={['GESTOR', 'ADMIN', 'CFO']}><ContractsPage /></RoleGuard>} />
                <Route path="/contratos/implantacao" element={<RoleGuard requiredRoles={['GESTOR', 'ADMIN', 'CFO']}><ImplantationPage /></RoleGuard>} />
                <Route path="/contracts/dashboard" element={<RoleGuard requiredRoles={['GESTOR', 'ADMIN', 'CFO']}><ContractDashboardPage /></RoleGuard>} />
                <Route path="/biddings" element={<RoleGuard requiredRoles={['GESTOR', 'ADMIN', 'CFO']}><BiddingsPage /></RoleGuard>} />

                {/* CFO / Financeiro Enterprise */}
                <Route path="/financeiro/dashboard" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><CfoDashboardPage /></RoleGuard>} />
                <Route path="/financeiro/receber" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><FinanceiroReceberPage /></RoleGuard>} />
                <Route path="/financeiro/pagar" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><ContasPagarPage /></RoleGuard>} />
                <Route path="/financeiro/perfil-fiscal" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><PerfilFiscalPage /></RoleGuard>} />
                <Route path="/financeiro/fechamento" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><FechamentoFinanceiroPage /></RoleGuard>} />
                <Route path="/financeiro/auditoria" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><AuditoriaFinanceiraPage /></RoleGuard>} />
                <Route path="/financeiro/open-finance" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><OpenFinancePage /></RoleGuard>} />
                <Route path="/financeiro/conciliacao" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><ConciliacaoPage /></RoleGuard>} />
                <Route path="/financeiro/aging" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><AgingPage /></RoleGuard>} />
                {/* Tesouraria */}
                <Route path="/financeiro/tesouraria" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><TesourariaPage /></RoleGuard>} />
                <Route path="/financeiro/repactuacao" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><RepactuacaoPage /></RoleGuard>} />
                <Route path="/financeiro/fornecedores" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><FornecedoresPage /></RoleGuard>} />
                <Route path="/financeiro/lancamentos" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><LancamentosPage /></RoleGuard>} />
                <Route path="/financeiro/relatorios" element={<RoleGuard requiredRoles={['CFO', 'ADMIN']}><RelatoriosPage /></RoleGuard>} />

                {/* IA */}
                <Route path="/ia" element={<RoleGuard requiredRoles={['ADMIN']}><IaConsolePage /></RoleGuard>} />
                <Route path="/ia/calls" element={<RoleGuard requiredRoles={['ADMIN']}><IaHistoryPage /></RoleGuard>} />

                {/* RH / Folha */}
                <Route path="/rh/folha" element={<RoleGuard requiredRoles={['RH', 'ADMIN']}><RhFolhaPage /></RoleGuard>} />
                <Route path="/rh/dashboard" element={<RoleGuard requiredRoles={['RH', 'ADMIN']}><RhDashboardPage /></RoleGuard>} />
                <Route path="/rh/esocial" element={<RoleGuard requiredRoles={['RH', 'ADMIN']}><EsocialPage /></RoleGuard>} />
                <Route path="/rh/employees" element={<RoleGuard requiredRoles={['RH', 'ADMIN']}><EmployeesPage /></RoleGuard>} />
                <Route path="/rh/alocacao-por-licitacao" element={<RoleGuard requiredRoles={['RH', 'ADMIN']}><AllocationByBiddingView /></RoleGuard>} />

                {/* Ponto & Cobertura */}
                <Route path="/ponto" element={<RoleGuard requiredRoles={['GESTOR', 'ADMIN', 'RH']}><PontoPage /></RoleGuard>} />

                {/* Operacional - Medição */}
                <Route path="/measurements" element={<RoleGuard requiredRoles={['GESTOR', 'ADMIN', 'CFO']}><MedicaoPage /></RoleGuard>} />

                {/* Contabilidade (Wave 3) */}
                <Route path="/contabilidade/contas" element={<RoleGuard requiredRoles={['CONTABILIDADE', 'ADMIN']}><ContabilidadePage /></RoleGuard>} />
                <Route path="/contabilidade/lancamentos-folha" element={<RoleGuard requiredRoles={['CONTABILIDADE', 'ADMIN']}><ContabilidadePage /></RoleGuard>} />
                <Route path="/contabilidade/relatorios" element={<RoleGuard requiredRoles={['CONTABILIDADE', 'ADMIN']}><ContabilidadePage /></RoleGuard>} />
                <Route path="/contabilidade/sped" element={<RoleGuard requiredRoles={['CONTABILIDADE', 'ADMIN']}><ContabilidadePage /></RoleGuard>} />

                {/* CCT Upload (Wave 3) */}
                <Route path="/cct" element={<CctUploadPage />} />

                {/* Glosas */}
                <Route path="/glosas" element={<GlosaPage />} />

                {/* Postos - Tela dedicada (recomendado pelo usuário) */}
                <Route path="/postos" element={<PostosPage />} />

                {/* Operacional Phase 1/2/8 */}
                <Route path="/operacional/notificacoes" element={<NotificacoesPage />} />
                <Route path="/operacional/equipamentos" element={<EquipamentosPage />} />
                <Route path="/operacional/uniformes" element={<UniformesPage />} />
                <Route path="/operacional/escala" element={<EscalaPage />} />
                <Route path="/operacional/volantes" element={<VolantesPage />} />

                {/* Compliance & Portal & Mobile */}
                <Route path="/compliance/monitors" element={<RoleGuard requiredRoles={['ADMIN', 'GESTOR']}><ComplianceMonitorPage /></RoleGuard>} />
                <Route path="/compliance/auditoria" element={<RoleGuard requiredRoles={['ADMIN', 'GESTOR']}><AuditoriaGlobalPage /></RoleGuard>} />
                <Route path="/portal/orgao" element={<PortalOrgaoPage />} />
                <Route path="/mobile/supervisor" element={<MobileSupervisorPage />} />

                {/* 404 */}
                <Route
                  path="*"
                  element={
                    <Box sx={{ textAlign: 'center', mt: 8, color: 'text.secondary' }}>
                      <Typography variant="h4" fontWeight={700} gutterBottom>
                        404 — Página não encontrada
                      </Typography>
                      <Typography variant="body1" sx={{ mb: 3 }}>
                        A rota que você tentou acessar não existe.
                      </Typography>
                      <Button variant="contained" onClick={() => window.location.href = '/financeiro/dashboard'}>
                        Voltar ao Dashboard
                      </Button>
                    </Box>
                  }
                />
              </Routes>
            </ProtectedShell>
          }
        />
      </Routes>

      <CommandPalette open={commandOpen} onClose={() => setCommandOpen(false)} />
    </>
  )
}

export default App
