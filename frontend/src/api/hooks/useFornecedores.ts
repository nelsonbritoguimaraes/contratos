import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'

export interface FornecedorRow {
  id: string
  razaoSocial: string
  cnpj?: string
  contato?: string
  categoria?: string
  ativo: boolean
}

export function useFornecedores() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['fornecedores', tenantId],
    queryFn: () => apiGet<FornecedorRow[]>(`/financeiro/fornecedores?tenantId=${tenantId}`),
  })
}

export function useCriarFornecedor() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: { razaoSocial: string; cnpj?: string; contato?: string; categoria?: string }) =>
      apiPost<FornecedorRow>(`/financeiro/fornecedores?tenantId=${tenantId}`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['fornecedores', tenantId] }),
  })
}
