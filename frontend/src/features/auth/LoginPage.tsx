/**
 * LoginPage — Enterprise authentication via Keycloak JWT
 *
 * - Authentication via JWT obtained from Keycloak / SSO provider
 * - No demo shortcuts — production-ready
 * - Clean, professional UI aligned with ContractOps design system
 */
import { useState, useEffect } from 'react'
import { Box, Paper, Typography, Button, TextField, Stack, Alert } from '@mui/material'
import { useAuth } from '../../api/hooks/useAuth'
import { useNavigate } from 'react-router-dom'
import { Key, ArrowRight } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { authLoginSchema, type AuthLoginForm } from '../../schemas/authSchema'

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm<AuthLoginForm>({
    resolver: zodResolver(authLoginSchema),
    defaultValues: { token: '' },
  })

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/financeiro/dashboard', { replace: true })
    }
  }, [isAuthenticated, navigate])

  const onLogin = async (data: AuthLoginForm) => {
    setIsLoading(true)
    setError(null)
    try {
      // Parse JWT to extract user info (roles, name) from claims
      const token = data.token.trim()
      const payload = JSON.parse(atob(token.split('.')[1]))
      const user = {
        name: payload.name || payload.preferred_username || 'Usuário',
        email: payload.email,
        roles: payload.realm_access?.roles || payload.roles || ['USER'],
      }
      login(token, user)
      navigate('/financeiro/dashboard', { replace: true })
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Token inválido ou expirado'
      setError(message)
    } finally {
      setIsLoading(false)
    }
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
          overflow: 'hidden',
        }}
      >
        {/* Header com branding */}
        <Box
          sx={{
            px: { xs: 2.5, sm: 3 },
            py: { xs: 2.25, sm: 2.5 },
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
          }}
        >
          <Box
            sx={{
              width: 42,
              height: 42,
              borderRadius: 1.5,
              bgcolor: 'rgba(255,255,255,0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Key size={22} />
          </Box>
          <Box>
            <Typography variant="h5" fontWeight={700} letterSpacing="-0.015em">
              ContractOps
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.85, display: 'block', mt: -0.25 }}>
              ERP de Terceirização de Mão de Obra
            </Typography>
          </Box>
        </Box>

        <Box sx={{ p: { xs: 2.5, sm: 3 } }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5, lineHeight: 1.45 }}>
            Plataforma completa para gestão de contratos públicos, folha, ponto, financeiro e contabilidade.
            <br />Autenticação via JWT (Spring Security + Keycloak).
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2.25, borderRadius: 2 }}>
              {error}
            </Alert>
          )}

          {/* === AUTENTICAÇÃO JWT === */}
          <Stack spacing={1.5}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Key size={16} />
              <Typography variant="subtitle2" fontWeight={600}>
                Autenticação JWT
              </Typography>
            </Box>

            <TextField
              fullWidth
              label="JWT Token (Bearer)"
              placeholder="Cole aqui o token obtido via Keycloak / SSO corporativo"
              multiline
              minRows={2}
              maxRows={3}
              error={!!errors.token}
              helperText={errors.token?.message}
              sx={{
                fontFamily: 'monospace',
                fontSize: '0.75rem',
                '& .MuiInputBase-root': { borderRadius: 2 },
              }}
              {...register('token')}
            />

            <Button
              fullWidth
              variant="contained"
              size="large"
              endIcon={<ArrowRight size={18} />}
              onClick={handleSubmit(onLogin)}
              disabled={isLoading}
              sx={{ py: 1.05, fontWeight: 600 }}
            >
              {isLoading ? 'Validando...' : 'Entrar com Token JWT'}
            </Button>
          </Stack>

          {/* Rodapé técnico */}
          <Box sx={{ mt: 3, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', textAlign: 'center', lineHeight: 1.5 }}>
              Backend: Spring Boot 3.4 + Java 21 + Spring Security + JWT<br />
              Frontend: React 18 + MUI v5 + TanStack Query • Multi-tenant (tenantId no token)
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', textAlign: 'center', mt: 0.75, opacity: 0.7 }}>
              Conecte ao seu provedor de identidade (Keycloak, Auth0, Azure AD...) para obter o token JWT.
            </Typography>
          </Box>
        </Box>
      </Paper>
    </Box>
  )
}