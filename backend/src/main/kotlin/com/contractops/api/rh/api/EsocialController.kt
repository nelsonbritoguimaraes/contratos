package com.contractops.api.rh.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.rh.service.EsocialService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/rh/esocial")
class EsocialController(
    private val esocialService: EsocialService
) {

    @PostMapping("/s2200/{employeeId}")
    fun gerarS2200(
        @PathVariable employeeId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val event = esocialService.generateS2200Admissao(employeeId, effectiveTenant)
        return ResponseEntity.ok(event)
    }

    @PostMapping("/s2299/{employeeId}")
    fun gerarS2299(
        @PathVariable employeeId: UUID,
        @RequestParam dataDesligamento: String,
        @RequestParam motivo: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val data = LocalDate.parse(dataDesligamento)
        val event = esocialService.generateS2299Desligamento(employeeId, effectiveTenant, data, motivo)
        return ResponseEntity.ok(event)
    }

    @PostMapping("/s1200/{employeeId}")
    fun gerarS1200(
        @PathVariable employeeId: UUID,
        @RequestParam competencia: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia)
        val event = esocialService.generateS1200Remuneracao(employeeId, effectiveTenant, comp)
        return ResponseEntity.ok(event)
    }

    @GetMapping("/pendentes")
    fun listarPendentes(
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val pendentes = esocialService.findPendingByTenant(effectiveTenant)
        return ResponseEntity.ok(pendentes)
    }

    @PostMapping("/simulate-send/{eventId}")
    fun simularEnvio(
        @PathVariable eventId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> = transmit(eventId, tenantId)

    @PostMapping("/transmit/{eventId}")
    fun transmit(
        @PathVariable eventId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val event = esocialService.transmitEvent(eventId, effectiveTenant)
        return ResponseEntity.ok(
            mapOf(
                "id" to event.id,
                "status" to event.status,
                "receiptNumber" to event.receiptNumber,
                "message" to "Transmissão eSocial processada (modo configurado em contractops.fiscal.mode)"
            )
        )
    }

    @PostMapping("/s1010")
    fun gerarS1010(
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val event = esocialService.generateS1010Rubricas(effectiveTenant)
        return ResponseEntity.ok(event)
    }

    @PostMapping("/simulate-reception/{eventId}")
    fun simularRecepcao(
        @PathVariable eventId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        // Em um sistema real, aqui processaríamos o retorno do eSocial
        val event = esocialService.simulateSend(eventId, effectiveTenant) // reaproveita para mudar status
        event.status = "ACCEPTED" // Simula aceitação pelo governo
        return ResponseEntity.ok(
            mapOf(
                "id" to event.id,
                "status" to "ACCEPTED",
                "message" to "Recepção simulada: evento aceito pelo eSocial (stub)"
            )
        )
    }

    // ==================== NOVOS ENDPOINTS DE EVENTOS eSOCIAL ====================

    @PostMapping("/s2205/{employeeId}")
    fun gerarS2205(@PathVariable employeeId: UUID, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(esocialService.generateS2205AlteracaoCadastral(employeeId, effectiveTenant))
    }

    @PostMapping("/s2206/{employeeId}")
    fun gerarS2206(
        @PathVariable employeeId: UUID,
        @RequestParam novoSalario: java.math.BigDecimal,
        @RequestParam(required = false) novaFuncao: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(esocialService.generateS2206AlteracaoContrato(employeeId, effectiveTenant, novoSalario, novaFuncao))
    }

    @PostMapping("/s2230/{employeeId}")
    fun gerarS2230(
        @PathVariable employeeId: UUID,
        @RequestParam tipoAfastamento: String,
        @RequestParam dataInicio: String,
        @RequestParam(required = false) dataFim: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val inicio = java.time.LocalDate.parse(dataInicio)
        val fim = dataFim?.let { java.time.LocalDate.parse(it) }
        return ResponseEntity.ok(esocialService.generateS2230Afastamento(employeeId, effectiveTenant, tipoAfastamento, inicio, fim))
    }

    @PostMapping("/s1299")
    fun gerarS1299(@RequestParam competencia: String, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia)
        return ResponseEntity.ok(esocialService.generateS1299FechamentoPeriodico(comp, effectiveTenant))
    }

    @PostMapping("/s2399")
    fun gerarS2399(@RequestParam competence: String, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = java.time.LocalDate.parse(competence)
        return ResponseEntity.ok(esocialService.generateS2399FechamentoPeriodico(comp, effectiveTenant))
    }

    @PostMapping("/s1210")
    fun gerarS1210(@RequestParam competence: String, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = java.time.LocalDate.parse(competence)
        return ResponseEntity.ok(esocialService.generateS1210Pagamento(comp, effectiveTenant))
    }

    @GetMapping("/event-types")
    fun listarTiposEventos(): ResponseEntity<Any> {
        return ResponseEntity.ok(
            mapOf(
                "eventosNaoPeriodicos" to com.contractops.api.rh.domain.EsocialEventType.getNonPeriodic().map { it.name to it.description },
                "eventosPeriodicos" to com.contractops.api.rh.domain.EsocialEventType.getPeriodic().map { it.name to it.description },
                "tabelas" to listOf("S1010", "S1070")
            )
        )
    }

    @PostMapping("/s2240/{employeeId}")
    fun gerarS2240(
        @PathVariable employeeId: UUID,
        @RequestParam tipo: String,
        @RequestParam grau: String,
        @RequestParam dataInicio: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val inicio = java.time.LocalDate.parse(dataInicio)
        return ResponseEntity.ok(esocialService.generateS2240CondicoesAmbientais(employeeId, effectiveTenant, tipo, grau, inicio))
    }
}