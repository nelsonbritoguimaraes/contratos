package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Parser real de extratos bancários.
 * Suporta:
 * - OFX (padrão mais comum nos bancos brasileiros)
 * - CSV simples (Data;Documento;Historico;Valor;Tipo)
 */
@Service
class BankStatementParserService {

    private val dateFormatterBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateFormatterIso = DateTimeFormatter.ISO_LOCAL_DATE
    private val ofxDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun parse(file: MultipartFile, contaBancariaId: UUID, tenantId: UUID): List<ExtratoBancarioItem> {
        val filename = file.originalFilename?.lowercase() ?: ""
        val content = file.inputStream.bufferedReader().use { it.readText() }

        return when {
            filename.endsWith(".ofx") || content.contains("<OFX>", ignoreCase = true) ||
                content.contains("<STMTTRN>", ignoreCase = true) ->
                parseOfx(content, contaBancariaId, tenantId)
            filename.endsWith(".csv") || content.contains(";") || content.contains(",") ->
                parseCsv(content, contaBancariaId, tenantId)
            else -> throw IllegalArgumentException("Formato de extrato não suportado. Use .ofx ou .csv")
        }
    }

    /**
     * Extrai blocos STMTTRN e lê tags em qualquer ordem (comum em OFX reais).
     */
    fun parseOfx(content: String, contaBancariaId: UUID, tenantId: UUID): List<ExtratoBancarioItem> {
        val itens = mutableListOf<ExtratoBancarioItem>()
        val blocks = Regex("""<STMTTRN>(.*?)</STMTTRN>""", RegexOption.IGNORE_CASE)
            .findAll(content.replace("\r", ""))

        blocks.forEach { block ->
            try {
                val body = block.groupValues[1]
                val trnType = tagValue(body, "TRNTYPE")?.uppercase()?.trim() ?: ""
                val dateStr = tagValue(body, "DTPOSTED")?.take(8) ?: return@forEach
                val amountStr = tagValue(body, "TRNAMT") ?: return@forEach
                val fitid = tagValue(body, "FITID")
                val checknum = tagValue(body, "CHECKNUM")
                val name = tagValue(body, "NAME")
                val memo = tagValue(body, "MEMO") ?: name ?: "Transação importada via OFX"

                val data = LocalDate.parse(dateStr, ofxDateFormatter)
                val valorSigned = parseMonetaryAmount(amountStr)

                val tipo = when {
                    trnType.contains("CREDIT") || trnType.contains("DEP") -> "CREDITO"
                    trnType.contains("DEBIT") || trnType.contains("PAY") || trnType.contains("XFER") -> "DEBITO"
                    valorSigned >= BigDecimal.ZERO -> "CREDITO"
                    else -> "DEBITO"
                }

                val valorAbs = valorSigned.abs().setScale(2, RoundingMode.HALF_UP)
                val documento = (fitid ?: checknum ?: "").take(30)

                itens.add(
                    ExtratoBancarioItem(
                        tenantId = tenantId,
                        contaBancariaId = contaBancariaId,
                        data = data,
                        documento = documento.ifBlank { null },
                        historico = memo.take(250),
                        valor = valorAbs,
                        tipo = tipo
                    )
                )
            } catch (_: Exception) {
                // ignora transação malformada
            }
        }

        return itens
    }

    fun parseCsv(content: String, contaBancariaId: UUID, tenantId: UUID): List<ExtratoBancarioItem> {
        val linhas = content.lines().filter { it.isNotBlank() }
        if (linhas.size < 2) return emptyList()

        val header = linhas.first().split(";", ",").map { it.trim().lowercase() }
        val dataIndex = header.indexOfFirst { it.contains("data") }
        val docIndex = header.indexOfFirst { it.contains("doc") || it.contains("num") }
        val histIndex = header.indexOfFirst { it.contains("hist") || it.contains("desc") || it.contains("memo") }
        val valorIndex = header.indexOfFirst { it.contains("valor") || it.contains("amount") }
        val tipoIndex = header.indexOfFirst { it == "tipo" || it.contains("cred") || it.contains("deb") }

        val itens = mutableListOf<ExtratoBancarioItem>()

        linhas.drop(1).forEach { linha ->
            try {
                val cols = linha.split(";", ",").map { it.trim() }

                val dataStr = if (dataIndex >= 0) cols.getOrNull(dataIndex) ?: return@forEach else return@forEach
                val data = try {
                    LocalDate.parse(dataStr, dateFormatterBr)
                } catch (_: Exception) {
                    LocalDate.parse(dataStr, dateFormatterIso)
                }

                val historico = if (histIndex >= 0) cols.getOrNull(histIndex) ?: "Importado via CSV" else "Importado via CSV"
                val documento = if (docIndex >= 0) cols.getOrNull(docIndex) else null

                val valorStr = if (valorIndex >= 0) cols.getOrNull(valorIndex) ?: "0" else "0"
                var valor = parseMonetaryAmount(valorStr)

                val tipo = when {
                    valor < BigDecimal.ZERO -> {
                        valor = valor.abs()
                        "DEBITO"
                    }
                    tipoIndex >= 0 -> {
                        val t = cols.getOrNull(tipoIndex)?.uppercase() ?: ""
                        if (t.contains("DEB")) "DEBITO" else "CREDITO"
                    }
                    else -> "CREDITO"
                }

                itens.add(
                    ExtratoBancarioItem(
                        tenantId = tenantId,
                        contaBancariaId = contaBancariaId,
                        data = data,
                        documento = documento?.take(30),
                        historico = historico.take(250),
                        valor = valor.setScale(2, RoundingMode.HALF_UP),
                        tipo = tipo
                    )
                )
            } catch (_: Exception) {
                // linha inválida
            }
        }

        return itens
    }

    private fun tagValue(block: String, tag: String): String? =
        Regex("""<$tag>([^<]*)""", RegexOption.IGNORE_CASE)
            .find(block)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /**
     * Aceita 4523,00 (BR), 4.523,00 (BR milhar), 4523.00 (US/OFX) e -1342.80.
     */
    internal fun parseMonetaryAmount(raw: String): BigDecimal {
        val cleaned = raw.trim().replace(" ", "")
        if (cleaned.isEmpty()) return BigDecimal.ZERO

        val negative = cleaned.startsWith("-")
        val positive = cleaned.removePrefix("-")

        val normalized = when {
            positive.contains(",") -> {
                // BR: milhar com ponto, decimal com vírgula
                positive.replace(".", "").replace(",", ".")
            }
            positive.count { it == '.' } > 1 -> {
                positive.replace(".", "")
            }
            else -> positive
        }

        return BigDecimal(normalized).let { if (negative) it.negate() else it }
    }
}
