/**
 * useEmployees — Hooks para Colaboradores + Alocações
 */
import { useQuery, useMutation } from '@tanstack/react-query'
import { apiGet, apiPost, apiPut, apiDelete } from '../client'
import { useTenant } from './useTenant'
import { 
  Employee, 
  EmployeeAssignment, 
  CreateEmployeeRequest, 
  UpdateEmployeeRequest,
  AssignEmployeeRequest,
  EmployeeEvent,
  CreateEmployeeEventRequest 
} from '../types'

export function useEmployees() {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: ['employees', tenantId],
    queryFn: () => apiGet<Employee[]>(`/employees?tenantId=${tenantId}`),
    staleTime: 1000 * 60 * 2,
  })
}

export function useEmployeeAssignments(contractId?: string) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: ['employee-assignments', tenantId, contractId],
    // Now backed by real endpoint (EmployeeController.listAssignmentsByContract)
    queryFn: () => apiGet<EmployeeAssignment[]>(`/employees/assignments?contractId=${contractId}`),
    enabled: !!contractId,
  })
}

export function useCreateEmployee() {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: (data: CreateEmployeeRequest) =>
      apiPost<Employee>(`/employees?tenantId=${tenantId}`, data),
  })
}

export function useUpdateEmployee(employeeId: string) {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: (data: UpdateEmployeeRequest) =>
      apiPut<Employee>(`/employees/${employeeId}?tenantId=${tenantId}`, data),
  })
}

// Eventos de DP (Admissão, Demissão, etc.) - chave para eSocial
export function useEmployeeEvents(employeeId?: string) {
  const { tenantId } = useTenant()

  return useQuery({
    queryKey: ['employee-events', tenantId, employeeId],
    queryFn: () => apiGet<EmployeeEvent[]>(`/rh/employees/${employeeId}/events?tenantId=${tenantId}`),
    enabled: !!employeeId,
  })
}

export function useRegisterEmployeeEvent(employeeId: string) {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: (data: CreateEmployeeEventRequest) =>
      apiPost<EmployeeEvent>(`/rh/employees/${employeeId}/events?tenantId=${tenantId}`, data),
  })
}

export function useAssignEmployee() {
  const { tenantId } = useTenant()

  return useMutation({
    mutationFn: (data: AssignEmployeeRequest) => {
      const empId = data.employeeId
      // Normalize to backend AssignEmployeeRequest shape + correct path (per-employee)
      const payload = {
        contractId: data.contractId,
        postId: data.postId,
        role: data.role,
        startDate: data.startDate || data.dataInicio || new Date().toISOString().slice(0, 10),
      }
      return apiPost<EmployeeAssignment>(`/employees/${empId}/assignments?tenantId=${tenantId}`, payload)
    },
  })
}

export function useUnassignEmployee(assignmentId: string) {
  const { tenantId } = useTenant()
  return useMutation({
    mutationFn: () => apiDelete(`/employees/assignments/${assignmentId}?tenantId=${tenantId}`),
  })
}
