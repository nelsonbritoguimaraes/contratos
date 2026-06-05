/**
 * useContasReceber — Hook real + resiliente para Contas a Receber (AR)
 * 
 * Backend-driven seguindo padrão ConciliacaoPage / AgingPage:
 * - Chama GET /financeiro/receber (retorna ContasAReceberResumoResponse rico com .contas + KPIs)
 * - Normaliza para lista de itens de grid (com campos derivados para compatibilidade de UI)
 * - Rich backend-driven data
 * - useRegistrarRecebimento → POST /financeiro/receber/{id}/baixar
 */
import { useQuery, useMutation } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'

export interface ContaReceberRow {
  id: string
  contratoId?: string
  contratoNumero?: string   // derivado ou fallback
  periodo?: string          // derivado de vencimento/measurement quando possível
  valorBruto: number
  glosaTotal: number        // 0 para dados reais (glosa está na medição)
  valorLiquido: number
  vencimento: string
  status: string
  diasAtraso: number
  nfseNumero?: string
  dataEmissaoNfse?: string
  measurementId?: string
  notaFiscalId?: string
  saldoAberto?: number
}

export interface ContasReceberResumo {
  total: number
  valorTotalAberto: number
  valorTotalVencido: number
  porStatus: Record<string, number>
  porAging: Record<string, number>
  contas: any[]
}

export function useContasReceber(filters?: Record<string, any>) {
  const { tenantId } = useTenant()

  const query = useQuery({
    queryKey: queryKeys.financeiro.contasReceber(tenantId, filters),
    queryFn: async () => {
      return apiGet<any>(`/financeiro/receber?tenantId=${tenantId}`)
    },
    staleTime: 1000 * 60,
    retry: 1,
    enabled: !!tenantId,
  })

  // Normaliza resposta rica do backend para lista + resumo
  const raw: any = query.data
  const backendContas: any[] = Array.isArray(raw?.contas) ? raw.contas : (Array.isArray(raw) ? raw : [])

  // Mapeia para shape amigável à grid (melhor esforço - campos não presentes no backend AR ficam com defaults)
  const contas: ContaReceberRow[] = backendContas.map((ar: any) => {
    const venc = ar.vencimento || ar.vencimento?.toString?.() || ''
    return {
      id: String(ar.id || ar.id?.toString?.() || ''),
      contratoId: ar.contratoId ? String(ar.contratoId) : undefined,
      contratoNumero: undefined, // será enriquecido no page via useContracts quando possível
      periodo: ar.observacoes ? String(ar.observacoes).slice(0, 20) : (venc ? venc.slice(0, 7) : '—'),
      valorBruto: Number(ar.valorBruto || 0),
      glosaTotal: 0, // glosa vive na medição; backend não traz no resumo AR atual
      valorLiquido: Number(ar.valorLiquido || 0),
      vencimento: venc,
      status: ar.status || 'ABERTO',
      diasAtraso: Number(ar.diasAtraso || 0),
      nfseNumero: ar.notaFiscalId ? `NFS-e vinc. ${String(ar.notaFiscalId).slice(0, 8)}` : undefined,
      dataEmissaoNfse: undefined,
      measurementId: ar.measurementId ? String(ar.measurementId) : undefined,
      notaFiscalId: ar.notaFiscalId ? String(ar.notaFiscalId) : undefined,
      saldoAberto: ar.saldoAberto != null ? Number(ar.saldoAberto) : undefined,
    }
  })

  const resumo: ContasReceberResumo | null = raw && !Array.isArray(raw) ? {
    total: Number(raw.total || contas.length),
    valorTotalAberto: Number(raw.valorTotalAberto || 0),
    valorTotalVencido: Number(raw.valorTotalVencido || 0),
    porStatus: raw.porStatus || {},
    porAging: raw.porAging || {},
    contas: backendContas,
  } : null

  return {
    // Compatibilidade + lista pronta para grid (real ou [])
    data: contas,
    resumo,

    // Estados completos (padrão Aging/Conciliacao)
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error as Error | null,
    refetch: query.refetch,
  }
}

export function useRegistrarRecebimento() {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: async (params: {
      contaAReceberId: string
      valor: number
      data: string
      observacao?: string
    }) => {
      return apiPost<any>(
        `/financeiro/receber/${params.contaAReceberId}/baixar?tenantId=${tenantId}`,
        params
      )
    },
  })
}

// Helper para emissão de NFS-e real (reutilizável)
export async function emitirNfsReal(params: {
  measurementId: string
  contratoId: string
  tomadorCnpj?: string
  valorServicos: number
}) {
  // Reutiliza o mesmo endpoint que MedicaoPage usa indiretamente
  return apiPost<any>('/financeiro/nfs/emitir', {
    measurementId: params.measurementId,
    contratoId: params.contratoId,
    tomadorCnpj: params.tomadorCnpj || '',
    valorServicos: params.valorServicos,
  })
}
