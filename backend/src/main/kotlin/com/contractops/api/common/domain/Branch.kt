package com.contractops.api.common.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Filial / Estabelecimento
 * Alinhado com SPEC v1.0 seções 4 e 25.1.
 */
@Entity
@Table(name = "branches")
class Branch(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "city", length = 100)
    var city: String? = null,

    @Column(name = "state", length = 2)
    var state: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "Branch(id=$id, name='$name', companyId=${company.id})"
}
