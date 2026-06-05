package com.contractops.api.common.events

import java.time.OffsetDateTime
import java.util.*

data class DomainEvent(
    val eventType: String,
    val tenantId: UUID,
    val entityType: String? = null,
    val entityId: UUID? = null,
    val contractId: UUID? = null,
    val payload: Map<String, Any?> = emptyMap(),
    val occurredAt: OffsetDateTime = OffsetDateTime.now()
)

object DomainEventTypes {
    const val GLOSA_CALCULATED = "GlosaCalculated"
    const val INVOICE_ISSUED = "InvoiceIssued"
    const val JOURNAL_ENTRY_CREATED = "JournalEntryCreated"
}

fun glosaCalculatedEvent(
    tenantId: UUID,
    glosaId: UUID,
    contractId: UUID,
    glosaType: String,
    amount: Any,
    period: String
) = DomainEvent(
    eventType = DomainEventTypes.GLOSA_CALCULATED,
    tenantId = tenantId,
    entityType = "GLOSA",
    entityId = glosaId,
    contractId = contractId,
    payload = mapOf(
        "glosaType" to glosaType,
        "amount" to amount,
        "period" to period
    )
)

fun invoiceIssuedEvent(
    tenantId: UUID,
    nfsId: UUID,
    contractId: UUID?,
    numero: String,
    valor: Any
) = DomainEvent(
    eventType = DomainEventTypes.INVOICE_ISSUED,
    tenantId = tenantId,
    entityType = "NFS_E",
    entityId = nfsId,
    contractId = contractId,
    payload = mapOf("numero" to numero, "valor" to valor)
)

fun journalEntryCreatedEvent(
    tenantId: UUID,
    lancamentoId: UUID,
    contratoId: UUID?,
    origemTipo: String?,
    valor: Any
) = DomainEvent(
    eventType = DomainEventTypes.JOURNAL_ENTRY_CREATED,
    tenantId = tenantId,
    entityType = "LANCAMENTO",
    entityId = lancamentoId,
    contractId = contratoId,
    payload = mapOf("origemTipo" to origemTipo, "valor" to valor)
)
