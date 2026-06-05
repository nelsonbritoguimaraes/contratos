package com.contractops.api.contabilidade.service

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.security.MessageDigest

@Service
class SpedValidatorService {

    data class ValidationResult(
        val valido: Boolean,
        val erros: List<String>,
        val avisos: List<String>,
        val totalRegistros: Int,
        val arquivoHash: String
    )

    fun validarECD(conteudo: String): ValidationResult {
        val erros = mutableListOf<String>()
        val avisos = mutableListOf<String>()
        val linhas = conteudo.lines().filter { it.isNotBlank() }

        if (linhas.isEmpty()) {
            return ValidationResult(false, listOf("Arquivo vazio"), emptyList(), 0, hash(conteudo))
        }

        linhas.forEachIndexed { idx, linha ->
            if (!linha.startsWith("|")) erros.add("Linha ${idx + 1}: deve iniciar com pipe (|)")
            if (!linha.endsWith("|")) erros.add("Linha ${idx + 1}: deve terminar com pipe (|)")
        }

        val blocos = linhas.map { parseRegistro(it) }
        val tipos = blocos.map { it.tipo }

        if (!tipos.contains("0000")) erros.add("Registro 0000 (abertura) ausente")
        if (!tipos.contains("9999")) erros.add("Registro 9999 (encerramento) ausente")
        if (!tipos.any { it.startsWith("I") }) avisos.add("Nenhum registro do bloco I (lançamentos) encontrado")

        validarPartidasDobradas(blocos, erros)

        val hash = hash(conteudo)
        return ValidationResult(
            valido = erros.isEmpty(),
            erros = erros,
            avisos = avisos,
            totalRegistros = linhas.size,
            arquivoHash = hash
        )
    }

    fun validarECF(conteudo: String): ValidationResult {
        val erros = mutableListOf<String>()
        val linhas = conteudo.lines().filter { it.isNotBlank() }
        if (linhas.none { it.contains("|0000|") }) erros.add("Registro 0000 ECF ausente")
        if (linhas.none { it.contains("|9999|") }) erros.add("Registro 9999 ECF ausente")
        return ValidationResult(erros.isEmpty(), erros, emptyList(), linhas.size, hash(conteudo))
    }

    private fun validarPartidasDobradas(blocos: List<RegistroSped>, erros: MutableList<String>) {
        val i200s = blocos.filter { it.tipo == "I200" }
        val i250s = blocos.filter { it.tipo == "I250" }

        if (i200s.isEmpty()) return

        var idx250 = 0
        i200s.forEach { i200 ->
            val valorDoc = parseValor(i200.campos.getOrNull(2))
            val partidas = mutableListOf<BigDecimal>()
            var somaD = BigDecimal.ZERO
            var somaC = BigDecimal.ZERO

            while (idx250 < i250s.size) {
                val i250 = i250s[idx250]
                val dc = i250.campos.getOrNull(2) ?: break
                val valor = parseValor(i250.campos.getOrNull(3))
                if (dc == "D") somaD = somaD.add(valor) else somaC = somaC.add(valor)
                partidas.add(valor)
                idx250++
                if (partidas.size >= 2 && somaD.compareTo(somaC) == 0) break
            }

            if (somaD.compareTo(somaC) != 0) {
                erros.add("I200 ${i200.campos.firstOrNull()}: partidas desbalanceadas D=$somaD C=$somaC (doc=$valorDoc)")
            }
        }
    }

    private fun parseRegistro(linha: String): RegistroSped {
        val partes = linha.trim('|').split("|")
        return RegistroSped(partes.firstOrNull() ?: "", partes.drop(1))
    }

    private fun parseValor(raw: String?): BigDecimal {
        if (raw.isNullOrBlank()) return BigDecimal.ZERO
        return raw.replace(",", ".").toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun hash(conteudo: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(conteudo.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private data class RegistroSped(val tipo: String, val campos: List<String>)
}
