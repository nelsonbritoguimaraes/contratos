package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Marcação normalizada após tratamento (deduplicação, correção de tipo, vinculação com colaborador).
 * Base para apuração de AttendanceDay.
 */
@Entity
@Table(name = "normalized_punches")
class NormalizedPunch(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "raw_punch_id")
    val rawPunchId: UUID? = null,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "post_id")
    val postId: UUID? = null,

    @Column(name = "contract_id")
    val contractId: UUID? = null,

    @Column(name = "punch_timestamp", nullable = false)
    var punchTimestamp: LocalDateTime,

    @Column(name = "punch_type", length = 20, nullable = false)
    var punchType: String,   // ENTRADA, SAIDA, etc.

    @Column(name = "source", length = 50)
    var source: String = "DEVICE",   // DEVICE, MANUAL, AFD, AEJ, IMPORT

    @Column(name = "justification", columnDefinition = "TEXT")
    var justification: String? = null

) : AuditEntity(), TenantAware
