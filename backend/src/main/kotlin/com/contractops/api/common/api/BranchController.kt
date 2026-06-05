package com.contractops.api.common.api

import com.contractops.api.common.domain.Branch
import com.contractops.api.common.service.BranchService
import com.contractops.api.common.tenant.TenantContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/branches")
class BranchController(
    private val branchService: BranchService
) {

    @GetMapping
    fun list(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<Branch>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(branchService.findAllByTenant(effectiveTenant))
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateBranchRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Branch> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val created = branchService.create(effectiveTenant, request.companyId, request.name, request.city, request.state)
        return ResponseEntity
            .created(URI.create("/api/branches/${created.id}"))
            .body(created)
    }
}

data class CreateBranchRequest(
    val companyId: UUID,
    val name: String,
    val city: String? = null,
    val state: String? = null
)
