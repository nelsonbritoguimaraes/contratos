package com.contractops.api.time.service

import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.domain.VolanteAssignment
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.repository.VolanteAssignmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

/**
 * Workflow volante: falta -> volante -> evidência (SPEC §11).
 */
@Service
class VolanteWorkflowService(
    private val volanteAssignmentRepository: VolanteAssignmentRepository,
    private val attendanceDayRepository: AttendanceDayRepository,
    private val assignmentRepository: EmployeeAssignmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val servicePostRepository: ServicePostRepository,
    private val coverageService: CoverageService
) {

    @Transactional
    fun detectAbsences(tenantId: UUID, contractId: UUID, date: LocalDate): List<VolanteAssignment> {
        val coverage = coverageService.getPostCoverageMap(tenantId, contractId, date)
        val uncovered = coverage.filter {
            it["status"] == CoverageService.CoverageStatus.DESCOBERTO.name ||
                it["status"] == CoverageService.CoverageStatus.PARCIAL.name
        }

        val created = mutableListOf<VolanteAssignment>()
        uncovered.forEach { postCoverage ->
            val postId = postCoverage["postId"] as UUID
            val titularId = postCoverage["titularEmployeeId"] as? UUID ?: return@forEach

            val existing = volanteAssignmentRepository.findByTenantIdAndContractIdAndAssignmentDate(tenantId, contractId, date)
                .firstOrNull { it.postId == postId && it.absentEmployeeId == titularId }
            if (existing != null) {
                created.add(existing)
                return@forEach
            }

            created.add(
                volanteAssignmentRepository.save(
                    VolanteAssignment(
                        tenantId = tenantId,
                        contractId = contractId,
                        postId = postId,
                        absentEmployeeId = titularId,
                        assignmentDate = date,
                        workflowStatus = "FALTA_DETECTADA",
                        detectedAt = OffsetDateTime.now(),
                        notes = "Falta detectada automaticamente — status ${postCoverage["status"]}"
                    )
                )
            )
        }
        return created
    }

    @Transactional
    fun assignVolante(
        assignmentId: UUID,
        tenantId: UUID,
        volanteEmployeeId: UUID,
        notes: String? = null
    ): VolanteAssignment {
        val assignment = findAssignment(assignmentId, tenantId)
        if (assignment.workflowStatus !in listOf("FALTA_DETECTADA", "VOLANTE_ATRIBUIDO")) {
            throw IllegalStateException("Workflow inválido para atribuição: ${assignment.workflowStatus}")
        }

        employeeRepository.findById(volanteEmployeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Volante não encontrado") }

        assignment.volanteEmployeeId = volanteEmployeeId
        assignment.workflowStatus = "VOLANTE_ATRIBUIDO"
        assignment.assignedAt = OffsetDateTime.now()
        notes?.let { assignment.notes = it }

        return volanteAssignmentRepository.save(assignment)
    }

    @Transactional
    fun confirmVolante(assignmentId: UUID, tenantId: UUID, confirmedBy: String? = null): VolanteAssignment {
        val assignment = findAssignment(assignmentId, tenantId)
        if (assignment.volanteEmployeeId == null) {
            throw IllegalStateException("Volante não atribuído")
        }
        if (assignment.workflowStatus != "VOLANTE_ATRIBUIDO") {
            throw IllegalStateException("Workflow inválido para confirmação: ${assignment.workflowStatus}")
        }

        assignment.workflowStatus = "VOLANTE_CONFIRMADO"
        assignment.confirmedAt = OffsetDateTime.now()
        confirmedBy?.let { assignment.notes = (assignment.notes ?: "") + " Confirmado por $it" }

        return volanteAssignmentRepository.save(assignment)
    }

    @Transactional
    fun registerEvidence(
        assignmentId: UUID,
        tenantId: UUID,
        evidenceUrl: String,
        evidenceNotes: String? = null
    ): VolanteAssignment {
        val assignment = findAssignment(assignmentId, tenantId)
        if (assignment.workflowStatus !in listOf("VOLANTE_ATRIBUIDO", "VOLANTE_CONFIRMADO")) {
            throw IllegalStateException("Workflow inválido para evidência: ${assignment.workflowStatus}")
        }

        assignment.evidenceUrl = evidenceUrl
        assignment.evidenceNotes = evidenceNotes
        assignment.evidenceAt = OffsetDateTime.now()
        assignment.workflowStatus = "EVIDENCIA_REGISTRADA"

        return volanteAssignmentRepository.save(assignment)
    }

    @Transactional
    fun completeAssignment(assignmentId: UUID, tenantId: UUID): VolanteAssignment {
        val assignment = findAssignment(assignmentId, tenantId)
        assignment.workflowStatus = "CONCLUIDO"
        return volanteAssignmentRepository.save(assignment)
    }

    fun listAssignments(tenantId: UUID, contractId: UUID, date: LocalDate): List<VolanteAssignment> =
        volanteAssignmentRepository.findByTenantIdAndContractIdAndAssignmentDate(tenantId, contractId, date)

    fun suggestVolantes(tenantId: UUID, contractId: UUID, postId: UUID): List<Map<String, Any?>> {
        val post = servicePostRepository.findById(postId).orElse(null) ?: return emptyList()
        val volantes = assignmentRepository.findByTenantIdAndContractId(tenantId, contractId)
            .filter { it.isActive && it.role?.uppercase() == "VOLANTE" }

        return volantes.map { v ->
            val emp = employeeRepository.findById(v.employeeId).orElse(null)
            mapOf(
                "employeeId" to v.employeeId,
                "employeeName" to emp?.fullName,
                "postId" to v.postId,
                "compatibleFunction" to (post.funcao == null || post.funcao == emp?.cargo),
                "role" to v.role
            )
        }
    }

    private fun findAssignment(id: UUID, tenantId: UUID): VolanteAssignment =
        volanteAssignmentRepository.findById(id)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Atribuição volante não encontrada") }
}
