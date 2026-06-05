package com.contractops.api.financeiro

import com.contractops.api.financeiro.config.NfsWorkflowProperties
import com.contractops.api.financeiro.repository.*
import com.contractops.api.financeiro.service.FinanceiroService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.util.*

class FinanceiroRetencoesTest {

    private fun service(): FinanceiroService = FinanceiroService(
        contaBancariaRepository = mock(),
        contaAReceberRepository = mock(),
        contaAPagarRepository = mock(),
        transacaoFinanceiraRepository = mock(),
        notaFiscalRepository = mock(),
        retencaoRepository = mock(),
        conciliacaoRepository = mock(),
        extratoRepository = mock(),
        previsaoRepository = mock(),
        pagamentoRepository = mock(),
        recebimentoRepository = mock(),
        fechamentoRepository = mock(),
        contabilidadeService = null,
        nfsEmissaoService = null,
        fgtsDigitalService = null,
        dctfWebService = null,
        cnabExportService = null,
        ofxExportService = null,
        financialReportService = null,
        agingReportService = null,
        taxCalendarService = null,
        bankStatementParser = null,
        nfseExtratoMatchService = null,
        nfsDanfseService = null,
        nfsOrgaoEmailService = null,
        nfsCobrancaWorkflowStarter = null,
        fgtsGateway = null,
        dctfGateway = null,
        contractRepository = null,
        cobrancaService = null,
        reinfNfsIntegrationService = null,
        auditService = null,
        tenantFiscalProfileRepository = null,
        tenantFiscalProfileService = null,
        workflowProperties = NfsWorkflowProperties()
    )

    @Test
    fun `calcularRetencoes inclui IRRF PIS COFINS CSLL ISS e INSS`() {
        val tenantId = UUID.randomUUID()
        val retencoes = service().calcularRetencoes(
            valorServico = BigDecimal("100000"),
            municipioIbge = "3550308",
            naturezaServico = "TERCEIRIZACAO",
            tenantId = tenantId,
            aplicarInss = true
        )
        val tipos = retencoes.map { it.tipo }.toSet()
        assertTrue("IRRF" in tipos)
        assertTrue("ISS" in tipos)
        assertTrue("INSS" in tipos)
        assertTrue(retencoes.sumOf { it.valorRetido } > BigDecimal.ZERO)
    }
}
