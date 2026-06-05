package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.repository.EsocialEventRepository
import com.contractops.api.rh.repository.PayslipRepository
import com.contractops.api.rh.service.PayrollRubricService
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/rh/dashboard")
class RhDashboardController(
    private val payslipRepository: PayslipRepository,
    private val esocialEventRepository: EsocialEventRepository,
    private val rubricService: PayrollRubricService
) {

    @GetMapping
    fun getDashboard(
        @RequestParam(required = false) competence: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): Map<String, Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = competence?.let { LocalDate.parse(it) } ?: LocalDate.now().withDayOfMonth(1)

        val payslips = payslipRepository.findByTenantIdAndCompetenceBetween(
            effectiveTenant, 
            comp.withDayOfMonth(1), 
            comp.withDayOfMonth(comp.lengthOfMonth())
        )

        val esocialPending = esocialEventRepository.findByTenantIdAndStatus(effectiveTenant, "PENDING")
        val esocialGenerated = esocialEventRepository.findByTenantIdAndStatus(effectiveTenant, "GENERATED")
        val esocialSent = esocialEventRepository.findByTenantIdAndStatus(effectiveTenant, "SENT")

        val totalNet = payslips.sumOf { it.netAmount ?: BigDecimal.ZERO }
        val totalEarnings = payslips.sumOf { it.totalEarnings ?: BigDecimal.ZERO }

        return mapOf(
            "competence" to comp.toString(),
            "resumoFolha" to mapOf(
                "totalHolerites" to payslips.size,
                "totalProventos" to totalEarnings,
                "totalLiquido" to totalNet,
                "holeritesPorStatus" to payslips.groupingBy { it.status }.eachCount()
            ),
            "eSocial" to mapOf(
                "pendentes" to esocialPending.size,
                "gerados" to esocialGenerated.size,
                "enviados" to esocialSent.size,
                "totalEventos" to (esocialPending.size + esocialGenerated.size + esocialSent.size)
            ),
            "rubricasAtivas" to rubricService.findAllActiveByTenant(effectiveTenant).size,
            "geradoEm" to java.time.OffsetDateTime.now().toString()
        )
    }

    @GetMapping("/encargos")
    fun getEncargosSummary(
        @RequestParam competence: String,
        @RequestParam(required = false) tenantId: UUID?
    ): Map<String, Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competence)

        val payslips = payslipRepository.findByTenantIdAndCompetenceBetween(
            effectiveTenant, comp.withDayOfMonth(1), comp.withDayOfMonth(comp.lengthOfMonth())
        ).filter { it.status == "APPROVED" || it.status == "EXPORTED" }

        val totalINSS = payslips.sumOf { (it.totalEarnings ?: BigDecimal.ZERO).multiply(BigDecimal("0.08")) }
        val totalFGTS = payslips.sumOf { (it.totalEarnings ?: BigDecimal.ZERO).multiply(BigDecimal("0.08")) }

        return mapOf(
            "competence" to competence,
            "totalColaboradores" to payslips.map { it.employeeId }.distinct().size,
            "encargos" to mapOf(
                "INSS" to totalINSS,
                "FGTS" to totalFGTS,
                "total" to totalINSS.add(totalFGTS)
            )
        )
    }
}