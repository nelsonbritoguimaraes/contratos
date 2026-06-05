package com.contractops.api.employee.service

import com.contractops.api.employee.api.AssignEmployeeRequest
import com.contractops.api.employee.domain.Employee
import com.contractops.api.employee.domain.EmployeeAssignment
import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val assignmentRepository: EmployeeAssignmentRepository
) {

    fun findAllByTenant(tenantId: UUID): List<Employee> =
        employeeRepository.findByTenantId(tenantId)

    fun findById(id: UUID): Employee? =
        employeeRepository.findById(id).orElse(null)

    @Transactional
    fun create(employee: Employee): Employee {
        return employeeRepository.save(employee)
    }

    @Transactional
    fun updateStatus(id: UUID, tenantId: UUID, newStatus: String): Employee? {
        val existing = employeeRepository.findById(id).orElse(null) ?: return null
        if (existing.tenantId != tenantId) return null

        existing.status = newStatus
        return employeeRepository.save(existing)
    }

    @Transactional
    fun update(id: UUID, tenantId: UUID, updater: (Employee) -> Unit): Employee? {
        val existing = employeeRepository.findById(id).orElse(null) ?: return null
        if (existing.tenantId != tenantId) return null
        updater(existing)
        return employeeRepository.save(existing)
    }

    // ==================== Assignment / Alocação (SPEC §8) ====================

    fun findAssignmentsByEmployee(employeeId: UUID, tenantId: UUID): List<EmployeeAssignment> =
        assignmentRepository.findByTenantIdAndEmployeeId(tenantId, employeeId)

    fun findAssignmentsByContract(contractId: UUID, tenantId: UUID): List<EmployeeAssignment> =
        assignmentRepository.findByTenantIdAndContractId(tenantId, contractId)

    fun findAssignmentsByPost(postId: UUID, tenantId: UUID): List<EmployeeAssignment> =
        assignmentRepository.findByTenantIdAndPostId(tenantId, postId)

    @Transactional
    fun assignEmployee(
        tenantId: UUID,
        employeeId: UUID,
        request: AssignEmployeeRequest
    ): EmployeeAssignment {
        // Validação básica de existência do employee pode ser adicionada depois
        val assignment = EmployeeAssignment(
            tenantId = tenantId,
            employeeId = employeeId,
            contractId = request.contractId,
            postId = request.postId,
            role = request.role,
            startDate = request.startDate ?: LocalDate.now(),
            endDate = request.endDate,
            isActive = true
        )
        return assignmentRepository.save(assignment)
    }

    @Transactional
    fun unassign(assignmentId: UUID, tenantId: UUID): Boolean {
        val existing = assignmentRepository.findById(assignmentId)
            .filter { it.tenantId == tenantId }
            .orElse(null) ?: return false

        existing.isActive = false
        existing.endDate = existing.endDate ?: LocalDate.now()
        assignmentRepository.save(existing)
        return true
    }

    @Transactional
    fun endAssignment(assignmentId: UUID, tenantId: UUID, endDate: LocalDate = LocalDate.now()): EmployeeAssignment? {
        val existing = assignmentRepository.findById(assignmentId)
            .filter { it.tenantId == tenantId }
            .orElse(null) ?: return null

        existing.endDate = endDate
        existing.isActive = false
        return assignmentRepository.save(existing)
    }
}
