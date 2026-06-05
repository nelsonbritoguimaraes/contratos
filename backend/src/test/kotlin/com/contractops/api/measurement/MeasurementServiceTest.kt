package com.contractops.api.measurement

import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.glosa.service.GlosaEngine
import com.contractops.api.measurement.domain.Measurement
import com.contractops.api.measurement.repository.MeasurementRepository
import com.contractops.api.measurement.service.MeasurementService
import com.contractops.api.time.service.AttendanceProcessingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class MeasurementServiceTest {

    @Mock lateinit var repository: MeasurementRepository
    @Mock lateinit var glosaEngine: GlosaEngine
    @Mock lateinit var attendanceService: AttendanceProcessingService
    @Mock lateinit var contractRepository: ContractRepository

    @InjectMocks lateinit var service: MeasurementService

    private val tenantId = UUID.randomUUID()
    private val measurementId = UUID.randomUUID()

    @Test
    fun `approveMeasurement rejeita medição já aprovada`() {
        val measurement = Measurement(
            tenantId = tenantId,
            contractId = UUID.randomUUID(),
            period = LocalDate.now(),
            baseValue = BigDecimal("1000"),
            glosaTotal = BigDecimal.ZERO,
            finalAmount = BigDecimal("1000"),
            status = "APPROVED"
        )
        whenever(repository.findById(measurementId)).thenReturn(Optional.of(measurement))

        assertThrows(IllegalStateException::class.java) {
            service.approveMeasurement(measurementId, tenantId)
        }
    }

    @Test
    fun `approveMeasurement atualiza status para APPROVED`() {
        val measurement = Measurement(
            id = measurementId,
            tenantId = tenantId,
            contractId = UUID.randomUUID(),
            period = LocalDate.now(),
            baseValue = BigDecimal("1000"),
            glosaTotal = BigDecimal.ZERO,
            finalAmount = BigDecimal("1000"),
            status = "DRAFT"
        )
        whenever(repository.findById(measurementId)).thenReturn(Optional.of(measurement))
        whenever(repository.save(any())).thenAnswer { it.arguments[0] as Measurement }

        val result = service.approveMeasurement(measurementId, tenantId)

        assertEquals("APPROVED", result.status)
    }
}
