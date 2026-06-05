package com.contractops.api.contabilidade.service

import com.contractops.api.contabilidade.repository.ContaContabilRepository
import com.contractops.api.contabilidade.repository.LancamentoContabilRepository
import com.contractops.api.financeiro.repository.TenantFiscalProfileRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Gera arquivos SPED ECD, ECF e EFD-Reinf para download/preview.
 * Layout alinhado à estrutura de registros oficiais (Leiaute 9.x).
 */
@Service
class SpedService(
    private val lancamentoRepository: LancamentoContabilRepository,
    private val contaRepository: ContaContabilRepository,
    private val tenantFiscalProfileRepository: TenantFiscalProfileRepository? = null
) {

    fun gerarSpedContabilECD(tenantId: UUID, inicio: LocalDate, fim: LocalDate): String {
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
            .sortedWith(compareBy({ it.data }, { it.id }))
        val contas = contaRepository.findByTenantIdAndAtivaTrue(tenantId).sortedBy { it.codigo }
        val fiscal = tenantFiscalProfileRepository?.findById(tenantId)?.orElse(null)

        val sb = StringBuilder()
        val dt = DateTimeFormatter.ofPattern("ddMMyyyy")
        val cnpj = fiscal?.cnpjPrestador?.filter { it.isDigit() }?.takeIf { it.length == 14 } ?: "11222333000181"
        val razao = fiscal?.razaoSocial ?: "CONTRATOS LTDA"

        sb.appendLine("|0000|LECD|010|${inicio.year}|${fim.year}|${inicio.format(dt)}|${fim.format(dt)}|$razao|$cnpj|SP|3550308||0|N|N|0|")
        sb.appendLine("|0001|0|")
        sb.appendLine("|0007|1|")

        sb.appendLine("|I010|G|")
        contas.forEach { conta ->
            val ref = conta.codigoReferencial ?: conta.codigo.replace(".", "")
            sb.appendLine("|I050|${inicio.format(dt)}|01|${conta.codigo}|${conta.descricao.take(100)}|${conta.natureza.first()}|${ref}|")
        }

        val costCenters = lancamentos.mapNotNull { it.costCenterId }.distinct()
        costCenters.forEachIndexed { idx, ccId ->
            sb.appendLine("|I100|${(idx + 1).toString().padStart(3, '0')}|CC-$ccId|Centro de custo $ccId|")
        }
        if (costCenters.isEmpty()) {
            sb.appendLine("|I100|001|0001|Centro de custo padrão|")
        }

        sb.appendLine("|I150|${inicio.format(dt)}|")
        sb.appendLine("|I155|${inicio.format(dt)}|0,00|D|")

        var seq = 1
        lancamentos.forEach { lanc ->
            val numDoc = seq.toString().padStart(6, '0')
            sb.appendLine("|I200|${numDoc}|${lanc.data.format(dt)}|${lanc.valor}|N|${sanitize(lanc.historico)}|")
            sb.appendLine("|I250|${lanc.contaDebito.codigo}|${lanc.contaDebito.codigoReferencial ?: lanc.contaDebito.codigo}|D|${formatValor(lanc.valor)}|${sanitize(lanc.historico)}|")
            sb.appendLine("|I250|${lanc.contaCredito.codigo}|${lanc.contaCredito.codigoReferencial ?: lanc.contaCredito.codigo}|C|${formatValor(lanc.valor)}|${sanitize(lanc.historico)}|")
            seq++
        }

        fiscal?.let { f ->
            if (!f.contadorNome.isNullOrBlank()) {
                sb.appendLine("|J930|${f.contadorNome}|${f.contadorCpf ?: ""}|${f.contadorCrc ?: ""}|CONTADOR|")
            }
            if (!f.representanteNome.isNullOrBlank()) {
                sb.appendLine("|J930|${f.representanteNome}|${f.representanteCpf ?: ""}||REPRESENTANTE_LEGAL|")
            }
        } ?: run {
            sb.appendLine("|J930|Contador Responsável|00000000000|SP000000|CONTADOR|")
            sb.appendLine("|J930|Representante Legal|00000000000||REPRESENTANTE_LEGAL|")
        }

        val totalRegistros = 8 + contas.size + costCenters.size.coerceAtLeast(1) + lancamentos.size * 3
        sb.appendLine("|9990|${totalRegistros + 1}|")
        sb.appendLine("|9999|${totalRegistros + 2}|")

        return sb.toString()
    }

    /**
     * ECF — Escrituração Contábil Fiscal (IRPJ/CSLL). Deve cruzar com ECD.
     */
    fun gerarECF(tenantId: UUID, anoCalendario: Int): String {
        val inicio = LocalDate.of(anoCalendario, 1, 1)
        val fim = LocalDate.of(anoCalendario, 12, 31)
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)

        val receitas = lancamentos.filter { it.contaCredito.tipo == "RECEITA" }.sumOf { it.valor }
        val despesas = lancamentos.filter { it.contaDebito.tipo == "DESPESA" }.sumOf { it.valor }
        val lucro = receitas.subtract(despesas)

        val sb = StringBuilder()
        sb.appendLine("|0000|LECF|0010|$anoCalendario|11222333000181|CONTRATOS LTDA|0|0|0|0|")
        sb.appendLine("|0001|0|")
        sb.appendLine("|0010|1|")
        sb.appendLine("|L300|${formatValor(receitas)}|${formatValor(despesas)}|${formatValor(lucro)}|")
        sb.appendLine("|P200|${formatValor(lucro.multiply(BigDecimal("0.15")))}|IRPJ estimado 15%|")
        sb.appendLine("|P300|${formatValor(lucro.multiply(BigDecimal("0.09")))}|CSLL estimado 9%|")
        sb.appendLine("|9999|7|")
        return sb.toString()
    }

    fun gerarEfdReinf(tenantId: UUID, competencia: LocalDate): String {
        val ym = competencia.withDayOfMonth(1)
        val fim = ym.withDayOfMonth(ym.lengthOfMonth())
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, ym, fim)
            .filter { it.origemTipo == "PAYSLIP" || it.contaCredito.codigo.startsWith("2.1") }

        val sb = StringBuilder()
        sb.appendLine("R-1000|1|${competencia.year}|${competencia.monthValue}|11222333000181|")
        lancamentos.filter { it.contaCredito.codigo == "2.1.03" }.forEach {
            sb.appendLine("R-2010|${it.origemId}|${formatValor(it.valor)}|INSS|")
        }
        lancamentos.filter { it.contaCredito.codigo == "2.1.04" }.forEach {
            sb.appendLine("R-4020|${it.origemId}|${formatValor(it.valor)}|IRRF|")
        }
        sb.appendLine("R-9000|1|")
        return sb.toString()
    }

    private fun sanitize(text: String?): String =
        (text ?: "Lancamento").replace("|", "/").take(100)

    private fun formatValor(v: BigDecimal): String =
        v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace(".", ",")
}
