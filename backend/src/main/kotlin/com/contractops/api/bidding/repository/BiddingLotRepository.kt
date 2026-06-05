package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.BiddingLot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BiddingLotRepository : JpaRepository<BiddingLot, UUID> {

    fun findByBiddingId(biddingId: UUID): List<BiddingLot>
}
