package com.contractops.api.document.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.util.*

/**
 * Versão de documento (suporte a versionamento).
 * Fase 2 - Document Portal.
 */
@Entity
@Table(name = "document_versions")
class DocumentVersion(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "document_id", nullable = false)
    var documentId: UUID,

    @Column(name = "version", length = 20, nullable = false)
    var version: String,

    @Column(name = "file_path", length = 500)
    var filePath: String? = null,

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    var extractedText: String? = null,   // resultado do OCR

    @Column(name = "is_current", nullable = false)
    var isCurrent: Boolean = true

) : AuditEntity(), TenantAware {
    override fun toString(): String = "DocumentVersion(id=$id, version='$version')"
}