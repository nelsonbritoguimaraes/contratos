package com.contractops.api.glosa.repository

import com.contractops.api.glosa.domain.GlosaRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GlosaRuleRepository : JpaRepository<GlosaRule, UUID> {

    fun findByContractIdAndIsActiveTrue(contractId: UUID): List<GlosaRule>
}
