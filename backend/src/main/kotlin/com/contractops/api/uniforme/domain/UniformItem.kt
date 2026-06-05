package com.contractops.api.uniforme.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Item de uniforme (catálogo).
 * SPEC 7 e 25.6
 */
@Entity
@Table(name = "uniform_items")
class UniformItem(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "name", nullable = false, length = 150)
    var name: String,

    @Column(name = "type", length = 50)
    var type: String? = null,           // CAMISA, CALCA, SAPATO, JAQUETA, etc.

    @Column(name = "size", length = 20)
    var size: String? = null,

    @Column(name = "cost", precision = 10, scale = 2)
    var cost: java.math.BigDecimal? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true

) : AuditEntity(), TenantAware {
    override fun toString(): String = "UniformItem(id=$id, name='$name')"
}