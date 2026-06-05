/**
 * useSimulation — Hook para o Simulador What-If do CFO
 * Chama POST /api/financeiro/simulacao
 */
import { useMutation } from '@tanstack/react-query'
import { apiPost } from '../client'
import { useTenant } from './useTenant'

export interface SimulationParams {
  atrasoMedioRecebimento?: number
  aumentoFolha?: number
  reducaoFaturamento?: number
  [key: string]: any
}

export interface SimulationResult {
  cenario: string
  entradasAjustadas: number
  saidasAjustadas: number
  saldoFinalSimulado: number
  impactoVsBase: number
  detalhes?: any
}

export function useSimulation() {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: async (params: SimulationParams) => {
      return apiPost<SimulationResult>(
        `/financeiro/simulacao?tenantId=${tenantId}`,
        params
      )
    },
  })
}
