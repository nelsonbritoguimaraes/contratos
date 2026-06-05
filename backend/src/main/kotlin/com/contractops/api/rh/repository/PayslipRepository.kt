package com.contractops.api.rh.repository

import com.contractops.api.rh.domain.Payslip
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface PayslipRepository : JpaRepository<Payslip, UUID> {

    fun findByTenantId(tenantId: UUID, pageable: Pageable): Page<Payslip>

    fun findByTenantIdAndContractId(tenantId: UUID, contractId: UUID, pageable: Pageable): Page<Payslip>

    fun findByTenantIdAndEmployeeIdAndCompetence(tenantId: UUID, employeeId: UUID, competence: LocalDate): Payslip?

    fun findByTenantIdAndEmployeeIdOrderByCompetenceDesc(tenantId: UUID, employeeId: UUID): List<Payslip>

    fun findByTenantIdAndContractIdAndCompetence(tenantId: UUID, contractId: UUID, competence: LocalDate): List<Payslip>

    fun findByTenantIdAndContractIdAndCompetenceBetween(
        tenantId: UUID, 
        contractId: UUID, 
        start: LocalDate, 
        end: LocalDate
    ): List<Payslip>

    fun findByTenantIdAndCompetenceBetween(tenantId: UUID, start: LocalDate, end: LocalDate): List<Payslip>
}