/**
 * useAuth — JWT Authentication Context (Enterprise)
 * Authentication via Keycloak JWT only. No demo tokens.
 * - Stores token in localStorage (via client.ts helpers)
 * - Auto logout on 401 (listens to window event from client)
 * - Exposes isAuthenticated + user info
 */
import { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react'
import { setAuthToken, clearAuthToken, getAuthToken } from '../client'

interface AuthUser {
  id?: string
  name?: string
  email?: string
  roles?: string[]
}

interface AuthContextValue {
  isAuthenticated: boolean
  token: string | null
  user: AuthUser | null
  login: (token: string, user?: AuthUser) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getAuthToken())
  const [user, setUser] = useState<AuthUser | null>(() => {
    const saved = localStorage.getItem('contractops:user')
    return saved ? JSON.parse(saved) : null
  })

  const isAuthenticated = !!token

  const login = useCallback((newToken: string, newUser?: AuthUser) => {
    setAuthToken(newToken)
    setToken(newToken)
    if (newUser) {
      setUser(newUser)
      localStorage.setItem('contractops:user', JSON.stringify(newUser))
    }
  }, [])

  const logout = useCallback(() => {
    clearAuthToken()
    setToken(null)
    setUser(null)
    localStorage.removeItem('contractops:user')
  }, [])

  // Listen for 401 from the API client
  useEffect(() => {
    const handleUnauthorized = () => {
      logout()
    }
    window.addEventListener('auth:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized)
  }, [logout])

  // Keep token in sync if changed from elsewhere (rare)
  useEffect(() => {
    const sync = () => setToken(getAuthToken())
    window.addEventListener('auth:token-changed', sync)
    return () => window.removeEventListener('auth:token-changed', sync)
  }, [])

  const value: AuthContextValue = {
    isAuthenticated,
    token,
    user,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return ctx
}