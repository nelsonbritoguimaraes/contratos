package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import com.contractops.api.financeiro.domain.NotaFiscalServico
import com.contractops.api.financeiro.domain.TransacaoFinanceira
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit
import java.util.UUID

data class MatchCandidate(
    val extratoId: UUID?,
    val transacaoId: UUID? = null,
    val notaFiscalId: UUID? = null,
    val confidence: BigDecimal,
    val metodo: String,
    val motivo: String
)

/**
 * Motor de match NFS-e ↔ extrato/transação com score de confiança (0–100).
 * SPEC §22.3 — conciliação inteligente + Open Finance.
 */
@Service
class NfseExtratoMatchService {

    fun matchExtratoToNfse(
        extrato: ExtratoBancarioItem,
        nfsList: List<NotaFiscalServico>
    ): MatchCandidate? {
        if (extrato.tipo != "CREDITO") return null
        var best: MatchCandidate? = null
        for (nf in nfsList) {
            val candidate = scoreNfse(extrato, nf)
            if (best == null || candidate.confidence > best.confidence) {
                best = candidate
            }
        }
        return best?.takeIf { it.confidence >= BigDecimal("55.00") }
    }

    fun matchExtratoToTransacao(
        extrato: ExtratoBancarioItem,
        transacao: TransacaoFinanceira
    ): MatchCandidate {
        var score = BigDecimal.ZERO
        val reasons = mutableListOf<String>()

        if (extrato.valor.compareTo(transacao.valor) == 0) {
            score = score.add(BigDecimal("40"))
            reasons += "valor exato"
        } else {
            val diff = extrato.valor.subtract(transacao.valor).abs()
            val pct = if (extrato.valor > BigDecimal.ZERO) {
                diff.divide(extrato.valor, 4, RoundingMode.HALF_UP)
            } else BigDecimal.ONE
            if (pct <= BigDecimal("0.01")) {
                score = score.add(BigDecimal("30"))
                reasons += "valor ±1%"
            }
        }

        val dias = kotlin.math.abs(ChronoUnit.DAYS.between(extrato.data, transacao.data))
        when {
            dias == 0L -> { score = score.add(BigDecimal("25")); reasons += "mesma data" }
            dias <= 2L -> { score = score.add(BigDecimal("15")); reasons += "data ±2d" }
            dias <= 5L -> { score = score.add(BigDecimal("5")); reasons += "data ±5d" }
        }

        if (extrato.documento != null && transacao.historico.contains(extrato.documento!!, ignoreCase = true)) {
            score = score.add(BigDecimal("25"))
            reasons += "documento no histórico"
        }

        val nfNum = Regex("""\d{4,}""").find(transacao.historico)?.value
        if (nfNum != null && extrato.historico.contains(nfNum)) {
            score = score.add(BigDecimal("10"))
            reasons += "número NFS no histórico"
        }

        val metodo = when {
            score >= BigDecimal("85") -> "ALTA_PRECISAO"
            score >= BigDecimal("70") -> "VALOR_DATA"
            score >= BigDecimal("55") -> "HEURISTICA"
            else -> "BAIXA"
        }

        return MatchCandidate(
            extratoId = extrato.id,
            transacaoId = transacao.id,
            confidence = score.min(BigDecimal("100")),
            metodo = metodo,
            motivo = reasons.joinToString(", ")
        )
    }

    private fun scoreNfse(extrato: ExtratoBancarioItem, nf: NotaFiscalServico): MatchCandidate {
        var score = BigDecimal.ZERO
        val reasons = mutableListOf<String>()

        val valorAlvo = nf.valorLiquido
        if (extrato.valor.compareTo(valorAlvo) == 0) {
            score = score.add(BigDecimal("45"))
            reasons += "valor líquido NFS-e"
        } else if (extrato.valor.compareTo(nf.valorServicos) == 0) {
            score = score.add(BigDecimal("35"))
            reasons += "valor bruto NFS-e"
        }

        if (extrato.historico.contains(nf.numero, ignoreCase = true)) {
            score = score.add(BigDecimal("35"))
            reasons += "número NFS-e no extrato"
        }

        nf.codigoVerificacao?.let { cod ->
            if (extrato.historico.contains(cod.take(8), ignoreCase = true)) {
                score = score.add(BigDecimal("15"))
                reasons += "código verificação"
            }
        }

        val dias = kotlin.math.abs(ChronoUnit.DAYS.between(extrato.data, nf.dataEmissao))
        if (dias <= 30) {
            score = score.add(BigDecimal("10").subtract(BigDecimal(dias.coerceAtMost(10))))
            reasons += "proximidade emissão"
        }

        nf.tomadorCnpj.replace(Regex("\\D"), "").takeLast(4).let { suffix ->
            if (suffix.length == 4 && extrato.historico.contains(suffix)) {
                score = score.add(BigDecimal("5"))
                reasons += "CNPJ tomador"
            }
        }

        return MatchCandidate(
            extratoId = extrato.id,
            notaFiscalId = nf.id,
            confidence = score.min(BigDecimal("100")),
            metodo = "NFS_E",
            motivo = reasons.joinToString(", ").ifBlank { "heurística NFS-e" }
        )
    }
}
