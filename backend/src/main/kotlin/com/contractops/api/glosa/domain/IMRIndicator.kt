package com.contractops.api.glosa.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Indicador de IMR (Índice de Medição de Resultado).
 * Configurável por contrato. SPEC §18 e §25.5.
 */
@Entity
@Table(name = "imr_indicators")
class IMRIndicator(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "name", nullable = false, length = 150)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "weight", precision = 5, scale = 2, nullable = false)
    var weight: BigDecimal = BigDecimal.ONE,   // Peso no cálculo geral do IMR

    @Column(name = "measurement_method", length = 100)
    var measurementMethod: String? = null,   // Ex: "PERCENTUAL_COBERTURA", "TEMPO_REPOSICAO"

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

) : AuditEntity(), TenantAware
