package com.contractops.api.rh.api

import com.contractops.api.rh.domain.PayrollRubric
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class PayrollRubricResponse(
    val id: UUID?,
    val code: String,
    val description: String,
    val type: String,
    val calculationType: String,
    val fixedValue: BigDecimal?,
    val percentage: BigDecimal?,
    val reference: String?,
    val incidesInss: Boolean,
    val incidesFgts: Boolean,
    val incidesIrrf: Boolean,
    val isActive: Boolean,
    val displayOrder: Int
) {
    companion object {
        fun fromEntity(r: PayrollRubric): PayrollRubricResponse = PayrollRubricResponse(
            id = r.id,
            code = r.code,
            description = r.description,
            type = r.type,
            calculationType = r.calculationType,
            fixedValue = r.fixedValue,
            percentage = r.percentage,
            reference = r.reference,
            incidesInss = r.incidesInss,
            incidesFgts = r.incidesFgts,
            incidesIrrf = r.incidesIrrf,
            isActive = r.isActive,
            displayOrder = r.displayOrder
        )
    }
}

data class CreateRubricRequest(
    val code: String,
    val description: String,
    val type: String,
    val calculationType: String,
    val fixedValue: BigDecimal? = null,
    val percentage: BigDecimal? = null,
    val reference: String? = null,
    val incidesInss: Boolean = false,
    val incidesFgts: Boolean = false,
    val incidesIrrf: Boolean = false,
    val displayOrder: Int = 0
)

data class UpdateRubricRequest(
    val description: String? = null,
    val type: String? = null,
    val calculationType: String? = null,
    val fixedValue: BigDecimal? = null,
    val percentage: BigDecimal? = null,
    val reference: String? = null,
    val incidesInss: Boolean? = null,
    val incidesFgts: Boolean? = null,
    val incidesIrrf: Boolean? = null,
    val isActive: Boolean? = null,
    val displayOrder: Int? = null
)

// ==================== Employee Events DTOs ====================

data class EmployeeEventResponse(
    val id: UUID?,
    val employeeId: UUID,
    val contractId: UUID?,
    val eventType: String,
    val eventDate: LocalDate,
    val description: String?,
    val previousValue: BigDecimal?,
    val newValue: BigDecimal?,
    val reason: String?,
    val documentReference: String?,
    val affectsPayrollFrom: LocalDate?
) {
    companion object {
        fun fromEntity(e: com.contractops.api.rh.domain.EmployeeEvent): EmployeeEventResponse =
            EmployeeEventResponse(
                id = e.id,
                employeeId = e.employeeId,
                contractId = e.contractId,
                eventType = e.eventType,
                eventDate = e.eventDate,
                description = e.description,
                previousValue = e.previousValue,
                newValue = e.newValue,
                reason = e.reason,
                documentReference = e.documentReference,
                affectsPayrollFrom = e.affectsPayrollFrom
            )
    }
}

data class CreateEmployeeEventRequest(
    val eventType: String,
    val eventDate: LocalDate,
    val description: String? = null,
    val previousValue: BigDecimal? = null,
    val newValue: BigDecimal? = null,
    val reason: String? = null,
    val documentReference: String? = null,
    val affectsPayrollFrom: LocalDate? = null,
    val contractId: UUID? = null
)