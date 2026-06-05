package com.contractops.api.equipment.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.equipment.domain.*
import com.contractops.api.equipment.service.EquipmentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/equipamentos")
class EquipmentController(private val service: EquipmentService) {

    @GetMapping("/items")
    fun items(@RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.listItems(tenantId ?: tenant(tenantId)))

    @PostMapping("/items")
    fun createItem(@RequestBody req: CreateEquipmentRequest, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.createItem(EquipmentItem(
            tenantId = tenant(tenantId), name = req.name, serialNumber = req.serialNumber,
            category = req.category, acquisitionCost = req.acquisitionCost, acquisitionDate = req.acquisitionDate,
            status = req.status ?: "DISPONIVEL"
        )))

    @GetMapping("/allocations")
    fun allocations(@RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.listAllocations(tenant(tenantId)))

    @PostMapping("/allocations")
    fun allocate(@RequestBody req: CreateAllocationRequest, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.allocate(EquipmentAllocation(
            tenantId = tenant(tenantId), equipmentId = req.equipmentId, postId = req.postId,
            employeeId = req.employeeId, contractId = req.contractId, status = req.status ?: "ATIVO"
        )))

    @GetMapping("/{equipmentId}/maintenance")
    fun maintenance(@PathVariable equipmentId: UUID, @RequestParam(required = false) tenantId: UUID?) =
        ResponseEntity.ok(service.listMaintenance(tenant(tenantId), equipmentId))

    private fun tenant(tenantId: UUID?) =
        tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
}

data class CreateEquipmentRequest(
    val name: String, val serialNumber: String? = null, val category: String? = null,
    val acquisitionCost: java.math.BigDecimal? = null, val acquisitionDate: java.time.LocalDate? = null,
    val status: String? = null
)

data class CreateAllocationRequest(
    val equipmentId: UUID, val postId: UUID? = null, val employeeId: UUID? = null,
    val contractId: UUID? = null, val status: String? = null
)
