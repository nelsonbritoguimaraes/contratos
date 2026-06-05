import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'

export interface LancamentoRow {
  id: string
  tipo: string
  fornecedorId?: string
  descricao: string
  valor: number
  data: string
  categoria?: string
  status: string
}

export function useLancamentos(tipo?: 'COMPRA' | 'DESPESA') {
  const { tenantId } = useTenant()
  const q = tipo ? `&tipo=${tipo}` : ''
  return useQuery({
    queryKey: ['lancamentos', tenantId, tipo],
    queryFn: () => apiGet<LancamentoRow[]>(`/financeiro/lancamentos?tenantId=${tenantId}${q}`),
  })
}

export function useCriarLancamento() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: {
      tipo: string
      fornecedorId?: string
      descricao: string
      valor: number
      data?: string
      categoria?: string
      status?: string
    }) => apiPost<LancamentoRow>(`/financeiro/lancamentos?tenantId=${tenantId}`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['lancamentos', tenantId] }),
  })
}
