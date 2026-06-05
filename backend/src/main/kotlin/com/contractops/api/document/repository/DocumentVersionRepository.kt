package com.contractops.api.document.repository

import com.contractops.api.document.domain.DocumentVersion
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DocumentVersionRepository : JpaRepository<DocumentVersion, UUID> {
    fun findByDocumentIdAndIsCurrentTrue(documentId: UUID): DocumentVersion?
}