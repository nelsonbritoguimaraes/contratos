package com.contractops.api.uniforme.service

import com.contractops.api.uniforme.domain.UniformAllocation
import com.contractops.api.uniforme.domain.UniformItem
import com.contractops.api.uniforme.repository.UniformAllocationRepository
import com.contractops.api.uniforme.repository.UniformItemRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class UniformService(
    private val itemRepo: UniformItemRepository,
    private val allocationRepo: UniformAllocationRepository
) {

    fun listItems(tenantId: UUID) = itemRepo.findByTenantIdAndActiveTrue(tenantId)

    fun createItem(item: UniformItem) = itemRepo.save(item)

    fun allocate(allocation: UniformAllocation) = allocationRepo.save(allocation)

    fun findAllocationsByEmployee(employeeId: UUID, tenantId: UUID) =
        allocationRepo.findByTenantIdAndEmployeeId(tenantId, employeeId)
}