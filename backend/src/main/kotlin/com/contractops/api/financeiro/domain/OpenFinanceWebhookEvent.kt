package com.contractops.api.financeiro.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "open_finance_webhook_events")
class OpenFinanceWebhookEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "conta_bancaria_id")
    var contaBancariaId: UUID? = null,

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    var payloadJson: String,

    @Column(name = "processado", nullable = false)
    var processado: Boolean = false,

    @Column(name = "itens_importados", nullable = false)
    var itensImportados: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
