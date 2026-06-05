package com.contractops.api.contract.service

import com.contractops.api.contract.api.CreateAmendmentRequest
import com.contractops.api.contract.api.UpdateAmendmentRequest
import com.contractops.api.contract.domain.ContractAmendment
import com.contractops.api.contract.repository.ContractAmendmentRepository
import com.contractops.api.contract.repository.ContractRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ContractAmendmentService(
    private val amendmentRepository: ContractAmendmentRepository,
    private val contractRepository: ContractRepository
) {

    fun findByContract(contractId: UUID, tenantId: UUID): List<ContractAmendment> {
        return amendmentRepository.findByTenantIdAndContractId(tenantId, contractId)
    }

    fun findById(amendmentId: UUID, tenantId: UUID): ContractAmendment? {
        return amendmentRepository.findById(amendmentId)
            .filter { it.tenantId == tenantId }
            .orElse(null)
    }

    @Transactional
    fun createAmendment(contractId: UUID, tenantId: UUID, request: CreateAmendmentRequest): ContractAmendment {
        val contract = contractRepository.findById(contractId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Contrato não encontrado ou não pertence ao tenant") }

        // Regras de negócio básicas (SPEC 6.2)
        val validTypes = setOf("PRORROGACAO", "ACRESIMO", "SUPRESSAO", "REPACTUACAO", "REAJUSTE", "REEQUILIBRIO")
        if (!validTypes.contains(request.type.uppercase())) {
            throw IllegalArgumentException("Tipo de aditivo inválido: ${request.type}")
        }

        val amendment = ContractAmendment(
            tenantId = tenantId,
            contract = contract,
            amendmentNumber = request.amendmentNumber,
            type = request.type.uppercase(),
            description = request.description,
            effectiveDate = request.effectiveDate,
            newEndDate = request.newEndDate,
            valueChange = request.valueChange,
            newMonthlyValue = request.newMonthlyValue,
            status = request.status.uppercase(),
            documentUrl = request.documentUrl
        )

        return amendmentRepository.save(amendment)
    }

    @Transactional
    fun updateAmendment(amendmentId: UUID, tenantId: UUID, request: UpdateAmendmentRequest): ContractAmendment? {
        val existing = findById(amendmentId, tenantId) ?: return null

        request.amendmentNumber?.let { existing.amendmentNumber = it }
        request.description?.let { existing.description = it }
        request.effectiveDate?.let { existing.effectiveDate = it }
        request.newEndDate?.let { existing.newEndDate = it }
        request.valueChange?.let { existing.valueChange = it }
        request.newMonthlyValue?.let { existing.newMonthlyValue = it }
        request.status?.let { existing.status = it.uppercase() }
        request.documentUrl?.let { existing.documentUrl = it }

        return amendmentRepository.save(existing)
    }

    @Transactional
    fun deleteAmendment(amendmentId: UUID, tenantId: UUID): Boolean {
        val existing = findById(amendmentId, tenantId) ?: return false
        amendmentRepository.delete(existing)
        return true
    }
}
