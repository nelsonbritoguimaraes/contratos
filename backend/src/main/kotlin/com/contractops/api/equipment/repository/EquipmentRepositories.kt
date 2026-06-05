package com.contractops.api.equipment.repository

import com.contractops.api.equipment.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EquipmentItemRepository : JpaRepository<EquipmentItem, UUID> {
    fun findByTenantId(tenantId: UUID): List<EquipmentItem>
}

interface EquipmentAllocationRepository : JpaRepository<EquipmentAllocation, UUID> {
    fun findByTenantId(tenantId: UUID): List<EquipmentAllocation>
    fun findByTenantIdAndEquipmentId(tenantId: UUID, equipmentId: UUID): List<EquipmentAllocation>
}

interface EquipmentMaintenanceRepository : JpaRepository<EquipmentMaintenance, UUID> {
    fun findByTenantIdAndEquipmentId(tenantId: UUID, equipmentId: UUID): List<EquipmentMaintenance>
}
