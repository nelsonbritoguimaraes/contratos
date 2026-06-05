import { useMutation } from '@tanstack/react-query'
import { apiPost } from '../client'
import { useTenant } from './useTenant'

export function useCalcularRetencoes() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (body: {
      valorServico: number
      municipioIbge?: string
      naturezaServico?: string
      aplicarInss?: boolean
    }) =>
      apiPost<any[]>(`/financeiro/compliance/retencoes/calcular?tenantId=${tenantId}`, {
        valorServico: body.valorServico,
        municipioIbge: body.municipioIbge ?? '3550308',
        naturezaServico: body.naturezaServico ?? 'TERCEIRIZACAO',
        aplicarInss: body.aplicarInss ?? true,
      }),
  })
}

export function useGerarDarf() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (retencaoId: string) =>
      apiPost<any>(`/financeiro/compliance/darf/${retencaoId}?tenantId=${tenantId}`, {}),
  })
}
