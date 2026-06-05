package com.contractops.api.time

import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.post.repository.ServicePostRepository
import com.contractops.api.time.repository.AttendanceDayRepository
import com.contractops.api.time.repository.BancoHorasRepository
import com.contractops.api.fiscal.crypto.CadesSignatureService
import com.contractops.api.time.service.AejExportService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class AejExportServiceTest {

    @Mock lateinit var attendanceDayRepository: AttendanceDayRepository
    @Mock lateinit var servicePostRepository: ServicePostRepository
    @Mock lateinit var employeeRepository: EmployeeRepository
    @Mock lateinit var bancoHorasRepository: BancoHorasRepository
    @Mock lateinit var cadesSignatureService: CadesSignatureService

    @InjectMocks lateinit var service: AejExportService

    @Test
    fun `generateAej inclui cabecalho e assinatura stub Portaria 671`() {
        val tenantId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val period = LocalDate.of(2025, 6, 1)

        whenever(attendanceDayRepository.findByTenantIdAndContractIdAndDateBetween(any(), any(), any(), any()))
            .thenReturn(emptyList())
        whenever(servicePostRepository.findByContractId(contractId)).thenReturn(emptyList())
        whenever(cadesSignatureService.signDetached(any(), any())).thenReturn(
            CadesSignatureService.CadesResult("stub-base64", "STUB", "aej.p7s.stub")
        )

        val content = service.generateAej(contractId, period, tenantId)

        assertTrue(content.startsWith("01|1|2|"))
        assertTrue(content.contains("#P7S="))
        assertTrue(content.contains("#P7S_FILE=aej.p7s.stub"))
    }

    @Test
    fun `generateAejP7s retorna arquivo detached`() {
        val tenantId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val period = LocalDate.of(2025, 6, 1)

        whenever(attendanceDayRepository.findByTenantIdAndContractIdAndDateBetween(any(), any(), any(), any()))
            .thenReturn(emptyList())
        whenever(servicePostRepository.findByContractId(contractId)).thenReturn(emptyList())
        whenever(cadesSignatureService.signDetached(any(), any())).thenReturn(
            CadesSignatureService.CadesResult("c2FtcGxl", "ICP_BRASIL", "aej_test.p7s")
        )

        val p7s = service.generateAejP7s(contractId, period, tenantId)

        assertTrue(p7s.filename.endsWith(".p7s"))
        assertTrue(p7s.signatureBase64.isNotBlank())
    }
}
