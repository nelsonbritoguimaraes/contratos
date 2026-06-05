package com.contractops.api.common.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Centro de Custo
 * Alinhado com SPEC v1.0 seções 4 e 25.1.
 * Pode estar ligado a um contrato, filial ou operação.
 */
@Entity
@Table(name = "cost_centers")
class CostCenter(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    var branch: Branch? = null,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "code", length = 50)
    var code: String? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "CostCenter(id=$id, code='$code', name='$name')"
}
