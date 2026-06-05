package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.repository.ContaAPagarRepository
import com.contractops.api.financeiro.repository.PagamentoRepository
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class CnabRetornoImportService(
    private val contaAPagarRepository: ContaAPagarRepository,
    private val pagamentoRepository: PagamentoRepository,
    @Lazy private val financeiroService: FinanceiroService,
    private val auditService: FinancialAuditService
) {
    private val fmt = DateTimeFormatter.ofPattern("ddMMyyyy")

    @Transactional
    fun importarRetornoCnab240(conteudo: String, tenantId: UUID, contaBancariaId: UUID): Map<String, Any> {
        val linhas = conteudo.lines().filter { it.isNotBlank() }
        var processados = 0
        var erros = 0
        val detalhes = mutableListOf<String>()

        linhas.filter { it.startsWith("2") && it.length > 40 }.forEach { linha ->
            try {
                val nossoNumero = linha.substring(20, 40).trim()
                val statusCodigo = linha.getOrNull(15)?.toString() ?: "0"
                if (statusCodigo !in listOf("0", "6", "B", "A")) return@forEach

                val valorStr = if (linha.length >= 55) {
                    linha.substring(40, 55).filter { it.isDigit() }
                } else {
                    linha.takeLast(15).filter { it.isDigit() }
                }
                val valor = if (valorStr.length >= 2) {
                    BigDecimal(valorStr).divide(BigDecimal(100))
                } else BigDecimal.ZERO

                val apId = parseApIdFromNossoNumero(nossoNumero, tenantId)
                if (apId == null) {
                    detalhes.add("Linha ignorada: nosso número '$nossoNumero' sem AP correspondente")
                    return@forEach
                }

                if (valor > BigDecimal.ZERO) {
                    financeiroService.pagarContaAPagar(apId, LocalDate.now(), valor, contaBancariaId, "CNAB_RETORNO", tenantId)
                    processados++
                    detalhes.add("AP $apId baixada via retorno (R$ $valor)")
                }
            } catch (e: Exception) {
                erros++
                detalhes.add("Erro linha: ${e.message}")
            }
        }

        auditService.registrar(tenantId, "CNAB_RETORNO", null, "IMPORTAR", "sistema", "$processados baixas, $erros erros")
        return mapOf("processados" to processados, "erros" to erros, "detalhes" to detalhes)
    }

    private fun parseApIdFromNossoNumero(nossoNumero: String, tenantId: UUID): UUID? {
        val compact = nossoNumero.filter { it.isLetterOrDigit() }
        runCatching {
            if (compact.length >= 32) {
                val uuidStr = "${compact.take(8)}-${compact.substring(8, 12)}-${compact.substring(12, 16)}-${compact.substring(16, 20)}-${compact.substring(20, 32)}"
                return UUID.fromString(uuidStr)
            }
        }
        contaAPagarRepository.findByTenantId(tenantId).firstOrNull {
            it.id.toString().replace("-", "").startsWith(compact.take(12))
        }?.id?.let { return it }
        return null
    }
}
