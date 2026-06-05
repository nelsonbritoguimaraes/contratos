package com.contractops.api.financeiro.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class BankStatementParserServiceTest {

    private lateinit var parser: BankStatementParserService
    private val tenantId = UUID.randomUUID()
    private val contaId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        parser = BankStatementParserService()
    }

    @Test
    fun `parseMonetaryAmount suporta BR e US`() {
        assertEquals(0, BigDecimal("4523.00").compareTo(parser.parseMonetaryAmount("4523,00")))
        assertEquals(0, BigDecimal("4523.00").compareTo(parser.parseMonetaryAmount("4523.00")))
        assertEquals(0, BigDecimal("-1342.80").compareTo(parser.parseMonetaryAmount("-1342.80")))
    }

    @Test
    fun `deve parsear OFX simples com transacoes de credito e debito`() {
        val ofxContent = """
            <OFX>
              <BANKMSGSRSV1>
                <STMTTRNRS>
                  <STMTRS>
                    <BANKTRANLIST>
                      <STMTTRN>
                        <TRNTYPE>CREDIT</TRNTYPE>
                        <DTPOSTED>20250605</DTPOSTED>
                        <TRNAMT>4523.00</TRNAMT>
                        <FITID>202506050001</FITID>
                        <MEMO>RECEBIMENTO CONTRATO 2024/001 - Medição Maio</MEMO>
                      </STMTTRN>
                      <STMTTRN>
                        <TRNTYPE>DEBIT</TRNTYPE>
                        <DTPOSTED>20250608</DTPOSTED>
                        <TRNAMT>-1342.80</TRNAMT>
                        <FITID>202506080045</FITID>
                        <MEMO>PAGAMENTO FOLHA COMPETÊNCIA 05/2025</MEMO>
                      </STMTTRN>
                    </BANKTRANLIST>
                  </STMTRS>
                </STMTTRNRS>
              </BANKMSGSRSV1>
            </OFX>
        """.trimIndent()

        val result = parser.parseOfx(ofxContent, contaId, tenantId)

        assertEquals(2, result.size)

        val credito = result.first { it.tipo == "CREDITO" }
        assertEquals(LocalDate.of(2025, 6, 5), credito.data)
        assertEquals(0, BigDecimal("4523.00").compareTo(credito.valor))
        assertEquals("202506050001", credito.documento)
        assertTrue(credito.historico.contains("RECEBIMENTO CONTRATO"))

        val debito = result.first { it.tipo == "DEBITO" }
        assertEquals(LocalDate.of(2025, 6, 8), debito.data)
        assertEquals(0, BigDecimal("1342.80").compareTo(debito.valor))
    }

    @Test
    fun `deve lidar com OFX sem TRNTYPE usando sinal do valor`() {
        val ofx = """<STMTTRN><DTPOSTED>20250610</DTPOSTED><TRNAMT>350.50</TRNAMT><MEMO>PIX RECEBIDO</MEMO></STMTTRN>"""

        val result = parser.parseOfx(ofx, contaId, tenantId)
        assertEquals(1, result.size)
        assertEquals("CREDITO", result[0].tipo)
        assertEquals(0, BigDecimal("350.50").compareTo(result[0].valor))
    }

    @Test
    fun `deve parsear CSV com cabecalho em portugues`() {
        val csv = """
            Data;Documento;Historico;Valor;Tipo
            05/06/2025;PIX-78432;RECEBIMENTO CONTRATO;4523,00;CREDITO
            08/06/2025;TED-99102;PAGAMENTO FOLHA;-1342,80;DEBITO
        """.trimIndent()

        val result = parser.parseCsv(csv, contaId, tenantId)

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2025, 6, 5), result[0].data)
        assertEquals(0, BigDecimal("4523.00").compareTo(result[0].valor))
        assertEquals("CREDITO", result[0].tipo)
    }

    @Test
    fun `deve parsear CSV detectando tipo pelo sinal quando coluna Tipo ausente`() {
        val csv = """
            Data,Historico,Valor
            2025-06-10,Recebimento cliente,1250.00
            2025-06-11,Pagamento fornecedor,-780.50
        """.trimIndent()

        val result = parser.parseCsv(csv, contaId, tenantId)

        assertEquals(2, result.size)
        assertEquals("CREDITO", result[0].tipo)
        assertEquals("DEBITO", result[1].tipo)
    }

    @Test
    fun `deve processar arquivo OFX via MultipartFile`() {
        val ofx = """<STMTTRN><DTPOSTED>20250615</DTPOSTED><TRNAMT>999.99</TRNAMT><MEMO>TESTE UPLOAD</MEMO></STMTTRN>"""
        val file = MockMultipartFile(
            "file", "extrato.ofx", "text/plain", ofx.toByteArray()
        )

        val result = parser.parse(file, contaId, tenantId)

        assertEquals(1, result.size)
        assertEquals("CREDITO", result[0].tipo)
        assertEquals(0, BigDecimal("999.99").compareTo(result[0].valor))
    }

    @Test
    fun `deve rejeitar formato desconhecido`() {
        val file = MockMultipartFile("file", "extrato.txt", "text/plain", "conteudo invalido".toByteArray())

        assertThrows(IllegalArgumentException::class.java) {
            parser.parse(file, contaId, tenantId)
        }
    }
}
