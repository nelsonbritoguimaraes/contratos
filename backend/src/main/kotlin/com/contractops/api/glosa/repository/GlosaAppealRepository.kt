package com.contractops.api.glosa.repository

import com.contractops.api.glosa.domain.GlosaAppeal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GlosaAppealRepository : JpaRepository<GlosaAppeal, UUID> {
    fun findByGlosaId(glosaId: UUID): List<GlosaAppeal>
    fun findByTenantIdAndGlosaId(tenantId: UUID, glosaId: UUID): List<GlosaAppeal>
    fun findByTenantIdAndAppealStatus(tenantId: UUID, status: String): List<GlosaAppeal>
}
