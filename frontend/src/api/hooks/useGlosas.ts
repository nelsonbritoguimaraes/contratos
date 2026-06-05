import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPatch } from '../client'
import { useTenant } from './useTenant'

export interface Glosa {
  id?: string
  contractId: string
  measurementPeriod: string
  glosaType: string
  description?: string
  glosaAmount: number
  status: string
}

export function useGlosas(contractId: string | undefined, period: string | undefined) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.glosas.all(tenantId, contractId || '', period),
    queryFn: () =>
      apiGet<Glosa[]>(`/glosas?contractId=${contractId}&period=${period}&tenantId=${tenantId}`),
    enabled: !!contractId && !!period,
  })
}

export function useUpdateGlosa() {
  const { tenantId } = useTenant()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      id,
      status,
      description,
    }: {
      id: string
      status: string
      description?: string
    }) =>
      apiPatch<Glosa>(`/glosas/${id}?tenantId=${tenantId}`, { status, description }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['glosas'] })
    },
  })
}
