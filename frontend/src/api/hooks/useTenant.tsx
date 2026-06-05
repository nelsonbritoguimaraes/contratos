/**
 * useTenant — Hook central de Multi-Tenancy
 * Fornece tenantId global, switcher e persistência.
 * Todas as queries TanStack Query devem injetar tenantId deste hook.
 */

import { useState, useEffect, useCallback, useMemo, createContext, useContext, ReactNode } from 'react'
import { UUID } from '../types'
import { queryClient } from '../queryClient'

interface TenantContextValue {
  tenantId: UUID
  tenantName: string
  setTenant: (id: UUID, name: string) => void
  availableTenants: Array<{ id: UUID; name: string }>
}

const DEFAULT_TENANT: UUID = '11111111-1111-1111-1111-111111111111'
const DEFAULT_TENANT_NAME = 'Grupo Segurança Brasil'

const TenantContext = createContext<TenantContextValue | null>(null)

export function TenantProvider({ children }: { children: ReactNode }) {
  const [tenantId, setTenantId] = useState<UUID>(() => {
    const saved = localStorage.getItem('contractops:tenantId')
    return (saved as UUID) || DEFAULT_TENANT
  })

  const [tenantName, setTenantName] = useState<string>(() => {
    return localStorage.getItem('contractops:tenantName') || DEFAULT_TENANT_NAME
  })

  const availableTenants = useMemo(() => [
    { id: DEFAULT_TENANT, name: 'Grupo Segurança Brasil' },
    { id: '22222222-2222-2222-2222-222222222222' as UUID, name: 'Limpeza Nacional Ltda' },
    { id: '33333333-3333-3333-3333-333333333333' as UUID, name: 'Vigilância Alpha (Filial SP)' },
  ], [])

  const setTenant = useCallback((id: UUID, name: string) => {
    setTenantId(id)
    setTenantName(name)
    localStorage.setItem('contractops:tenantId', id)
    localStorage.setItem('contractops:tenantName', name)
    // Invalida TODAS as queries TanStack (melhor UX após troca de tenant)
    queryClient.invalidateQueries()
    window.dispatchEvent(new CustomEvent('tenant-changed', { detail: { tenantId: id } }))
  }, [])

  useEffect(() => {
    // Garante que o tenant salvo sempre exista na lista
    const exists = availableTenants.some(t => t.id === tenantId)
    if (!exists) {
      setTenant(DEFAULT_TENANT, DEFAULT_TENANT_NAME)
    }
  }, [availableTenants, tenantId, setTenant])

  const value: TenantContextValue = {
    tenantId,
    tenantName,
    setTenant,
    availableTenants,
  }

  return <TenantContext.Provider value={value}>{children}</TenantContext.Provider>
}

export function useTenant() {
  const context = useContext(TenantContext)
  if (!context) {
    throw new Error('useTenant must be used within a TenantProvider')
  }
  return context
}