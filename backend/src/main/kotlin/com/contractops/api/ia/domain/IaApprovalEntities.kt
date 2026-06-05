package com.contractops.api.ia.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "ia_approval_queue")
class IaApprovalQueueItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "agent_name", nullable = false, length = 80)
    var agentName: String,

    @Column(name = "action_type", nullable = false, length = 80)
    var actionType: String,

    @Column(name = "entity_type", length = 80)
    var entityType: String? = null,

    @Column(name = "entity_id")
    var entityId: UUID? = null,

    @Column(name = "contract_id")
    var contractId: UUID? = null,

    @Column(name = "title", nullable = false, length = 300)
    var title: String,

    @Column(name = "summary", columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(name = "payload", columnDefinition = "TEXT")
    var payload: String? = null,

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "PENDING",

    @Column(name = "requested_by", length = 150)
    var requestedBy: String? = null,

    @Column(name = "reviewed_by", length = 150)
    var reviewedBy: String? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: OffsetDateTime? = null,

    @Column(name = "review_notes", columnDefinition = "TEXT")
    var reviewNotes: String? = null
) : AuditEntity(), TenantAware

@Entity
@Table(name = "rag_index_documents")
class RagIndexDocument(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "doc_key", nullable = false, length = 200)
    var docKey: String,

    @Column(name = "entity_type", length = 50)
    var entityType: String? = null,

    @Column(name = "entity_id")
    var entityId: UUID? = null,

    @Column(name = "title", length = 300)
    var title: String? = null,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "embedding_stub", columnDefinition = "TEXT")
    var embeddingStub: String? = null,

    @Column(name = "indexed_at", nullable = false)
    var indexedAt: OffsetDateTime = OffsetDateTime.now()
) : TenantAware
