package com.contractops.api.common.api

import com.contractops.api.common.domain.CostCenter
import com.contractops.api.common.service.CostCenterService
import com.contractops.api.common.tenant.TenantContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/cost-centers")
class CostCenterController(
    private val costCenterService: CostCenterService
) {

    @GetMapping
    fun list(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<CostCenter>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(costCenterService.findAllByTenant(effectiveTenant))
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateCostCenterRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<CostCenter> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val created = costCenterService.create(
            effectiveTenant,
            request.companyId,
            request.branchId,
            request.name,
            request.code,
            request.description
        )
        return ResponseEntity
            .created(URI.create("/api/cost-centers/${created.id}"))
            .body(created)
    }
}

data class CreateCostCenterRequest(
    val companyId: UUID,
    val branchId: UUID? = null,
    val name: String,
    val code: String? = null,
    val description: String? = null
)
