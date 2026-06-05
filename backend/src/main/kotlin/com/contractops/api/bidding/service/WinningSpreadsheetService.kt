package com.contractops.api.bidding.service

import com.contractops.api.bidding.api.WinningSpreadsheetRequest
import com.contractops.api.bidding.api.WinningSpreadsheetResponse
import com.contractops.api.bidding.domain.WinningSpreadsheet
import com.contractops.api.bidding.repository.WinningSpreadsheetRepository
import com.contractops.api.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class WinningSpreadsheetService(
    private val winningSpreadsheetRepository: WinningSpreadsheetRepository,
    private val biddingService: BiddingService
) {

    fun findByBidding(biddingId: UUID, tenantId: UUID): List<WinningSpreadsheetResponse> {
        if (!biddingService.existsByIdAndTenant(biddingId, tenantId)) {
            throw ResourceNotFoundException("Licitação não encontrada ou não pertence ao tenant")
        }
        return winningSpreadsheetRepository.findByBiddingId(biddingId)
            .map { WinningSpreadsheetResponse.fromEntity(it) }
    }

    @Transactional
    fun create(tenantId: UUID, request: WinningSpreadsheetRequest): WinningSpreadsheetResponse {
        val bidding = request.biddingId?.let { bidId ->
            biddingService.getEntityByIdAndTenant(bidId, tenantId)
                ?: throw ResourceNotFoundException("Licitação não encontrada ou não pertence ao tenant")
        }

        // Lógica simples de versionamento:
        // Se a nova planilha for marcada como vencedora, desmarcamos as anteriores do mesmo bidding.
        if (request.isVencedora && request.biddingId != null) {
            val previousWinners = winningSpreadsheetRepository.findByBiddingId(request.biddingId)
                .filter { it.isVencedora }

            previousWinners.forEach {
                it.isVencedora = false
                winningSpreadsheetRepository.save(it)
            }
        }

        val ws = WinningSpreadsheet(
            tenantId = tenantId,
            bidding = bidding,
            contractId = request.contractId,
            versao = request.versao,
            arquivoNome = request.arquivoNome,
            arquivoUrl = request.arquivoUrl,
            memoriaCalculo = request.memoriaCalculo,
            isVencedora = request.isVencedora
        )

        val saved = winningSpreadsheetRepository.save(ws)
        return WinningSpreadsheetResponse.fromEntity(saved)
    }

    @Transactional
    fun setVencedora(spreadsheetId: UUID, tenantId: UUID): WinningSpreadsheetResponse {
        val ws = winningSpreadsheetRepository.findById(spreadsheetId)
            .orElseThrow { ResourceNotFoundException("Planilha não encontrada") }
        if (ws.tenantId != tenantId) throw IllegalArgumentException("Planilha não pertence ao tenant")
        val biddingId = ws.bidding?.id ?: throw IllegalStateException("Planilha sem licitação vinculada")
        winningSpreadsheetRepository.findByBiddingId(biddingId).forEach {
            it.isVencedora = false
            winningSpreadsheetRepository.save(it)
        }
        ws.isVencedora = true
        return WinningSpreadsheetResponse.fromEntity(winningSpreadsheetRepository.save(ws))
    }
}
