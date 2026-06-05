import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../client'
import { queryKeys } from '../queryKeys'
import { useTenant } from './useTenant'

export function useFluxoCaixaReal(inicio: string, fim: string) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.financeiro.fluxoReal(tenantId, inicio, fim),
    queryFn: () =>
      apiGet<Record<string, unknown>>(
        `/financeiro/fluxo-caixa/real?inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`
      ),
    enabled: !!tenantId && !!inicio && !!fim,
    staleTime: 60_000,
  })
}
