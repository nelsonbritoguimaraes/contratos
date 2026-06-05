import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../client'
import { queryKeys } from '../queryKeys'
import { useTenant } from './useTenant'

export function useCalendarioObrigacoes(mes: number, ano: number) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.financeiro.calendarioObrigacoes(tenantId, mes, ano),
    queryFn: () =>
      apiGet<any[]>(
        `/financeiro/relatorios/calendario-obrigacoes?mes=${mes}&ano=${ano}&tenantId=${tenantId}`
      ),
    enabled: !!tenantId && mes >= 1 && mes <= 12,
    staleTime: 120_000,
  })
}
