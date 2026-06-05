/**
 * useCfoDashboard — Hook real de dados para o CFO (Fase 0/2)
 * Usa TanStack Query + queryKeys + apiGet.
 * Totalmente multi-tenant via useTenant.
 */
import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet } from '../client'
import { useTenant } from './useTenant'
import { CfoDashboardResponse } from '../types'

export function useCfoDashboard(dataCorte?: string) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.financeiro.cfoDashboard(tenantId, dataCorte),
    queryFn: () => {
      const params = dataCorte ? `?dataCorte=${dataCorte}` : ''
      return apiGet<CfoDashboardResponse>(`/financeiro/dashboard/cfo${params}`)
    },
    enabled: !!tenantId,
    staleTime: 1000 * 60, // 1 minuto para dashboards executivos
  })
}
