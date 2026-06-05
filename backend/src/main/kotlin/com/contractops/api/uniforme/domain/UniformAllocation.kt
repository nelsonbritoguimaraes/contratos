package com.contractops.api.uniforme.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

/**
 * Alocação de uniforme para funcionário ou posto.
 */
@Entity
@Table(name = "uniform_allocations")
class UniformAllocation(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "uniform_item_id", nullable = false)
    var uniformItemId: UUID,

    @Column(name = "employee_id")
    var employeeId: UUID? = null,

    @Column(name = "post_id")
    var postId: UUID? = null,

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1,

    @Column(name = "delivery_date")
    var deliveryDate: LocalDate? = null,

    @Column(name = "return_date")
    var returnDate: LocalDate? = null,

    @Column(name = "status", length = 30)
    var status: String = "DELIVERED"   // DELIVERED, RETURNED, LOST

) : AuditEntity(), TenantAware {
    override fun toString(): String = "UniformAllocation(id=$id, status='$status')"
}