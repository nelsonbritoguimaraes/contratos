package com.contractops.api.ia.repository

import com.contractops.api.ia.domain.IaApprovalQueueItem
import com.contractops.api.ia.domain.RagIndexDocument
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface IaApprovalQueueRepository : JpaRepository<IaApprovalQueueItem, UUID> {
    fun findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId: UUID, status: String): List<IaApprovalQueueItem>
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<IaApprovalQueueItem>
}

interface RagIndexDocumentRepository : JpaRepository<RagIndexDocument, UUID> {
    fun findByTenantIdAndDocKey(tenantId: UUID, docKey: String): RagIndexDocument?
    fun findByTenantIdOrderByIndexedAtDesc(tenantId: UUID): List<RagIndexDocument>
}
