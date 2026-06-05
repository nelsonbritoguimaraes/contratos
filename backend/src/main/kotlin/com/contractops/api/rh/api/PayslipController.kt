package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.service.EsocialService
import com.contractops.api.rh.service.PayrollCalculationService
import com.contractops.api.rh.service.PayslipService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/api/rh/payslips")
class PayslipController(
    private val calculationService: PayrollCalculationService,
    private val payslipService: PayslipService,
    private val esocialService: EsocialService
) {

    @PostMapping("/calculate")
    fun calculate(
        @RequestParam employeeId: UUID,
        @RequestParam contractId: UUID,
        @RequestParam competence: String,   // YYYY-MM-DD
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val competenceDate = java.time.LocalDate.parse(competence)

        val payslip = calculationService.calculatePayslip(employeeId, contractId, competenceDate, effectiveTenant)

        return ResponseEntity.ok(
            mapOf(
                "id" to payslip.id,
                "employeeId" to payslip.employeeId,
                "competence" to payslip.competence,
                "baseSalary" to payslip.baseSalary,
                "totalEarnings" to payslip.totalEarnings,
                "totalDeductions" to payslip.totalDeductions,
                "netAmount" to payslip.netAmount,
                "status" to payslip.status,
                "message" to "Folha calculada (MVP). Rubricas e Attendance integrados."
            )
        )
    }

    @PostMapping("/{payslipId}/approve")
    fun approve(
        @PathVariable payslipId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val approvedPayslip = payslipService.approvePayslip(payslipId, effectiveTenant)

        return ResponseEntity.ok(
            mapOf(
                "id" to approvedPayslip.id,
                "status" to approvedPayslip.status,
                "message" to "Holerite aprovado com sucesso. Evento S-1200 gerado automaticamente no eSocial."
            )
        )
    }

    @GetMapping("/report")
    fun reportByContractAndCompetence(
        @RequestParam contractId: UUID,
        @RequestParam competence: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val compDate = java.time.LocalDate.parse(competence)

        val payslips = payslipService.findByContractAndCompetence(contractId, compDate, effectiveTenant)

        val totalNet = payslips.sumOf { it.netAmount ?: BigDecimal.ZERO }
        val totalEarnings = payslips.sumOf { it.totalEarnings ?: BigDecimal.ZERO }
        val totalDeductions = payslips.sumOf { it.totalDeductions ?: BigDecimal.ZERO }

        return ResponseEntity.ok(
            mapOf(
                "contractId" to contractId,
                "competence" to competence,
                "totalHolerites" to payslips.size,
                "resumoFinanceiro" to mapOf(
                    "totalProventos" to totalEarnings,
                    "totalDescontos" to totalDeductions,
                    "totalLiquido" to totalNet
                ),
                "holerites" to payslips.map {
                    mapOf(
                        "id" to it.id,
                        "employeeId" to it.employeeId,
                        "baseSalary" to it.baseSalary,
                        "totalEarnings" to it.totalEarnings,
                        "totalDeductions" to it.totalDeductions,
                        "netAmount" to it.netAmount,
                        "status" to it.status
                    )
                }
            )
        )
    }

    @PostMapping("/contracts/{contractId}/close-competence")
    fun closeCompetence(
        @PathVariable contractId: UUID,
        @RequestParam competence: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val compDate = java.time.LocalDate.parse(competence)

        val closed = payslipService.closeCompetenceForContract(
            contractId,
            compDate,
            effectiveTenant,
            calculationService,
            // Para injeção completa do EsocialService, seria melhor ter um bean separado.
            // Por enquanto, o método interno cuida da geração via EsocialService injetado no PayslipService.
            esocialService
        )

        return ResponseEntity.ok(
            mapOf(
                "contractId" to contractId,
                "competence" to competence,
                "holeritesAprovados" to closed.size,
                "message" to "Competência fechada com sucesso (cálculo + aprovação em batch)."
            )
        )
    }

    @GetMapping("/contracts/{contractId}/encargos-summary")
    fun encargosSummary(
        @PathVariable contractId: UUID,
        @RequestParam competence: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val compDate = java.time.LocalDate.parse(competence)

        val payslips = payslipService.findByContractAndCompetence(contractId, compDate, effectiveTenant)
            .filter { it.status == "APPROVED" || it.status == "EXPORTED" }

        val totalINSS = payslips.sumOf { it.totalDeductions?.multiply(BigDecimal("0.6")) ?: BigDecimal.ZERO } // estimativa
        val totalFGTS = payslips.sumOf { it.totalEarnings?.multiply(BigDecimal("0.08")) ?: BigDecimal.ZERO }

        return ResponseEntity.ok(
            mapOf(
                "contractId" to contractId,
                "competence" to competence,
                "totalHoleritesAprovados" to payslips.size,
                "encargosEstimados" to mapOf(
                    "INSS" to totalINSS,
                    "FGTS" to totalFGTS,
                    "totalEncargos" to totalINSS.add(totalFGTS)
                ),
                "observacao" to "Valores são estimativas baseadas nos holerites aprovados."
            )
        )
    }

    @PostMapping("/global-close-competence")
    fun globalCloseCompetence(
        @RequestParam competence: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val compDate = java.time.LocalDate.parse(competence)

        val count = payslipService.closeAllContractsForCompetence(
            compDate,
            effectiveTenant,
            calculationService,
            esocialService
        )

        return ResponseEntity.ok(
            mapOf(
                "competence" to competence,
                "holeritesAprovados" to count,
                "message" to "Fechamento mensal geral executado com sucesso."
            )
        )
    }

    @PostMapping("/contracts/{contractId}/reopen-competence")
    fun reopenCompetence(
        @PathVariable contractId: UUID,
        @RequestParam competence: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val compDate = java.time.LocalDate.parse(competence)

        val count = payslipService.reopenCompetenceForContract(contractId, compDate, effectiveTenant)

        return ResponseEntity.ok(
            mapOf(
                "contractId" to contractId,
                "competence" to competence,
                "holeritesReabertos" to count,
                "message" to "Competência reaberta com sucesso. Holerites voltaram para status CALCULATED."
            )
        )
    }
}