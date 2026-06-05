import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../client'
import { queryKeys } from '../queryKeys'
import { useTenant } from './useTenant'

export function useContasPagar(filters?: {
  contratoId?: string
  status?: string
  origem?: string
  vencimentoDe?: string
  vencimentoAte?: string
}) {
  const { tenantId } = useTenant()

  const params = new URLSearchParams({ tenantId: tenantId || '' })
  if (filters?.contratoId) params.set('contratoId', filters.contratoId)
  if (filters?.status) params.set('status', filters.status)
  if (filters?.origem) params.set('origem', filters.origem)
  if (filters?.vencimentoDe) params.set('vencimentoDe', filters.vencimentoDe)
  if (filters?.vencimentoAte) params.set('vencimentoAte', filters.vencimentoAte)

  const query = useQuery({
    queryKey: queryKeys.financeiro.contasPagar(tenantId, filters),
    queryFn: () => apiGet<any>(`/financeiro/pagar?${params}`),
    staleTime: 60_000,
    enabled: !!tenantId,
  })

  const raw = query.data
  const contas = Array.isArray(raw?.contas) ? raw.contas : []

  return {
    data: contas,
    resumo: raw,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error as Error | null,
    refetch: query.refetch,
  }
}

export function usePagarConta() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: {
      contaAPagarId: string
      data: string
      valor: number
      contaBancariaId: string
      formaPagamento?: string
    }) =>
      apiPost(`/financeiro/pagar/${payload.contaAPagarId}/baixar?tenantId=${tenantId}`, {
        data: payload.data,
        valor: payload.valor,
        contaBancariaId: payload.contaBancariaId,
        formaPagamento: payload.formaPagamento || 'PIX',
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId] }),
  })
}

export function useGerarCobranca() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (payload: { contaAReceberId: string; tipo: 'PIX' | 'BOLETO' }) =>
      apiPost(`/financeiro/receber/${payload.contaAReceberId}/cobranca?tenantId=${tenantId}`, {
        tipo: payload.tipo,
      }),
  })
}

export function useImportarCnabRetorno() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: { contaBancariaId: string; conteudo: string }) =>
      apiPost(
        `/financeiro/pagamentos/import/cnab-retorno?contaBancariaId=${payload.contaBancariaId}&tenantId=${tenantId}`,
        payload.conteudo
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId] }),
  })
}
