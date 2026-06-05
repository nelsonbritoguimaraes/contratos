package com.contractops.api.contract.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Aditivo / Apostilamento / Repactuação / Reajuste de Contrato
 * Alinhado com SPEC v1.0 seção 6.2.
 */
@Entity
@Table(name = "contract_amendments")
class ContractAmendment(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    var contract: Contract,

    @Column(name = "amendment_number", length = 50)
    var amendmentNumber: String? = null,

    @Column(name = "type", nullable = false, length = 50)
    var type: String,   // PRORROGACAO, ACRÉSCIMO, SUPRESSÃO, REPACTUACAO, REAJUSTE, REEQUILIBRIO

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "effective_date")
    var effectiveDate: LocalDate? = null,

    @Column(name = "new_end_date")
    var newEndDate: LocalDate? = null,

    @Column(name = "value_change", precision = 16, scale = 2)
    var valueChange: BigDecimal? = null,

    @Column(name = "new_monthly_value", precision = 14, scale = 2)
    var newMonthlyValue: BigDecimal? = null,

    @Column(name = "status", length = 30)
    var status: String = "VIGENTE",   // VIGENTE, PENDENTE, REVOGADO

    @Column(name = "document_url", columnDefinition = "TEXT")
    var documentUrl: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "ContractAmendment(id=$id, type='$type', contractId=${contract.id})"
}
