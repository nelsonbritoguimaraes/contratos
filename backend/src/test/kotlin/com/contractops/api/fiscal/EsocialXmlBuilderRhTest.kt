package com.contractops.api.fiscal

import com.contractops.api.employee.domain.Employee
import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.esocial.EsocialXmlBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class EsocialXmlBuilderRhTest {

    private val builder = EsocialXmlBuilder(FiscalProperties())

    private fun sampleEmployee() = Employee(
        tenantId = UUID.randomUUID(),
        companyId = UUID.randomUUID(),
        fullName = "João Silva",
        cpf = "12345678901",
        matricula = "MAT001",
        cargo = "Vigilante",
        salaryBase = BigDecimal("3500.00")
    )

    @Test
    fun `S1200 inclui rubricas e competencia`() {
        val xml = builder.buildS1200(
            "12345678901",
            LocalDate.of(2025, 6, 1),
            listOf("SALARIO_BASE" to "3500.00", "INSS" to "385.00")
        )
        assertTrue(xml.contains("evtRemun"))
        assertTrue(xml.contains("2025-06"))
        assertTrue(xml.contains("SALARIO_BASE"))
    }

    @Test
    fun `S1299 fechamento periodico`() {
        val xml = builder.buildS1299(LocalDate.of(2025, 6, 1))
        assertTrue(xml.contains("evtFechaEvPer"))
        assertTrue(xml.contains("2025-06"))
    }

    @Test
    fun `S1210 pagamento`() {
        val xml = builder.buildS1210("12345678901", LocalDate.of(2025, 6, 1), "3200.00", LocalDate.of(2025, 7, 5))
        assertTrue(xml.contains("evtPgtos"))
        assertTrue(xml.contains("3200.00"))
    }

    @Test
    fun `S2205 alteracao cadastral`() {
        val xml = builder.buildS2205(sampleEmployee())
        assertTrue(xml.contains("evtAltCadastral"))
        assertTrue(xml.contains("João Silva"))
    }

    @Test
    fun `S2206 alteracao contratual`() {
        val xml = builder.buildS2206(sampleEmployee(), "5200.00", "Supervisor")
        assertTrue(xml.contains("evtAltContratual"))
        assertTrue(xml.contains("5200.00"))
    }

    @Test
    fun `S2230 afastamento`() {
        val xml = builder.buildS2230(sampleEmployee(), "01", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15))
        assertTrue(xml.contains("evtAfastTemp"))
        assertTrue(xml.contains("2026-06-01"))
    }

    @Test
    fun `S2240 condicoes ambientais`() {
        val xml = builder.buildS2240(sampleEmployee(), "INSALUBRIDADE", "MEDIO", LocalDate.of(2026, 1, 1))
        assertTrue(xml.contains("evtExpRisco"))
        assertTrue(xml.contains("INSALUBRIDADE"))
    }

    @Test
    fun `S2299 desligamento`() {
        val xml = builder.buildS2299(sampleEmployee(), LocalDate.of(2026, 5, 15), "11")
        assertTrue(xml.contains("evtDeslig"))
        assertTrue(xml.contains("2026-05-15"))
    }
}

