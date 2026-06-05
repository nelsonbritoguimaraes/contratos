package com.contractops.api.contract.repository

import com.contractops.api.contract.domain.Contract
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContractRepository : JpaRepository<Contract, UUID> {

    fun findByTenantId(tenantId: UUID): List<Contract>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<Contract>

    fun findByTenantIdOrderByOrgaoAscNumeroAsc(tenantId: UUID): List<Contract>

    fun findByTenantIdAndBidding_Id(tenantId: UUID, biddingId: UUID): List<Contract>
}
