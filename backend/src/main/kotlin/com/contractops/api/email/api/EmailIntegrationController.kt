package com.contractops.api.email.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.email.service.EmailIntegrationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/email")
class EmailIntegrationController(
    private val service: EmailIntegrationService
) {

    @PostMapping("/receive")
    fun receive(
        @RequestParam from: String,
        @RequestParam subject: String,
        @RequestParam body: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val email = service.receiveEmail(from, subject, body, t)
        return ResponseEntity.ok(mapOf(
            "id" to email.id,
            "classification" to email.classification,
            "message" to "E-mail classificado automaticamente (Fase 2)"
        ))
    }

    @GetMapping("/classified")
    fun listClassified(
        @RequestParam classification: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.findByClassification(classification, t))
    }

    @PostMapping("/sync-imap")
    fun syncImap(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<Any> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val synced = service.syncFromImap(t)
        return ResponseEntity.ok(mapOf("synced" to synced.size, "messages" to synced))
    }
}