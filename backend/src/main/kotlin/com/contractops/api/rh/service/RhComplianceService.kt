package com.contractops.api.rh.service

import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Calendário de obrigações trabalhistas/fiscais (eSocial, FGTS Digital, DCTFWeb, EFD-Reinf).
 * Prazos: MOS eSocial S-1.3 e rotinas 2026 (dia 15 eventos periódicos; FGTS dia 20).
 */
@Service
class RhComplianceService {

    data class ObrigacaoRh(
        val tipo: String,
        val descricao: String,
        val competencia: String,
        val vencimento: LocalDate,
        val baseLegal: String,
        val integracao: String
    )

    fun calendarioCompetencia(competencia: LocalDate): List<ObrigacaoRh> {
        val comp = competencia.withDayOfMonth(1)
        val mesSeguinte = comp.plusMonths(1)
        val dia15 = proximoDiaUtil(mesSeguinte.withDayOfMonth(15))
        val dia20 = proximoDiaUtil(mesSeguinte.withDayOfMonth(20))
        val compStr = comp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))

        return listOf(
            ObrigacaoRh(
                "ESOCIAL_S1200",
                "S-1200 Remuneração RGPS",
                compStr,
                dia15,
                "MOS eSocial S-1.3 — eventos periódicos",
                "POST /api/rh/esocial/s1200/{employeeId}"
            ),
            ObrigacaoRh(
                "ESOCIAL_S1210",
                "S-1210 Pagamentos de rendimentos",
                compStr,
                dia15,
                "MOS eSocial S-1.3",
                "Gerado no fechamento via PayrollEsocialOrchestrator"
            ),
            ObrigacaoRh(
                "ESOCIAL_S1299",
                "S-1299 Fechamento eventos periódicos",
                compStr,
                dia15,
                "Último evento da competência — habilita DCTFWeb",
                "POST /api/rh/compliance/fechar-competencia"
            ),
            ObrigacaoRh(
                "FGTS_DIGITAL",
                "FGTS Digital — guia competência",
                compStr,
                dia20,
                "Lei 14.438/2022 — vencimento dia 20",
                "contractops.obligations.fgts + eSocial S-1200/S-1210"
            ),
            ObrigacaoRh(
                "EFD_REINF_R2010",
                "EFD-Reinf R-2010 — retenção INSS terceirização",
                compStr,
                dia15,
                "Retenção 11% cessão/empreitada mão de obra",
                "EfdReinfGateway"
            ),
            ObrigacaoRh(
                "DCTFWEB",
                "DCTFWeb — débitos previdenciários",
                compStr,
                dia15,
                "Integração automática pós S-1299 processado",
                "contractops.obligations.dctfweb"
            )
        )
    }

    private fun proximoDiaUtil(data: LocalDate): LocalDate {
        var d = data
        while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
            d = d.plusDays(1)
        }
        return d
    }
}
