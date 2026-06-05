/**
 * useFluxoProjetado — Hook para Fluxo de Caixa Projetado 13 semanas (CFO)
 */
import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet } from '../client'
import { useTenant } from './useTenant'
import { FluxoCaixaProjetadoResponse } from '../types'

export function useFluxoProjetado(semanas: number = 13, cenario: string = 'BASE') {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.financeiro.fluxoProjetado(tenantId, semanas, cenario),
    queryFn: () =>
      apiGet<FluxoCaixaProjetadoResponse>(
        `/financeiro/fluxo-caixa/projetado?semanas=${semanas}&cenario=${cenario}`
      ),
    enabled: !!tenantId,
    staleTime: 1000 * 60 * 3,
  })
}
