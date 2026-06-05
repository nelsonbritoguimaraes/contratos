package com.contractops.api.grid.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.contabilidade.domain.LancamentoContabil
import com.contractops.api.contabilidade.repository.LancamentoContabilRepository
import com.contractops.api.glosa.domain.Glosa
import com.contractops.api.glosa.repository.GlosaRepository
import com.contractops.api.rh.domain.Payslip
import com.contractops.api.rh.repository.PayslipRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

data class ServerSideGridResponse<T>(
    val rows: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@RestController
@RequestMapping("/api/grid")
class ServerSideGridController(
    private val glosaRepository: GlosaRepository,
    private val lancamentoRepository: LancamentoContabilRepository,
    private val payslipRepository: PayslipRepository
) {
    @GetMapping("/glosas")
    fun glosas(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") pageSize: Int,
        @RequestParam(required = false) contractId: UUID?
    ): ResponseEntity<ServerSideGridResponse<Map<String, Any?>>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val pageable = PageRequest.of(page, pageSize.coerceIn(1, 200), Sort.by(Sort.Direction.DESC, "measurementPeriod"))
        val result = if (contractId != null) {
            glosaRepository.findByTenantIdAndContractId(t, contractId, pageable)
        } else {
            glosaRepository.findByTenantId(t, pageable)
        }
        return ResponseEntity.ok(
            ServerSideGridResponse(
                rows = result.content.map { g -> glosaRow(g) },
                total = result.totalElements,
                page = page,
                pageSize = pageSize
            )
        )
    }

    @GetMapping("/lancamentos")
    fun lancamentos(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") pageSize: Int,
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?
    ): ResponseEntity<ServerSideGridResponse<Map<String, Any?>>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val start = inicio?.let { LocalDate.parse(it) } ?: LocalDate.now().minusMonths(3)
        val end = fim?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val pageable = PageRequest.of(page, pageSize.coerceIn(1, 200), Sort.by(Sort.Direction.DESC, "data"))
        val result = lancamentoRepository.findByTenantIdAndDataBetween(t, start, end, pageable)
        return ResponseEntity.ok(
            ServerSideGridResponse(
                rows = result.content.map { l -> lancamentoRow(l) },
                total = result.totalElements,
                page = page,
                pageSize = pageSize
            )
        )
    }

    @GetMapping("/payslips")
    fun payslips(
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") pageSize: Int,
        @RequestParam(required = false) contractId: UUID?
    ): ResponseEntity<ServerSideGridResponse<Map<String, Any?>>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val pageable = PageRequest.of(page, pageSize.coerceIn(1, 200), Sort.by(Sort.Direction.DESC, "competence"))
        val result = if (contractId != null) {
            payslipRepository.findByTenantIdAndContractId(t, contractId, pageable)
        } else {
            payslipRepository.findByTenantId(t, pageable)
        }
        return ResponseEntity.ok(
            ServerSideGridResponse(
                rows = result.content.map { p -> payslipRow(p) },
                total = result.totalElements,
                page = page,
                pageSize = pageSize
            )
        )
    }

    private fun glosaRow(g: Glosa) = mapOf(
        "id" to g.id,
        "contractId" to g.contractId,
        "measurementPeriod" to g.measurementPeriod.toString(),
        "glosaType" to g.glosaType,
        "description" to g.description,
        "glosaAmount" to g.glosaAmount,
        "status" to g.status
    )

    private fun lancamentoRow(l: LancamentoContabil) = mapOf(
        "id" to l.id,
        "data" to l.data.toString(),
        "valor" to l.valor,
        "historico" to l.historico,
        "origemTipo" to l.origemTipo,
        "contratoId" to l.contratoId
    )

    private fun payslipRow(p: Payslip) = mapOf(
        "id" to p.id,
        "employeeId" to p.employeeId,
        "contractId" to p.contractId,
        "competence" to p.competence.toString(),
        "totalEarnings" to p.totalEarnings,
        "totalDeductions" to p.totalDeductions,
        "netPay" to p.netAmount,
        "status" to p.status
    )
}
