package com.contractops.api.contabilidade

import com.contractops.api.contabilidade.service.SpedValidatorService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpedValidatorServiceTest {

    private val validator = SpedValidatorService()

    @Test
    fun `validarECD aprova arquivo com estrutura minima`() {
        val conteudo = """
            |0000|LECD|010|2025|2025|01012025|31122025|EMPRESA|11222333000181|SP|3550308||0|N|N|0|
            |0001|0|
            |I200|000001|01012025|1000,00|N|Teste|
            |I250|3.1.01|3.1.01|D|1000,00|Teste|
            |I250|2.1.01|2.1.01|C|1000,00|Teste|
            |9999|6|
        """.trimIndent()

        val result = validator.validarECD(conteudo)
        assertTrue(result.valido, result.erros.toString())
        assertTrue(result.arquivoHash.isNotBlank())
    }

    @Test
    fun `validarECD rejeita partidas desbalanceadas`() {
        val conteudo = """
            |0000|LECD|010|2025|2025|01012025|31122025|EMPRESA|11222333000181|SP|3550308||0|N|N|0|
            |I200|000001|01012025|1000,00|N|Teste|
            |I250|3.1.01|3.1.01|D|1000,00|Teste|
            |I250|2.1.01|2.1.01|C|500,00|Teste|
            |9999|5|
        """.trimIndent()

        val result = validator.validarECD(conteudo)
        assertFalse(result.valido)
        assertTrue(result.erros.any { it.contains("desbalanceadas") })
    }
}
