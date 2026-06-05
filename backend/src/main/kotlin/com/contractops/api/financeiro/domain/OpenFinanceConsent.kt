package com.contractops.api.financeiro.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "open_finance_consents")
class OpenFinanceConsent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "conta_bancaria_id")
    var contaBancariaId: UUID? = null,

    @Column(name = "institution_id", length = 100)
    var institutionId: String? = null,

    @Column(name = "institution_name", length = 200)
    var institutionName: String? = null,

    @Column(name = "consent_id", length = 200)
    var consentId: String? = null,

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "PENDING",

    @Column(name = "authorization_url", columnDefinition = "TEXT")
    var authorizationUrl: String? = null,

    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
