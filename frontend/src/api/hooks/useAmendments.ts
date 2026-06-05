/**
 * useAmendments — Hook para Aditivos / Repactuação / Amendments de Contrato (SPEC 6.2)
 * Alinhado com ContractController + ContractAmendmentService
 */
import { useQuery, useMutation } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPost, apiPut, apiDelete } from '../client'
import { useTenant } from './useTenant'
import { ContractAmendment, CreateAmendmentRequest } from '../types'

export function useAmendments(contractId?: string) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.contracts.amendments(tenantId, contractId || ''),
    queryFn: () => apiGet<ContractAmendment[]>(`/contracts/${contractId}/amendments?tenantId=${tenantId}`),
    enabled: !!contractId,
    staleTime: 1000 * 60,
  })
}

export function useCreateAmendment(contractId: string) {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: (payload: CreateAmendmentRequest) =>
      apiPost<ContractAmendment>(`/contracts/${contractId}/amendments?tenantId=${tenantId}`, payload),
  })
}

export function useUpdateAmendment(contractId: string, amendmentId: string) {
  return useMutation({
    mutationFn: (payload: Partial<CreateAmendmentRequest>) =>
      apiPut<ContractAmendment>(`/contracts/${contractId}/amendments/${amendmentId}`, payload),
  })
}

export function useDeleteAmendment(contractId: string, amendmentId: string) {
  return useMutation({
    mutationFn: () => apiDelete(`/contracts/${contractId}/amendments/${amendmentId}`),
  })
}
