import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../client'

export interface FiscalStatusResponse {
  mode: string
  esocial?: {
    tpAmb?: string
    transmitUrl?: string
    certificateConfigured?: boolean
  }
  nfse?: {
    municipioIbge?: string
    emitUrl?: string
    certificateConfigured?: boolean
  }
  sped?: { layoutVersion?: string }
  message?: string
}

export function useFiscalStatus() {
  return useQuery({
    queryKey: ['fiscal-status'],
    queryFn: () => apiGet<FiscalStatusResponse>('/fiscal/status'),
    staleTime: 60_000,
    retry: 1,
  })
}
