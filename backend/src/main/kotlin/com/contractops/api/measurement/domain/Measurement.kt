package com.contractops.api.measurement.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Medição / Faturamento por período de contrato.
 * Consolida glosas + cobertura + regras contratuais.
 * SPEC §15, §16 (início do motor de faturamento).
 */
@Entity
@Table(name = "measurements")
class Measurement(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "period", nullable = false)
    var period: LocalDate,   // competência (1º dia do mês)

    @Column(name = "base_value", precision = 16, scale = 2)
    var baseValue: BigDecimal? = null,

    @Column(name = "glosa_total", precision = 16, scale = 2)
    var glosaTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "coverage_adjustment", precision = 16, scale = 2)
    var coverageAdjustment: BigDecimal = BigDecimal.ZERO,

    @Column(name = "final_amount", precision = 16, scale = 2)
    var finalAmount: BigDecimal? = null,

    @Column(name = "status", length = 30)
    var status: String = "DRAFT",   // DRAFT, APPROVED, INVOICED, PAID

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

) : AuditEntity(), TenantAware