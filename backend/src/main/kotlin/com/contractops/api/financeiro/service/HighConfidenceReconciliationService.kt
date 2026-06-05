package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import com.contractops.api.financeiro.domain.NotaFiscalServico
import com.contractops.api.financeiro.domain.TransacaoFinanceira
import com.contractops.api.financeiro.repository.ExtratoBancarioItemRepository
import com.contractops.api.financeiro.repository.NotaFiscalServicoRepository
import com.contractops.api.financeiro.repository.TransacaoFinanceiraRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class ReconciliationMatch(
    val extratoId: UUID,
    val notaFiscalId: UUID?,
    val transacaoId: UUID?,
    val confidence: BigDecimal,
    val metodo: String,
    val motivo: String,
    val autoConciliar: Boolean
)

/**
 * Conciliação de alta confiança (≥85) entre NFS-e e movimentos bancários.
 */
@Service
class HighConfidenceReconciliationService(
    private val nfseExtratoMatchService: NfseExtratoMatchService,
    private val extratoRepository: ExtratoBancarioItemRepository,
    private val notaFiscalRepository: NotaFiscalServicoRepository,
    private val transacaoRepository: TransacaoFinanceiraRepository
) {
    companion object {
        val THRESHOLD_AUTO = BigDecimal("85.00")
        val THRESHOLD_SUGESTAO = BigDecimal("70.00")
    }

    fun reconciliarPeriodo(
        tenantId: UUID,
        contaBancariaId: UUID,
        periodoInicio: LocalDate,
        periodoFim: LocalDate
    ): List<ReconciliationMatch> {
        val extratos = extratoRepository.findByTenantIdAndContaBancariaIdAndConciliadoFalse(tenantId, contaBancariaId)
            .filter { it.data in periodoInicio..periodoFim && it.tipo == "CREDITO" }

        val nfsList = notaFiscalRepository.findByTenantIdAndDataEmissaoBetween(tenantId, periodoInicio, periodoFim)
            .filter { it.status == "EMITIDA" || it.status == "AUTORIZADA" }

        val transacoes = transacaoRepository.findByTenantIdAndContaBancariaIdAndDataBetween(
            tenantId, contaBancariaId, periodoInicio, periodoFim
        ).filter { it.tipo == "ENTRADA" && !it.conciliado }

        val matches = mutableListOf<ReconciliationMatch>()

        extratos.forEach { extrato ->
            val nfMatch = nfseExtratoMatchService.matchExtratoToNfse(extrato, nfsList)
            if (nfMatch != null && nfMatch.confidence >= THRESHOLD_SUGESTAO) {
                matches.add(
                    ReconciliationMatch(
                        extratoId = extrato.id!!,
                        notaFiscalId = nfMatch.notaFiscalId,
                        transacaoId = null,
                        confidence = nfMatch.confidence,
                        metodo = nfMatch.metodo,
                        motivo = nfMatch.motivo,
                        autoConciliar = nfMatch.confidence >= THRESHOLD_AUTO
                    )
                )
                return@forEach
            }

            var bestTx: MatchCandidate? = null
            transacoes.forEach { tx ->
                val c = nfseExtratoMatchService.matchExtratoToTransacao(extrato, tx)
                if (bestTx == null || c.confidence > bestTx!!.confidence) bestTx = c
            }
            bestTx?.takeIf { it.confidence >= THRESHOLD_SUGESTAO }?.let { c ->
                matches.add(
                    ReconciliationMatch(
                        extratoId = extrato.id!!,
                        notaFiscalId = null,
                        transacaoId = c.transacaoId,
                        confidence = c.confidence,
                        metodo = c.metodo,
                        motivo = c.motivo,
                        autoConciliar = c.confidence >= THRESHOLD_AUTO
                    )
                )
            }
        }

        return matches.sortedByDescending { it.confidence }
    }

    fun aplicarMatchesAltaConfianca(
        tenantId: UUID,
        matches: List<ReconciliationMatch>,
        conciliarFn: (extratoId: UUID, notaFiscalId: UUID?, transacaoId: UUID?) -> Unit
    ): Int {
        var count = 0
        matches.filter { it.autoConciliar }.forEach {
            conciliarFn(it.extratoId, it.notaFiscalId, it.transacaoId)
            count++
        }
        return count
    }
}
