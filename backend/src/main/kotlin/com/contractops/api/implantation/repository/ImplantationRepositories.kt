package com.contractops.api.implantation.repository

import com.contractops.api.implantation.domain.ContractImplantation
import com.contractops.api.implantation.domain.ImplantationChecklistItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ContractImplantationRepository : JpaRepository<ContractImplantation, UUID> {
    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): ContractImplantation?
    fun findByTenantId(tenantId: UUID): List<ContractImplantation>
}

interface ImplantationChecklistRepository : JpaRepository<ImplantationChecklistItem, UUID> {
    fun findByImplantationIdOrderBySortOrder(implantationId: UUID): List<ImplantationChecklistItem>
}
