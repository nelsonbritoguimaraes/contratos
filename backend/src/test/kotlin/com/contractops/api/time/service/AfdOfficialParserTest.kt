package com.contractops.api.time.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class AfdOfficialParserTest {

    private val parser = AfdOfficialParser()
    private val tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @Test
    fun `parse delimited AFD with CPF`() {
        val content = """
            1|20250609180000|12345678901|E
            2|20250609170000|12345678901|S
        """.trimIndent()
        val result = parser.parse(content, null, tenantId)
        assertEquals(2, result.punches.size)
        assertEquals("AFD_DELIMITADO", result.formato)
        assertEquals("12345678901", result.punches[0].cpf)
    }

    @Test
    fun `parse fixed width type 3 record`() {
        val line = buildString {
            append("000000001") // NSR 9
            append("3")         // tipo
            append("202506091800") // timestamp 12
            append("12345678901  ") // CPF 12
            append("E")
            while (length < 50) append(" ")
        }
        val result = parser.parse(line, null, tenantId)
        assertTrue(result.punches.isNotEmpty())
        assertEquals("AFD_ANEXO_V", result.formato)
    }
}
