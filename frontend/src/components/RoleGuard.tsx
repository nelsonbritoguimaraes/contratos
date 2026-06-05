/**
 * RoleGuard — Verifica se o usuário autenticado possui as roles necessárias
 *
 * - Se não autenticado, redireciona para /login
 * - Se autenticado mas sem role, redireciona para /unauthorized
 * - Se autorizado, renderiza children
 */
import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../api/hooks/useAuth'

interface RoleGuardProps {
  requiredRoles: string[]
  children: ReactNode
}

export default function RoleGuard({ requiredRoles, children }: RoleGuardProps) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  // Não autenticado → login
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  // Verifica se o usuário possui pelo menos UMA das roles necessárias
  const userRoles = user?.roles ?? []
  const hasRequiredRole = requiredRoles.some((role) => userRoles.includes(role))

  // Autenticado mas sem role → unauthorized
  if (!hasRequiredRole) {
    return <Navigate to="/unauthorized" replace />
  }

  // Autorizado → renderiza children
  return <>{children}</>
}