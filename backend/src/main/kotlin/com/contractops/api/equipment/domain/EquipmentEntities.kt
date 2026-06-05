package com.contractops.api.equipment.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "equipment_items")
class EquipmentItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) override val tenantId: UUID,
    @Column(name = "name", nullable = false, length = 200) var name: String,
    @Column(name = "serial_number", length = 100) var serialNumber: String? = null,
    @Column(name = "category", length = 80) var category: String? = null,
    @Column(name = "acquisition_cost", precision = 14, scale = 2) var acquisitionCost: BigDecimal? = null,
    @Column(name = "acquisition_date") var acquisitionDate: LocalDate? = null,
    @Column(name = "status", nullable = false, length = 30) var status: String = "DISPONIVEL"
) : AuditEntity(), TenantAware

@Entity
@Table(name = "equipment_allocations")
class EquipmentAllocation(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) override val tenantId: UUID,
    @Column(name = "equipment_id", nullable = false) val equipmentId: UUID,
    @Column(name = "post_id") var postId: UUID? = null,
    @Column(name = "employee_id") var employeeId: UUID? = null,
    @Column(name = "contract_id") var contractId: UUID? = null,
    @Column(name = "allocated_at", nullable = false) var allocatedAt: LocalDate = LocalDate.now(),
    @Column(name = "returned_at") var returnedAt: LocalDate? = null,
    @Column(name = "status", nullable = false, length = 30) var status: String = "ATIVO"
) : AuditEntity(), TenantAware

@Entity
@Table(name = "equipment_maintenance")
class EquipmentMaintenance(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) override val tenantId: UUID,
    @Column(name = "equipment_id", nullable = false) val equipmentId: UUID,
    @Column(name = "maintenance_type", nullable = false, length = 50) var maintenanceType: String,
    @Column(name = "description", columnDefinition = "TEXT") var description: String? = null,
    @Column(name = "cost", precision = 12, scale = 2) var cost: BigDecimal? = null,
    @Column(name = "performed_at", nullable = false) var performedAt: LocalDate,
    @Column(name = "next_due_date") var nextDueDate: LocalDate? = null
) : AuditEntity(), TenantAware
