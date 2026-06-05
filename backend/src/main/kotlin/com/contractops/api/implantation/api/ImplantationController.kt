package com.contractops.api.implantation.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.implantation.domain.ContractImplantation
import com.contractops.api.implantation.domain.ImplantationChecklistItem
import com.contractops.api.implantation.service.ImplantationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/implantations")
@PreAuthorize("hasAnyRole('ADMIN','GESTOR_GRUPO','GESTOR_CONTRATO')")
class ImplantationController(
    private val service: ImplantationService
) {
    @GetMapping
    fun listar(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<ContractImplantation>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.listar(t))
    }

    @GetMapping("/contract/{contractId}")
    fun porContrato(
        @PathVariable contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractImplantation> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val impl = service.buscarPorContrato(t, contractId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(impl)
    }

    @PostMapping("/contract/{contractId}/iniciar")
    fun iniciar(
        @PathVariable contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractImplantation> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.iniciar(t, contractId))
    }

    @GetMapping("/{id}/checklist")
    fun checklist(@PathVariable id: UUID): ResponseEntity<List<ImplantationChecklistItem>> =
        ResponseEntity.ok(service.checklist(id))

    @PostMapping("/checklist/{itemId}/concluir")
    fun concluirItem(
        @PathVariable itemId: UUID,
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "sistema") actor: String
    ): ResponseEntity<ImplantationChecklistItem> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.concluirItem(itemId, t, actor))
    }
}
