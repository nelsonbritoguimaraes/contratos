package com.contractops.api.employee.api

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class CreateEmployeeRequest(
    val companyId: UUID,
    val branchId: UUID? = null,
    val fullName: String,
    val cpf: String,
    val rg: String? = null,
    val pisNis: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val cargo: String? = null,
    val cbo: String? = null,
    val salaryBase: BigDecimal? = null,
    val admissionDate: LocalDate? = null,
    val contractType: String? = null
)

data class EmployeeResponse(
    val id: UUID?,
    val fullName: String,
    val cpf: String,
    val cargo: String?,
    val status: String,
    val admissionDate: LocalDate?,
    val companyId: UUID,
    val branchId: UUID?
)

/** Request para alocar um colaborador em contrato/posto (SPEC §8) */
data class AssignEmployeeRequest(
    val contractId: UUID,
    val postId: UUID? = null,
    val role: String = "TITULAR",           // TITULAR, RESERVA, VOLANTE, SUPERVISOR
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

/** Response de alocação */
data class EmployeeAssignmentResponse(
    val id: UUID?,
    val employeeId: UUID,
    val contractId: UUID,
    val postId: UUID?,
    val role: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val isActive: Boolean
) {
    companion object {
        fun fromEntity(e: com.contractops.api.employee.domain.EmployeeAssignment): EmployeeAssignmentResponse =
            EmployeeAssignmentResponse(
                id = e.id,
                employeeId = e.employeeId,
                contractId = e.contractId,
                postId = e.postId,
                role = e.role,
                startDate = e.startDate,
                endDate = e.endDate,
                isActive = e.isActive
            )
    }
}
