package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.ContaAPagar
import com.contractops.api.financeiro.domain.ContaAReceber
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Serviço de Aging Report (Contas a Receber e Contas a Pagar) - Fase 4 Polish.
 * Muito útil para visão de CFO.
 */
@Service
class AgingReportService {

    data class AgingBucket(
        val faixa: String,
        val quantidade: Int,
        val valor: BigDecimal
    )

    data class AgingReport(
        val tipo: String,
        val dataCorte: LocalDate,
        val buckets: List<AgingBucket>,
        val total: BigDecimal
    )

    fun gerarAgingContasAReceber(contas: List<ContaAReceber>, dataCorte: LocalDate = LocalDate.now()): AgingReport {
        val buckets = mutableListOf<AgingBucket>()

        val faixas = listOf(
            "0-30" to { dias: Int -> dias in 0..30 },
            "31-60" to { dias: Int -> dias in 31..60 },
            "61-90" to { dias: Int -> dias in 61..90 },
            ">90" to { dias: Int -> dias > 90 }
        )

        faixas.forEach { (nome, condicao) ->
            val filtradas = contas.filter { conta ->
                val dias = if (conta.vencimento.isBefore(dataCorte)) {
                    java.time.temporal.ChronoUnit.DAYS.between(conta.vencimento, dataCorte).toInt()
                } else 0
                condicao(dias)
            }
            val valor = filtradas.sumOf { it.valorLiquido }
            buckets.add(AgingBucket(nome, filtradas.size, valor))
        }

        val total = buckets.sumOf { it.valor }
        return AgingReport("CONTAS_A_RECEBER", dataCorte, buckets, total)
    }

    fun gerarAgingContasAPagar(contas: List<ContaAPagar>, dataCorte: LocalDate = LocalDate.now()): AgingReport {
        val buckets = mutableListOf<AgingBucket>()

        val faixas = listOf(
            "0-30" to { dias: Int -> dias in 0..30 },
            "31-60" to { dias: Int -> dias in 31..60 },
            "61-90" to { dias: Int -> dias in 61..90 },
            ">90" to { dias: Int -> dias > 90 }
        )

        faixas.forEach { (nome, condicao) ->
            val filtradas = contas.filter { conta ->
                val dias = if (conta.vencimento.isBefore(dataCorte)) {
                    java.time.temporal.ChronoUnit.DAYS.between(conta.vencimento, dataCorte).toInt()
                } else 0
                condicao(dias)
            }
            val valor = filtradas.sumOf { it.valor }
            buckets.add(AgingBucket(nome, filtradas.size, valor))
        }

        val total = buckets.sumOf { it.valor }
        return AgingReport("CONTAS_A_PAGAR", dataCorte, buckets, total)
    }
}