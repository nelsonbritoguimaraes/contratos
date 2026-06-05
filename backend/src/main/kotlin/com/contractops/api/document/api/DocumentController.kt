package com.contractops.api.document.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.document.domain.Document
import com.contractops.api.document.service.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('ADMIN','GESTOR_GRUPO','GESTOR_CONTRATO','SUPERVISOR','FISCAL_INTERNO')")
class DocumentController(
    private val service: DocumentService
) {

    @GetMapping
    fun listByEntity(
        @RequestParam entityType: String,
        @RequestParam entityId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<Document>> {
        val effective = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.findByEntity(entityType, entityId, effective))
    }

    @PostMapping
    fun upload(
        @RequestBody doc: Document,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Document> {
        val effective = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val toSave = Document(
            tenantId = effective,
            title = doc.title,
            description = doc.description,
            entityType = doc.entityType,
            entityId = doc.entityId,
            filePath = doc.filePath,
            mimeType = doc.mimeType,
            fileSize = doc.fileSize,
            version = doc.version,
            expiryDate = doc.expiryDate,
            status = doc.status
        )
        return ResponseEntity.ok(service.uploadDocument(toSave))
    }
}