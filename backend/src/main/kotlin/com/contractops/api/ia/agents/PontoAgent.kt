package com.contractops.api.ia.agents

import com.contractops.api.time.service.AttendanceProcessingService
import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class PontoAgent(
    private val orchestrator: AiOrchestrator,
    private val attendanceProcessingService: AttendanceProcessingService
) {

    fun analisarCobertura(
        contratoId: String,
        resumoDiario: String,
        metaCobertura: Double
    ): String {
        val prompt = """
            Você é especialista em apuração de ponto e cobertura de postos em contratos de mão de obra exclusiva.
            Contrato: $contratoId | Meta: $metaCobertura%
            Dados: $resumoDiario
            Identifique problemas (faltas, atrasos, postos descobertos, volantes) e sugira 3 ações operacionais.
        """.trimIndent()
        return orchestrator.execute(prompt)
    }

    fun analisarContratoReal(contractId: UUID, date: LocalDate, tenantId: UUID, meta: Double = 95.0): String {
        val summary = attendanceProcessingService.getDailyCoverageSummary(contractId, date, tenantId)
        val volantes = attendanceProcessingService.detectVolantes(tenantId, contractId, date)
        val resumo = """
            Cobertura: ${summary["coverage_percent"]}%
            Postos com trabalho: ${summary["posts_with_work"]}/${summary["total_expected_posts"]}
            Minutos trabalhados: ${summary["total_worked_minutes"]}
            Volantes ausentes: ${volantes.size}
        """.trimIndent()
        return analisarCobertura(contractId.toString(), resumo, meta)
    }
}
