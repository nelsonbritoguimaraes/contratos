package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Marcação bruta vinda do relógio (antes de qualquer tratamento).
 * Essencial para conformidade com AFD/AEJ (Portaria 671).
 */
@Entity
@Table(name = "raw_punches")
class RawPunch(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "device_id")
    val deviceId: UUID? = null,

    @Column(name = "employee_id")
    val employeeId: UUID? = null,   // Pode vir nulo no raw e ser resolvido depois

    @Column(name = "matricula", length = 50)
    var matricula: String? = null,

    @Column(name = "cpf", length = 14)
    var cpf: String? = null,

    @Column(name = "punch_timestamp", nullable = false)
    var punchTimestamp: LocalDateTime,

    @Column(name = "punch_type", length = 20)
    var punchType: String? = null,

    @Column(name = "nsr", length = 50)
    var nsr: String? = null,

    @Column(name = "raw_data", columnDefinition = "TEXT")
    var rawData: String? = null,

    @Column(name = "import_batch_id")
    val importBatchId: UUID? = null,

    @Column(name = "latitude", precision = 10, scale = 7)
    var latitude: java.math.BigDecimal? = null,

    @Column(name = "longitude", precision = 10, scale = 7)
    var longitude: java.math.BigDecimal? = null,

    @Column(name = "source_channel", length = 30)
    var sourceChannel: String? = "DEVICE"

) : AuditEntity(), TenantAware {

    override fun toString(): String = "RawPunch(id=$id, timestamp=$punchTimestamp, nsr='$nsr')"
}
