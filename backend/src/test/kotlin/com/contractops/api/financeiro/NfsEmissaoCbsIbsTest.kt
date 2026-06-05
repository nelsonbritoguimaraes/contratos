package com.contractops.api.financeiro

import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.nfse.NfseGatewayRouter
import com.contractops.api.financeiro.service.NfsEmissaoService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.time.LocalDate

class NfsEmissaoCbsIbsTest {

    private val service = NfsEmissaoService(FiscalProperties(), mock<NfseGatewayRouter>())

    @Test
    fun `XML inclui IBSCBS apos agosto 2026`() {
        val xml = service.gerarXmlNfs(
            numero = "1",
            tomadorCnpj = "12345678000190",
            valorServicos = BigDecimal("10000"),
            iss = BigDecimal("500"),
            outrasRetencoes = BigDecimal("200"),
            dataEmissao = LocalDate.of(2026, 8, 15)
        )
        assertTrue(xml.contains("IBSCBS"))
        assertTrue(xml.contains("gCBS"))
    }

    @Test
    fun `XML sem IBSCBS antes de agosto 2026`() {
        val xml = service.gerarXmlNfs(
            numero = "2",
            tomadorCnpj = "12345678000190",
            valorServicos = BigDecimal("10000"),
            iss = BigDecimal("500"),
            outrasRetencoes = BigDecimal("200"),
            dataEmissao = LocalDate.of(2026, 7, 31)
        )
        assertTrue(!xml.contains("IBSCBS"))
    }
}
