package com.contractops.api.bidding.repository

import com.contractops.api.bidding.domain.WinningSpreadsheet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WinningSpreadsheetRepository : JpaRepository<WinningSpreadsheet, UUID> {

    fun findByBiddingId(biddingId: UUID): List<WinningSpreadsheet>

    fun findByContractId(contractId: UUID): List<WinningSpreadsheet>
}
