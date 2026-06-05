package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.service.PayrollEsocialOrchestratorService
import com.contractops.api.rh.service.PayrollFiscalDivergenceService
import com.contractops.api.rh.service.RhComplianceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/rh/compliance")
class RhComplianceController(
    private val complianceService: RhComplianceService,
    private val employeeComplianceService: com.contractops.api.rh.service.EmployeeComplianceService,
    private val payrollOrchestrator: PayrollEsocialOrchestratorService,
    private val divergenceService: PayrollFiscalDivergenceService
) {
    @GetMapping("/employee/{employeeId}")
    fun complianceColaborador(
        @PathVariable employeeId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<com.contractops.api.rh.domain.EmployeeCompliance>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(employeeComplianceService.listarPorColaborador(t, employeeId))
    }

    @PostMapping("/employee/{employeeId}")
    fun registrarCompliance(
        @PathVariable employeeId: UUID,
        @RequestBody request: RegistrarComplianceRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<com.contractops.api.rh.domain.EmployeeCompliance> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(
            employeeComplianceService.registrar(
                tenantId = t,
                employeeId = employeeId,
                tipo = request.tipo,
                descricao = request.descricao,
                dataRealizacao = request.dataRealizacao,
                dataValidade = request.dataValidade,
                documentoRef = request.documentoRef,
                observacao = request.observacao
            )
        )
    }
    @GetMapping("/calendario")
    fun calendario(
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<RhComplianceService.ObrigacaoRh>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia).withDayOfMonth(1)
        return ResponseEntity.ok(complianceService.calendarioCompetencia(comp))
    }

    @GetMapping("/divergencias")
    fun divergencias(
        @RequestParam competencia: String,
        @RequestParam(required = false) contractId: UUID?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<PayrollFiscalDivergenceService.RelatorioDivergencias> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia).withDayOfMonth(1)
        return ResponseEntity.ok(divergenceService.analisar(t, comp, contractId))
    }

    @PostMapping("/fechar-competencia")
    fun fecharCompetenciaFiscal(
        @RequestParam competencia: String,
        @RequestParam(required = false) contractId: UUID?,
        @RequestParam(defaultValue = "false") transmitir: Boolean,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia).withDayOfMonth(1)
        return ResponseEntity.ok(
            payrollOrchestrator.fecharCompetenciaFiscal(t, comp, contractId, transmitir)
        )
    }
}

data class RegistrarComplianceRequest(
    val tipo: String,
    val descricao: String? = null,
    val dataRealizacao: LocalDate? = null,
    val dataValidade: LocalDate? = null,
    val documentoRef: String? = null,
    val observacao: String? = null
)
