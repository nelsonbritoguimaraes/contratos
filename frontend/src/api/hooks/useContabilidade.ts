/**
 * useContabilidade — Hooks para Contabilidade (plano, lançamentos, DRE, balancete, SPED, fechamento)
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPost, apiPut, apiDelete, API_BASE, getAuthToken } from '../client'
import { useTenant } from './useTenant'

export interface LancamentoContabil {
  id: string
  data: string
  contaDebito: string | { codigo: string; descricao: string }
  contaCredito: string | { codigo: string; descricao: string }
  valor: number
  historico: string
  origemTipo?: string
  origemId?: string
  contratoId?: string
}

async function downloadText(path: string, filename: string) {
  const token = getAuthToken()
  const res = await fetch(`${API_BASE}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new Error(`Download failed: ${res.status}`)
  const text = await res.text()
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
  return text
}

export function usePlanoDeContas() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: queryKeys.contabilidade.contas(tenantId),
    queryFn: () => apiGet<any[]>(`/contabilidade/contas?tenantId=${tenantId}`),
    staleTime: 1000 * 60 * 5,
  })
}

export function useLancamentos(inicio: string, fim: string, origemTipo?: string, origemId?: string) {
  const { tenantId } = useTenant()
  const params = new URLSearchParams({ inicio, fim, tenantId })
  if (origemTipo) params.set('origemTipo', origemTipo)
  if (origemId) params.set('origemId', origemId)
  return useQuery({
    queryKey: queryKeys.contabilidade.lancamentos(tenantId, inicio, fim, origemTipo, origemId),
    queryFn: () => apiGet<LancamentoContabil[]>(`/contabilidade/lancamentos?${params}`),
    enabled: !!inicio && !!fim,
    staleTime: 1000 * 60,
  })
}

export function useDRE(contratoId?: string, inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: queryKeys.contabilidade.dre(tenantId, contratoId || '', inicio || '', fim || ''),
    queryFn: () => apiGet<any>(`/contabilidade/dre?contratoId=${contratoId}&inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`),
    enabled: !!contratoId && !!inicio && !!fim,
    staleTime: 1000 * 60 * 2,
  })
}

export function useBalancete(inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['contabilidade', tenantId, 'balancete', inicio, fim],
    queryFn: () => apiGet<any[]>(`/contabilidade/balancete?inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`),
    enabled: !!inicio && !!fim,
    staleTime: 1000 * 60 * 2,
  })
}

export function useRazao(contaId?: string, inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['contabilidade', tenantId, 'razao', contaId, inicio, fim],
    queryFn: () => apiGet<any>(`/contabilidade/razao/${contaId}?inicio=${inicio}&fim=${fim}&tenantId=${tenantId}`),
    enabled: !!contaId && !!inicio && !!fim,
  })
}

export function useBalanco(data?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['contabilidade', tenantId, 'balanco', data],
    queryFn: () => apiGet<any>(`/contabilidade/balanco?data=${data}&tenantId=${tenantId}`),
    enabled: !!data,
  })
}

export function usePeriodosContabeis() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['contabilidade', tenantId, 'periodos'],
    queryFn: () => apiGet<any[]>(`/contabilidade/periodos?tenantId=${tenantId}`),
  })
}

export function useLancamentosDaFolha(payslipId?: string, inicio = '2020-01-01', fim = '2030-12-31') {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['contabilidade', tenantId, 'lancamentos-folha', payslipId],
    queryFn: () =>
      apiGet<any[]>(
        `/contabilidade/lancamentos?inicio=${inicio}&fim=${fim}&origemTipo=PAYSLIP&origemId=${payslipId}&tenantId=${tenantId}`
      ),
    enabled: !!payslipId,
  })
}

export function useLancarFolhaFechada() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: { payslipId: string; contratoId: string; valorTotal: number }) =>
      apiPost(`/contabilidade/lancamentos/folha/${payload.payslipId}?tenantId=${tenantId}&contratoId=${payload.contratoId}&valorTotal=${payload.valorTotal}`, {}),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['contabilidade'] })
    },
  })
}

export function useCreateLancamento() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: {
      data: string
      contaDebitoId: string
      contaCreditoId: string
      valor: number
      historico: string
      origemTipo?: string
    }) => apiPost(`/contabilidade/lancamentos?tenantId=${tenantId}`, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contabilidade', tenantId] }),
  })
}

export function useFechamentoMensal() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { inicio: string; fim: string }) =>
      apiPost(`/contabilidade/fechamento-mensal?inicio=${params.inicio}&fim=${params.fim}&tenantId=${tenantId}`, {}),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['contabilidade'] })
    },
  })
}

export function useGerarSPED() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: async (params: { tipo: 'ecd' | 'ecf' | 'efd-reinf'; inicio?: string; fim?: string; ano?: number; competencia?: string }) => {
      if (params.tipo === 'ecd') {
        return downloadText(
          `/contabilidade/sped/ecd?inicio=${params.inicio}&fim=${params.fim}&tenantId=${tenantId}`,
          `sped_ecd_${params.inicio?.slice(0, 4)}.txt`
        )
      }
      if (params.tipo === 'ecf') {
        return downloadText(
          `/contabilidade/sped/ecf?ano=${params.ano}&tenantId=${tenantId}`,
          `ecf_${params.ano}.txt`
        )
      }
      return downloadText(
        `/contabilidade/sped/efd-reinf?competencia=${params.competencia}&tenantId=${tenantId}`,
        `efd_reinf_${params.competencia}.txt`
      )
    },
  })
}

export function useExportBalanceteCsv() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (params: { inicio: string; fim: string }) =>
      downloadText(
        `/contabilidade/export/balancete?inicio=${params.inicio}&fim=${params.fim}&tenantId=${tenantId}`,
        'balancete.csv'
      ),
  })
}

// ==================== PARAMETRIZAÇÃO (P3) ====================

export interface AccountingRuleDto {
  id?: string
  codigo: string
  descricao?: string
  origemTipo: string
  contaDebitoCodigo: string
  contaCreditoCodigo: string
  historicoPadrao?: string
  rubricCode?: string
  rubricType?: string
  ativa?: boolean
}

export function useAccountingRules(origemTipo?: string) {
  const { tenantId } = useTenant()
  const q = origemTipo ? `&origemTipo=${origemTipo}` : ''
  return useQuery({
    queryKey: ['contabilidade', tenantId, 'regras', origemTipo],
    queryFn: () => apiGet<AccountingRuleDto[]>(`/contabilidade/regras?tenantId=${tenantId}${q}`),
  })
}

export function useSaveAccountingRule() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: AccountingRuleDto & { id?: string }) =>
      payload.id
        ? apiPut(`/contabilidade/regras/${payload.id}?tenantId=${tenantId}`, payload)
        : apiPost(`/contabilidade/regras?tenantId=${tenantId}`, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contabilidade', tenantId, 'regras'] }),
  })
}

export function useDeleteAccountingRule() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => apiDelete(`/contabilidade/regras/${id}?tenantId=${tenantId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contabilidade', tenantId, 'regras'] }),
  })
}

export function useValidarSped() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { tipo: string; inicio?: string; fim?: string; ano?: number }) => {
      const p = new URLSearchParams({ tipo: params.tipo, tenantId })
      if (params.inicio) p.set('inicio', params.inicio)
      if (params.fim) p.set('fim', params.fim)
      if (params.ano) p.set('ano', String(params.ano))
      return apiPost(`/contabilidade/sped/validar?${p}`, {})
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contabilidade', tenantId, 'sped-transmissoes'] }),
  })
}

export function useTransmitirSped() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { transmissionId: string; aprovadoPor?: string }) =>
      apiPost(
        `/contabilidade/sped/transmitir/${params.transmissionId}?tenantId=${tenantId}&aprovadoPor=${params.aprovadoPor || 'usuario'}`,
        {}
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contabilidade', tenantId, 'sped-transmissoes'] }),
  })
}

export function useSpedTransmissoes() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['contabilidade', tenantId, 'sped-transmissoes'],
    queryFn: () => apiGet<any[]>(`/contabilidade/sped/transmissoes?tenantId=${tenantId}`),
  })
}

export function useReabrirPeriodo() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { competencia: string; motivo: string; usuario?: string }) =>
      apiPost(
        `/contabilidade/reabrir-periodo?competencia=${params.competencia}&motivo=${encodeURIComponent(params.motivo)}&usuario=${params.usuario || 'usuario'}&tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['contabilidade', tenantId, 'periodos'] }),
  })
}
