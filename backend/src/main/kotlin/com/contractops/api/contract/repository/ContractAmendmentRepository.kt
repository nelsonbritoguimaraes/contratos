package com.contractops.api.contract.repository

import com.contractops.api.contract.domain.ContractAmendment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContractAmendmentRepository : JpaRepository<ContractAmendment, UUID> {

    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): List<ContractAmendment>
}
