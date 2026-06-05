package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.service.EsocialService
import com.contractops.api.rh.service.PayrollCalculationService
import com.contractops.api.rh.service.PayslipService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/payroll/periods")
class PayrollAliasController(
    private val calculationService: PayrollCalculationService,
    private val payslipService: PayslipService,
    private val esocialService: EsocialService
) {
    @PostMapping("/calculate")
    fun calculate(
        @RequestParam employeeId: UUID,
        @RequestParam contractId: UUID,
        @RequestParam competence: String,
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
                "netAmount" to payslip.netAmount,
                "status" to payslip.status
            )
        )
    }

    @PostMapping("/{payslipId}/close")
    fun close(
        @PathVariable payslipId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val closed = payslipService.approvePayslip(payslipId, effectiveTenant)
        return ResponseEntity.ok(mapOf("id" to closed.id, "status" to closed.status))
    }

    @PostMapping("/{payslipId}/transmit-esocial")
    fun transmitEsocial(
        @PathVariable payslipId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val payslip = payslipService.findById(payslipId, effectiveTenant)
            ?: return ResponseEntity.notFound().build()
        val event = esocialService.generateS1200Remuneracao(
            employeeId = payslip.employeeId,
            tenantId = effectiveTenant,
            competencia = payslip.competence
        )
        val result = esocialService.transmitEvent(event.id!!, effectiveTenant)
        return ResponseEntity.ok(mapOf("eventId" to result.id, "status" to result.status))
    }
}
