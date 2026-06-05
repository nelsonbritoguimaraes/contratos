/**
 * useMeasurements — Hooks para o módulo de Medição (Fase 1)
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'
import { Measurement } from '../types'

export function useMeasurements(contractId: string | undefined) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.measurements.all(tenantId, contractId || ''),
    queryFn: () => apiGet<Measurement[]>(`/measurements?contractId=${contractId}`),
    enabled: !!contractId,
  })
}

interface CalculateMeasurementParams {
  contractId: string
  period: string // YYYY-MM-DD
}

export function useCalculateMeasurement() {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: async ({ contractId, period }: CalculateMeasurementParams) => {
      return apiPost<any>(
        `/measurements/calculate?contractId=${contractId}&period=${period}&tenantId=${tenantId}`,
        {}
      )
    },
  })
}

export function useApproveMeasurement() {
  const { tenantId } = useTenant()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      measurementId,
      nivel = 1,
      justificativa = '',
    }: {
      measurementId: string
      nivel?: number
      justificativa?: string
    }) =>
      apiPost<any>(
        `/measurements/${measurementId}/approve?tenantId=${tenantId}`,
        { nivel, justificativa }
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['measurements'] })
      queryClient.invalidateQueries({ queryKey: ['financeiro'] })
    },
  })
}
