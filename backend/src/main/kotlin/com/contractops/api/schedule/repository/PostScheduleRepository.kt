package com.contractops.api.schedule.repository

import com.contractops.api.schedule.domain.PostSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface PostScheduleRepository : JpaRepository<PostSchedule, UUID> {
    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID): List<PostSchedule>
    fun findByTenantIdAndPostIdAndStatus(tenantId: UUID, postId: UUID, status: String): List<PostSchedule>
    fun findByTenantIdAndContractIdAndEffectiveFromLessThanEqualAndStatus(
        tenantId: UUID,
        contractId: UUID,
        date: LocalDate,
        status: String
    ): List<PostSchedule>
}
