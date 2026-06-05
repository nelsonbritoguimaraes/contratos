/**
 * useIAsk — Hook principal para o motor de IA (Fase 2)
 * Chama o AgentRouter inteligente via POST /api/ia/ask
 * Também expõe chamadas diretas para agentes específicos.
 */
import { useMutation, useQuery } from '@tanstack/react-query'
import { apiPost, apiGet } from '../client'
import { useTenant } from './useTenant'
import { IaAskResponse } from '../types'
import { queryKeys } from '../queryKeys'

interface AskParams {
  question: string
  context?: Record<string, any>
}

export function useIAsk() {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: async ({ question, context = {} }: AskParams) => {
      return apiPost<IaAskResponse>('/ia/ask', {
        question,
        tenantId,
        ...context,
      })
    },
  })
}

// Chamada direta a um agente específico (útil para botões "Analisar com X Agent")
export function useAgente(agente: string) {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: async (payload: Record<string, any>) => {
      const path = `/ia/agente/${agente.toLowerCase().replace('agent', '')}`
      return apiPost<string>(path, {
        ...payload,
        tenantId,
      })
    },
  })
}

// Histórico de chamadas IA
export function useIaCalls(limit: number = 30) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.ia.calls(tenantId, limit),
    queryFn: () => apiGet<any[]>(`/ia/calls?limit=${limit}&tenantId=${tenantId}`),
    staleTime: 1000 * 30,
  })
}
