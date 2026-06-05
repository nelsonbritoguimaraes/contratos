package com.contractops.api.uniforme.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.uniforme.domain.UniformAllocation
import com.contractops.api.uniforme.domain.UniformItem
import com.contractops.api.uniforme.service.UniformService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/uniformes")
class UniformController(
    private val service: UniformService
) {

    @GetMapping("/items")
    fun listItems(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<UniformItem>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.listItems(t))
    }

    @PostMapping("/items")
    fun createItem(@RequestBody item: UniformItem, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<UniformItem> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val toSave = UniformItem(
            tenantId = t,
            name = item.name,
            type = item.type,
            size = item.size,
            cost = item.cost,
            active = item.active
        )
        return ResponseEntity.ok(service.createItem(toSave))
    }

    @PostMapping("/allocations")
    fun allocate(@RequestBody allocation: UniformAllocation, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<UniformAllocation> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val toSave = UniformAllocation(
            tenantId = t,
            uniformItemId = allocation.uniformItemId,
            employeeId = allocation.employeeId,
            postId = allocation.postId,
            quantity = allocation.quantity,
            deliveryDate = allocation.deliveryDate,
            returnDate = allocation.returnDate,
            status = allocation.status
        )
        return ResponseEntity.ok(service.allocate(toSave))
    }

    @GetMapping("/allocations")
    fun listByEmployee(
        @RequestParam employeeId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<UniformAllocation>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.findAllocationsByEmployee(employeeId, t))
    }
}