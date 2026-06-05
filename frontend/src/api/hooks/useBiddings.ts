/**
 * Hooks para módulo de Licitações
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiDelete, apiGet, apiPost, apiPut } from '../client'
import { useTenant } from './useTenant'
import { Bidding, BiddingPosto, CreateBiddingLotRequest } from '../types'

export function useBiddings() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: queryKeys.biddings.all(tenantId),
    queryFn: () => apiGet<Bidding[]>(`/biddings?tenantId=${tenantId}`),
    staleTime: 1000 * 60 * 2,
  })
}

export function useBiddingPostos(biddingId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['bidding-postos', tenantId, biddingId],
    queryFn: () => apiGet<BiddingPosto[]>(`/biddings/${biddingId}/postos?tenantId=${tenantId}`),
    enabled: !!biddingId,
  })
}

export function useBiddingStatuses() {
  return useQuery({
    queryKey: ['bidding-statuses'],
    queryFn: () => apiGet<string[]>('/biddings/statuses'),
  })
}

export function usePncpSearch(termo?: string, cnpjOrgao?: string) {
  return useQuery({
    queryKey: ['pncp-search', termo, cnpjOrgao],
    queryFn: () =>
      apiGet<any>(
        `/biddings/pncp/search?termo=${encodeURIComponent(termo || '')}&cnpjOrgao=${encodeURIComponent(cnpjOrgao || '')}`
      ),
    enabled: !!(termo || cnpjOrgao),
  })
}

export function useUpdateBidding() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Bidding> }) =>
      apiPut<Bidding>(`/biddings/${id}?tenantId=${tenantId}`, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.biddings.all(tenantId) })
    },
  })
}

export function useCreateBiddingPosto(biddingId: string) {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: Partial<BiddingPosto>) =>
      apiPost<BiddingPosto>(`/biddings/${biddingId}/postos?tenantId=${tenantId}`, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['bidding-postos', tenantId, biddingId] }),
  })
}

export function useDeleteBiddingPosto() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (postoId: string) => apiDelete(`/biddings/postos/${postoId}?tenantId=${tenantId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['bidding-postos'] }),
  })
}

export function useCreateBiddingLot(biddingId: string) {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (data: CreateBiddingLotRequest) =>
      apiPost(`/biddings/${biddingId}/lots?tenantId=${tenantId}`, data),
  })
}

export function useSetVencedoraSpreadsheet() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (sheetId: string) =>
      apiPost(`/winning-spreadsheets/${sheetId}/set-vencedora?tenantId=${tenantId}`, {}),
  })
}

export function useTransitionBiddingStatus() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, novoStatus }: { id: string; novoStatus: string }) =>
      apiPost(`/biddings/${id}/status?tenantId=${tenantId}`, { novoStatus }),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.biddings.all(tenantId) }),
  })
}

export function useBiddingProposals(biddingId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['bidding-proposals', tenantId, biddingId],
    queryFn: () => apiGet<any[]>(`/biddings/${biddingId}/proposals?tenantId=${tenantId}`),
    enabled: !!biddingId,
  })
}

export function useCreateBiddingProposal(biddingId: string) {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (data: any) => apiPost(`/biddings/${biddingId}/proposals?tenantId=${tenantId}`, data),
  })
}

export function useBiddingDeadlines(biddingId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['bidding-deadlines', tenantId, biddingId],
    queryFn: () => apiGet<any[]>(`/biddings/${biddingId}/deadlines?tenantId=${tenantId}`),
    enabled: !!biddingId,
  })
}

export function useCreateBiddingDeadline(biddingId: string) {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (data: any) => apiPost(`/biddings/${biddingId}/deadlines?tenantId=${tenantId}`, data),
  })
}

export function useBiddingImpugnacoes(biddingId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['bidding-impugnacoes', tenantId, biddingId],
    queryFn: () => apiGet<any[]>(`/biddings/${biddingId}/impugnacoes?tenantId=${tenantId}`),
    enabled: !!biddingId,
  })
}

export function useCreateBiddingImpugnacao(biddingId: string) {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (data: any) => apiPost(`/biddings/${biddingId}/impugnacoes?tenantId=${tenantId}`, data),
  })
}

export function useBiddingCertidoes(biddingId?: string, cnpj?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['bidding-certidoes', tenantId, biddingId, cnpj],
    queryFn: () => apiGet<any[]>(`/biddings/${biddingId}/certidoes?tenantId=${tenantId}&cnpj=${cnpj}`),
    enabled: !!biddingId && !!cnpj && cnpj.replace(/\D/g, '').length >= 14,
  })
}

export function useBiddingDre(biddingId?: string, competencia?: string) {
  const { tenantId } = useTenant()
  const q = competencia ? `&competencia=${competencia}` : ''
  return useQuery({
    queryKey: ['bidding-dre', tenantId, biddingId, competencia],
    queryFn: () => apiGet<any>(`/biddings/${biddingId}/dre?tenantId=${tenantId}${q}`),
    enabled: !!biddingId,
  })
}

export function useBiddingAllocation(biddingId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['bidding-allocation', tenantId, biddingId],
    queryFn: () => apiGet<any>(`/biddings/${biddingId}/allocation-summary?tenantId=${tenantId}`),
    enabled: !!biddingId,
  })
}

export function useImportSpreadsheet(biddingId: string) {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: ({ file, markVencedora }: { file: File; markVencedora?: boolean }) => {
      const fd = new FormData()
      fd.append('file', file)
      return apiPost<any>(
        `/biddings/${biddingId}/winning-spreadsheets/import?tenantId=${tenantId}&markVencedora=${!!markVencedora}`,
        fd
      )
    },
  })
}
