package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.BiddingDocument
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BiddingDocumentRepository : JpaRepository<BiddingDocument, UUID> {
    fun findByBiddingIdAndTenantIdOrderByCreatedAtDesc(biddingId: UUID, tenantId: UUID): List<BiddingDocument>
}
