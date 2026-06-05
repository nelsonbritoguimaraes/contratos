import { Component, ReactNode, ErrorInfo } from 'react'
import { Box, Typography, Button, Paper } from '@mui/material'
import { AlertTriangle, RefreshCw } from 'lucide-react'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[ErrorBoundary]', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            minHeight: '100dvh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: 'background.default',
            p: 3,
          }}
        >
          <Paper
            elevation={0}
            sx={{
              maxWidth: 520,
              width: '100%',
              p: 4,
              borderRadius: 3,
              border: '1px solid',
              borderColor: 'error.light',
              textAlign: 'center',
            }}
          >
            <AlertTriangle size={48} color="#d32f2f" />
            <Typography variant="h5" fontWeight={700} sx={{ mt: 2 }}>
              Erro inesperado
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 3 }}>
              Ocorreu um erro que impediu a renderização desta página. Tente recarregar ou entre em contato com o suporte.
            </Typography>
            {this.state.error && (
              <Paper
                variant="outlined"
                sx={{
                  p: 2,
                  mb: 3,
                  bgcolor: 'action.hover',
                  textAlign: 'left',
                  fontFamily: 'monospace',
                  fontSize: '0.8rem',
                  overflow: 'auto',
                  maxHeight: 120,
                }}
              >
                {this.state.error.message}
              </Paper>
            )}
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
              <Button variant="outlined" startIcon={<RefreshCw size={16} />} onClick={this.handleReset}>
                Tentar novamente
              </Button>
              <Button variant="contained" onClick={this.handleReload}>
                Recarregar página
              </Button>
            </Box>
          </Paper>
        </Box>
      )
    }

    return this.props.children
  }
}