import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../client'
import { useTenant } from './useTenant'

export interface FinancialAuditLogEntry {
  id?: string
  tenantId: string
  entidadeTipo: string
  entidadeId?: string
  acao: string
  usuario?: string
  detalhe?: string
  createdAt: string
}

export function useAuditoriaFinanceira(limit = 100) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'auditoria', limit],
    queryFn: () =>
      apiGet<FinancialAuditLogEntry[]>(`/financeiro/auditoria?limit=${limit}&tenantId=${tenantId}`),
    enabled: !!tenantId,
    staleTime: 1000 * 30,
  })
}
