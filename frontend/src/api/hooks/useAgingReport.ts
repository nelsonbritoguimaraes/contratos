/**
 * useAgingReport — Hook real para Relatório de Aging (AR / AP)
 * Chama endpoints de produção:
 *   GET /api/financeiro/relatorios/aging/ar
 *   GET /api/financeiro/relatorios/aging/ap
 *
 * Retorna dados transformados prontos para EnterpriseDataGrid (com percentual calculado).
 * Multi-tenant via useTenant + TanStack Query (queryKeys.financeiro.agingAR/AP).
 * Fallback graceful quando backend indisponível ou sem dados.
 */
import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet } from '../client'
import { useTenant } from './useTenant'
import { AgingReportDTO, AgingBucket } from '../types'

export interface AgingGridRow {
  faixa: string
  quantidade: number
  valor: number
  percentual: string
}

function formatFaixa(faixa: string): string {
  const f = (faixa || '').trim()
  if (f === '>90' || f === '> 90' || f.toLowerCase() === '>90') return '> 90 dias'
  if (f.includes('-') && !f.toLowerCase().includes('dia')) return `${f} dias`
  if (f === '0-30' || f === '31-60' || f === '61-90') return `${f} dias`
  return f || 'N/A'
}

function processToGridRows(report?: AgingReportDTO): AgingGridRow[] {
  if (!report || !Array.isArray(report.buckets) || report.buckets.length === 0) {
    return []
  }

  const total = Number(report.total ?? report.buckets.reduce((sum, b) => sum + Number(b.valor || 0), 0))

  return report.buckets.map((b: AgingBucket) => {
    const valor = Number(b.valor || 0)
    const qtd = Number(b.quantidade || 0)
    const pct = total > 0 ? Math.round((valor / total) * 100) : 0

    return {
      faixa: formatFaixa(b.faixa),
      quantidade: qtd,
      valor,
      percentual: `${pct}%`,
    }
  })
}

export function useAgingReport(dataCorte?: string) {
  const { tenantId } = useTenant()

  const arQuery = useQuery({
    queryKey: queryKeys.financeiro.agingAR(tenantId, dataCorte),
    queryFn: () => {
      const params = dataCorte ? `?dataCorte=${encodeURIComponent(dataCorte)}` : ''
      return apiGet<AgingReportDTO>(`/financeiro/relatorios/aging/ar${params}`)
    },
    enabled: !!tenantId,
    staleTime: 1000 * 60 * 2, // 2 minutos — relatório executivo
    retry: 1,
  })

  const apQuery = useQuery({
    queryKey: queryKeys.financeiro.agingAP(tenantId, dataCorte),
    queryFn: () => {
      const params = dataCorte ? `?dataCorte=${encodeURIComponent(dataCorte)}` : ''
      return apiGet<AgingReportDTO>(`/financeiro/relatorios/aging/ap${params}`)
    },
    enabled: !!tenantId,
    staleTime: 1000 * 60 * 2,
    retry: 1,
  })

  const agingAR = processToGridRows(arQuery.data)
  const agingAP = processToGridRows(apQuery.data)

  const isLoading = arQuery.isLoading || apQuery.isLoading
  const isError = arQuery.isError || apQuery.isError
  const error = (arQuery.error || apQuery.error) as Error | null

  return {
    // Dados prontos para grid (mesmo shape do mock anterior)
    agingAR,
    agingAP,

    // Estados consolidados
    isLoading,
    isError,
    error,

    // Refetch individual ou ambos (útil para botão Atualizar)
    refetchAR: arQuery.refetch,
    refetchAP: apQuery.refetch,
    refetch: () => {
      return Promise.all([arQuery.refetch(), apQuery.refetch()])
    },

    // Raw para casos avançados
    rawAR: arQuery.data,
    rawAP: apQuery.data,
  }
}
