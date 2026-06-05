package com.contractops.api.rh.service

import com.contractops.api.financeiro.service.DctfWebGateway
import com.contractops.api.financeiro.service.FgtsDigitalGateway
import com.contractops.api.fiscal.efdreinf.EfdReinfGateway
import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.rh.repository.PayslipRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Orquestra fechamento de competência: eSocial (S-1200, S-1210, S-1299) + FGTS Digital + EFD-Reinf + DCTFWeb.
 * SPEC §19.5–19.6, §28.4 — prazos legais: eventos periódicos até dia 15; FGTS até dia 20.
 */
@Service
class PayrollEsocialOrchestratorService(
    private val payslipRepository: PayslipRepository,
    private val esocialService: EsocialService,
    private val fgtsGateway: FgtsDigitalGateway,
    private val dctfGateway: DctfWebGateway,
    private val efdReinfGateway: EfdReinfGateway,
    private val fiscalProperties: FiscalProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun fecharCompetenciaFiscal(
        tenantId: UUID,
        competencia: LocalDate,
        contractId: UUID? = null,
        transmitirAutomatico: Boolean = false
    ): Map<String, Any> {
        val comp = competencia.withDayOfMonth(1)
        val payslips = payslipRepository.findByTenantIdAndCompetenceBetween(
            tenantId,
            comp,
            comp.withDayOfMonth(comp.lengthOfMonth())
        ).filter {
            (it.status == "APPROVED" || it.status == "EXPORTED") &&
                (contractId == null || it.contractId == contractId)
        }

        val eventosS1200 = mutableListOf<UUID>()
        payslips.forEach { ps ->
            val ev = esocialService.generateS1200Remuneracao(ps.employeeId, tenantId, comp)
            eventosS1200 += ev.id!!
            if (ps.netAmount != null && ps.netAmount!! > BigDecimal.ZERO) {
                esocialService.generateS1210PagamentoColaborador(
                    employeeId = ps.employeeId,
                    tenantId = tenantId,
                    competencia = comp,
                    valorLiquido = ps.netAmount!!,
                    dataPagamento = comp.plusMonths(1).withDayOfMonth(5)
                )
            }
        }

        val s1299 = esocialService.generateS1299FechamentoPeriodico(comp, tenantId)
        val s1210Consolidado = esocialService.generateS1210Pagamento(comp, tenantId)

        val totalBruto = payslips.sumOf { it.totalEarnings ?: BigDecimal.ZERO }
        val totalLiquido = payslips.sumOf { it.netAmount ?: BigDecimal.ZERO }
        val totalFgts = totalBruto.multiply(BigDecimal("0.08"))
        val retencaoInssTerceirizacao = totalBruto.multiply(BigDecimal("0.11"))

        val cnpj = fiscalProperties.esocial.employerCnpj
        val fgts = fgtsGateway.transmitGuia(comp, totalFgts, cnpj)
        val dctf = dctfGateway.transmitDeclaracao(comp, cnpj, retencaoInssTerceirizacao)
        val reinf = efdReinfGateway.transmitR2010Servicos(
            competencia = comp,
            cnpjPrestador = cnpj,
            cnpjTomador = cnpj,
            valorServicos = totalBruto,
            valorRetencaoInss = retencaoInssTerceirizacao
        )

        if (transmitirAutomatico) {
            (eventosS1200 + listOf(s1299.id!!, s1210Consolidado.id!!)).forEach { id ->
                try {
                    esocialService.transmitEvent(id, tenantId)
                } catch (ex: Exception) {
                    log.warn("Falha transmissão eSocial {}: {}", id, ex.message)
                }
            }
        }

        return mapOf(
            "competencia" to comp.toString(),
            "holeritesProcessados" to payslips.size,
            "eventosS1200" to eventosS1200.size,
            "eventoS1299Id" to (s1299.id?.toString() ?: ""),
            "eventoS1210Id" to (s1210Consolidado.id?.toString() ?: ""),
            "totalBruto" to totalBruto,
            "totalLiquido" to totalLiquido,
            "fgts" to mapOf("protocolo" to fgts.protocol, "modo" to fgts.mode),
            "dctfWeb" to mapOf("protocolo" to dctf.protocol, "modo" to dctf.mode),
            "efdReinfR2010" to mapOf("protocolo" to reinf.protocol, "modo" to reinf.mode),
            "transmitido" to transmitirAutomatico
        )
    }
}
