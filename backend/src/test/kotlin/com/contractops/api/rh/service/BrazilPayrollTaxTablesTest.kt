package com.contractops.api.rh.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BrazilPayrollTaxTablesTest {

    @Test
    fun `INSS 2026 progressivo na primeira faixa`() {
        val inss = BrazilPayrollTaxTables.calculateInss2026(BigDecimal("1500.00"))
        assertEquals(BigDecimal("112.50"), inss)
    }

    @Test
    fun `INSS 2026 progressivo multi-faixa`() {
        val inss = BrazilPayrollTaxTables.calculateInss2026(BigDecimal("5000.00"))
        assertTrue(inss > BigDecimal("400.00"))
        assertTrue(inss < BigDecimal("700.00"))
    }

    @Test
    fun `INSS 2026 respeita teto`() {
        val inss = BrazilPayrollTaxTables.calculateInss2026(BigDecimal("20000.00"))
        val teto = BrazilPayrollTaxTables.calculateInss2026(BigDecimal("8157.41"))
        assertEquals(0, inss.compareTo(teto))
    }

    @Test
    fun `IRRF 2026 isento abaixo do limite`() {
        val irrf = BrazilPayrollTaxTables.calculateIrrf2026(
            grossEarnings = BigDecimal("2000.00"),
            inss = BigDecimal("150.00")
        )
        assertEquals(BigDecimal("0.00"), irrf)
    }

    @Test
    fun `IRRF 2026 faixa intermediaria`() {
        val inss = BrazilPayrollTaxTables.calculateInss2026(BigDecimal("5000.00"))
        val irrf = BrazilPayrollTaxTables.calculateIrrf2026(
            grossEarnings = BigDecimal("5000.00"),
            inss = inss
        )
        assertTrue(irrf > BigDecimal.ZERO)
    }
}
