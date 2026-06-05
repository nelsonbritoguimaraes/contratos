package com.contractops.api.document.service

import com.contractops.api.document.domain.Document
import com.contractops.api.document.domain.DocumentVersion
import com.contractops.api.document.repository.DocumentRepository
import com.contractops.api.document.repository.DocumentVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class DocumentService(
    private val repository: DocumentRepository,
    private val versionRepository: DocumentVersionRepository,
    private val ocrService: OcrService
) {

    fun findByEntity(entityType: String, entityId: UUID, tenantId: UUID): List<Document> =
        repository.findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId)

    @Transactional
    fun uploadDocument(doc: Document): Document {
        val saved = repository.save(doc)

        // Cria versão + OCR stub (Fase 2)
        val extracted = ocrService.extractText(doc.filePath ?: "", doc.mimeType)
        val version = DocumentVersion(
            tenantId = saved.tenantId,
            documentId = saved.id!!,
            version = "1.0",
            filePath = saved.filePath,
            extractedText = extracted,
            isCurrent = true
        )
        versionRepository.save(version)

        return saved
    }

    fun findById(id: UUID, tenantId: UUID): Document? =
        repository.findById(id).filter { it.tenantId == tenantId }.orElse(null)
}