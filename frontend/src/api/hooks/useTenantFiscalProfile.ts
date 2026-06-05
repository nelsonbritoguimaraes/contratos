import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPut } from '../client'
import { useTenant } from './useTenant'

export interface TenantFiscalProfile {
  tenantId: string
  desoneracaoFolha: boolean
  aliquotaInssRetencao: number
  simplesNacional: boolean
  municipioIbgePadrao?: string
  cnpjPrestador?: string
  updatedAt?: string
}

export function useTenantFiscalProfile() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'perfil-fiscal'],
    queryFn: () => apiGet<TenantFiscalProfile>(`/financeiro/perfil-fiscal?tenantId=${tenantId}`),
    enabled: !!tenantId,
  })
}

export function useUpdateTenantFiscalProfile() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: Partial<TenantFiscalProfile>) =>
      apiPut(`/financeiro/perfil-fiscal?tenantId=${tenantId}`, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId, 'perfil-fiscal'] }),
  })
}
