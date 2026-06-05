package com.contractops.api.schedule.api

import java.time.LocalDate
import java.time.LocalTime
import java.util.*

data class CreateShiftTemplateRequest(
    val contractId: UUID? = null,
    val name: String,
    val shiftType: String,
    val workHours: Int? = null,
    val restHours: Int? = null,
    val entryTime: LocalTime? = null,
    val exitTime: LocalTime? = null,
    val cycleDays: Int = 2
)

data class CreatePostScheduleRequest(
    val contractId: UUID,
    val postId: UUID,
    val scheduleType: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null,
    val shiftTemplateId: UUID? = null,
    val notes: String? = null
)

data class CreateEmployeeRosterRequest(
    val contractId: UUID,
    val postId: UUID,
    val employeeId: UUID,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null,
    val role: String = "TITULAR",
    val postScheduleId: UUID? = null
)

typealias CreateRosterRequest = CreateEmployeeRosterRequest

data class ShiftTemplateResponse(
    val id: UUID?,
    val contractId: UUID?,
    val name: String,
    val shiftType: String,
    val workHours: Int?,
    val restHours: Int?,
    val entryTime: LocalTime?,
    val exitTime: LocalTime?,
    val cycleDays: Int,
    val isActive: Boolean
)

data class PostScheduleResponse(
    val id: UUID?,
    val contractId: UUID,
    val postId: UUID,
    val shiftTemplateId: UUID?,
    val scheduleType: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val status: String
)

data class EmployeeRosterResponse(
    val id: UUID?,
    val contractId: UUID,
    val postId: UUID,
    val employeeId: UUID,
    val role: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val status: String
)
