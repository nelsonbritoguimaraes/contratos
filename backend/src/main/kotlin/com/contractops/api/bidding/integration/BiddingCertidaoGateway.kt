package com.contractops.api.bidding.integration

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Consulta certidões de habilitação (RF, FGTS/CNDT, CNDT trabalhista).
 * Modo sandbox: validação determinística por CNPJ; produção exigiria APIs gov.br.
 */
@Component
class BiddingCertidaoGateway {

    data class CertidaoResult(
        val tipo: String,
        val orgao: String,
        val status: String,
        val validade: String?,
        val protocolo: String?,
        val mensagem: String
    )

    fun consultar(cnpj: String): List<CertidaoResult> {
        val digits = cnpj.filter { it.isDigit() }
        if (digits.length != 14) {
            return listOf(
                CertidaoResult("CNPJ", "Receita Federal", "INVALIDO", null, null, "CNPJ inválido: informe 14 dígitos")
            )
        }
        val validade = LocalDate.now().plusMonths(6).format(DateTimeFormatter.ISO_DATE)
        val checksum = digits.takeLast(2).toIntOrNull() ?: 0
        val rfOk = checksum % 3 != 0
        val fgtsOk = checksum % 5 != 0
        val cndtOk = checksum % 7 != 0

        return listOf(
            CertidaoResult(
                tipo = "CND_RF",
                orgao = "Receita Federal",
                status = if (rfOk) "REGULAR" else "PENDENTE",
                validade = if (rfOk) validade else null,
                protocolo = "RF-${digits.takeLast(6)}-${System.currentTimeMillis() % 100000}",
                mensagem = if (rfOk) "Certidão negativa de débitos (simulada)" else "Débitos em análise — regularize no e-CAC"
            ),
            CertidaoResult(
                tipo = "CRF_FGTS",
                orgao = "FGTS Digital",
                status = if (fgtsOk) "REGULAR" else "IRREGULAR",
                validade = if (fgtsOk) validade else null,
                protocolo = "FGTS-${digits.takeLast(6)}",
                mensagem = if (fgtsOk) "Certificado de Regularidade FGTS (simulado)" else "Irregularidade FGTS — verificar guias em aberto"
            ),
            CertidaoResult(
                tipo = "CNDT",
                orgao = "TST / CNDT",
                status = if (cndtOk) "REGULAR" else "PENDENTE",
                validade = if (cndtOk) validade else null,
                protocolo = "CNDT-${digits.takeLast(6)}",
                mensagem = if (cndtOk) "Certidão negativa de débitos trabalhistas (simulada)" else "Processos trabalhistas em consulta"
            ),
            CertidaoResult(
                tipo = "SICAF",
                orgao = "Compras.gov.br",
                status = if (rfOk && fgtsOk) "HABILITADO" else "VERIFICAR",
                validade = validade,
                protocolo = "SICAF-${digits.take(8)}",
                mensagem = "Situação cadastral SICAF (sandbox)"
            )
        )
    }
}
