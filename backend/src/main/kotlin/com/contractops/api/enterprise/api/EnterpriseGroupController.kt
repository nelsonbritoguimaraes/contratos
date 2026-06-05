package com.contractops.api.enterprise.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.enterprise.domain.EnterpriseGroup
import com.contractops.api.enterprise.service.EnterpriseGroupService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/enterprise-groups")
@PreAuthorize("hasAnyRole('ADMIN','GESTOR_GRUPO','GESTOR_CONTRATO')")
class EnterpriseGroupController(
    private val service: EnterpriseGroupService
) {
    @GetMapping
    fun list(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<EnterpriseGroup>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.list(t))
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EnterpriseGroup> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return service.findById(id, t)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateEnterpriseGroupRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EnterpriseGroup> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val created = service.create(t, request.name)
        return ResponseEntity.created(URI.create("/api/enterprise-groups/${created.id}")).body(created)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: CreateEnterpriseGroupRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<EnterpriseGroup> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return service.update(id, t, request.name)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}

data class CreateEnterpriseGroupRequest(val name: String)
