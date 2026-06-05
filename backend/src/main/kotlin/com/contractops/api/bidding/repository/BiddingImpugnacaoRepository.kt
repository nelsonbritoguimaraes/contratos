package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.BiddingAta
import com.contractops.api.bidding.domain.BiddingImpugnacao
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BiddingImpugnacaoRepository : JpaRepository<BiddingImpugnacao, UUID> {
    fun findByBiddingIdAndTenantId(biddingId: UUID, tenantId: UUID): List<BiddingImpugnacao>
}

interface BiddingAtaRepository : JpaRepository<BiddingAta, UUID> {
    fun findByBiddingIdAndTenantId(biddingId: UUID, tenantId: UUID): List<BiddingAta>
}
