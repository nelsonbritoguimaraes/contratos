package com.contractops.api.rh.service

import com.contractops.api.employee.domain.Employee
import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.rh.domain.EsocialEvent
import com.contractops.api.rh.domain.Payslip
import com.contractops.api.rh.repository.EsocialEventRepository
import com.contractops.api.rh.repository.PayslipRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PayrollFiscalDivergenceServiceTest {

    @Mock lateinit var payslipRepository: PayslipRepository
    @Mock lateinit var esocialEventRepository: EsocialEventRepository
    @Mock lateinit var employeeRepository: EmployeeRepository

    @InjectMocks lateinit var service: PayrollFiscalDivergenceService

    @Test
    fun `analisar detecta holerite sem S-1200 como critico`() {
        val tenantId = UUID.randomUUID()
        val employeeId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val comp = LocalDate.of(2025, 6, 1)

        val payslip = Payslip(
            tenantId = tenantId,
            employeeId = employeeId,
            contractId = contractId,
            competence = comp,
            totalEarnings = BigDecimal("5000.00"),
            netAmount = BigDecimal("4200.00"),
            status = "APPROVED"
        )

        whenever(payslipRepository.findByTenantIdAndCompetenceBetween(any(), any(), any()))
            .thenReturn(listOf(payslip))
        whenever(esocialEventRepository.findByTenantIdAndStatus(tenantId, "GENERATED")).thenReturn(emptyList())
        whenever(esocialEventRepository.findByTenantIdAndStatus(tenantId, "SENT")).thenReturn(emptyList())
        whenever(esocialEventRepository.findByTenantIdAndStatus(tenantId, "PENDING")).thenReturn(emptyList())
        whenever(employeeRepository.findById(employeeId)).thenReturn(
            Optional.of(
                Employee(
                    tenantId = tenantId,
                    companyId = UUID.randomUUID(),
                    fullName = "João Silva",
                    cpf = "12345678901"
                )
            )
        )

        val rel = service.analisar(tenantId, comp)

        assertEquals("CRITICO", rel.statusGeral)
        assertTrue(rel.linhas.any { it.indicador == "ESOCIAL_S1200" && it.severidade == "CRITICO" })
        assertTrue(rel.linhas.any { it.indicador == "S1299_FECHAMENTO" })
    }

    @Test
    fun `analisar alinha folha e S-1200 quando valores iguais`() {
        val tenantId = UUID.randomUUID()
        val employeeId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val comp = LocalDate.of(2025, 6, 1)

        val payslip = Payslip(
            tenantId = tenantId,
            employeeId = employeeId,
            contractId = contractId,
            competence = comp,
            totalEarnings = BigDecimal("3000.00"),
            netAmount = BigDecimal("2500.00"),
            status = "APPROVED"
        )

        val s1200 = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S1200",
            competence = comp,
            payload = "| XML: <vrRubr>3000.00</vrRubr>",
            status = "GENERATED"
        )
        val s1299 = EsocialEvent(
            tenantId = tenantId,
            employeeId = null,
            eventType = "S1299",
            competence = comp,
            payload = "fechamento",
            status = "GENERATED"
        )

        whenever(payslipRepository.findByTenantIdAndCompetenceBetween(any(), any(), any()))
            .thenReturn(listOf(payslip))
        whenever(esocialEventRepository.findByTenantIdAndStatus(tenantId, "GENERATED"))
            .thenReturn(listOf(s1200, s1299))
        whenever(esocialEventRepository.findByTenantIdAndStatus(tenantId, "SENT")).thenReturn(emptyList())
        whenever(esocialEventRepository.findByTenantIdAndStatus(tenantId, "PENDING")).thenReturn(emptyList())
        whenever(employeeRepository.findById(employeeId)).thenReturn(
            Optional.of(
                Employee(
                    tenantId = tenantId,
                    companyId = UUID.randomUUID(),
                    fullName = "Maria",
                    cpf = "98765432100"
                )
            )
        )

        val rel = service.analisar(tenantId, comp)

        assertEquals("OK", rel.statusGeral)
        assertTrue(rel.linhas.any { it.indicador == "REMUNERACAO_BRUTA" && it.severidade == "OK" })
        assertEquals(true, rel.resumo["s1299Presente"])
    }
}
