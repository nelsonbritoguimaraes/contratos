package com.contractops.api.time.service

import com.contractops.api.time.domain.PunchAdjustment
import com.contractops.api.time.repository.PunchAdjustmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class PunchAdjustmentService(
    private val repository: PunchAdjustmentRepository,
    private val attendanceProcessingService: AttendanceProcessingService
) {

    fun listPending(tenantId: UUID): List<PunchAdjustment> =
        repository.findByTenantIdAndStatus(tenantId, "PENDENTE")

    @Transactional
    fun solicitar(adjustment: PunchAdjustment): PunchAdjustment {
        adjustment.status = "PENDENTE"
        return repository.save(adjustment)
    }

    @Transactional
    fun aprovarSupervisor(id: UUID, aprovador: String): PunchAdjustment {
        val adj = repository.findById(id).orElseThrow()
        adj.status = "APROVADO_SUPERVISOR"
        adj.aprovadoSupervisorPor = aprovador
        return repository.save(adj)
    }

    @Transactional
    fun aprovarDp(id: UUID, aprovador: String): PunchAdjustment {
        val adj = repository.findById(id).orElseThrow()
        adj.status = "APROVADO"
        adj.aprovadoDpPor = aprovador
        attendanceProcessingService.processEmployeeDay(
            adj.employeeId, adj.date, adj.tenantId, adj.postId, adj.contractId
        )
        return repository.save(adj)
    }

    @Transactional
    fun rejeitar(id: UUID, motivo: String): PunchAdjustment {
        val adj = repository.findById(id).orElseThrow()
        adj.status = "REJEITADO"
        adj.motivo = "${adj.motivo}\nRejeitado: $motivo"
        return repository.save(adj)
    }
}
