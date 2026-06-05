package com.contractops.api.contabilidade.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.contabilidade.service.AccountingRuleRequest
import com.contractops.api.contabilidade.service.AccountingRuleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/contabilidade/regras")
class AccountingRuleController(
    private val service: AccountingRuleService
) {

    @GetMapping
    fun listar(
        @RequestParam(required = false) origemTipo: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<Map<String, Any?>>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val rules = if (origemTipo != null) service.listarPorOrigem(effectiveTenant, origemTipo)
        else service.listar(effectiveTenant)
        return ResponseEntity.ok(rules.map { it.toDto() })
    }

    @PostMapping
    fun criar(
        @RequestBody request: AccountingRuleRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any?>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.criar(effectiveTenant, request).toDto())
    }

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
        @RequestBody request: AccountingRuleRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any?>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.atualizar(id, effectiveTenant, request).toDto())
    }

    @DeleteMapping("/{id}")
    fun excluir(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Void> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        service.excluir(id, effectiveTenant)
        return ResponseEntity.noContent().build()
    }

    private fun com.contractops.api.contabilidade.domain.AccountingRule.toDto() = mapOf(
        "id" to id,
        "codigo" to codigo,
        "descricao" to descricao,
        "origemTipo" to origemTipo,
        "contaDebitoCodigo" to contaDebitoCodigo,
        "contaCreditoCodigo" to contaCreditoCodigo,
        "historicoPadrao" to historicoPadrao,
        "rubricCode" to rubricCode,
        "rubricType" to rubricType,
        "ativa" to ativa
    )
}
