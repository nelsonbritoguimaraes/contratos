package com.contractops.api.common.audit

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "audit_events")
class AuditEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "entity_type", nullable = false, length = 80)
    val entityType: String,

    @Column(name = "entity_id")
    val entityId: UUID? = null,

    @Column(name = "action", nullable = false, length = 80)
    val action: String,

    @Column(name = "actor", length = 150)
    val actor: String? = null,

    @Column(name = "details", columnDefinition = "TEXT")
    val details: String? = null,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: OffsetDateTime = OffsetDateTime.now()
)
