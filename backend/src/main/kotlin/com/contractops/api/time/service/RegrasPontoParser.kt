package com.contractops.api.time.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Interpreta Contract.regrasPonto (JSON ou chave=valor).
 * Exemplo JSON: {"toleranciaMinutos":10,"jornadaDiariaMinutos":480,"hePercentual":50,"coberturaMeta":95}
 */
@Component
class RegrasPontoParser(private val objectMapper: ObjectMapper) {

    data class RegrasPonto(
        val toleranciaMinutos: Int = 10,
        val jornadaDiariaMinutos: Int = 480,
        val hePercentual: Int = 50,
        val coberturaMeta: Int = 95,
        val intervaloMinimoMinutos: Int = 60,
        val bancoHorasAtivo: Boolean = true
    )

    fun parse(raw: String?): RegrasPonto {
        if (raw.isNullOrBlank()) return RegrasPonto()
        return try {
            if (raw.trimStart().startsWith("{")) {
                val map = objectMapper.readValue(raw, Map::class.java)
                RegrasPonto(
                    toleranciaMinutos = (map["toleranciaMinutos"] as? Number)?.toInt() ?: 10,
                    jornadaDiariaMinutos = (map["jornadaDiariaMinutos"] as? Number)?.toInt() ?: 480,
                    hePercentual = (map["hePercentual"] as? Number)?.toInt() ?: 50,
                    coberturaMeta = (map["coberturaMeta"] as? Number)?.toInt() ?: 95,
                    intervaloMinimoMinutos = (map["intervaloMinimoMinutos"] as? Number)?.toInt() ?: 60,
                    bancoHorasAtivo = map["bancoHorasAtivo"] as? Boolean ?: true
                )
            } else {
                val kv = raw.split(";").mapNotNull { part ->
                    val p = part.split("=")
                    if (p.size == 2) p[0].trim() to p[1].trim() else null
                }.toMap()
                RegrasPonto(
                    toleranciaMinutos = kv["toleranciaMinutos"]?.toIntOrNull() ?: 10,
                    jornadaDiariaMinutos = kv["jornadaDiariaMinutos"]?.toIntOrNull() ?: 480,
                    hePercentual = kv["hePercentual"]?.toIntOrNull() ?: 50,
                    coberturaMeta = kv["coberturaMeta"]?.toIntOrNull() ?: 95
                )
            }
        } catch (_: Exception) {
            RegrasPonto()
        }
    }

    fun applyTolerancia(delayMinutes: Int, regras: RegrasPonto): Int =
        (delayMinutes - regras.toleranciaMinutos).coerceAtLeast(0)

    fun valorHora(baseSalary: BigDecimal): BigDecimal =
        baseSalary.divide(BigDecimal("220"), 4, java.math.RoundingMode.HALF_UP)
}
