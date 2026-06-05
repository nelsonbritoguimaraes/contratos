/**
 * useContracts — Hook simples para listar contratos (usado em seletores de Medição, etc.)
 */
import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet } from '../client'
import { useTenant } from './useTenant'
import { Contract } from '../types'

export function useContracts() {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.contracts.all(tenantId),
    queryFn: () => apiGet<Contract[]>(`/contracts?tenantId=${tenantId}`),
    staleTime: 1000 * 60 * 2,
  })
}
