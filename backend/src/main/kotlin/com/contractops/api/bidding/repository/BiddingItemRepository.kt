package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.BiddingItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BiddingItemRepository : JpaRepository<BiddingItem, UUID> {
    fun findByBiddingLotIdAndTenantId(biddingLotId: UUID, tenantId: UUID): List<BiddingItem>
}
