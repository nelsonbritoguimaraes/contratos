package com.contractops.api.glosa.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Regra configurável de Glosa por Contrato.
 * Permite definir como glosas são calculadas (falta, atraso, IMR, etc.).
 * Alinhado com SPEC v1.0 seções 17 e 25.5.
 */
@Entity
@Table(name = "glosa_rules")
class GlosaRule(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id", nullable = false)
    val contractId: UUID,

    @Column(name = "rule_type", nullable = false, length = 50)
    var ruleType: String,   // FALTA, ATRASO, POSTO_DESCOBERTO, IMR, NAO_SUBSTITUICAO, etc.

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "factor", precision = 5, scale = 2)
    var factor: BigDecimal = BigDecimal.ONE,   // Multiplicador da glosa (1x, 2x, etc.)

    @Column(name = "tolerance_minutes")
    var toleranceMinutes: Int? = null,   // Tolerância para atrasos

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "priority", nullable = false)
    var priority: Int = 10   // Ordem de aplicação das regras

) : AuditEntity(), TenantAware {

    override fun toString(): String = "GlosaRule(id=$id, contractId=$contractId, type='$ruleType')"
}
