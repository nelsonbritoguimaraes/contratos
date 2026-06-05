import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPost, apiPut, apiDelete } from '../client'
import { useTenant } from './useTenant'

export interface PayrollRubric {
  id?: string
  code: string
  description: string
  type: string
  calculationType: string
  fixedValue?: number
  percentage?: number
  reference?: string
  isActive?: boolean
  displayOrder?: number
}

export function useRubrics(activeOnly = true) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.rh.rubricas(tenantId, activeOnly),
    queryFn: () =>
      apiGet<PayrollRubric[]>(`/rh/rubrics?activeOnly=${activeOnly}&tenantId=${tenantId}`),
  })
}

export function useCreateRubric() {
  const { tenantId } = useTenant()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (body: Partial<PayrollRubric>) =>
      apiPost<PayrollRubric>(`/rh/rubrics?tenantId=${tenantId}`, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.rh.rubricas(tenantId) })
    },
  })
}

export function useUpdateRubric() {
  const { tenantId } = useTenant()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...body }: Partial<PayrollRubric> & { id: string }) =>
      apiPut<PayrollRubric>(`/rh/rubrics/${id}?tenantId=${tenantId}`, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.rh.rubricas(tenantId) })
    },
  })
}

export function useDeactivateRubric() {
  const { tenantId } = useTenant()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => apiDelete(`/rh/rubrics/${id}?tenantId=${tenantId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.rh.rubricas(tenantId) })
    },
  })
}
