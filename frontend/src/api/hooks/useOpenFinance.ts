import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'

export interface OpenFinanceConsent {
  id?: string
  tenantId: string
  contaBancariaId?: string
  institutionId?: string
  institutionName?: string
  consentId?: string
  status: string
  authorizationUrl?: string
  expiresAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface IniciarOpenFinanceConsentPayload {
  contaBancariaId?: string
  institutionId: string
  institutionName: string
}

export function useOpenFinanceConsents() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['financeiro', tenantId, 'open-finance', 'consents'],
    queryFn: () =>
      apiGet<OpenFinanceConsent[]>(`/financeiro/open-finance/consents?tenantId=${tenantId}`),
    enabled: !!tenantId,
    staleTime: 1000 * 30,
  })
}

export function useIniciarOpenFinanceConsent() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: IniciarOpenFinanceConsentPayload) =>
      apiPost<OpenFinanceConsent>(
        `/financeiro/open-finance/consents/iniciar?tenantId=${tenantId}`,
        payload
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId, 'open-finance'] }),
  })
}

export function useConfirmarOpenFinanceConsent() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (consentId: string) =>
      apiPost<OpenFinanceConsent>(
        `/financeiro/open-finance/consents/${consentId}/confirmar?tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId, 'open-finance'] }),
  })
}

export function useRevogarOpenFinanceConsent() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (consentId: string) =>
      apiPost<OpenFinanceConsent>(
        `/financeiro/open-finance/consents/${consentId}/revogar?tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['financeiro', tenantId, 'open-finance'] }),
  })
}
