package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.TransacaoFinanceira
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Gerador simples de relatórios financeiros (Fase 4 Polish).
 */
@Service
class FinancialReportService {

    fun gerarPosicaoCaixaPorPeriodo(
        transacoes: List<TransacaoFinanceira>,
        dataInicio: LocalDate,
        dataFim: LocalDate
    ): String {
        val entradas = transacoes.filter { it.tipo == "ENTRADA" && it.data in dataInicio..dataFim }.sumOf { it.valor }
        val saidas = transacoes.filter { it.tipo == "SAIDA" && it.data in dataInicio..dataFim }.sumOf { it.valor }
        val saldo = entradas.subtract(saidas)

        return """
            =============================================
                  POSIÇÃO DE CAIXA (SIMULADO)
            =============================================
            Período: $dataInicio a $dataFim
            
            Entradas: R$ $entradas
            Saídas:   R$ $saidas
            Saldo:    R$ $saldo
            
            [Relatório gerado pelo módulo Financeiro - Fase 4 Polish]
            =============================================
        """.trimIndent()
    }
}