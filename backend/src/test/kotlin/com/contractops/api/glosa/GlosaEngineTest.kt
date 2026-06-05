package com.contractops.api.glosa

import com.contractops.api.contract.domain.Contract
import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.glosa.domain.GlosaRule
import com.contractops.api.glosa.repository.GlosaRepository
import com.contractops.api.glosa.repository.GlosaRuleRepository
import com.contractops.api.glosa.repository.IMRIndicatorRepository
import com.contractops.api.glosa.service.GlosaEngine
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.domain.AttendanceDay
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.service.CoverageService
import org.junit.jupiter.api.Assertions.assertTrue
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
class GlosaEngineTest {

    @Mock lateinit var glosaRuleRepository: GlosaRuleRepository
    @Mock lateinit var imrIndicatorRepository: IMRIndicatorRepository
    @Mock lateinit var glosaRepository: GlosaRepository
    @Mock lateinit var contractRepository: ContractRepository
    @Mock lateinit var attendanceDayRepository: AttendanceDayRepository
    @Mock lateinit var coverageService: CoverageService
    @Mock lateinit var servicePostRepository: ServicePostRepository
    @Mock lateinit var volanteAssignmentRepository: VolanteAssignmentRepository

    @InjectMocks lateinit var engine: GlosaEngine

    private val tenantId = UUID.randomUUID()
    private val contractId = UUID.randomUUID()
    private val period = LocalDate.of(2025, 6, 1)

    @Test
    fun `calcula glosa FALTA com base em attendance`() {
        val contract = Contract(
            tenantId = tenantId,
            companyId = UUID.randomUUID(),
            numero = "CT-001",
            orgao = "Org",
            valorMensal = BigDecimal("30000"),
            status = "ATIVO"
        )
        whenever(contractRepository.findById(contractId)).thenReturn(Optional.of(contract))
        whenever(glosaRuleRepository.findByContractIdAndIsActiveTrue(contractId)).thenReturn(
            listOf(
                GlosaRule(
                    tenantId = tenantId,
                    contractId = contractId,
                    ruleType = "FALTA",
                    description = "Falta",
                    factor = BigDecimal.ONE,
                    isActive = true
                )
            )
        )
        whenever(servicePostRepository.findByContractId(contractId)).thenReturn(emptyList())
        whenever(attendanceDayRepository.findByTenantIdAndContractIdAndDateBetween(any(), any(), any(), any()))
            .thenReturn(
                listOf(
                    AttendanceDay(
                        tenantId = tenantId,
                        employeeId = UUID.randomUUID(),
                        contractId = contractId,
                        date = LocalDate.of(2025, 6, 5),
                        totalWorkedMinutes = 0,
                        absenceMinutes = 480
                    )
                )
            )
        whenever(coverageService.getDailyCoverageSummary(any(), any(), any()))
            .thenReturn(mapOf("coverage_percent" to 80.0))
        whenever(volanteAssignmentRepository.findByTenantIdAndContractIdAndAssignmentDateBetween(any(), any(), any(), any()))
            .thenReturn(emptyList())
        whenever(imrIndicatorRepository.findByContractIdAndIsActiveTrue(contractId)).thenReturn(emptyList())
        whenever(glosaRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = engine.calculateAndSaveGlosas(contractId, period, tenantId)

        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.glosaType == "FALTA" })
    }
}
