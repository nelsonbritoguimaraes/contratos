package com.contractops.api.glosa.service

import com.contractops.api.glosa.domain.GlosaRule
import com.contractops.api.glosa.repository.GlosaRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class GlosaRuleService(
    private val glosaRuleRepository: GlosaRuleRepository
) {

    fun findByContract(contractId: UUID, tenantId: UUID): List<GlosaRule> =
        glosaRuleRepository.findByContractIdAndIsActiveTrue(contractId)
            .filter { it.tenantId == tenantId }

    fun findById(id: UUID, tenantId: UUID): GlosaRule? =
        glosaRuleRepository.findById(id).orElse(null)?.takeIf { it.tenantId == tenantId }

    @Transactional
    fun createRule(
        tenantId: UUID,
        contractId: UUID,
        ruleType: String,
        description: String? = null,
        factor: BigDecimal = BigDecimal.ONE,
        toleranceMinutes: Int? = null,
        priority: Int = 10
    ): GlosaRule = glosaRuleRepository.save(
        GlosaRule(
            tenantId = tenantId,
            contractId = contractId,
            ruleType = ruleType.uppercase(),
            description = description,
            factor = factor,
            toleranceMinutes = toleranceMinutes,
            priority = priority
        )
    )

    @Transactional
    fun updateRule(
        id: UUID,
        tenantId: UUID,
        ruleType: String? = null,
        description: String? = null,
        factor: BigDecimal? = null,
        toleranceMinutes: Int? = null,
        priority: Int? = null,
        isActive: Boolean? = null
    ): GlosaRule {
        val rule = findById(id, tenantId) ?: throw IllegalArgumentException("Regra não encontrada")
        ruleType?.let { rule.ruleType = it.uppercase() }
        description?.let { rule.description = it }
        factor?.let { rule.factor = it }
        toleranceMinutes?.let { rule.toleranceMinutes = it }
        priority?.let { rule.priority = it }
        isActive?.let { rule.isActive = it }
        return glosaRuleRepository.save(rule)
    }

    @Transactional
    fun deactivateRule(id: UUID, tenantId: UUID): GlosaRule {
        val rule = findById(id, tenantId) ?: throw IllegalArgumentException("Regra não encontrada")
        rule.isActive = false
        return glosaRuleRepository.save(rule)
    }

    companion object {
        val SPEC_RULE_TYPES = listOf(
            "FALTA", "ATRASO", "SAIDA_ANTECIPADA", "POSTO_DESCOBERTO", "COBERTURA_PARCIAL",
            "NAO_SUBSTITUICAO", "IMR", "AUSENCIA_DOCUMENTO", "UNIFORME", "EQUIPAMENTO",
            "QUALIDADE", "DESCUMPRIMENTO_SLA", "NOTIFICACAO_NAO_RESPONDIDA", "ADMINISTRATIVA", "FINANCEIRA"
        )
    }
}
