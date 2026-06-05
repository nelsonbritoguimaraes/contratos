package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "employee_benefits")
class EmployeeBenefit(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "benefit_type", length = 50, nullable = false)
    var benefitType: String,

    @Column(name = "description", length = 200)
    var description: String? = null,

    @Column(name = "monthly_value", precision = 12, scale = 2, nullable = false)
    var monthlyValue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : AuditEntity(), TenantAware
