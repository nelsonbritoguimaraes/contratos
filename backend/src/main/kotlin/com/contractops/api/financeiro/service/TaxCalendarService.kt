package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.RetencaoTributaria
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Gerador de Calendário de Obrigações Tributárias - Fase 4 Polish.
 */
@Service
class TaxCalendarService {

    data class Obrigacao(
        val tipo: String,
        val descricao: String,
        val vencimento: LocalDate,
        val valorEstimado: java.math.BigDecimal,
        val status: String
    )

    fun gerarCalendario(
        retencoes: List<RetencaoTributaria>,
        mes: Int,
        ano: Int
    ): List<Obrigacao> {
        val obrigacoes = mutableListOf<Obrigacao>()

        retencoes.filter { it.dataVencimento?.monthValue == mes && it.dataVencimento?.year == ano }
            .forEach { ret ->
                obrigacoes.add(
                    Obrigacao(
                        tipo = ret.tipo,
                        descricao = "Retenção ${ret.tipo} - NFS-e ${ret.notaFiscalId}",
                        vencimento = ret.dataVencimento ?: LocalDate.now(),
                        valorEstimado = ret.valorRetido,
                        status = ret.status
                    )
                )
            }

        val dia7 = LocalDate.of(ano, mes, minOf(7, LocalDate.of(ano, mes, 1).lengthOfMonth()))
        val dia20 = LocalDate.of(ano, mes, minOf(20, LocalDate.of(ano, mes, 1).lengthOfMonth()))
        obrigacoes.add(
            Obrigacao(
                tipo = "FGTS",
                descricao = "FGTS Digital — competência ${mes.toString().padStart(2, '0')}/$ano",
                vencimento = dia7,
                valorEstimado = java.math.BigDecimal.ZERO,
                status = "PENDENTE"
            )
        )
        obrigacoes.add(
            Obrigacao(
                tipo = "DCTFWeb",
                descricao = "DCTFWeb — retenções federais (IRRF/CSRF)",
                vencimento = dia20,
                valorEstimado = java.math.BigDecimal.ZERO,
                status = "PENDENTE"
            )
        )
        obrigacoes.add(
            Obrigacao(
                tipo = "INSS",
                descricao = "GPS/INSS — retenção 11% (quando aplicável)",
                vencimento = dia20,
                valorEstimado = java.math.BigDecimal.ZERO,
                status = "PENDENTE"
            )
        )

        return obrigacoes.sortedBy { it.vencimento }
    }
}