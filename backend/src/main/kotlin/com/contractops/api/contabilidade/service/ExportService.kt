package com.contractops.api.contabilidade.service

import com.contractops.api.contabilidade.repository.LancamentoContabilRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class ExportService(
    private val lancamentoRepository: LancamentoContabilRepository,
    private val contabilidadeService: ContabilidadeService
) {

    fun exportarBalanceteCsv(inicio: LocalDate, fim: LocalDate, tenantId: UUID): String {
        val balancete = contabilidadeService.gerarBalancete(inicio, fim, tenantId)

        val header = "Codigo,Descricao,Natureza,Total Debito,Total Credito,Saldo\n"

        val linhas = balancete.joinToString("\n") { linha ->
            "${linha["codigo"]},${linha["descricao"]},${linha["natureza"]},${linha["totalDebito"]},${linha["totalCredito"]},${linha["saldo"]}"
        }

        return header + linhas
    }

    fun exportarDreCsv(contratoId: UUID, inicio: LocalDate, fim: LocalDate, tenantId: UUID): String {
        val dre = contabilidadeService.gerarDreContrato(contratoId, inicio, fim, tenantId)

        return """
            DRE - Contrato: $contratoId
            Período: $inicio a $fim
            
            Receitas,${dre["receitas"]}
            Despesas,${dre["despesas"]}
            Lucro/Prejuízo,${dre["lucroPrejuizo"]}
        """.trimIndent()
    }

    fun exportarLancamentosCsv(inicio: LocalDate, fim: LocalDate, tenantId: UUID): String {
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)

        val header = "Data,Conta Debito,Conta Credito,Valor,Historico,Origem\n"

        val linhas = lancamentos.joinToString("\n") { l ->
            "${l.data},${l.contaDebito.codigo},${l.contaCredito.codigo},${l.valor},\"${l.historico ?: ""}\",${l.origemTipo ?: ""}"
        }

        return header + linhas
    }
}