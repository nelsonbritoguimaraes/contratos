package com.contractops.api.glosa.repository

import com.contractops.api.glosa.domain.GlosaEvidence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GlosaEvidenceRepository : JpaRepository<GlosaEvidence, UUID> {
    fun findByGlosaId(glosaId: UUID): List<GlosaEvidence>
    fun findByTenantIdAndGlosaId(tenantId: UUID, glosaId: UUID): List<GlosaEvidence>
}
