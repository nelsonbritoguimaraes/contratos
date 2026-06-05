package com.contractops.api.rh.service

import com.contractops.api.rh.domain.EmployeeEvent
import com.contractops.api.rh.repository.EmployeeEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class EmployeeEventService(
    private val repository: EmployeeEventRepository
) {

    fun findByEmployee(employeeId: UUID, tenantId: UUID): List<EmployeeEvent> =
        repository.findByTenantIdAndEmployeeIdOrderByEventDateDesc(tenantId, employeeId)

    fun findByEmployeeAndPeriod(employeeId: UUID, tenantId: UUID, start: LocalDate, end: LocalDate): List<EmployeeEvent> =
        repository.findByTenantIdAndEmployeeIdAndEventDateBetween(tenantId, employeeId, start, end)

    @Transactional
    fun registerEvent(event: EmployeeEvent): EmployeeEvent {
        // Validação básica
        if (event.eventDate.isAfter(LocalDate.now().plusDays(1))) {
            throw IllegalArgumentException("Data do evento não pode ser no futuro")
        }
        return repository.save(event)
    }

    @Transactional
    fun updateEvent(id: UUID, tenantId: UUID, updated: EmployeeEvent): EmployeeEvent? {
        val existing = repository.findById(id)
            .filter { it.tenantId == tenantId }
            .orElse(null) ?: return null

        existing.eventType = updated.eventType
        existing.eventDate = updated.eventDate
        existing.description = updated.description
        existing.previousValue = updated.previousValue
        existing.newValue = updated.newValue
        existing.reason = updated.reason
        existing.documentReference = updated.documentReference
        existing.affectsPayrollFrom = updated.affectsPayrollFrom

        return repository.save(existing)
    }
}