package com.contractops.api.notification.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "contract_notifications")
class ContractNotification(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) override val tenantId: UUID,
    @Column(name = "contract_id", nullable = false) val contractId: UUID,
    @Column(name = "notification_number", length = 80) var notificationNumber: String? = null,
    @Column(name = "orgao", length = 255) var orgao: String? = null,
    @Column(name = "subject", nullable = false, length = 300) var subject: String,
    @Column(name = "description", columnDefinition = "TEXT") var description: String? = null,
    @Column(name = "received_at", nullable = false) var receivedAt: LocalDate,
    @Column(name = "response_deadline") var responseDeadline: LocalDate? = null,
    @Column(name = "responded_at") var respondedAt: LocalDate? = null,
    @Column(name = "status", nullable = false, length = 30) var status: String = "PENDENTE",
    @Column(name = "linked_glosa_id") var linkedGlosaId: UUID? = null
) : AuditEntity(), TenantAware
