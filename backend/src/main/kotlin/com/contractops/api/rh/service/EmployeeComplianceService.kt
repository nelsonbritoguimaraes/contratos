package com.contractops.api.rh.service

import com.contractops.api.rh.domain.EmployeeCompliance
import com.contractops.api.rh.repository.EmployeeComplianceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class EmployeeComplianceService(
    private val repository: EmployeeComplianceRepository
) {
    fun listarPorColaborador(tenantId: UUID, employeeId: UUID): List<EmployeeCompliance> =
        repository.findByTenantIdAndEmployeeIdOrderByDataValidadeDesc(tenantId, employeeId)

    fun listarVencendo(tenantId: UUID, ate: LocalDate = LocalDate.now().plusDays(30)): List<EmployeeCompliance> =
        repository.findByTenantIdAndStatus(tenantId, "VALIDO")
            .filter { it.dataValidade != null && !it.dataValidade!!.isAfter(ate) }

    @Transactional
    fun registrar(
        tenantId: UUID,
        employeeId: UUID,
        tipo: String,
        descricao: String? = null,
        dataRealizacao: LocalDate? = null,
        dataValidade: LocalDate? = null,
        documentoRef: String? = null,
        observacao: String? = null
    ): EmployeeCompliance = repository.save(
        EmployeeCompliance(
            tenantId = tenantId,
            employeeId = employeeId,
            tipo = tipo.uppercase(),
            descricao = descricao,
            dataRealizacao = dataRealizacao,
            dataValidade = dataValidade,
            status = "VALIDO",
            documentoRef = documentoRef,
            observacao = observacao
        )
    )
}
