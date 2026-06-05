package com.contractops.api.contabilidade.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.contabilidade.service.SpedService
import com.contractops.api.contabilidade.service.SpedTransmissionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/contabilidade/sped")
class SpedController(
    private val spedService: SpedService,
    private val transmissionService: SpedTransmissionService
) {

    @GetMapping("/ecd")
    fun gerarECD(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val arquivo = spedService.gerarSpedContabilECD(effectiveTenant, dataInicio, dataFim)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=sped_ecd_${dataInicio.year}.txt")
            .body(arquivo)
    }

    @GetMapping("/ecf")
    fun gerarECF(
        @RequestParam ano: Int,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val arquivo = spedService.gerarECF(effectiveTenant, ano)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=ecf_$ano.txt")
            .body(arquivo)
    }

    @GetMapping("/efd-reinf")
    fun gerarEfdReinf(
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia)

        val arquivo = spedService.gerarEfdReinf(effectiveTenant, comp)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=efd_reinf_${comp}.txt")
            .body(arquivo)
    }

    @PostMapping("/validar")
    fun validar(
        @RequestParam tipo: String,
        @RequestParam(required = false) inicio: String?,
        @RequestParam(required = false) fim: String?,
        @RequestParam(required = false) ano: Int?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val result = transmissionService.validar(
            effectiveTenant,
            tipo,
            inicio?.let { LocalDate.parse(it) },
            fim?.let { LocalDate.parse(it) },
            ano
        )
        return ResponseEntity.ok(result)
    }

    @PostMapping("/transmitir/{transmissionId}")
    fun transmitir(
        @PathVariable transmissionId: UUID,
        @RequestParam(required = false) aprovadoPor: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(transmissionService.transmitir(effectiveTenant, transmissionId, aprovadoPor))
    }

    @GetMapping("/transmissoes")
    fun listarTransmissoes(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<Map<String, Any?>>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val list = transmissionService.listar(effectiveTenant).map {
            mapOf(
                "id" to it.id,
                "tipo" to it.tipo,
                "status" to it.status,
                "competenciaInicio" to it.competenciaInicio?.toString(),
                "competenciaFim" to it.competenciaFim?.toString(),
                "anoCalendario" to it.anoCalendario,
                "protocolo" to it.protocolo,
                "mensagem" to it.mensagem,
                "totalRegistros" to it.totalRegistros,
                "errosValidacao" to it.errosValidacao,
                "transmitidoEm" to it.transmitidoEm?.toString()
            )
        }
        return ResponseEntity.ok(list)
    }
}
