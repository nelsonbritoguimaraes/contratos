/**
 * AppShell — Layout Enterprise Limpo (reescrita)
 * 
 * Estrutura clara:
 * - AppBar fixa no topo
 * - Drawer permanente (desktop) ou temporary (mobile)
 * - Main com overflow controlado — o scroll real fica no wrapper da página (ProtectedShell)
 * - Tipografia consistente via tema + ajustes mínimos
 * - Sidebar colapsável no desktop (estado persistido)
 * - Sem sobreposições, fontes uniformes, bom comportamento de scroll
 */
import { ReactNode, useState, useEffect } from 'react'
import {
  Box, AppBar, Toolbar, Typography, IconButton, Avatar,
  List, ListItemButton, ListItemText, ListItemIcon,
  Collapse, Divider, Drawer, useMediaQuery, useTheme, Chip, Tooltip
} from '@mui/material'
import {
  Menu, Search, Bell, ChevronDown, ChevronRight,
  Users, FileText, TrendingUp, Bot, BookOpen, Settings
} from 'lucide-react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useTenant } from '../../api/hooks/useTenant'

interface AppShellProps {
  children: ReactNode
  onOpenCommandPalette?: () => void
}

const NAV_GROUPS = [
  {
    title: 'Operacional',
    icon: <FileText size={18} />,
    items: [
      { label: 'Contratos', path: '/contracts' },
      { label: 'Dashboard Contrato', path: '/contracts/dashboard' },
      { label: 'Implantação', path: '/contratos/implantacao' },
      { label: 'Licitações', path: '/biddings' },
      { label: 'Medições', path: '/measurements' },
      { label: 'Glosas', path: '/glosas' },
      { label: 'Postos (Gestão)', path: '/postos' },
      { label: 'Notificações', path: '/operacional/notificacoes' },
      { label: 'Equipamentos', path: '/operacional/equipamentos' },
      { label: 'Uniformes & EPIs', path: '/operacional/uniformes' },
      { label: 'Escala & Turnos', path: '/operacional/escala' },
      { label: 'Volantes', path: '/operacional/volantes' },
      { label: 'Ponto (AFD + Docs)', path: '/ponto' },
    ],
  },
  {
    title: 'Financeiro (CFO)',
    icon: <TrendingUp size={18} />,
    items: [
      { label: 'Dashboard CFO', path: '/financeiro/dashboard' },
      { label: 'Contas a Receber', path: '/financeiro/receber' },
      { label: 'Contas a Pagar', path: '/financeiro/pagar' },
      { label: 'Perfil Fiscal', path: '/financeiro/perfil-fiscal' },
      { label: 'Fechamento Financeiro', path: '/financeiro/fechamento' },
      { label: 'Auditoria Financeira', path: '/financeiro/auditoria' },
      { label: 'Open Finance', path: '/financeiro/open-finance' },
      { label: 'Aging (Risco)', path: '/financeiro/aging' },
      { label: 'Conciliação Bancária', path: '/financeiro/conciliacao' },
      { label: 'Tesouraria & Conciliação', path: '/financeiro/tesouraria' },
      { label: 'Repactuação & Reajustes', path: '/financeiro/repactuacao' },
      { label: 'Fornecedores', path: '/financeiro/fornecedores' },
      { label: 'Lançamentos (Compras/Despesas)', path: '/financeiro/lancamentos' },
      { label: 'Relatórios Financeiros', path: '/financeiro/relatorios' },
    ],
  },
  {
    title: 'RH & Folha',
    icon: <Users size={18} />,
    items: [
      { label: 'Dashboard RH', path: '/rh/dashboard' },
      { label: 'Folha de Pagamento', path: '/rh/folha' },
      { label: 'eSocial', path: '/rh/esocial' },
      { label: 'Colaboradores (Employees)', path: '/rh/employees' },
      { label: 'Alocação por Licitação', path: '/rh/alocacao-por-licitacao' },
      { label: 'Ponto & Cobertura', path: '/ponto' },
    ],
  },
  {
    title: 'IA & Inteligência',
    icon: <Bot size={18} />,
    items: [
      { label: 'Console de IA (/ask)', path: '/ia' },
      { label: 'Histórico de Chamadas', path: '/ia/calls' },
      { label: 'CCT Upload + Impacto', path: '/cct' },
    ],
  },
  {
    title: 'Contabilidade',
    icon: <BookOpen size={18} />,
    items: [
      { label: 'Plano de Contas', path: '/contabilidade/contas' },
      { label: 'DRE & Balancete', path: '/contabilidade/relatorios' },
      { label: 'SPED', path: '/contabilidade/sped' },
      { label: 'Lançamentos Folha', path: '/contabilidade/lancamentos-folha' },
    ],
  },
  {
    title: 'Compliance & Portal',
    icon: <Settings size={18} />,
    items: [
      { label: 'Monitores Compliance', path: '/compliance/monitors' },
      { label: 'Auditoria Global', path: '/compliance/auditoria' },
      { label: 'Portal Órgão', path: '/portal/orgao' },
      { label: 'Mobile Supervisor', path: '/mobile/supervisor' },
    ],
  },
]

