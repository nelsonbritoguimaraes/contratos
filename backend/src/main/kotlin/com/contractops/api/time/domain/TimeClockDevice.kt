package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Dispositivo de Ponto (Relógio REP-C, REP-A, REP-P, ou sistema alternativo).
 * Alinhado com SPEC v1.0 seções 9 e 10 (Portaria 671/2021).
 */
@Entity
@Table(name = "time_clock_devices")
class TimeClockDevice(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "contract_id")
    val contractId: UUID? = null,

    @Column(name = "name", nullable = false, length = 150)
    var name: String,

    @Column(name = "manufacturer", length = 100)
    var manufacturer: String? = null,   // Control iD, Topdata, Henry, Dimep, etc.

    @Column(name = "model", length = 100)
    var model: String? = null,

    @Column(name = "serial_number", length = 100)
    var serialNumber: String? = null,

    @Column(name = "device_type", length = 50)
    var deviceType: String? = null,   // REP-C, REP-A, REP-P, SOFTWARE, MOBILE

    @Column(name = "ip_address", length = 50)
    var ipAddress: String? = null,

    @Column(name = "api_url", columnDefinition = "TEXT")
    var apiUrl: String? = null,

    @Column(name = "last_sync_at")
    var lastSyncAt: java.time.OffsetDateTime? = null,

    @Column(name = "status", length = 30)
    var status: String = "ACTIVE"   // ACTIVE, OFFLINE, ERROR, DEACTIVATED

) : AuditEntity(), TenantAware {

    override fun toString(): String = "TimeClockDevice(id=$id, name='$name', manufacturer='$manufacturer')"
}
