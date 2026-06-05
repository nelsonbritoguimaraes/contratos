package com.contractops.api.glosa.repository

import com.contractops.api.glosa.domain.IMRIndicator
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface IMRIndicatorRepository : JpaRepository<IMRIndicator, UUID> {
    fun findByContractIdAndIsActiveTrue(contractId: UUID): List<IMRIndicator>
}
