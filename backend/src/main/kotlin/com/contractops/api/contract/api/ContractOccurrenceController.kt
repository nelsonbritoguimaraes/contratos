package com.contractops.api.contract.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.contract.domain.ContractOccurrence
import com.contractops.api.contract.service.ContractOccurrenceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/contratos/{contractId}/ocorrencias")
class ContractOccurrenceController(
    private val service: ContractOccurrenceService
) {
    @GetMapping
    fun listar(
        @PathVariable contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<ContractOccurrence>> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.listar(contractId, t))
    }

    @PostMapping
    fun registrar(
        @PathVariable contractId: UUID,
        @RequestBody request: RegistrarOcorrenciaRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContractOccurrence> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val saved = service.registrar(
            tenantId = t,
            contractId = contractId,
            dataOcorrencia = request.dataOcorrencia ?: LocalDate.now(),
            tipo = request.tipo,
            titulo = request.titulo,
            descricao = request.descricao,
            severidade = request.severidade ?: "INFO",
            registradoPor = request.registradoPor,
            anexoRef = request.anexoRef
        )
        return ResponseEntity.ok(saved)
    }
}

data class RegistrarOcorrenciaRequest(
    val dataOcorrencia: LocalDate? = null,
    val tipo: String,
    val titulo: String,
    val descricao: String? = null,
    val severidade: String? = null,
    val registradoPor: String? = null,
    val anexoRef: String? = null
)
