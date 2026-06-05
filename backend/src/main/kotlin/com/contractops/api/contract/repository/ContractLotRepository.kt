package com.contractops.api.contract.repository

import com.contractops.api.contract.domain.ContractLot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContractLotRepository : JpaRepository<ContractLot, UUID> {

    fun findByContractId(contractId: UUID): List<ContractLot>
}
