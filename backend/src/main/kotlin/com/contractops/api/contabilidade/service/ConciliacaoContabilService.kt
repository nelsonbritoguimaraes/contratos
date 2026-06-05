package com.contractops.api.contabilidade.service

import com.contractops.api.contabilidade.repository.LancamentoContabilRepository
import com.contractops.api.financeiro.repository.ContaAReceberRepository
import com.contractops.api.financeiro.repository.ContaAPagarRepository
import com.contractops.api.financeiro.repository.ContaBancariaRepository
import com.contractops.api.financeiro.repository.TransacaoFinanceiraRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class ConciliacaoContabilService(
    private val lancamentoRepository: LancamentoContabilRepository,
    private val contaAReceberRepository: ContaAReceberRepository,
    private val contaAPagarRepository: ContaAPagarRepository,
    private val contaBancariaRepository: ContaBancariaRepository,
    private val transacaoRepository: TransacaoFinanceiraRepository
) {
    fun conciliarSaldos(tenantId: UUID, dataCorte: LocalDate): Map<String, Any> {
        val inicio = LocalDate.of(dataCorte.year, 1, 1)
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, dataCorte)

        val saldoClientesContabil = saldoContaCodigo(lancamentos, "1.1.02", "DEVEDORA")
        val saldoCaixaContabil = saldoContaCodigo(lancamentos, "1.1.01", "DEVEDORA")
        val saldoFornecedoresContabil = saldoContaCodigo(lancamentos, "2.1.06", "CREDORA")
            .add(saldoContaCodigo(lancamentos, "2.1.01", "CREDORA"))

        val arAberto = contaAReceberRepository.findByTenantId(tenantId)
            .filter { it.status in listOf("ABERTO", "PARCIAL", "FATURADO") }
            .sumOf { (it.valorLiquido ?: BigDecimal.ZERO).subtract(it.valorRecebido ?: BigDecimal.ZERO) }

        val apAberto = contaAPagarRepository.findByTenantId(tenantId)
            .filter { it.status == "ABERTO" || it.status == "PARCIAL" }
            .sumOf { it.valor ?: BigDecimal.ZERO }

        val contasBancarias = contaBancariaRepository.findByTenantIdAndAtivaTrue(tenantId)
        val saldoBancarioOperacional = contasBancarias.sumOf { it.saldoAtual ?: BigDecimal.ZERO }

        val transacoes = transacaoRepository.findByTenantIdAndDataBetween(tenantId, inicio, dataCorte)
        val entradas = transacoes.filter { it.tipo == "ENTRADA" }.sumOf { it.valor }
        val saidas = transacoes.filter { it.tipo == "SAIDA" }.sumOf { it.valor }

        val diffAr = saldoClientesContabil.subtract(arAberto)
        val diffAp = saldoFornecedoresContabil.subtract(apAberto)
        val diffBanco = saldoCaixaContabil.subtract(saldoBancarioOperacional)

        return mapOf(
            "dataCorte" to dataCorte.toString(),
            "contabil" to mapOf(
                "clientes" to saldoClientesContabil,
                "caixa" to saldoCaixaContabil,
                "fornecedores" to saldoFornecedoresContabil
            ),
            "operacional" to mapOf(
                "contasAReceberAberto" to arAberto,
                "contasAPagarAberto" to apAberto,
                "saldoBancario" to saldoBancarioOperacional,
                "entradasPeriodo" to entradas,
                "saidasPeriodo" to saidas
            ),
            "divergencias" to mapOf(
                "arVsClientes" to diffAr,
                "apVsFornecedores" to diffAp,
                "caixaVsBanco" to diffBanco
            ),
            "conciliado" to (diffAr.abs() < BigDecimal("0.01") &&
                diffAp.abs() < BigDecimal("0.01") &&
                diffBanco.abs() < BigDecimal("1.00"))
        )
    }

    private fun saldoContaCodigo(
        lancamentos: List<com.contractops.api.contabilidade.domain.LancamentoContabil>,
        codigo: String,
        natureza: String
    ): BigDecimal {
        val debitos = lancamentos.filter { it.contaDebito.codigo == codigo }.sumOf { it.valor }
        val creditos = lancamentos.filter { it.contaCredito.codigo == codigo }.sumOf { it.valor }
        return if (natureza == "DEVEDORA") debitos.subtract(creditos) else creditos.subtract(debitos)
    }
}
