package com.contractops.api.schedule.service

import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.schedule.domain.EmployeeRoster
import com.contractops.api.schedule.domain.PostSchedule
import com.contractops.api.schedule.domain.ShiftTemplate
import com.contractops.api.schedule.repository.EmployeeRosterRepository
import com.contractops.api.schedule.repository.PostScheduleRepository
import com.contractops.api.schedule.repository.ShiftTemplateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class ScheduleService(
    private val shiftTemplateRepository: ShiftTemplateRepository,
    private val postScheduleRepository: PostScheduleRepository,
    private val employeeRosterRepository: EmployeeRosterRepository,
    private val servicePostRepository: ServicePostRepository,
    private val contractRepository: ContractRepository
) {
    fun listTemplates(tenantId: UUID, contractId: UUID? = null): List<ShiftTemplate> =
        if (contractId != null) {
            shiftTemplateRepository.findByTenantIdAndContractIdAndIsActiveTrue(tenantId, contractId)
        } else {
            shiftTemplateRepository.findByTenantIdAndIsActiveTrue(tenantId)
        }

    @Transactional
    fun createTemplate(template: ShiftTemplate): ShiftTemplate = shiftTemplateRepository.save(template)

    fun listPostSchedules(tenantId: UUID, contractId: UUID): List<PostSchedule> =
        postScheduleRepository.findByTenantIdAndContractId(tenantId, contractId)

    @Transactional
    fun createPostSchedule(schedule: PostSchedule): PostSchedule {
        contractRepository.findById(schedule.contractId)
            .filter { it.tenantId == schedule.tenantId }
            .orElseThrow { IllegalArgumentException("Contrato não encontrado") }
        servicePostRepository.findById(schedule.postId)
            .filter { it.tenantId == schedule.tenantId && it.contractId == schedule.contractId }
            .orElseThrow { IllegalArgumentException("Posto não encontrado") }
        return postScheduleRepository.save(schedule)
    }

    fun listRosters(tenantId: UUID, contractId: UUID): List<EmployeeRoster> =
        employeeRosterRepository.findByTenantIdAndContractId(tenantId, contractId)

    @Transactional
    fun createRoster(roster: EmployeeRoster): EmployeeRoster = employeeRosterRepository.save(roster)

    /**
     * Gera grade de escala 12x36 / plantão para um intervalo de datas.
     */
    fun generateScheduleGrid(
        tenantId: UUID,
        contractId: UUID,
        start: LocalDate,
        end: LocalDate
    ): List<Map<String, Any?>> {
        val posts = servicePostRepository.findByContractId(contractId).filter { it.tenantId == tenantId }
        val schedules = postScheduleRepository.findByTenantIdAndContractId(tenantId, contractId)
        val rosters = employeeRosterRepository.findByTenantIdAndContractId(tenantId, contractId)
        val templates = shiftTemplateRepository.findByTenantIdAndContractIdAndIsActiveTrue(tenantId, contractId)
            .associateBy { it.id }

        val grid = mutableListOf<Map<String, Any?>>()
        var date = start
        while (!date.isAfter(end)) {
            posts.forEach { post ->
                val schedule = schedules.firstOrNull { s ->
                    s.postId == post.id &&
                        !date.isBefore(s.effectiveFrom) &&
                        (s.effectiveTo == null || !date.isAfter(s.effectiveTo)) &&
                        s.status == "ACTIVE"
                }
                val template = schedule?.shiftTemplateId?.let { templates[it] }
                val roster = rosters.firstOrNull { r ->
                    r.postId == post.id &&
                        r.status == "ACTIVE" &&
                        !date.isBefore(r.effectiveFrom) &&
                        (r.effectiveTo == null || !date.isAfter(r.effectiveTo))
                }
                val scheduleType = schedule?.scheduleType ?: template?.shiftType ?: post.escala
                val worksToday = isWorkDay(date, scheduleType, template, roster?.effectiveFrom)

                grid.add(
                    mapOf(
                        "date" to date.toString(),
                        "postId" to post.id,
                        "postName" to post.nome,
                        "scheduleType" to scheduleType,
                        "employeeId" to roster?.employeeId,
                        "role" to roster?.role,
                        "worksToday" to worksToday,
                        "entryTime" to (template?.entryTime?.toString() ?: defaultEntry(scheduleType)),
                        "exitTime" to (template?.exitTime?.toString() ?: defaultExit(scheduleType))
                    )
                )
            }
            date = date.plusDays(1)
        }
        return grid
    }

    private fun isWorkDay(
        date: LocalDate,
        scheduleType: String?,
        template: ShiftTemplate?,
        rosterStart: LocalDate?
    ): Boolean {
        val type = scheduleType?.uppercase() ?: return true
        val anchor = rosterStart ?: date.withDayOfMonth(1)
        val daysSince = ChronoUnit.DAYS.between(anchor, date).toInt().coerceAtLeast(0)

        return when {
            type.contains("12X36") || type.contains("12X") -> daysSince % (template?.cycleDays ?: 2) == 0
            type.contains("PLANTAO") -> true
            else -> true
        }
    }

    private fun defaultEntry(scheduleType: String?) = when {
        scheduleType?.contains("12X", ignoreCase = true) == true -> "07:00"
        scheduleType?.contains("PLANTAO", ignoreCase = true) == true -> "07:00"
        else -> "08:00"
    }

    private fun defaultExit(scheduleType: String?) = when {
        scheduleType?.contains("12X", ignoreCase = true) == true -> "19:00"
        scheduleType?.contains("PLANTAO", ignoreCase = true) == true -> "07:00"
        else -> "17:00"
    }
}
