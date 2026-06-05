/**
 * useTesouraria — Hooks para Tesouraria Avançada (Onda 1)
 * CNAB 240, Aprovação de Contas a Pagar, Fluxo, Pagamentos
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPost, apiUpload, getAuthToken } from '../client'
import { useTenant } from './useTenant'

export function useContasBancarias() {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: queryKeys.financeiro.contasBancarias(tenantId),
    queryFn: () => apiGet<any[]>(`/financeiro/tesouraria/contas?tenantId=${tenantId}`),
    staleTime: 1000 * 60,
  })
}

export function useImportarExtrato() {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: async (payload: { contaBancariaId: string; arquivo: File }) => {
      const formData = new FormData()
      formData.append('file', payload.arquivo)  // controller expects "file"
      // Correct multipart endpoint (tesouraria/upload handles OFX/CSV)
      return apiUpload(`/financeiro/tesouraria/extrato/upload?contaBancariaId=${payload.contaBancariaId}&tenantId=${tenantId}`, formData)
    },
  })
}

/** Exporta CNAB 240 para pagamento em lote de Contas a Pagar (wired to real FinanceiroController) */
export function useExportCnab240() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (payload: {
      contasIds: string[]
      agencia: string
      conta: string
      dv: string
      cnpj: string
      nomeEmpresa: string
      dataPagamento: string
    }) =>
      apiPost(
        `/financeiro/pagamentos/export/cnab240?agencia=${payload.agencia}&conta=${payload.conta}&dv=${payload.dv}&cnpj=${payload.cnpj}&nomeEmpresa=${payload.nomeEmpresa}&dataPagamento=${payload.dataPagamento}&tenantId=${tenantId}`,
        payload.contasIds
      ),
  })
}

/** Aprova uma Conta a Pagar (multi-nível) */
export function useAprovarContaAPagar() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: { contaAPagarId: string; nivel: number; usuario: string }) =>
      apiPost(`/financeiro/pagamentos/${payload.contaAPagarId}/aprovar?nivel=${payload.nivel}&usuario=${payload.usuario}&tenantId=${tenantId}`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId] }),
  })
}

export function useConciliacao() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()

  return useMutation({
    mutationFn: (payload: { contaBancariaId: string; dataInicio: string; dataFim: string }) =>
      apiPost(
        `/financeiro/conciliacao?contaBancariaId=${payload.contaBancariaId}&inicio=${payload.dataInicio}&fim=${payload.dataFim}&tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => {
      // Invalidate conciliation related queries
      qc.invalidateQueries({ queryKey: ['financeiro', tenantId, 'conciliacao'] })
    },
  })
}

/** @deprecated Prefer useContasPagar from useContasPagar.ts (GET /financeiro/pagar) */
export function useContasAPagar(_dataCorte?: string) {
  const { tenantId } = useTenant()
  const query = useQuery({
    queryKey: queryKeys.financeiro.contasPagar(tenantId),
    queryFn: async () => {
      const res = await apiGet<any>(`/financeiro/pagar?tenantId=${tenantId}`)
      return res?.contas || []
    },
    staleTime: 45_000,
  })
  return {
    ...query,
    contasAPagar: query.data || [],
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error as Error | null,
    refetch: query.refetch,
  }
}

/** Helper robusto para download real do CNAB 240 (trata resposta texto/binário do backend).
 * Evita o json() do apiPost que quebra em Content-Type text/plain + attachment.
 * Usado para dar experiência real de export (header Content-Disposition respeitado).
 */
export async function downloadCnab240Robust(payload: {
  contasIds: string[]
  agencia: string
  conta: string
  dv: string
  cnpj: string
  nomeEmpresa: string
  dataPagamento: string
  tenantId?: string
}): Promise<{ ok: boolean; filename: string; message?: string }> {
  const tId = payload.tenantId || ''

  // Reconstrói a URL exatamente como o controller espera (body = ids, params = config)
  const base = '/api/financeiro/pagamentos/export/cnab240'
  const qs = new URLSearchParams({
    agencia: payload.agencia,
    conta: payload.conta,
    dv: payload.dv,
    cnpj: payload.cnpj,
    nomeEmpresa: payload.nomeEmpresa,
    dataPagamento: payload.dataPagamento,
    tenantId: tId,
  }).toString()

  const url = `${base}?${qs}`

  const token = getAuthToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }

  try {
    const res = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload.contasIds || []),
    })

    if (!res.ok) {
      const txt = await res.text().catch(() => '')
      throw new Error(`CNAB export falhou: ${res.status} ${txt}`)
    }

    // O backend retorna o conteúdo CNAB como texto (pode ter header attachment)
    const content = await res.text()
    const disposition = res.headers.get('Content-Disposition') || ''
    const match = disposition.match(/filename="?([^"]+)"?/)
    const filename = match?.[1] || `CNAB240_${payload.dataPagamento}.txt`

    // Trigger download nativo (experiência real de banco)
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
    const downloadUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(downloadUrl)

    return { ok: true, filename }
  } catch (e: any) {
    return { ok: false, filename: `CNAB240_${payload.dataPagamento}_FALLBACK.txt`, message: e?.message }
  }
}

// ============================================================
// CONCILIAÇÃO BANCÁRIA — hooks para carregar listas reais + manual
// ============================================================

export function useExtratoPendentes(contaBancariaId?: string, inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'conciliacao', 'extratos-pendentes', contaBancariaId, inicio, fim],
    queryFn: () =>
      apiGet<any[]>(
        `/financeiro/conciliacao/extratos-pendentes?contaBancariaId=${contaBancariaId}&inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`
      ),
    enabled: !!contaBancariaId && !!inicio && !!fim,
    staleTime: 1000 * 30,
  })
}

export function useTransacoesPendentes(contaBancariaId?: string, inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'conciliacao', 'transacoes-pendentes', contaBancariaId, inicio, fim],
    queryFn: () =>
      apiGet<any[]>(
        `/financeiro/conciliacao/transacoes-pendentes?contaBancariaId=${contaBancariaId}&inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`
      ),
    enabled: !!contaBancariaId && !!inicio && !!fim,
    staleTime: 1000 * 30,
  })
}

export function useConciliacaoManual() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: { contaBancariaId: string; extratoId: string; transacaoId: string }) =>
      apiPost(`/financeiro/conciliacao/manual?tenantId=${tenantId}`, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['financeiro', tenantId, 'conciliacao'] })
    },
  })
}

export function useConciliacaoSugestoes(contaBancariaId?: string, inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'conciliacao', 'sugestoes', contaBancariaId, inicio, fim],
    queryFn: () =>
      apiGet<any[]>(
        `/financeiro/conciliacao/sugestoes?contaBancariaId=${contaBancariaId}&inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`
      ),
    enabled: !!contaBancariaId && !!inicio && !!fim && !!tenantId,
    staleTime: 1000 * 30,
  })
}
