package com.contractops.api.glosa.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Registro de Glosa aplicada.
 * SPEC §17 e §25.5.
 */
@Entity
@Table(name = "glosas")
class Glosa(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "measurement_period", nullable = false)
    var measurementPeriod: LocalDate,   // Competência (ex: 2025-05-01)

    @Column(name = "glosa_type", nullable = false, length = 50)
    var glosaType: String,   // FALTA, ATRASO, IMR, POSTO_DESCOBERTO...

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "base_value", precision = 14, scale = 2)
    var baseValue: BigDecimal? = null,   // Valor base usado no cálculo

    @Column(name = "glosa_amount", precision = 14, scale = 2, nullable = false)
    var glosaAmount: BigDecimal,   // Valor efetivo da glosa

    @Column(name = "status", length = 30)
    var status: String = "APURADA",   // APURADA, CONTESTADA, MANTIDA, RECUPERADA, CANCELADA

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    var evidenceUrl: String? = null

) : AuditEntity(), TenantAware
