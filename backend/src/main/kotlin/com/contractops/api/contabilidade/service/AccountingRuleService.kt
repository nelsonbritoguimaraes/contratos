package com.contractops.api.contabilidade.service

import com.contractops.api.contabilidade.domain.AccountingRule
import com.contractops.api.contabilidade.repository.AccountingRuleRepository
import com.contractops.api.contabilidade.repository.ContaContabilRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AccountingRuleService(
    private val ruleRepository: AccountingRuleRepository,
    private val contaRepository: ContaContabilRepository
) {

    fun listar(tenantId: UUID): List<AccountingRule> =
        ruleRepository.findByTenantIdOrderByOrigemTipoAscCodigoAsc(tenantId)

    fun listarPorOrigem(tenantId: UUID, origemTipo: String): List<AccountingRule> =
        ruleRepository.findByTenantIdAndOrigemTipoAndAtivaTrue(tenantId, origemTipo)

    fun buscarPorRubrica(tenantId: UUID, rubricCode: String): AccountingRule? =
        ruleRepository.findByTenantIdAndRubricCodeAndAtivaTrue(tenantId, rubricCode)

    @Transactional
    fun criar(tenantId: UUID, request: AccountingRuleRequest): AccountingRule {
        validarContas(tenantId, request.contaDebitoCodigo, request.contaCreditoCodigo)
        if (request.rubricCode != null &&
            ruleRepository.findByTenantIdAndRubricCodeAndAtivaTrue(tenantId, request.rubricCode) != null
        ) {
            throw IllegalArgumentException("Já existe regra para rubrica ${request.rubricCode}")
        }
        return ruleRepository.save(
            AccountingRule(
                tenantId = tenantId,
                codigo = request.codigo,
                descricao = request.descricao,
                origemTipo = request.origemTipo,
                contaDebitoCodigo = request.contaDebitoCodigo,
                contaCreditoCodigo = request.contaCreditoCodigo,
                historicoPadrao = request.historicoPadrao,
                rubricCode = request.rubricCode,
                rubricType = request.rubricType,
                ativa = request.ativa
            )
        )
    }

    @Transactional
    fun atualizar(id: UUID, tenantId: UUID, request: AccountingRuleRequest): AccountingRule {
        val rule = ruleRepository.findById(id).orElseThrow { IllegalArgumentException("Regra não encontrada") }
        if (rule.tenantId != tenantId) throw IllegalArgumentException("Regra não pertence ao tenant")
        validarContas(tenantId, request.contaDebitoCodigo, request.contaCreditoCodigo)

        rule.codigo = request.codigo
        rule.descricao = request.descricao
        rule.origemTipo = request.origemTipo
        rule.contaDebitoCodigo = request.contaDebitoCodigo
        rule.contaCreditoCodigo = request.contaCreditoCodigo
        rule.historicoPadrao = request.historicoPadrao
        rule.rubricCode = request.rubricCode
        rule.rubricType = request.rubricType
        rule.ativa = request.ativa
        return ruleRepository.save(rule)
    }

    @Transactional
    fun excluir(id: UUID, tenantId: UUID) {
        val rule = ruleRepository.findById(id).orElseThrow { IllegalArgumentException("Regra não encontrada") }
        if (rule.tenantId != tenantId) throw IllegalArgumentException("Regra não pertence ao tenant")
        rule.ativa = false
        ruleRepository.save(rule)
    }

    private fun validarContas(tenantId: UUID, debito: String, credito: String) {
        contaRepository.findByTenantIdAndCodigo(tenantId, debito)
            ?: throw IllegalArgumentException("Conta débito $debito não existe no plano")
        contaRepository.findByTenantIdAndCodigo(tenantId, credito)
            ?: throw IllegalArgumentException("Conta crédito $credito não existe no plano")
    }
}

data class AccountingRuleRequest(
    val codigo: String,
    val descricao: String? = null,
    val origemTipo: String,
    val contaDebitoCodigo: String,
    val contaCreditoCodigo: String,
    val historicoPadrao: String? = null,
    val rubricCode: String? = null,
    val rubricType: String? = null,
    val ativa: Boolean = true
)
