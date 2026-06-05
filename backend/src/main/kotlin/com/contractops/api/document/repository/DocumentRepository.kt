package com.contractops.api.document.repository

import com.contractops.api.document.domain.Document
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DocumentRepository : JpaRepository<Document, UUID> {

    fun findByTenantIdAndEntityTypeAndEntityId(
        tenantId: UUID,
        entityType: String,
        entityId: UUID
    ): List<Document>
}