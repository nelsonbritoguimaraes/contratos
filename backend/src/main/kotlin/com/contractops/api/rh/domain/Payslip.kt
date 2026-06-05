package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Holerite / Folha de Pagamento mensal de um colaborador.
 * Representa o resultado do cálculo de folha para uma competência.
 */
@Entity
@Table(name = "payslips")
class Payslip(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "competence", nullable = false)   // Competência (ex: 2026-05-01)
    var competence: LocalDate,

    @Column(name = "base_salary", precision = 14, scale = 2)
    var baseSalary: BigDecimal? = null,

    @Column(name = "total_earnings", precision = 14, scale = 2)
    var totalEarnings: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_deductions", precision = 14, scale = 2)
    var totalDeductions: BigDecimal = BigDecimal.ZERO,

    @Column(name = "net_amount", precision = 14, scale = 2)
    var netAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "DRAFT",   // DRAFT, CALCULATED, APPROVED, EXPORTED, PAID

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "Payslip(id=$id, employeeId=$employeeId, competence=$competence, net=$netAmount)"
}