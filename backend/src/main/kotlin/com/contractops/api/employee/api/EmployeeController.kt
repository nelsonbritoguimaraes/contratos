package com.contractops.api.employee.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.employee.api.AssignEmployeeRequest
import com.contractops.api.employee.api.EmployeeAssignmentResponse
import com.contractops.api.employee.domain.Employee
import com.contractops.api.employee.service.EmployeeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * EmployeeController - CRUD básico de Colaboradores
 * Alinhado com SPEC v1.0 seções 8 e 26.
 */
@RestController
@RequestMapping("/api/employees")
class EmployeeController(
    private val employeeService: EmployeeService
) {

    @GetMapping
    fun list(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<EmployeeResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val employees = employeeService.findAllByTenant(effectiveTenant)
        return ResponseEntity.ok(employees.map { toResponse(it) })
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<EmployeeResponse> {
        val employee = employeeService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toResponse(employee))
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateEmployeeRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val employee = Employee(
            tenantId = effectiveTenant,
            companyId = request.companyId,
            branchId = request.branchId,
            fullName = request.fullName,
            cpf = request.cpf,
            rg = request.rg,
            pisNis = request.pisNis,
            email = request.email,
            phone = request.phone,
            cargo = request.cargo,
            cbo = request.cbo,
            salaryBase = request.salaryBase,
            admissionDate = request.admissionDate,
            contractType = request.contractType
        )

        val created = employeeService.create(employee)
        return ResponseEntity
            .created(URI.create("/api/employees/${created.id}"))
            .body(toResponse(created))
    }

    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody request: Map<String, String>,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val newStatus = request["status"] ?: return ResponseEntity.badRequest().build()

        val updated = employeeService.updateStatus(id, effectiveTenant, newStatus)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(toResponse(updated))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: CreateEmployeeRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val updated = employeeService.update(id, effectiveTenant) { emp ->
            request.fullName.takeIf { it.isNotBlank() }?.let { emp.fullName = it }
            request.cpf.takeIf { it.isNotBlank() }?.let { emp.cpf = it }
            request.rg?.let { emp.rg = it }
            request.pisNis?.let { emp.pisNis = it }
            request.email?.let { emp.email = it }
            request.phone?.let { emp.phone = it }
            request.cargo?.let { emp.cargo = it }
            request.cbo?.let { emp.cbo = it }
            request.salaryBase?.let { emp.salaryBase = it }
            request.admissionDate?.let { emp.admissionDate = it }
            request.contractType?.let { emp.contractType = it }
        } ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(toResponse(updated))
    }

    // ==================== Employee Assignments / Alocações (SPEC §8) ====================

    @GetMapping("/{employeeId}/assignments")
    fun getAssignments(
        @PathVariable employeeId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<EmployeeAssignmentResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val assignments = employeeService.findAssignmentsByEmployee(employeeId, effectiveTenant)
        return ResponseEntity.ok(assignments.map { EmployeeAssignmentResponse.fromEntity(it) })
    }

    @PostMapping("/{employeeId}/assignments")
    fun assign(
        @PathVariable employeeId: UUID,
        @RequestBody request: AssignEmployeeRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EmployeeAssignmentResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val created = employeeService.assignEmployee(effectiveTenant, employeeId, request)
        return ResponseEntity
            .created(URI.create("/api/employees/$employeeId/assignments/${created.id}"))
            .body(EmployeeAssignmentResponse.fromEntity(created))
    }

    /**
     * Lista alocações (EmployeeAssignments) por contrato.
     * Suporte ao hook frontend useEmployeeAssignments(contractId).
     * Essencial para AllocationByBiddingView e visões de cobertura RH (Onda 2/3).
     */
    @GetMapping("/assignments")
    fun listAssignmentsByContract(
        @RequestParam(required = false) contractId: UUID?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<EmployeeAssignmentResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val assignments = if (contractId != null) {
            employeeService.findAssignmentsByContract(contractId, effectiveTenant)
        } else {
            emptyList()
        }
        return ResponseEntity.ok(assignments.map { EmployeeAssignmentResponse.fromEntity(it) })
    }

    @DeleteMapping("/assignments/{assignmentId}")
    fun unassign(
        @PathVariable assignmentId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Void> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val ok = employeeService.unassign(assignmentId, effectiveTenant)
        return if (ok) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    private fun toResponse(e: Employee): EmployeeResponse {
        return EmployeeResponse(
            id = e.id,
            fullName = e.fullName,
            cpf = e.cpf,
            cargo = e.cargo,
            status = e.status,
            admissionDate = e.admissionDate,
            companyId = e.companyId,
            branchId = e.branchId
        )
    }
}
