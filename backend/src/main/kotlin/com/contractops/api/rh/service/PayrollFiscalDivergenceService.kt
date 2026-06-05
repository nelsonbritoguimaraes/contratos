package com.contractops.api.rh.service

import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.rh.repository.EsocialEventRepository
import com.contractops.api.rh.repository.PayslipRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID
import java.util.regex.Pattern

/**
 * Compara folha de pagamento × eventos eSocial (S-1200) × FGTS esperado (8%).
 * SPEC §19.5–19.6 — apontar divergências antes do fechamento S-1299 / DCTFWeb.
 */
@Service
class PayrollFiscalDivergenceService(
    private val payslipRepository: PayslipRepository,
    private val esocialEventRepository: EsocialEventRepository,
    private val employeeRepository: EmployeeRepository
) {

    data class LinhaDivergencia(
        val employeeId: UUID?,
        val colaborador: String?,
        val indicador: String,
        val valorFolha: BigDecimal,
        val valorEsocial: BigDecimal?,
        val valorFgtsEsperado: BigDecimal?,
        val diferenca: BigDecimal,
        val severidade: String,
        val mensagem: String
    )

    data class RelatorioDivergencias(
        val competencia: String,
        val resumo: Map<String, Any>,
        val linhas: List<LinhaDivergencia>,
        val statusGeral: String
    )

    fun analisar(tenantId: UUID, competencia: LocalDate, contractId: UUID? = null): RelatorioDivergencias {
        val comp = competencia.withDayOfMonth(1)
        val fim = comp.withDayOfMonth(comp.lengthOfMonth())

        val payslips = payslipRepository.findByTenantIdAndCompetenceBetween(tenantId, comp, fim)
            .filter { contractId == null || it.contractId == contractId }
            .filter { it.status == "APPROVED" || it.status == "EXPORTED" || it.status == "DRAFT" }

        val eventos = esocialEventRepository.findByTenantIdAndStatus(tenantId, "GENERATED") +
            esocialEventRepository.findByTenantIdAndStatus(tenantId, "SENT") +
            esocialEventRepository.findByTenantIdAndStatus(tenantId, "PENDING")
        val s1200PorEmployee = eventos
            .filter { it.eventType == "S1200" && it.competence?.withDayOfMonth(1) == comp }
            .groupBy { it.employeeId }

        val linhas = mutableListOf<LinhaDivergencia>()

        payslips.forEach { ps ->
            val emp = employeeRepository.findById(ps.employeeId).orElse(null)
            val nome = emp?.fullName
            val bruto = ps.totalEarnings ?: BigDecimal.ZERO
            val liquido = ps.netAmount ?: BigDecimal.ZERO
            val fgtsEsperado = bruto.multiply(BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP)

            val evento = s1200PorEmployee[ps.employeeId]?.firstOrNull()
            val valorEsocial = evento?.let { extrairTotalRemuneracao(it.payload) }

            if (evento == null) {
                linhas += LinhaDivergencia(
                    ps.employeeId, nome, "ESOCIAL_S1200",
                    bruto, null, fgtsEsperado, bruto,
                    "CRITICO", "Holerite sem evento S-1200 gerado para a competência"
                )
            } else if (valorEsocial != null) {
                val diff = bruto.subtract(valorEsocial).abs()
                val sev = when {
                    diff <= BigDecimal("0.01") -> "OK"
                    diff <= bruto.multiply(BigDecimal("0.01")) -> "ALERTA"
                    else -> "CRITICO"
                }
                linhas += LinhaDivergencia(
                    ps.employeeId, nome, "REMUNERACAO_BRUTA",
                    bruto, valorEsocial, fgtsEsperado, diff, sev,
                    if (sev == "OK") "Folha alinhada ao S-1200" else "Diferença folha vs S-1200: R$ $diff"
                )
            }

            linhas += LinhaDivergencia(
                ps.employeeId, nome, "FGTS_8PCT",
                fgtsEsperado, valorEsocial?.multiply(BigDecimal("0.08"))?.setScale(2, RoundingMode.HALF_UP),
                fgtsEsperado, BigDecimal.ZERO, "OK",
                "FGTS esperado 8% sobre bruto R$ $bruto"
            )

            linhas += LinhaDivergencia(
                ps.employeeId, nome, "LIQUIDO",
                liquido, null, null, BigDecimal.ZERO,
                if (liquido > BigDecimal.ZERO) "OK" else "ALERTA",
                "Líquido holerite R$ $liquido"
            )
        }

        // Holerites faltantes: S-1200 sem payslip
        s1200PorEmployee.keys.filter { eid -> payslips.none { it.employeeId == eid } }.forEach { eid ->
            val emp = employeeRepository.findById(eid!!).orElse(null)
            linhas += LinhaDivergencia(
                eid, emp?.fullName, "ESOCIAL_ORFAO",
                BigDecimal.ZERO, extrairTotalRemuneracao(s1200PorEmployee[eid]?.first()?.payload),
                null, BigDecimal.ZERO, "CRITICO",
                "S-1200 existente sem holerite correspondente"
            )
        }

        val s1299 = eventos.any { it.eventType == "S1299" && it.competence?.withDayOfMonth(1) == comp }
        if (payslips.isNotEmpty() && !s1299) {
            linhas += LinhaDivergencia(
                null, null, "S1299_FECHAMENTO",
                BigDecimal.ZERO, null, null, BigDecimal.ZERO, "ALERTA",
                "Competência sem S-1299 (fechamento periódico) — DCTFWeb não será alimentada"
            )
        }

        val criticos = linhas.count { it.severidade == "CRITICO" }
        val alertas = linhas.count { it.severidade == "ALERTA" }
        val statusGeral = when {
            criticos > 0 -> "CRITICO"
            alertas > 0 -> "ALERTA"
            else -> "OK"
        }

        return RelatorioDivergencias(
            competencia = comp.toString(),
            resumo = mapOf(
                "holerites" to payslips.size,
                "eventosS1200" to s1200PorEmployee.size,
                "criticos" to criticos,
                "alertas" to alertas,
                "s1299Presente" to s1299
            ),
            linhas = linhas,
            statusGeral = statusGeral
        )
    }

    private fun extrairTotalRemuneracao(payload: String?): BigDecimal? {
        if (payload.isNullOrBlank()) return null
        val xmlPart = payload.substringAfter("| XML: ", payload)
        val pattern = Pattern.compile("<vrRubr>([\\d.]+)</vrRubr>")
        val matcher = pattern.matcher(xmlPart)
        var total = BigDecimal.ZERO
        var found = false
        while (matcher.find()) {
            found = true
            total = total.add(BigDecimal(matcher.group(1)))
        }
        if (found) return total.setScale(2, RoundingMode.HALF_UP)
        val jsonMatch = Regex(""""valorServico"\s*:\s*([\d.]+)""").find(payload)
        return jsonMatch?.groupValues?.get(1)?.let { BigDecimal(it) }
    }
}
