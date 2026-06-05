package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.Bidding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BiddingRepository : JpaRepository<Bidding, UUID> {

    fun findByTenantId(tenantId: UUID): List<Bidding>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<Bidding>
}
