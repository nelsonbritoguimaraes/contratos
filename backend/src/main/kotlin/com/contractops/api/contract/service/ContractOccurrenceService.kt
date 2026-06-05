package com.contractops.api.contract.service

import com.contractops.api.contract.domain.ContractOccurrence
import com.contractops.api.contract.repository.ContractOccurrenceRepository
import com.contractops.api.contract.repository.ContractRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class ContractOccurrenceService(
    private val repository: ContractOccurrenceRepository,
    private val contractRepository: ContractRepository
) {
    fun listar(contractId: UUID, tenantId: UUID): List<ContractOccurrence> =
        repository.findByTenantIdAndContractIdOrderByDataOcorrenciaDesc(tenantId, contractId)

    @Transactional
    fun registrar(
        tenantId: UUID,
        contractId: UUID,
        dataOcorrencia: LocalDate,
        tipo: String,
        titulo: String,
        descricao: String? = null,
        severidade: String = "INFO",
        registradoPor: String? = null,
        anexoRef: String? = null
    ): ContractOccurrence {
        contractRepository.findById(contractId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Contrato não encontrado") }

        return repository.save(
            ContractOccurrence(
                tenantId = tenantId,
                contractId = contractId,
                dataOcorrencia = dataOcorrencia,
                tipo = tipo.uppercase(),
                titulo = titulo,
                descricao = descricao,
                severidade = severidade.uppercase(),
                registradoPor = registradoPor,
                anexoRef = anexoRef
            )
        )
    }
}
