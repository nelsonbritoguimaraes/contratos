package com.contractops.api.contract.service

import com.contractops.api.bidding.domain.WinningSpreadsheet
import com.contractops.api.bidding.repository.WinningSpreadsheetRepository
import com.contractops.api.contract.domain.Contract
import com.contractops.api.contract.repository.ContractRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class ContractService(
    private val contractRepository: ContractRepository,
    private val winningSpreadsheetRepository: WinningSpreadsheetRepository,
    private val biddingRepository: com.contractops.api.bidding.repository.BiddingRepository
) {

    fun findAllByTenant(tenantId: UUID): List<Contract> =
        contractRepository.findByTenantId(tenantId)

    fun findById(id: UUID): Contract? =
        contractRepository.findById(id).orElse(null)

    @Transactional
    fun createContract(
        tenantId: UUID,
        companyId: UUID,
        branchId: UUID?,
        numero: String,
        orgao: String,
        cnpjOrgao: String?,
        objeto: String?,
        vigenciaInicio: LocalDate?,
        vigenciaFim: LocalDate?,
        valorMensal: BigDecimal?,
        valorGlobal: BigDecimal?,
        status: String = "ATIVO",
        winningSpreadsheetId: UUID? = null,
        biddingId: UUID? = null
    ): Contract {

        val winningSpreadsheet: WinningSpreadsheet? = winningSpreadsheetId?.let {
            winningSpreadsheetRepository.findById(it).orElse(null)
        }

        val bidding: com.contractops.api.bidding.domain.Bidding? = biddingId?.let {
            biddingRepository.findById(it).orElse(null)?.takeIf { b -> b.tenantId == tenantId }
        }

        val contract = Contract(
            tenantId = tenantId,
            companyId = companyId,
            branchId = branchId,
            numero = numero,
            orgao = orgao,
            cnpjOrgao = cnpjOrgao,
            objeto = objeto,
            vigenciaInicio = vigenciaInicio,
            vigenciaFim = vigenciaFim,
            valorMensal = valorMensal,
            valorGlobal = valorGlobal,
            status = status,
            qtdPostosContratados = 0,
            winningSpreadsheet = winningSpreadsheet,
            bidding = bidding
        )

        return contractRepository.save(contract)
    }

    @Transactional
    fun linkWinningSpreadsheet(contractId: UUID, winningSpreadsheetId: UUID, tenantId: UUID): Contract? {
        val contract = contractRepository.findById(contractId).orElse(null) ?: return null
        if (contract.tenantId != tenantId) return null

        val ws = winningSpreadsheetRepository.findById(winningSpreadsheetId).orElse(null) ?: return null

        contract.winningSpreadsheet = ws
        return contractRepository.save(contract)
    }

    @Transactional
    fun updateContract(
        id: UUID,
        tenantId: UUID,
        updates: (Contract) -> Unit
    ): Contract? {
        val existing = contractRepository.findById(id).orElse(null) ?: return null
        if (existing.tenantId != tenantId) return null

        updates(existing)
        return contractRepository.save(existing)
    }
}

