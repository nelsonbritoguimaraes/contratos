package com.contractops.api.common.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

/**
 * Tenant (SPEC seção 4 e 25)
 * Representa uma organização cliente do SaaS (empresa, grupo empresarial ou holding).
 * Toda entidade de negócio deve referenciar um tenant_id.
 */
@Entity
@Table(name = "tenants")
class Tenant(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "slug", nullable = false, length = 100, unique = true)
    var slug: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null

) {
    override fun toString(): String = "Tenant(id=$id, slug='$slug', name='$name')"
}
