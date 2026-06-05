import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'

export function useRhDashboard(competence?: string) {
  const { tenantId } = useTenant()
  const q = competence ? `&competence=${competence}` : ''
  return useQuery({
    queryKey: ['rh-dashboard', tenantId, competence],
    queryFn: () => apiGet<any>(`/rh/dashboard?tenantId=${tenantId}${q}`),
  })
}

export function useRhEncargos(competence: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['rh-encargos', tenantId, competence],
    queryFn: () => apiGet<any>(`/rh/dashboard/encargos?tenantId=${tenantId}&competence=${competence}`),
    enabled: !!competence,
  })
}

export function useRhComplianceCalendario(competence: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['rh-compliance', tenantId, competence],
    queryFn: () => apiGet<any[]>(`/rh/compliance/calendario?tenantId=${tenantId}&competencia=${competence}`),
    enabled: !!competence,
  })
}

export function useRhDivergencias(competence: string, contractId?: string) {
  const { tenantId } = useTenant()
  const contractQ = contractId ? `&contractId=${contractId}` : ''
  return useQuery({
    queryKey: ['rh-divergencias', tenantId, competence, contractId],
    queryFn: () =>
      apiGet<any>(`/rh/compliance/divergencias?tenantId=${tenantId}&competencia=${competence}${contractQ}`),
    enabled: !!competence,
  })
}

export function useFecharCompetenciaFiscal() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: { competencia: string; contractId?: string; transmitir?: boolean }) => {
      const contractQ = params.contractId ? `&contractId=${params.contractId}` : ''
      const transmitQ = params.transmitir ? '&transmitir=true' : ''
      return apiPost<any>(
        `/rh/compliance/fechar-competencia?tenantId=${tenantId}&competencia=${params.competencia}${contractQ}${transmitQ}`,
        {}
      )
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['rh-divergencias'] })
      qc.invalidateQueries({ queryKey: ['rh-dashboard'] })
      qc.invalidateQueries({ queryKey: ['esocial'] })
    },
  })
}
