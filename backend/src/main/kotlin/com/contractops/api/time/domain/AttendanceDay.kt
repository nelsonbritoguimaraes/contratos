package com.contractops.api.time.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Apuração diária de presença de um colaborador em um posto (após pareamento de marcações).
 * Núcleo do módulo de Ponto (SPEC §9 e §10 — Portaria 671/2021).
 *
 * Gerada por AttendanceProcessingService a partir de RawPunch + NormalizedPunch.
 * Usada para cobertura %, glosas (POSTO_DESCOBERTO / FALTA), medição e folha.
 */
@Entity
@Table(name = "attendance_days")
class AttendanceDay(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "post_id")
    var postId: UUID? = null,

    @Column(name = "contract_id")
    var contractId: UUID? = null,

    @Column(name = "date", nullable = false)
    var date: LocalDate,

    @Column(name = "first_entry")
    var firstEntry: LocalDateTime? = null,

    @Column(name = "last_exit")
    var lastExit: LocalDateTime? = null,

    @Column(name = "total_worked_minutes")
    var totalWorkedMinutes: Int = 0,

    @Column(name = "delay_minutes")
    var delayMinutes: Int = 0,

    @Column(name = "absence_minutes")
    var absenceMinutes: Int = 0,

    @Column(name = "source", length = 50)
    var source: String = "AUTO_PROCESSED",   // AUTO_PROCESSED, MANUAL, AFD, IMPORT

    @Column(name = "justification", columnDefinition = "TEXT")
    var justification: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String =
        "AttendanceDay(id=$id, employeeId=$employeeId, date=$date, worked=$totalWorkedMinutes, source=$source)"
}