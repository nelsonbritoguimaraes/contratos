/**
 * UnauthorizedPage — Página 403 (Acesso Negado)
 *
 * Exibida quando o usuário autenticado não possui as roles necessárias
 * para acessar uma rota protegida pelo RoleGuard.
 */
import { Box, Paper, Typography, Button, Stack } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { ShieldOff, ArrowLeft, LogOut } from 'lucide-react'
import { useAuth } from '../../api/hooks/useAuth'

export default function UnauthorizedPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()

  const handleGoDashboard = () => {
    navigate('/financeiro/dashboard', { replace: true })
  }

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <Box
      sx={{
        minHeight: '100dvh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: { xs: 2, sm: 2.5, md: 3 },
      }}
    >
      <Paper
        elevation={0}
        sx={{
          width: '100%',
          maxWidth: 460,
          borderRadius: 3,
          border: '1px solid',
          borderColor: 'divider',
          p: { xs: 3, sm: 4 },
          textAlign: 'center',
        }}
      >
        <Box
          sx={{
            width: 64,
            height: 64,
            borderRadius: 2,
            bgcolor: 'error.light',
            color: 'error.main',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            mx: 'auto',
            mb: 3,
          }}
        >
          <ShieldOff size={32} />
        </Box>

        <Typography variant="h4" fontWeight={700} gutterBottom>
          Acesso Negado
        </Typography>

        <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
          Você não tem permissão para acessar esta página.
        </Typography>

        <Typography variant="body2" color="text.secondary" sx={{ mb: 4, opacity: 0.8 }}>
          Entre em contato com o administrador do sistema se acredita que isso é um erro,
          ou navegue para uma página que você tenha acesso.
        </Typography>

        <Stack direction="row" spacing={1.5} justifyContent="center">
          <Button
            variant="contained"
            startIcon={<ArrowLeft size={16} />}
            onClick={handleGoDashboard}
            sx={{ fontWeight: 600 }}
          >
            Voltar ao Dashboard
          </Button>

          <Button
            variant="outlined"
            color="error"
            startIcon={<LogOut size={16} />}
            onClick={handleLogout}
            sx={{ fontWeight: 600 }}
          >
            Sair
          </Button>
        </Stack>
      </Paper>
    </Box>
  )
}