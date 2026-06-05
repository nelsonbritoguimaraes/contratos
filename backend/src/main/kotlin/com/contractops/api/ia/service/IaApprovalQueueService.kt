package com.contractops.api.ia.service

import com.contractops.api.ia.domain.IaApprovalQueueItem
import com.contractops.api.ia.repository.IaApprovalQueueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
class IaApprovalQueueService(
    private val repository: IaApprovalQueueRepository
) {
    fun listPending(tenantId: UUID): List<IaApprovalQueueItem> =
        repository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "PENDING")

    fun listAll(tenantId: UUID): List<IaApprovalQueueItem> =
        repository.findByTenantIdOrderByCreatedAtDesc(tenantId)

    @Transactional
    fun enqueue(item: IaApprovalQueueItem): IaApprovalQueueItem =
        repository.save(item)

    @Transactional
    fun approve(id: UUID, tenantId: UUID, reviewer: String, notes: String? = null): IaApprovalQueueItem {
        val item = repository.findById(id).orElseThrow()
        require(item.tenantId == tenantId) { "Item não pertence ao tenant" }
        item.status = "APPROVED"
        item.reviewedBy = reviewer
        item.reviewedAt = OffsetDateTime.now()
        item.reviewNotes = notes
        return repository.save(item)
    }

    @Transactional
    fun reject(id: UUID, tenantId: UUID, reviewer: String, notes: String? = null): IaApprovalQueueItem {
        val item = repository.findById(id).orElseThrow()
        require(item.tenantId == tenantId) { "Item não pertence ao tenant" }
        item.status = "REJECTED"
        item.reviewedBy = reviewer
        item.reviewedAt = OffsetDateTime.now()
        item.reviewNotes = notes
        return repository.save(item)
    }
}
