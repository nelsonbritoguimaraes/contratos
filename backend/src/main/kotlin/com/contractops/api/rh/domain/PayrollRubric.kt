package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Rubrica de Folha de Pagamento.
 * Representa proventos e descontos que podem ser aplicados na remuneração do colaborador.
 *
 * Alinhado com SPEC v1.0 seção 8 (DP) e Fase 3 (Folha).
 */
@Entity
@Table(name = "payroll_rubrics")
class PayrollRubric(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "code", nullable = false, length = 50)
    var code: String,   // Ex: SALARIO_BASE, INSALUBRIDADE, INSS, FGTS

    @Column(name = "description", nullable = false, length = 200)
    var description: String,

    @Column(name = "type", nullable = false, length = 20)
    var type: String,   // PROVENTO, DESCONTO

    @Column(name = "calculation_type", nullable = false, length = 30)
    var calculationType: String,   // FIXED, PERCENTAGE_OF_BASE, FORMULA_SIMPLE

    @Column(name = "fixed_value", precision = 12, scale = 2)
    var fixedValue: BigDecimal? = null,

    @Column(name = "percentage", precision = 5, scale = 2)
    var percentage: BigDecimal? = null,

    @Column(name = "reference", length = 50)
    var reference: String? = null,   // Ex: "SALARIO_BASE", "TOTAL_PROVENTOS"

    // Incidências
    @Column(name = "incides_inss", nullable = false)
    var incidesInss: Boolean = false,

    @Column(name = "incides_fgts", nullable = false)
    var incidesFgts: Boolean = false,

    @Column(name = "incides_irrf", nullable = false)
    var incidesIrrf: Boolean = false,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "display_order")
    var displayOrder: Int = 0

) : AuditEntity(), TenantAware {

    override fun toString(): String = "PayrollRubric(id=$id, code='$code', type='$type')"
}