import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiPost } from '../client'
import { useTenant } from './useTenant'

export interface ProvisionarGlosaPayload {
  measurementId: string
  valorGlosa: number
}

export function useProvisionarGlosa() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: ProvisionarGlosaPayload) =>
      apiPost(`/financeiro/provisao-glosa?tenantId=${tenantId}`, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['financeiro', tenantId] })
      qc.invalidateQueries({ queryKey: ['glosas'] })
    },
  })
}
