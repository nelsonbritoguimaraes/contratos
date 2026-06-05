package com.contractops.api.financeiro

import com.contractops.api.financeiro.domain.ContaAPagar
import com.contractops.api.financeiro.service.CnabExportService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class CnabExportServiceTest {

    private val service = CnabExportService()

    @Test
    fun `gerar CNAB 240 contem header e segmentos`() {
        val conta = ContaAPagar(
            tenantId = UUID.randomUUID(),
            origem = "FORNECEDOR",
            valor = BigDecimal("1500.00"),
            vencimento = LocalDate.now().plusDays(5),
            observacoes = "Fornecedor teste"
        )
        val cnab = service.gerarCnab240Pagamentos(
            contas = listOf(conta),
            contaBancariaAgencia = "1234",
            contaBancariaConta = "56789",
            contaBancariaDv = "0",
            empresaCnpj = "12345678000190",
            empresaNome = "ContractOps",
            dataPagamento = LocalDate.now()
        )
        assertTrue(cnab.contains("REMESSA-PAGAMENTO"))
        assertTrue(cnab.lineSequence().count() >= 4)
    }
}
