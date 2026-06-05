package com.contractops.api.equipment.service

import com.contractops.api.equipment.domain.*
import com.contractops.api.equipment.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EquipmentService(
    private val itemRepository: EquipmentItemRepository,
    private val allocationRepository: EquipmentAllocationRepository,
    private val maintenanceRepository: EquipmentMaintenanceRepository
) {
    fun listItems(tenantId: UUID): List<EquipmentItem> = itemRepository.findByTenantId(tenantId)

    @Transactional
    fun createItem(item: EquipmentItem): EquipmentItem = itemRepository.save(item)

    fun listAllocations(tenantId: UUID): List<EquipmentAllocation> =
        allocationRepository.findByTenantId(tenantId)

    @Transactional
    fun allocate(allocation: EquipmentAllocation): EquipmentAllocation =
        allocationRepository.save(allocation)

    fun listMaintenance(tenantId: UUID, equipmentId: UUID): List<EquipmentMaintenance> =
        maintenanceRepository.findByTenantIdAndEquipmentId(tenantId, equipmentId)

    @Transactional
    fun registerMaintenance(m: EquipmentMaintenance): EquipmentMaintenance =
        maintenanceRepository.save(m)
}
