package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "clock_sync_status")
class ClockSyncStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "device_id", nullable = false)
    val deviceId: UUID,

    @Column(name = "last_sync_at")
    var lastSyncAt: OffsetDateTime? = null,

    @Column(name = "last_sync_status", length = 30)
    var lastSyncStatus: String? = null,

    @Column(name = "punches_imported", nullable = false)
    var punchesImported: Int = 0,

    @Column(name = "errors_count", nullable = false)
    var errorsCount: Int = 0,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "next_sync_at")
    var nextSyncAt: OffsetDateTime? = null
) : AuditEntity(), TenantAware
