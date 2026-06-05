package com.contractops.api.glosa.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Faixa de desempenho para cálculo de dedução no IMR.
 * Ex: 95-100% = 0% dedução, 80-94% = 5%, etc.
 */
@Entity
@Table(name = "imr_ranges")
class IMRRange(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "indicator_id", nullable = false)
    val indicatorId: UUID,

    @Column(name = "min_value", precision = 5, scale = 2, nullable = false)
    var minValue: BigDecimal,

    @Column(name = "max_value", precision = 5, scale = 2, nullable = false)
    var maxValue: BigDecimal,

    @Column(name = "deduction_percent", precision = 5, scale = 2, nullable = false)
    var deductionPercent: BigDecimal,   // % de glosa/dedução nesta faixa

    @Column(name = "priority", nullable = false)
    var priority: Int = 10

) : AuditEntity(), TenantAware
