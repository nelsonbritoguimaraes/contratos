import './lib/agGridSetup'
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider, CssBaseline } from '@mui/material'
import { QueryClientProvider } from '@tanstack/react-query'
import { contractopsTheme } from './theme/contractops-theme'
import { NotificationProvider } from './components/NotificationProvider'
import { TenantProvider } from './api/hooks/useTenant'
import { AuthProvider } from './api/hooks/useAuth'
import { queryClient } from './api/queryClient'
import { Toaster } from './components/Toaster'
import { ErrorBoundary } from './components/ErrorBoundary'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={contractopsTheme}>
          <CssBaseline />
          <AuthProvider>
            <TenantProvider>
              <NotificationProvider>
                <BrowserRouter>
                  <App />
                </BrowserRouter>
                <Toaster />
              </NotificationProvider>
            </TenantProvider>
          </AuthProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  </React.StrictMode>,
)
