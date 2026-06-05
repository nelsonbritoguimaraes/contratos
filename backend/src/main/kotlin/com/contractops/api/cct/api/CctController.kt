package com.contractops.api.cct.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.cct.service.CctService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.*

/**
 * Upload e consulta de CCTs (Convenções Coletivas).
 * Entrada principal para regras salariais e benefícios (SPEC 4.15).
 * Extração básica implementada; IA avançada virá depois.
 */
@RestController
@RequestMapping("/api/ccts")
class CctController(
    private val service: CctService
) {

    @GetMapping
    fun list(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val list = service.findAllByTenant(effectiveTenant)
        return ResponseEntity.ok(list.map {
            mapOf(
                "id" to it.id,
                "sindicato" to it.sindicato,
                "vigenciaInicio" to it.vigenciaInicio,
                "vigenciaFim" to it.vigenciaFim,
                "arquivoNome" to it.arquivoNome,
                "status" to it.status,
                "extractedData" to it.extractedData
            )
        })
    }

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) contractId: UUID?,
        @RequestParam(required = false) sindicato: String?,
        @RequestParam(required = false) vigenciaInicio: LocalDate?,
        @RequestParam(required = false) vigenciaFim: LocalDate?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any?>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId()
            ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val cct = service.uploadCct(effectiveTenant, contractId, file, sindicato, vigenciaInicio, vigenciaFim)

        return ResponseEntity.ok(
            mapOf(
                "id" to cct.id,
                "message" to "CCT importada com sucesso. Extração básica de cláusulas realizada.",
                "extracted" to cct.extractedData,
                "arquivo" to cct.arquivoNome
            )
        )
    }
}