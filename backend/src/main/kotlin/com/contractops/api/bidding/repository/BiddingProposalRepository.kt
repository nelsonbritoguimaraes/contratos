package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.BiddingProposal
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BiddingProposalRepository : JpaRepository<BiddingProposal, UUID> {
    fun findByBiddingIdAndTenantIdOrderByVersaoDesc(biddingId: UUID, tenantId: UUID): List<BiddingProposal>
}
