import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../client'
import { useTenant } from './useTenant'

export interface DreFinanceiroContrato {
  contratoId: string
  periodo: { inicio: string; fim: string }
  receitaBruta: number
  retencoes: number
  receitaLiquida: number
  custosDiretos: number
  glosaProvisao: number
  margemOperacional: number
  nfsEmitidas: number
}

export function useDreFinanceiro(contratoId?: string, inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'dre-contrato', contratoId, inicio, fim],
    queryFn: () =>
      apiGet<DreFinanceiroContrato>(
        `/financeiro/relatorios/dre-contrato/${contratoId}?inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`
      ),
    enabled: !!tenantId && !!contratoId && !!inicio && !!fim,
    staleTime: 1000 * 60 * 2,
  })
}
