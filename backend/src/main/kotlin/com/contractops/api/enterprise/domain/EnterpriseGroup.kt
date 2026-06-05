package com.contractops.api.enterprise.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "enterprise_groups")
class EnterpriseGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "name", nullable = false, length = 255)
    var name: String
) : AuditEntity(), TenantAware
