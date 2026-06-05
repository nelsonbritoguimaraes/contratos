package com.contractops.api.bidding.service

import com.contractops.api.bidding.api.BiddingLotRequest
import com.contractops.api.bidding.api.BiddingLotResponse
import com.contractops.api.bidding.domain.BiddingLot
import com.contractops.api.bidding.repository.BiddingLotRepository
import com.contractops.api.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BiddingLotService(
    private val biddingLotRepository: BiddingLotRepository,
    private val biddingService: BiddingService
) {

    fun findByBidding(biddingId: UUID, tenantId: UUID): List<BiddingLotResponse> {
        if (!biddingService.existsByIdAndTenant(biddingId, tenantId)) {
            throw ResourceNotFoundException("Licitação não encontrada ou não pertence ao tenant")
        }
        return biddingLotRepository.findByBiddingId(biddingId)
            .map { BiddingLotResponse.fromEntity(it) }
    }

    @Transactional
    fun createLot(biddingId: UUID, tenantId: UUID, request: BiddingLotRequest): BiddingLotResponse {
        val bidding = biddingService.getEntityByIdAndTenant(biddingId, tenantId)
            ?: throw ResourceNotFoundException("Licitação não encontrada ou não pertence ao tenant")

        val lot = BiddingLot(
            tenantId = tenantId,
            bidding = bidding,
            numeroLote = request.numeroLote,
            descricao = request.descricao,
            quantitativoPostos = request.quantitativoPostos,
            valorMensal = request.valorMensal,
            valorAnual = request.valorAnual,
            valorGlobal = request.valorGlobal,
            prazoMeses = request.prazoMeses
        )

        val saved = biddingLotRepository.save(lot)
        return BiddingLotResponse.fromEntity(saved)
    }
}
