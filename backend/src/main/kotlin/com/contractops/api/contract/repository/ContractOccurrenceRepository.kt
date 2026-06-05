package com.contractops.api.contract.repository

import com.contractops.api.contract.domain.ContractOccurrence
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ContractOccurrenceRepository : JpaRepository<ContractOccurrence, UUID> {
    fun findByTenantIdAndContractIdOrderByDataOcorrenciaDesc(tenantId: UUID, contractId: UUID): List<ContractOccurrence>
}
