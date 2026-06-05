package com.contractops.api.glosa.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.glosa.domain.GlosaRule
import com.contractops.api.glosa.repository.GlosaRuleRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/glosas/rules")
class GlosaRuleController(
    private val repository: GlosaRuleRepository
) {
    @GetMapping
    fun list(
        @RequestParam contractId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<GlosaRule>> {
        return ResponseEntity.ok(repository.findByContractIdAndIsActiveTrue(contractId))
    }

    @PostMapping
    fun create(
        @RequestBody req: CreateGlosaRuleRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<GlosaRule> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val rule = GlosaRule(
            tenantId = t,
            contractId = req.contractId,
            ruleType = req.ruleType,
            description = req.description,
            factor = req.factor ?: java.math.BigDecimal.ONE,
            toleranceMinutes = req.toleranceMinutes,
            isActive = req.isActive ?: true,
            priority = req.priority ?: 10
        )
        return ResponseEntity.ok(repository.save(rule))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody req: UpdateGlosaRuleRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<GlosaRule> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val rule = repository.findById(id).orElseThrow()
        require(rule.tenantId == t)
        req.ruleType?.let { rule.ruleType = it }
        req.description?.let { rule.description = it }
        req.factor?.let { rule.factor = it }
        req.toleranceMinutes?.let { rule.toleranceMinutes = it }
        req.isActive?.let { rule.isActive = it }
        req.priority?.let { rule.priority = it }
        return ResponseEntity.ok(repository.save(rule))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID, @RequestParam(required = false) tenantId: UUID?): ResponseEntity<Void> {
        val t = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val rule = repository.findById(id).orElseThrow()
        require(rule.tenantId == t)
        rule.isActive = false
        repository.save(rule)
        return ResponseEntity.noContent().build()
    }
}

data class CreateGlosaRuleRequest(
    val contractId: UUID,
    val ruleType: String,
    val description: String? = null,
    val factor: java.math.BigDecimal? = null,
    val toleranceMinutes: Int? = null,
    val isActive: Boolean? = null,
    val priority: Int? = null
)

data class UpdateGlosaRuleRequest(
    val ruleType: String? = null,
    val description: String? = null,
    val factor: java.math.BigDecimal? = null,
    val toleranceMinutes: Int? = null,
    val isActive: Boolean? = null,
    val priority: Int? = null
)
