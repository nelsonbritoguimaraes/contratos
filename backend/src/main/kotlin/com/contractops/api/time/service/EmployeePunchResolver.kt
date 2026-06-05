package com.contractops.api.time.service

import com.contractops.api.employee.domain.Employee
import com.contractops.api.employee.domain.EmployeeAssignment
import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class EmployeePunchResolver(
    private val employeeRepository: EmployeeRepository,
    private val assignmentRepository: EmployeeAssignmentRepository
) {

    fun resolve(tenantId: UUID, matricula: String?, cpf: String?): Employee? {
        val cpfDigits = cpf?.replace(Regex("\\D"), "")?.takeIf { it.length >= 11 }
        if (cpfDigits != null) {
            employeeRepository.findByTenantIdAndCpf(tenantId, cpfDigits)?.let { return it }
            employeeRepository.findByCpf(cpfDigits)?.takeIf { it.tenantId == tenantId }?.let { return it }
        }
        val mat = matricula?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return employeeRepository.findByTenantIdAndMatricula(tenantId, mat)
    }

    fun activeAssignment(tenantId: UUID, employeeId: UUID): EmployeeAssignment? {
        return assignmentRepository.findByTenantIdAndEmployeeId(tenantId, employeeId)
            .firstOrNull { it.isActive }
    }

    fun normalizeCpf(cpf: String?): String? = cpf?.replace(Regex("\\D"), "")?.takeIf { it.length >= 11 }
}
