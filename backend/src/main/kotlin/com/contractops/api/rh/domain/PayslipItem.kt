package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

/**
 * Item de uma folha de pagamento (linha do holerite).
 * Liga uma rubrica ao holerite com o valor calculado.
 */
@Entity
@Table(name = "payslip_items")
class PayslipItem(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id", nullable = false)
    var payslip: Payslip,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    var rubric: PayrollRubric,

    @Column(name = "description", length = 200)
    var description: String? = null,

    @Column(name = "quantity", precision = 10, scale = 2)
    var quantity: BigDecimal = BigDecimal.ONE,

    @Column(name = "unit_value", precision = 14, scale = 2)
    var unitValue: BigDecimal? = null,

    @Column(name = "total_value", precision = 14, scale = 2, nullable = false)
    var totalValue: BigDecimal,

    @Column(name = "type", length = 20)
    var type: String? = null   // PROVENTO or DESCONTO (denormalizado para facilitar relatórios)

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "PayslipItem(id=$id, rubric=${rubric.code}, value=$totalValue)"
}