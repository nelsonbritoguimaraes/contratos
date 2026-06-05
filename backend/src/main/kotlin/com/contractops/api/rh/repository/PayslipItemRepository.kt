package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.PayslipItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PayslipItemRepository : JpaRepository<PayslipItem, UUID> {

    fun findByPayslipId(payslipId: UUID): List<PayslipItem>

    fun deleteByPayslipId(payslipId: UUID)
}