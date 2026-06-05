package com.contractops.api.time.service

import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.employee.repository.EmployeeAssignmentRepository
import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.time.repository.BancoHorasRepository
import com.contractops.api.post.domain.ServicePost
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.repository.NormalizedPunchRepository
import com.contractops.api.time.repository.RawPunchRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class AttendanceProcessingServiceTest {

    @Mock lateinit var rawPunchRepository: RawPunchRepository
    @Mock lateinit var normalizedPunchRepository: NormalizedPunchRepository
    @Mock lateinit var attendanceDayRepository: AttendanceDayRepository
    @Mock lateinit var employeeRepository: EmployeeRepository
    @Mock lateinit var servicePostRepository: ServicePostRepository
    @Mock lateinit var contractRepository: ContractRepository
    @Mock lateinit var assignmentRepository: EmployeeAssignmentRepository
    @Mock lateinit var employeePunchResolver: EmployeePunchResolver
    @Mock lateinit var regrasPontoParser: RegrasPontoParser
    @Mock lateinit var coverageService: CoverageService

    @InjectMocks lateinit var service: AttendanceProcessingService

    @Test
    fun `atraso calculado contra escala do posto`() {
        val postId = UUID.randomUUID()
        val date = LocalDate.of(2026, 5, 28)
        val post = ServicePost(
            tenantId = UUID.randomUUID(),
            contractId = UUID.randomUUID(),
            nome = "Portaria",
            escala = "08:00-17:00"
        )
        whenever(servicePostRepository.findById(postId)).thenReturn(Optional.of(post))

        val firstEntry = LocalDateTime.of(2026, 5, 28, 8, 25)
        val delay = service.calculateDelayMinutes(date, firstEntry, postId)

        assertEquals(25, delay)
    }

    @Test
    fun `sem atraso quando entrada antes da escala`() {
        val postId = UUID.randomUUID()
        val date = LocalDate.of(2026, 5, 28)
        val post = ServicePost(
            tenantId = UUID.randomUUID(),
            contractId = UUID.randomUUID(),
            nome = "Portaria",
            escala = "DIURNA"
        )
        whenever(servicePostRepository.findById(postId)).thenReturn(Optional.of(post))

        val firstEntry = LocalDateTime.of(2026, 5, 28, 7, 55)
        val delay = service.calculateDelayMinutes(date, firstEntry, postId)

        assertEquals(0, delay)
    }
}
