package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Serviço para geração de extrato no formato OFX (Open Financial Exchange).
 * Útil para exportar conciliações bancárias (Fase 4 Polish).
 */
@Service
class OfxExportService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun gerarOfxExtrato(
        contaBancariaId: String,
        itens: List<ExtratoBancarioItem>,
        dataInicio: LocalDate,
        dataFim: LocalDate,
        saldoFinal: java.math.BigDecimal
    ): String {
        val sb = StringBuilder()

        sb.appendLine("OFXHEADER:100")
        sb.appendLine("DATA:OFXSGML")
        sb.appendLine("VERSION:102")
        sb.appendLine("SECURITY:NONE")
        sb.appendLine("ENCODING:USASCII")
        sb.appendLine("CHARSET:1252")
        sb.appendLine("COMPRESSION:NONE")
        sb.appendLine("OLDFILEUID:NONE")
        sb.appendLine("NEWFILEUID:NONE")
        sb.appendLine()

        sb.appendLine("<OFX>")
        sb.appendLine("  <BANKMSGSRSV1>")
        sb.appendLine("    <STMTTRNRS>")
        sb.appendLine("      <TRNUID>1")
        sb.appendLine("      <STATUS>")
        sb.appendLine("        <CODE>0")
        sb.appendLine("        <SEVERITY>INFO")
        sb.appendLine("      </STATUS>")
        sb.appendLine("      <STMTRS>")
        sb.appendLine("        <CURDEF>BRL")
        sb.appendLine("        <BANKACCTFROM>")
        sb.appendLine("          <BANKID>000000001")
        sb.appendLine("          <ACCTID>$contaBancariaId")
        sb.appendLine("          <ACCTTYPE>CHECKING")
        sb.appendLine("        </BANKACCTFROM>")
        sb.appendLine("        <BANKTRANLIST>")
        sb.appendLine("          <DTSTART>${dataInicio.format(dateFormatter)}")
        sb.appendLine("          <DTEND>${dataFim.format(dateFormatter)}")

        itens.forEach { item ->
            sb.appendLine("          <STMTTRN>")
            sb.appendLine("            <TRNTYPE>${if (item.tipo == "CREDITO") "CREDIT" else "DEBIT"}")
            sb.appendLine("            <DTPOSTED>${item.data.format(dateFormatter)}")
            sb.appendLine("            <TRNAMT>${item.valor}")
            sb.appendLine("            <FITID>${item.id}")
            sb.appendLine("            <NAME>${item.historico.take(32)}")
            sb.appendLine("          </STMTTRN>")
        }

        sb.appendLine("        </BANKTRANLIST>")
        sb.appendLine("        <LEDGERBAL>")
        sb.appendLine("          <BALAMT>$saldoFinal")
        sb.appendLine("          <DTASOF>${dataFim.format(dateFormatter)}")
        sb.appendLine("        </LEDGERBAL>")
        sb.appendLine("      </STMTRS>")
        sb.appendLine("    </STMTTRNRS>")
        sb.appendLine("  </BANKMSGSRSV1>")
        sb.appendLine("</OFX>")

        return sb.toString()
    }
}