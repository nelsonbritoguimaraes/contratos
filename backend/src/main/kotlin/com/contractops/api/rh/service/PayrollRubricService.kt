package com.contractops.api.rh.service

import com.contractops.api.rh.domain.PayrollRubric
import com.contractops.api.rh.repository.PayrollRubricRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class PayrollRubricService(
    private val repository: PayrollRubricRepository
) {

    fun findAllActiveByTenant(tenantId: UUID): List<PayrollRubric> =
        repository.findByTenantIdAndIsActiveTrue(tenantId)

    fun findAllByTenant(tenantId: UUID): List<PayrollRubric> =
        repository.findByTenantId(tenantId)

    fun findById(id: UUID, tenantId: UUID): PayrollRubric? =
        repository.findById(id)
            .filter { it.tenantId == tenantId }
            .orElse(null)

    fun findByCode(code: String, tenantId: UUID): PayrollRubric? =
        repository.findByTenantIdAndCode(tenantId, code)

    @Transactional
    fun create(rubric: PayrollRubric): PayrollRubric {
        if (repository.findByTenantIdAndCode(rubric.tenantId, rubric.code) != null) {
            throw com.contractops.api.rh.exception.RhBusinessException("Já existe rubrica com o código '${rubric.code}' neste tenant")
        }
        return repository.save(rubric)
    }

    @Transactional
    fun update(id: UUID, tenantId: UUID, updated: PayrollRubric): PayrollRubric? {
        val existing = findById(id, tenantId) ?: return null

        existing.description = updated.description
        existing.type = updated.type
        existing.calculationType = updated.calculationType
        existing.fixedValue = updated.fixedValue
        existing.percentage = updated.percentage
        existing.reference = updated.reference
        existing.incidesInss = updated.incidesInss
        existing.incidesFgts = updated.incidesFgts
        existing.incidesIrrf = updated.incidesIrrf
        existing.isActive = updated.isActive
        existing.displayOrder = updated.displayOrder

        return repository.save(existing)
    }

    @Transactional
    fun deactivate(id: UUID, tenantId: UUID): Boolean {
        val existing = findById(id, tenantId) ?: return false
        existing.isActive = false
        repository.save(existing)
        return true
    }
}