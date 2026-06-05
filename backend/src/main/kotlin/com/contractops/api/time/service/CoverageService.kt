package com.contractops.api.time.service

import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.rh.repository.EmployeeVacationRepository
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.repository.VolanteAssignmentRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

/**
 * Serviço de cobertura operacional — estende apuração de ponto com mapa completo de status.
 * Status: COBERTO, PARCIAL, DESCOBERTO, FERIAS, VOLANTE
 */
@Service
class CoverageService(
    private val attendanceDayRepository: AttendanceDayRepository,
    private val servicePostRepository: ServicePostRepository,
    private val employeeVacationRepository: EmployeeVacationRepository,
    private val volanteAssignmentRepository: VolanteAssignmentRepository,
    private val assignmentRepository: EmployeeAssignmentRepository
) {

    enum class CoverageStatus {
        COBERTO, PARCIAL, DESCOBERTO, FERIAS, VOLANTE
    }

    fun getPostCoverageMap(tenantId: UUID, contractId: UUID, date: LocalDate): List<Map<String, Any?>> {
        val posts = servicePostRepository.findByContractId(contractId).filter { it.tenantId == tenantId }
        val attendances = attendanceDayRepository.findByTenantIdAndContractIdAndDate(tenantId, contractId, date)
        val vacations = employeeVacationRepository.findActiveOnDate(tenantId, contractId, date)
        val volanteAssignments = volanteAssignmentRepository.findByTenantIdAndContractIdAndAssignmentDate(
            tenantId, contractId, date
        )
        val assignments = assignmentRepository.findByTenantIdAndContractId(tenantId, contractId)
            .filter { it.isActive }

        return posts.map { post ->
            val titular = assignments.firstOrNull {
                it.postId == post.id && it.role?.uppercase() != "VOLANTE"
            }
            val postAttendances = attendances.filter { it.postId == post.id || titular?.employeeId == it.employeeId }
            val volanteCoverage = volanteAssignments.firstOrNull {
                it.postId == post.id &&
                    it.workflowStatus in listOf("VOLANTE_ATRIBUIDO", "VOLANTE_CONFIRMADO", "EVIDENCIA_REGISTRADA", "CONCLUIDO")
            }
            val onVacation = titular?.employeeId?.let { empId ->
                vacations.any { it.employeeId == empId }
            } ?: false

            val status = resolveStatus(postAttendances, onVacation, volanteCoverage != null)
            val workedMinutes = postAttendances.sumOf { it.totalWorkedMinutes }
            val jornada = (post.jornadaHoras ?: 8) * 60

            mapOf(
                "postId" to post.id,
                "postName" to post.nome,
                "postCode" to post.codigo,
                "status" to status.name,
                "titularEmployeeId" to titular?.employeeId,
                "volanteEmployeeId" to volanteCoverage?.volanteEmployeeId,
                "volanteAssignmentId" to volanteCoverage?.id,
                "workedMinutes" to workedMinutes,
                "expectedMinutes" to jornada,
                "coveragePercent" to if (jornada > 0) (workedMinutes * 100.0 / jornada).coerceAtMost(100.0) else 0.0,
                "onVacation" to onVacation,
                "attendanceCount" to postAttendances.size
            )
        }
    }

    fun getDailyCoverageSummary(contractId: UUID, date: LocalDate, tenantId: UUID): Map<String, Any> {
        val postMap = getPostCoverageMap(tenantId, contractId, date)
        val totalPosts = postMap.size.coerceAtLeast(1)
        val covered = postMap.count { it["status"] == CoverageStatus.COBERTO.name || it["status"] == CoverageStatus.VOLANTE.name }
        val partial = postMap.count { it["status"] == CoverageStatus.PARCIAL.name }
        val uncovered = postMap.count { it["status"] == CoverageStatus.DESCOBERTO.name }
        val onVacation = postMap.count { it["status"] == CoverageStatus.FERIAS.name }
        val volante = postMap.count { it["status"] == CoverageStatus.VOLANTE.name }
        val coveragePercent = covered * 100.0 / totalPosts

        val attendances = attendanceDayRepository.findByTenantIdAndContractIdAndDate(tenantId, contractId, date)

        return mapOf(
            "date" to date,
            "contract_id" to contractId,
            "posts_with_work" to covered,
            "total_expected_posts" to totalPosts,
            "coverage_percent" to coveragePercent,
            "total_worked_minutes" to attendances.sumOf { it.totalWorkedMinutes },
            "total_absence_minutes" to attendances.sumOf { it.absenceMinutes },
            "volantes_detectados" to volante,
            "attendances" to attendances.size,
            "status_breakdown" to mapOf(
                "coberto" to covered,
                "parcial" to partial,
                "descoberto" to uncovered,
                "ferias" to onVacation,
                "volante" to volante
            ),
            "post_coverage" to postMap
        )
    }

    private fun resolveStatus(
        attendances: List<com.contractops.api.time.domain.AttendanceDay>,
        onVacation: Boolean,
        hasVolante: Boolean
    ): CoverageStatus {
        if (hasVolante) return CoverageStatus.VOLANTE
        if (onVacation && attendances.none { it.totalWorkedMinutes > 0 }) return CoverageStatus.FERIAS

        val worked = attendances.sumOf { it.totalWorkedMinutes }
        return when {
            worked == 0 -> CoverageStatus.DESCOBERTO
            worked >= 360 -> CoverageStatus.COBERTO
            else -> CoverageStatus.PARCIAL
        }
    }
}
