package com.contractops.api.contract.service

import com.contractops.api.bidding.repository.BiddingLotRepository
import com.contractops.api.contract.api.ContractLotRequest
import com.contractops.api.contract.api.ContractLotResponse
import com.contractops.api.contract.domain.ContractLot
import com.contractops.api.contract.repository.ContractLotRepository
import com.contractops.api.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ContractLotService(
    private val contractLotRepository: ContractLotRepository,
    private val contractService: ContractService,
    private val biddingLotRepository: BiddingLotRepository
) {

    fun findByContract(contractId: UUID, tenantId: UUID): List<ContractLotResponse> {
        // Basic tenant validation via contract
        val contract = contractService.findById(contractId)
            ?: throw ResourceNotFoundException("Contrato não encontrado")

        if (contract.tenantId != tenantId) {
            throw ResourceNotFoundException("Contrato não encontrado")
        }

        return contractLotRepository.findByContractId(contractId)
            .map { ContractLotResponse.fromEntity(it) }
    }

    @Transactional
    fun createLot(contractId: UUID, tenantId: UUID, request: ContractLotRequest): ContractLotResponse {
        val contract = contractService.findById(contractId)
            ?: throw ResourceNotFoundException("Contrato não encontrado")

        if (contract.tenantId != tenantId) {
            throw ResourceNotFoundException("Contrato não encontrado")
        }

        val originalBiddingLot = request.originalBiddingLotId?.let { lotId ->
            val biddingLot = biddingLotRepository.findById(lotId)
                .orElseThrow { ResourceNotFoundException("Lote de licitação não encontrado: $lotId") }

            val contractBiddingId = contract.bidding?.id
            if (contractBiddingId == null) {
                throw IllegalArgumentException("Contrato não está vinculado a uma licitação; não é possível associar originalBiddingLotId")
            }
            if (biddingLot.bidding.id != contractBiddingId) {
                throw IllegalArgumentException("originalBiddingLotId must belong to the same bidding as the contract")
            }
            biddingLot
        }

        val lot = ContractLot(
            tenantId = tenantId,
            contract = contract,
            originalBiddingLot = originalBiddingLot,
            numeroLote = request.numeroLote,
            descricao = request.descricao,
            quantitativoPostos = request.quantitativoPostos,
            valorMensal = request.valorMensal,
            valorGlobal = request.valorGlobal
        )

        val saved = contractLotRepository.save(lot)
        return ContractLotResponse.fromEntity(saved)
    }
}
