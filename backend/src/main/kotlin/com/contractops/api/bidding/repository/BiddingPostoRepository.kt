package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.BiddingPosto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BiddingPostoRepository : JpaRepository<BiddingPosto, UUID> {
    fun findByBiddingIdAndTenantId(biddingId: UUID, tenantId: UUID): List<BiddingPosto>
}