const SIDEBAR_WIDTH = 256
const SIDEBAR_COLLAPSED_WIDTH = 64
const STORAGE_KEY = 'contractops.sidebarOpen'

export default function AppShell({ children, onOpenCommandPalette }: AppShellProps) {
  const { tenantName } = useTenant()
  const navigate = useNavigate()
  const location = useLocation()
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md'))

  // Persistência do estado da sidebar (desktop)
  const [sidebarOpen, setSidebarOpen] = useState<boolean>(() => {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved !== null ? saved === 'true' : true
  })
  const [mobileOpen, setMobileOpen] = useState(false)

  // Grupos abertos por padrão (Financeiro e IA são os mais usados)
  const [openGroups, setOpenGroups] = useState<string[]>(['Financeiro (CFO)', 'IA & Inteligência'])

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, String(sidebarOpen))
  }, [sidebarOpen])

  const toggleGroup = (title: string) => {
    setOpenGroups((prev) =>
      prev.includes(title) ? prev.filter((g) => g !== title) : [...prev, title]
    )
  }

  const toggleSidebar = () => {
    if (isMobile) {
      setMobileOpen((v) => !v)
    } else {
      setSidebarOpen((v) => !v)
    }
  }

  // Conteúdo da sidebar (reutilizado em permanent + temporary)
  const sidebarContent = (
    <Box
      sx={{
        width: isMobile ? SIDEBAR_WIDTH : (sidebarOpen ? SIDEBAR_WIDTH : SIDEBAR_COLLAPSED_WIDTH),
        height: '100%',
        overflowY: 'auto',
        overflowX: 'hidden',
        bgcolor: 'background.paper',
        borderRight: '1px solid',
        borderColor: 'divider',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Header da sidebar */}
      <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider', flexShrink: 0 }}>
        <Typography variant="caption" color="text.secondary" sx={{ letterSpacing: 0.5 }}>
          TENANT ATUAL
        </Typography>
        <Typography
          variant="subtitle2"
          fontWeight={600}
          sx={{
            fontSize: '0.875rem',
            lineHeight: 1.3,
            mt: 0.25,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {tenantName}
        </Typography>
      </Box>

      {/* Navegação */}
      <Box sx={{ flex: 1, py: 0.5, overflowY: 'auto' }}>
        <List dense disablePadding>
          {NAV_GROUPS.map((group) => {
            const isOpen = openGroups.includes(group.title)
            return (
              <Box key={group.title}>
                <Tooltip title={group.title} placement="right" disableHoverListener={sidebarOpen || isMobile}>
                <ListItemButton
                  onClick={() => {
                    if (!sidebarOpen && !isMobile) setSidebarOpen(true)
                    toggleGroup(group.title)
                  }}
                  sx={{
                    px: 1.75,
                    py: 0.9,
                    '&:hover': { bgcolor: 'action.hover' },
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 30, color: 'text.secondary' }}>
                    {group.icon}
                  </ListItemIcon>
                  {(sidebarOpen || isMobile) && (
                    <>
                      <ListItemText
                        primary={group.title}
                        primaryTypographyProps={{
                          fontWeight: 600,
                          fontSize: '0.8125rem',
                          lineHeight: 1.2,
                        }}
                      />
                      {isOpen ? <ChevronDown size={15} /> : <ChevronRight size={15} />}
                    </>
                  )}
                </ListItemButton>
                </Tooltip>

                <Collapse in={isOpen && (sidebarOpen || isMobile)} timeout="auto" unmountOnExit>
                  {group.items.map((item) => {
                    const [itemPath, itemHash] = item.path.split('#')
                    const active = location.pathname === itemPath && (!itemHash || location.hash === `#${itemHash}`)
                    return (
                      <ListItemButton
                        key={`${item.path}-${item.label}`}
                        selected={active}
                        onClick={() => {
                          navigate(item.path)
                          if (itemHash) {
                            setTimeout(() => {
                              document.getElementById(itemHash)?.scrollIntoView({ behavior: 'smooth' })
                            }, 100)
                          }
                          if (isMobile) setMobileOpen(false)
                        }}
                        sx={{
                          pl: 5.25,
                          py: 0.55,
                          pr: 1.5,
                          minHeight: 34,
                          '&.Mui-selected': {
                            bgcolor: 'primary.main',
                            color: 'primary.contrastText',
                            '&:hover': { bgcolor: 'primary.dark' },
                          },
                        }}
                      >
                        <ListItemText
                          primary={item.label}
                          primaryTypographyProps={{
                            fontSize: '0.8125rem',
                            fontWeight: active ? 600 : 400,
                            lineHeight: 1.25,
                          }}
                        />
                      </ListItemButton>
                    )
                  })}
                </Collapse>
              </Box>
            )
          })}
        </List>
      </Box>

      <Divider sx={{ mt: 'auto' }} />

      {/* Ações fixas no rodapé da sidebar */}
      <List dense disablePadding sx={{ py: 0.5, flexShrink: 0 }}>
        <ListItemButton onClick={() => onOpenCommandPalette?.()} sx={{ py: 0.7, px: 1.75 }}>
          <ListItemIcon sx={{ minWidth: 30 }}><Bot size={17} /></ListItemIcon>
          {(sidebarOpen || isMobile) && (
            <ListItemText
              primary="Perguntar à IA (Ctrl+K)"
              primaryTypographyProps={{ fontSize: '0.8125rem' }}
            />
          )}
        </ListItemButton>
        <ListItemButton sx={{ py: 0.7, px: 1.75 }}>
          <ListItemIcon sx={{ minWidth: 30 }}><Settings size={17} /></ListItemIcon>
          {(sidebarOpen || isMobile) && (
            <ListItemText
              primary="Configurações"
              primaryTypographyProps={{ fontSize: '0.8125rem' }}
            />
          )}
        </ListItemButton>
      </List>
    </Box>
  )

  const currentSidebarWidth = isMobile
    ? SIDEBAR_WIDTH
    : (sidebarOpen ? SIDEBAR_WIDTH : SIDEBAR_COLLAPSED_WIDTH)

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100dvh', overflow: 'hidden' }}>
      {/* Topbar */}
      <AppBar position="fixed" elevation={0} sx={{ zIndex: theme.zIndex.drawer + 1 }}>
        <Toolbar sx={{ minHeight: 56, px: { xs: 1.5, sm: 2 }, gap: 1.5 }}>
          <IconButton
            edge="start"
            color="inherit"
            onClick={toggleSidebar}
            aria-label="Alternar menu"
            sx={{ mr: 0.5 }}
          >
            <Menu size={21} />
          </IconButton>

          <Typography
            variant="h6"
            sx={{
              fontWeight: 600,
              letterSpacing: '-0.015em',
              fontSize: { xs: '1.05rem', sm: '1.15rem' },
              flexGrow: 1,
            }}
          >
            ContractOps AI
          </Typography>

          {/* Status / Production Mode indicator (Onda 4 hardening) */}
          <Chip
            label={import.meta.env.PROD ? 'PRODUÇÃO' : 'Online'}
            size="small"
            color={import.meta.env.PROD ? 'success' : 'default'}
            variant={import.meta.env.PROD ? 'filled' : 'outlined'}
            sx={{ height: 22, fontSize: '0.7rem', borderRadius: 1, display: { xs: 'none', sm: 'flex' }, fontWeight: 600 }}
          />

          <IconButton
            color="inherit"
            onClick={onOpenCommandPalette}
            aria-label="Abrir paleta de comandos (Ctrl+K)"
            size="medium"
          >
            <Search size={19} />
          </IconButton>

          <IconButton color="inherit" aria-label="Notificações" size="medium">
            <Bell size={19} />
          </IconButton>

          <Avatar
            sx={{
              width: 28,
              height: 28,
              bgcolor: 'secondary.main',
              fontSize: '0.75rem',
              fontWeight: 600,
            }}
          >
            CF
          </Avatar>
        </Toolbar>
      </AppBar>

      {/* Espaçador da AppBar fixa */}
      <Box sx={{ height: 56, flexShrink: 0 }} />

      <Box sx={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Sidebar Desktop (permanent) */}
        {!isMobile && (
          <Box
            sx={{
              width: currentSidebarWidth,
              flexShrink: 0,
              transition: 'width 160ms cubic-bezier(0.2, 0, 0, 1)',
              borderRight: '1px solid',
              borderColor: 'divider',
              overflow: 'hidden',
            }}
          >
            {sidebarContent}
          </Box>
        )}

        {/* Mobile Drawer */}
        {isMobile && (
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={() => setMobileOpen(false)}
            ModalProps={{ keepMounted: true }}
            PaperProps={{ sx: { width: SIDEBAR_WIDTH } }}
          >
            {sidebarContent}
          </Drawer>
        )}

        {/* Área Principal — scroll delegado para o conteúdo da página */}
        <Box
          component="main"
          sx={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            minWidth: 0, // evita overflow em telas muito estreitas
          }}
        >
          {children}
        </Box>
      </Box>
    </Box>
  )
}
