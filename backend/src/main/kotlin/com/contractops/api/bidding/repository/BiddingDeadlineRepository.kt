package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.BiddingDeadline
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BiddingDeadlineRepository : JpaRepository<BiddingDeadline, UUID> {
    fun findByBiddingIdAndTenantIdOrderByDataLimiteAsc(biddingId: UUID, tenantId: UUID): List<BiddingDeadline>
}
