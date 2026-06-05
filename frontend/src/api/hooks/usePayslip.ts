/**
 * usePayslip — Hooks para cálculo, aprovação e fechamento de competência da Folha
 * 
 * Integra com:
 * - PayslipController (calculate, approve, close-competence, global-close, encargos-summary)
 * - Dispara (via backend) o fluxo completo RH → Financeiro → Contabilidade
 *   quando closeCompetence é chamado.
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../queryKeys'
import { apiGet, apiPost } from '../client'
import { useTenant } from './useTenant'
import type { PayslipCloseResponse, GlobalCloseCompetenceResponse, EncargosSummaryResponse } from '../types'

export interface PayslipCalculateParams {
  employeeId: string
  contractId: string
  competence: string // YYYY-MM-DD
}

export interface PayslipCloseParams {
  contractId: string
  competence: string // YYYY-MM-DD
}

export interface PayslipApproveResponse {
  id: string
  status: string
  message: string
}

/**
 * Hook principal para operações de Payslip / Folha
 */
export function usePayslip() {
  const { tenantId } = useTenant()
  const queryClient = useQueryClient()

  // Calcular holerite individual
  const calculate = useMutation({
    mutationFn: (params: PayslipCalculateParams) =>
      apiPost<any>(
        `/rh/payslips/calculate?employeeId=${params.employeeId}&contractId=${params.contractId}&competence=${params.competence}&tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => {
      // Invalida caches relacionados se necessário
      queryClient.invalidateQueries({ queryKey: queryKeys.rh.payslips(tenantId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.rh.all(tenantId) })
    },
  })

  // Aprovar um holerite específico
  const approve = useMutation({
    mutationFn: (payslipId: string) =>
      apiPost<PayslipApproveResponse>(
        `/rh/payslips/${payslipId}/approve?tenantId=${tenantId}`,
        {}
      ),
  })

  // Fechar competência de um contrato específico (o mais usado)
  const closeCompetence = useMutation({
    mutationFn: (params: PayslipCloseParams) =>
      apiPost<PayslipCloseResponse>(
        `/rh/payslips/contracts/${params.contractId}/close-competence?competence=${params.competence}&tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => {
      // Após fechar, invalidamos dashboards e tesouraria para refletir o impacto
      queryClient.invalidateQueries({ queryKey: queryKeys.financeiro.cfoDashboard(tenantId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.financeiro.all(tenantId) })
      // Integração chave Onda 1/3: fecha folha → dispara lançamentos contábeis automáticos visíveis em ContabilidadePage
      queryClient.invalidateQueries({ queryKey: queryKeys.contabilidade.all(tenantId) })
    },
  })

  // Fechamento global (todas as empresas/contratos do tenant)
  const globalCloseCompetence = useMutation({
    mutationFn: (competence: string) =>
      apiPost<GlobalCloseCompetenceResponse>(
        `/rh/payslips/global-close-competence?competence=${competence}&tenantId=${tenantId}`,
        {}
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.financeiro.cfoDashboard(tenantId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.financeiro.all(tenantId) })
      // Integração chave Onda 1/3: fecha folha → dispara lançamentos contábeis automáticos visíveis em ContabilidadePage
      queryClient.invalidateQueries({ queryKey: queryKeys.contabilidade.all(tenantId) })
    },
  })

  // Resumo de encargos (INSS + FGTS estimados)
  const useEncargosSummary = (contractId?: string, competence?: string) => {
    return useQuery({
      queryKey: queryKeys.rh.encargosSummary(tenantId, contractId as any, competence),
      queryFn: () =>
        apiGet<EncargosSummaryResponse>(
          `/rh/payslips/contracts/${contractId}/encargos-summary?competence=${competence}&tenantId=${tenantId}`
        ),
      enabled: !!contractId && !!competence,
    })
  }

  // Reabrir competência (útil para correções)
  const reopenCompetence = useMutation({
    mutationFn: (params: PayslipCloseParams) =>
      apiPost<any>(
        `/rh/payslips/contracts/${params.contractId}/reopen-competence?competence=${params.competence}&tenantId=${tenantId}`,
        {}
      ),
  })

  return {
    calculate,
    approve,
    closeCompetence,
    globalCloseCompetence,
    reopenCompetence,
    useEncargosSummary,
  }
}
