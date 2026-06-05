/**
 * Hooks Phase 7-8 + operational APIs
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost, apiPut, apiDelete } from '../client'
import { useTenant } from './useTenant'

// ==================== IMPLANTAÇÃO ====================

export function useImplantations() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['implantations', tenantId],
    queryFn: () => apiGet<any[]>(`/implantations?tenantId=${tenantId}`),
  })
}

export function useImplantationByContract(contractId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['implantation', tenantId, contractId],
    queryFn: () => apiGet<any>(`/implantations/contract/${contractId}?tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}

export function useImplantationChecklist(implantationId?: string) {
  return useQuery({
    queryKey: ['implantation-checklist', implantationId],
    queryFn: () => apiGet<any[]>(`/implantations/${implantationId}/checklist`),
    enabled: !!implantationId,
  })
}

export function useStartImplantation() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (contractId: string) =>
      apiPost(`/implantations/contract/${contractId}/iniciar?tenantId=${tenantId}`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantations'] }),
  })
}

export function useCompleteChecklistItem() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (itemId: string) =>
      apiPost(`/implantations/checklist/${itemId}/concluir?tenantId=${tenantId}`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantation-checklist'] }),
  })
}

// ==================== NOTIFICAÇÕES ====================

export function useNotificacoes(contractId?: string) {
  const { tenantId } = useTenant()
  const q = contractId ? `&contractId=${contractId}` : ''
  return useQuery({
    queryKey: ['notificacoes', tenantId, contractId],
    queryFn: () => apiGet<any[]>(`/notificacoes?tenantId=${tenantId}${q}`),
  })
}

export function useCreateNotificacao() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: any) => apiPost(`/notificacoes?tenantId=${tenantId}`, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notificacoes'] }),
  })
}

// ==================== EQUIPAMENTOS ====================

export function useEquipamentos() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['equipamentos', tenantId],
    queryFn: () => apiGet<any[]>(`/equipamentos/items?tenantId=${tenantId}`),
  })
}

export function useEquipamentoAllocations() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['equipamentos-alloc', tenantId],
    queryFn: () => apiGet<any[]>(`/equipamentos/allocations?tenantId=${tenantId}`),
  })
}

export function useCreateEquipamento() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: any) => apiPost(`/equipamentos/items?tenantId=${tenantId}`, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['equipamentos'] }),
  })
}

// ==================== UNIFORMES ====================

export function useUniformItems() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['uniformes', tenantId],
    queryFn: () => apiGet<any[]>(`/uniformes/items?tenantId=${tenantId}`),
  })
}

export function useUniformAllocations(employeeId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['uniform-alloc', tenantId, employeeId],
    queryFn: () => apiGet<any[]>(`/uniformes/allocations?employeeId=${employeeId}&tenantId=${tenantId}`),
    enabled: !!employeeId,
  })
}

// ==================== VOLANTES WORKFLOW ====================

export function useVolanteAssignments(contractId?: string, date?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['volante-assignments', tenantId, contractId, date],
    queryFn: () =>
      apiGet<any[]>(`/volantes?contractId=${contractId}&date=${date}&tenantId=${tenantId}`),
    enabled: !!contractId && !!date,
  })
}

export function useDetectVolantes() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ contractId, date }: { contractId: string; date: string }) =>
      apiPost(`/volantes/detect?contractId=${contractId}&date=${date}&tenantId=${tenantId}`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['volante-assignments'] }),
  })
}

export function useAssignVolante() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, volanteEmployeeId, notes }: { id: string; volanteEmployeeId: string; notes?: string }) =>
      apiPost(`/volantes/${id}/assign?tenantId=${tenantId}`, { volanteEmployeeId, notes }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['volante-assignments'] }),
  })
}

export function useConfirmVolante() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => apiPost(`/volantes/${id}/confirm?tenantId=${tenantId}`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['volante-assignments'] }),
  })
}

export function useVolanteCoverage(contractId?: string, date?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['volante-coverage', tenantId, contractId, date],
    queryFn: () =>
      apiGet<any>(`/volantes/coverage?contractId=${contractId}&date=${date}&tenantId=${tenantId}`),
    enabled: !!contractId && !!date,
  })
}

// ==================== AUDITORIA GLOBAL ====================

export function useGlobalAudit(entityType?: string) {
  const { tenantId } = useTenant()
  const q = entityType ? `&entityType=${entityType}` : ''
  return useQuery({
    queryKey: ['global-audit', tenantId, entityType],
    queryFn: () => apiGet<any[]>(`/audit?tenantId=${tenantId}${q}`),
  })
}

// ==================== ESCALA ====================

export function useShiftTemplates(contractId?: string) {
  const { tenantId } = useTenant()
  const q = contractId ? `&contractId=${contractId}` : ''
  return useQuery({
    queryKey: ['escala-templates', tenantId, contractId],
    queryFn: () => apiGet<any[]>(`/escala/templates?tenantId=${tenantId}${q}`),
  })
}

export function usePostSchedules(contractId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['escala-posts', tenantId, contractId],
    queryFn: () => apiGet<any[]>(`/escala/posts?contractId=${contractId}&tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}

export function useRosters(contractId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['escala-rosters', tenantId, contractId],
    queryFn: () => apiGet<any[]>(`/escala/rosters?contractId=${contractId}&tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}

// ==================== COMPLIANCE ====================

export function useComplianceMonitors() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['compliance-monitors', tenantId],
    queryFn: () => apiGet<any[]>(`/compliance/monitors?tenantId=${tenantId}`),
    staleTime: 60_000,
  })
}

// ==================== PORTAL ÓRGÃO ====================

export function usePortalDocuments(contractId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['portal-docs', tenantId, contractId],
    queryFn: () => apiGet<any[]>(`/portal/orgao/contracts/${contractId}/documents?tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}

export function usePortalMeasurements(contractId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['portal-medicoes', tenantId, contractId],
    queryFn: () => apiGet<any[]>(`/portal/orgao/contracts/${contractId}/measurements?tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}

// ==================== SERVER-SIDE GRID ====================

export function useGridGlosas(page = 0, pageSize = 50, contractId?: string) {
  const { tenantId } = useTenant()
  const q = new URLSearchParams({ tenantId, page: String(page), pageSize: String(pageSize) })
  if (contractId) q.set('contractId', contractId)
  return useQuery({
    queryKey: ['grid-glosas', tenantId, page, pageSize, contractId],
    queryFn: () => apiGet<any>(`/grid/glosas?${q}`),
  })
}

export function useGridLancamentos(page = 0, pageSize = 50, inicio?: string, fim?: string) {
  const { tenantId } = useTenant()
  const q = new URLSearchParams({ tenantId, page: String(page), pageSize: String(pageSize) })
  if (inicio) q.set('inicio', inicio)
  if (fim) q.set('fim', fim)
  return useQuery({
    queryKey: ['grid-lancamentos', tenantId, page, inicio, fim],
    queryFn: () => apiGet<any>(`/grid/lancamentos?${q}`),
  })
}

export function useGridPayslips(page = 0, pageSize = 50, contractId?: string) {
  const { tenantId } = useTenant()
  const q = new URLSearchParams({ tenantId, page: String(page), pageSize: String(pageSize) })
  if (contractId) q.set('contractId', contractId)
  return useQuery({
    queryKey: ['grid-payslips', tenantId, page, contractId],
    queryFn: () => apiGet<any>(`/grid/payslips?${q}`),
  })
}

// ==================== IA APPROVALS ====================

export function useIaApprovals(pendingOnly = true) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ia-approvals', tenantId, pendingOnly],
    queryFn: () => apiGet<any[]>(`/ia/approvals?tenantId=${tenantId}&pendingOnly=${pendingOnly}`),
  })
}

export function useApproveIaAction() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, notes }: { id: string; notes?: string }) =>
      apiPost(`/ia/approvals/${id}/approve?tenantId=${tenantId}&notes=${encodeURIComponent(notes || '')}`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ia-approvals'] }),
  })
}

export function useRejectIaAction() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, notes }: { id: string; notes?: string }) =>
      apiPost(`/ia/approvals/${id}/reject?tenantId=${tenantId}&notes=${encodeURIComponent(notes || '')}`, {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ia-approvals'] }),
  })
}

// ==================== GLOSA RULES ====================

export function useGlosaRules(contractId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['glosa-rules', tenantId, contractId],
    queryFn: () => apiGet<any[]>(`/glosas/rules?contractId=${contractId}&tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}

export function useSaveGlosaRule() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: any & { id?: string }) =>
      payload.id
        ? apiPut(`/glosas/rules/${payload.id}?tenantId=${tenantId}`, payload)
        : apiPost(`/glosas/rules?tenantId=${tenantId}`, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['glosa-rules'] }),
  })
}

export function useDeleteGlosaRule() {
  const { tenantId } = useTenant()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => apiDelete(`/glosas/rules/${id}?tenantId=${tenantId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['glosa-rules'] }),
  })
}

// ==================== CONTRACT DASHBOARD ====================

export function useContractDashboard(contractId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['contract-dashboard', tenantId, contractId],
    queryFn: () => apiGet<any>(`/contracts/${contractId}/dashboard?tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}

// ==================== CUSTO POR POSTO ====================

export function useCustoPorPosto(contractId?: string, competencia = '2025-06-01') {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['custo-por-posto', tenantId, contractId, competencia],
    queryFn: () =>
      apiGet<any>(`/rh/reports/custo-por-posto?contractId=${contractId}&competencia=${competencia}&tenantId=${tenantId}`),
    enabled: !!contractId,
  })
}
