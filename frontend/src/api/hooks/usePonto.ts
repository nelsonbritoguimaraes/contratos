/**
 * usePonto — hooks completos P0–P2 (Portaria 671 PTRP)
 */
import { useQuery, useMutation } from '@tanstack/react-query'
import { apiGet, apiPost, apiUpload, getAuthToken, API_BASE } from '../client'
import { useTenant } from './useTenant'

export function useDailyCoverage(contractId?: string, data?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ponto', tenantId, 'coverage', contractId, data],
    queryFn: () => apiGet<any>(`/time-punches/daily-summary?contractId=${contractId}&date=${data}&tenantId=${tenantId}`),
    enabled: !!contractId && !!data,
  })
}

export function useDailySummary(contractId?: string, date?: string) {
  return useDailyCoverage(contractId, date)
}

export function useAfdImport() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: async ({ file, deviceId, contractId, autoProcess = true }: {
      file: File; deviceId?: string; contractId?: string; autoProcess?: boolean
    }) => {
      const formData = new FormData()
      formData.append('file', file)
      if (deviceId) formData.append('deviceId', deviceId)
      const q = new URLSearchParams({ tenantId, autoProcess: String(autoProcess) })
      if (contractId) q.append('contractId', contractId)
      return apiUpload<any>(`/time-punches/import-afd?${q}`, formData)
    },
  })
}

export function useProcessDay() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: async ({ employeeId, date, postId, contractId }: {
      employeeId: string; date: string; postId?: string; contractId?: string
    }) => {
      const params = new URLSearchParams({ employeeId, date, tenantId })
      if (postId) params.append('postId', postId)
      if (contractId) params.append('contractId', contractId)
      return apiPost<any>(`/time-punches/process-day?${params}`, {})
    },
  })
}

export function useProcessMonth() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: ({ contractId, competencia }: { contractId: string; competencia: string }) =>
      apiPost<any>(`/time-punches/process-month?contractId=${contractId}&competencia=${competencia}&tenantId=${tenantId}`, {}),
  })
}

export function useClockDevices() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ponto', tenantId, 'clock-devices'],
    queryFn: () => apiGet<any[]>(`/time/clock-devices?tenantId=${tenantId}`),
  })
}

export function useEspelhoPonto(employeeId?: string, competencia?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ponto-espelho', tenantId, employeeId, competencia],
    queryFn: () => apiGet<any>(`/time-punches/espelho?employeeId=${employeeId}&competencia=${competencia}&tenantId=${tenantId}`),
    enabled: !!employeeId && !!competencia,
  })
}

export function usePunchList(start?: string, end?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ponto-punches', tenantId, start, end],
    queryFn: () => apiGet<any[]>(`/time-punches?start=${start}&end=${end}&tenantId=${tenantId}`),
    enabled: !!start && !!end,
  })
}

export function useVolantes(contractId?: string, date?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ponto-volantes', tenantId, contractId, date],
    queryFn: () => apiGet<any[]>(`/time-punches/volantes?contractId=${contractId}&date=${date}&tenantId=${tenantId}`),
    enabled: !!contractId && !!date,
  })
}

export function usePendingAdjustments() {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ponto-adjustments', tenantId],
    queryFn: () => apiGet<any[]>(`/time-punches/adjustments/pending?tenantId=${tenantId}`),
  })
}

export function useMobilePunch() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: (data: any) => apiPost<any>(`/time-punches/mobile-punch?tenantId=${tenantId}`, data),
  })
}

export function useComprovantes(employeeId?: string) {
  const { tenantId } = useTenant()
  return useQuery({
    queryKey: ['ponto-comprovantes', tenantId, employeeId],
    queryFn: () => apiGet<any[]>(`/time-punches/comprovantes?employeeId=${employeeId}&tenantId=${tenantId}`),
    enabled: !!employeeId,
  })
}

export function useExportAej() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: async ({ contractId, period }: { contractId: string; period: string }) => {
      const path = `/time-punches/export-aej?contractId=${contractId}&period=${period}&tenantId=${tenantId}`
      const token = getAuthToken()
      const headers: Record<string, string> = {}
      if (token) headers['Authorization'] = `Bearer ${token}`
      const res = await fetch(`${API_BASE}${path}`, { headers })
      if (!res.ok) throw new Error(`Export AEJ falhou: ${res.status}`)
      const content = await res.text()
      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `aej_${contractId}_${period.slice(0, 7)}.txt`
      a.click()
      URL.revokeObjectURL(url)
      return { ok: true, p7sMode: res.headers.get('X-P7S-Mode') }
    },
  })
}

export function useExportEspelhoText() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: async ({ employeeId, competencia }: { employeeId: string; competencia: string }) => {
      const path = `/time-punches/espelho/text?employeeId=${employeeId}&competencia=${competencia}&tenantId=${tenantId}`
      const token = getAuthToken()
      const headers: Record<string, string> = {}
      if (token) headers['Authorization'] = `Bearer ${token}`
      const res = await fetch(`${API_BASE}${path}`, { headers })
      if (!res.ok) throw new Error('Export espelho falhou')
      const content = await res.text()
      const blob = new Blob([content], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `espelho_${employeeId}_${competencia.slice(0, 7)}.txt`
      a.click()
      URL.revokeObjectURL(url)
    },
  })
}

export function useBridgeImport() {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: ({ deviceId, content, contractId }: { deviceId: string; content: string; contractId?: string }) => {
      const q = new URLSearchParams({ deviceId, tenantId, autoProcess: 'true' })
      if (contractId) q.append('contractId', contractId)
      return apiPost<any>(`/time-punches/bridge/import?${q}`, content)
    },
  })
}
