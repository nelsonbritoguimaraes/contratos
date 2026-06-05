import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'

export interface FechamentoFinanceiro {
  id?: string
  dataInicio: string
  dataFim: string
  status: string
  saldoCaixaFinal?: number
  totalRecebimentos?: number
  totalPagamentos?: number
  observacoes?: string
  dataFechamento?: string
}

export function useFechamentosFinanceiros() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'fechamento-mensal'],
    queryFn: () => apiGet<FechamentoFinanceiro[]>(`/financeiro/fechamento-mensal?tenantId=${tenantId}`),
    enabled: !!tenantId,
  })
}

export function useFecharMesFinanceiro() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { inicio: string; fim: string; fechadoPor?: string }) =>
      apiPost(
        `/financeiro/fechamento-mensal/fechar?inicio=${params.inicio}&fim=${params.fim}&fechadoPor=${params.fechadoPor || 'cfo@contractops'}&tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId] }),
  })
}

export function useReabrirMesFinanceiro() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { inicio: string; fim: string; usuario?: string }) =>
      apiPost(
        `/financeiro/fechamento-mensal/reabrir?inicio=${params.inicio}&fim=${params.fim}&usuario=${params.usuario || 'cfo@contractops'}&tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId] }),
  })
}
