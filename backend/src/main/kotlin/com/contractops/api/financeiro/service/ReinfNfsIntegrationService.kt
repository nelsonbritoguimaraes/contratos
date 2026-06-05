package com.contractops.api.financeiro.service

import com.contractops.api.fiscal.efdreinf.EfdReinfGateway
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class ReinfNfsIntegrationService(
    private val efdReinfGateway: EfdReinfGateway
) {
    fun transmitirRetencoesNfs(
        competencia: LocalDate,
        cnpjPrestador: String,
        cnpjTomador: String,
        valorServicos: BigDecimal,
        valorRetencaoInss: BigDecimal
    ): String? {
        if (valorRetencaoInss <= BigDecimal.ZERO || cnpjTomador.isBlank() || cnpjTomador.all { it == '0' }) {
            return null
        }
        val result = efdReinfGateway.transmitR2010Servicos(
            competencia = competencia,
            cnpjPrestador = cnpjPrestador.filter { it.isDigit() },
            cnpjTomador = cnpjTomador.filter { it.isDigit() },
            valorServicos = valorServicos,
            valorRetencaoInss = valorRetencaoInss
        )
        return if (result.success) result.protocol else null
    }
}
