package com.contractops.api.time.service

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Interpreta escala do ServicePost (ex: "08:00-17:00", "DIURNA", "NOTURNA") para horário esperado de entrada.
 */
object EscalaScheduleParser {

    private val TIME_RANGE = Regex("""(\d{1,2}):(\d{2})\s*[-–]\s*(\d{1,2}):(\d{2})""")
    private val SINGLE_TIME = Regex("""(\d{1,2}):(\d{2})""")

    fun expectedEntryTime(date: LocalDate, escala: String?): LocalDateTime? {
        if (escala.isNullOrBlank()) return null
        val normalized = escala.trim().uppercase()

        val entryTime = when {
            normalized.contains("NOTUR") -> LocalTime.of(22, 0)
            normalized.contains("DIUR") || normalized.contains("COMERCIAL") || normalized.contains("08X17") ->
                LocalTime.of(8, 0)
            normalized.contains("06X") || normalized.contains("06:00") -> LocalTime.of(6, 0)
            else -> {
                TIME_RANGE.find(escala)?.let { m ->
                    LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
                } ?: SINGLE_TIME.find(escala)?.let { m ->
                    LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
                }
            }
        }
        return entryTime?.let { date.atTime(it) }
    }

    fun calculateDelayMinutes(firstEntry: LocalDateTime, expectedEntry: LocalDateTime): Int {
        if (!firstEntry.isAfter(expectedEntry)) return 0
        return java.time.Duration.between(expectedEntry, firstEntry).toMinutes().toInt()
    }
}
