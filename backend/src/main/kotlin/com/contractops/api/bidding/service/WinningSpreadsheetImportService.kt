package com.contractops.api.bidding.service

import com.contractops.api.bidding.api.BiddingPostoRequest
import com.contractops.api.bidding.domain.WinningSpreadsheet
import com.contractops.api.bidding.repository.WinningSpreadsheetRepository
import com.contractops.api.common.exception.ResourceNotFoundException
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.*

@Service
class WinningSpreadsheetImportService(
    private val winningSpreadsheetRepository: WinningSpreadsheetRepository,
    private val biddingService: BiddingService,
    private val postoService: BiddingPostoService
) {

    @Transactional
    fun importSpreadsheet(
        biddingId: UUID,
        tenantId: UUID,
        file: MultipartFile,
        markVencedora: Boolean = false
    ): Map<String, Any> {
        val name = file.originalFilename?.lowercase() ?: ""
        val validMimeTypes = setOf(
            "text/csv", "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val contentType = file.contentType?.lowercase() ?: ""
        val isExcel = name.endsWith(".xlsx") || name.endsWith(".xls")
        val isCsv = name.endsWith(".csv") || name.endsWith(".txt") || name.endsWith(".tsv")
        if (!isExcel && !isCsv) {
            throw IllegalArgumentException("Formato de arquivo não suportado. Use .xlsx, .xls, .csv, .txt ou .tsv")
        }
        val rows = when {
            isExcel -> parseExcel(file)
            else -> parseDelimited(file)
        }
        return persistImport(biddingId, tenantId, file, rows, markVencedora)
    }

    private fun parseExcel(file: MultipartFile): List<List<String>> {
        val formatter = DataFormatter()
        file.inputStream.use { input ->
            WorkbookFactory.create(input).use { wb ->
                val sheet = wb.getSheetAt(0)
                val maxRows = 50000 // Limite de segurança contra OOM
                return sheet.take(maxRows).map { row ->
                    (0 until row.lastCellNum.coerceAtLeast(0).coerceAtMost(200)).map { ci ->
                        row.getCell(ci)?.let { formatter.formatCellValue(it) }?.trim() ?: ""
                    }
                }.filter { line -> line.any { it.isNotBlank() } }
            }
        }
    }

    private fun parseDelimited(file: MultipartFile): List<List<String>> {
        val lines = BufferedReader(InputStreamReader(file.inputStream, StandardCharsets.UTF_8))
            .lineSequence()
            .take(100000) // Limite de segurança: previne OOM com arquivos massivos
            .toList()
        return lines.map { it.split(';', ',', '\t').map { c -> c.trim() } }
    }

    private fun persistImport(
        biddingId: UUID,
        tenantId: UUID,
        file: MultipartFile,
        rows: List<List<String>>,
        markVencedora: Boolean
    ): Map<String, Any> {
        val bidding = biddingService.getEntityByIdAndTenant(biddingId, tenantId)
            ?: throw ResourceNotFoundException("Licitação não encontrada")

        var postosCriados = 0
        rows.drop(1).forEach { cols ->
            if (cols.size < 2) return@forEach
            val nome = cols.getOrNull(0)?.trim().orEmpty()
            if (nome.isBlank() || nome.equals("nome", ignoreCase = true)) return@forEach
            postoService.create(
                biddingId,
                tenantId,
                BiddingPostoRequest(
                    nome = nome,
                    funcao = cols.getOrNull(1)?.trim(),
                    cbo = cols.getOrNull(2)?.trim(),
                    escala = cols.getOrNull(3)?.trim(),
                    valorMensal = parseMoney(cols.getOrNull(4)),
                    localExecucao = cols.getOrNull(5)?.trim(),
                    municipioExecucao = cols.getOrNull(6)?.trim()
                )
            )
            postosCriados++
        }

        val versao = (winningSpreadsheetRepository.findByBiddingId(biddingId).maxOfOrNull { it.versao } ?: 0) + 1
        if (markVencedora) {
            winningSpreadsheetRepository.findByBiddingId(biddingId).forEach {
                it.isVencedora = false
                winningSpreadsheetRepository.save(it)
            }
        }
        val ws = WinningSpreadsheet(
            tenantId = tenantId,
            bidding = bidding,
            versao = versao,
            arquivoNome = file.originalFilename,
            memoriaCalculo = "Importado $postosCriados postos (${file.originalFilename})",
            isVencedora = markVencedora
        )
        val saved = winningSpreadsheetRepository.save(ws)

        return mapOf(
            "planilhaId" to saved.id!!,
            "postosImportados" to postosCriados,
            "versao" to versao,
            "arquivo" to (file.originalFilename ?: "planilha"),
            "formato" to if ((file.originalFilename ?: "").lowercase().endsWith(".xlsx")) "XLSX" else "CSV"
        )
    }

    private fun parseMoney(raw: String?): BigDecimal? =
        raw?.trim()?.replace(".", "")?.replace(",", ".")?.toBigDecimalOrNull()
            ?: raw?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
}
