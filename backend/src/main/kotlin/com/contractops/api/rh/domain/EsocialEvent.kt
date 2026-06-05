package com.contractops.api.rh.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

/**
 * Evento eSocial (stubs).
 * Representa eventos que seriam enviados para o eSocial (S-2200, S-2299, S-1200, S-1010, etc.).
 *
 * Por enquanto geramos apenas a estrutura + dados necessários (stub de alta qualidade).
 */
@Entity
@Table(name = "esocial_events")
class EsocialEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "employee_id")
    val employeeId: UUID? = null,

    @Column(name = "event_type", nullable = false, length = 20)
    var eventType: String,   // S2200, S2299, S1200, S1010, etc.

    @Column(name = "competence")
    var competence: LocalDate? = null,

    @Column(name = "payload", columnDefinition = "TEXT")
    var payload: String? = null,   // JSON ou XML estruturado (stub)

    @Column(name = "status", length = 30)
    var status: String = "PENDING",   // PENDING, GENERATED, SENT, ACCEPTED, REJECTED

    @Column(name = "generated_at")
    var generatedAt: OffsetDateTime? = null,

    @Column(name = "receipt_number", length = 100)
    var receiptNumber: String? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "EsocialEvent(id=$id, type='$eventType', status='$status')"
}