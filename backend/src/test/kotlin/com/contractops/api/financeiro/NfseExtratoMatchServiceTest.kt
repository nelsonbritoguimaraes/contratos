package com.contractops.api.financeiro

import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import com.contractops.api.financeiro.domain.NotaFiscalServico
import com.contractops.api.financeiro.service.NfseExtratoMatchService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class NfseExtratoMatchServiceTest {

    private val service = NfseExtratoMatchService()
    private val tenantId = UUID.randomUUID()

    @Test
    fun `match NFS-e por valor e numero no historico`() {
        val nf = NotaFiscalServico(
            tenantId = tenantId,
            numero = "45892",
            dataEmissao = LocalDate.of(2025, 6, 5),
            tomadorCnpj = "12345678000190",
            valorServicos = BigDecimal("485000"),
            valorLiquido = BigDecimal("452250"),
            issRetido = BigDecimal("10000"),
            codigoVerificacao = "ABCD1234"
        )
        val extrato = ExtratoBancarioItem(
            tenantId = tenantId,
            contaBancariaId = UUID.randomUUID(),
            data = LocalDate.of(2025, 6, 15),
            historico = "CREDITO NFS 45892 ORGAO",
            valor = BigDecimal("452250"),
            tipo = "CREDITO"
        )
        val match = service.matchExtratoToNfse(extrato, listOf(nf))
        assertTrue(match != null)
        assertTrue(match!!.confidence >= BigDecimal("70"))
    }
}
