package com.contractops.api.contabilidade.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.contabilidade.domain.ContaContabil
import com.contractops.api.contabilidade.domain.LancamentoContabil
import com.contractops.api.contabilidade.service.ContabilidadeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/contabilidade")
class ContabilidadeController(
    private val service: ContabilidadeService,
    private val exportService: com.contractops.api.contabilidade.service.ExportService,
    private val encerramentoExercicioService: com.contractops.api.contabilidade.service.EncerramentoExercicioService,
    private val conciliacaoContabilService: com.contractops.api.contabilidade.service.ConciliacaoContabilService
) {

    // ==================== PLANO DE CONTAS ====================

    @GetMapping("/contas")
    fun listarContas(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<ContaContabil>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.findContasAtivas(effectiveTenant))
    }

    @PostMapping("/contas")
    fun criarConta(
        @RequestBody request: CriarContaRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContaContabil> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val conta = ContaContabil(
            tenantId = effectiveTenant,
            codigo = request.codigo,
            descricao = request.descricao,
            tipo = request.tipo,
            natureza = request.natureza,
            contaMae = request.contaMaeId?.let { service.findContaPorCodigo(it, effectiveTenant) }, // simplificado
            nivel = request.nivel,
            aceitaLancamento = request.aceitaLancamento
        )

        return ResponseEntity.ok(service.criarConta(conta))
    }

    // ==================== LANÇAMENTOS ====================

    @PostMapping("/lancamentos")
    fun lancar(
        @RequestBody request: LancarRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<LancamentoContabil> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val lineInputs = request.lines?.map {
            com.contractops.api.contabilidade.service.LancamentoLineInput(
                contaId = it.contaId,
                natureza = it.natureza,
                valor = it.valor,
                historico = it.historico
            )
        }
        val lancamento = service.lancar(
            tenantId = effectiveTenant,
            data = request.data,
            contaDebitoId = request.contaDebitoId,
            contaCreditoId = request.contaCreditoId,
            valor = request.valor,
            historico = request.historico,
            origemTipo = request.origemTipo,
            origemId = request.origemId,
            contratoId = request.contratoId,
            costCenterId = request.costCenterId,
            branchId = request.branchId,
            lines = lineInputs
        )

        return ResponseEntity.ok(lancamento)
    }

    @GetMapping("/lancamentos")
    fun listarLancamentos(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) origem: String?,
        @RequestParam(required = false) origemTipo: String?,
        @RequestParam(required = false) origemId: UUID?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<LancamentoContabil>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)
        val tipo = origemTipo ?: origem

        return ResponseEntity.ok(
            service.buscarLancamentosPorPeriodo(effectiveTenant, dataInicio, dataFim, tipo, origemId)
        )
    }

    // ==================== LANÇAMENTOS AUTOMÁTICOS (para teste e integração) ====================

    @PostMapping("/lancamentos/folha/{payslipId}")
    fun lancarFolha(
        @PathVariable payslipId: UUID,
        @RequestParam tenantId: UUID,
        @RequestParam contratoId: UUID,
        @RequestParam valorTotal: BigDecimal
    ): ResponseEntity<String> {
        service.lancarFolhaAprovada(payslipId, tenantId, contratoId, valorTotal)
        return ResponseEntity.ok("Lançamentos da folha gerados com sucesso.")
    }

    @PostMapping("/lancamentos/medicao/{measurementId}")
    fun lancarMedicao(
        @PathVariable measurementId: UUID,
        @RequestParam tenantId: UUID,
        @RequestParam contratoId: UUID,
        @RequestParam valorLiquido: BigDecimal
    ): ResponseEntity<String> {
        service.lancarMedicaoAprovada(measurementId, tenantId, contratoId, valorLiquido)
        return ResponseEntity.ok("Lançamento da medição gerado com sucesso.")
    }

    @PostMapping("/lancamentos/provisao-rh")
    fun lancarProvisaoRH(
        @RequestParam tenantId: UUID,
        @RequestParam contratoId: UUID,
        @RequestParam tipoProvisao: String,
        @RequestParam valor: BigDecimal,
        @RequestParam competencia: String
    ): ResponseEntity<String> {
        val comp = LocalDate.parse(competencia)
        service.lancarProvisaoRH(tenantId, contratoId, tipoProvisao, valor, comp)
        return ResponseEntity.ok("Provisão de RH lançada com sucesso.")
    }

    @PostMapping("/lancamentos/glosa/{glosaId}")
    fun lancarGlosa(
        @PathVariable glosaId: UUID,
        @RequestParam tenantId: UUID,
        @RequestParam contratoId: UUID,
        @RequestParam valor: BigDecimal,
        @RequestParam competencia: String
    ): ResponseEntity<String> {
        service.lancarGlosaAplicada(glosaId, tenantId, contratoId, valor, LocalDate.parse(competencia))
        return ResponseEntity.ok("Lançamento de glosa gerado.")
    }

    @GetMapping("/periodos")
    fun listarPeriodos(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<Map<String, Any?>>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val periodos = service.listarPeriodos(effectiveTenant).map {
            mapOf(
                "id" to it.id,
                "competencia" to it.competencia.toString(),
                "status" to it.status,
                "fechadoEm" to it.fechadoEm?.toString(),
                "fechadoPor" to it.fechadoPor
            )
        }
        return ResponseEntity.ok(periodos)
    }

    // ==================== RELATÓRIOS ====================

    @GetMapping("/dre")
    fun drePorContrato(
        @RequestParam contratoId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val dre = service.gerarDreContrato(contratoId, dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok(dre)
    }

    @GetMapping("/razao/{contaId}")
    fun razao(
        @PathVariable contaId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val razao = service.gerarRazao(contaId, dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok(razao)
    }

    @GetMapping("/balancete")
    fun balancete(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<Map<String, Any>>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val balancete = service.gerarBalancete(dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok(balancete)
    }

    @GetMapping("/balanco")
    fun balancoPatrimonial(
        @RequestParam data: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataCorte = LocalDate.parse(data)

        val balanco = service.gerarBalancoPatrimonial(dataCorte, effectiveTenant)
        return ResponseEntity.ok(balanco)
    }

    // ==================== EXPORTAÇÃO CSV ====================

    @GetMapping("/export/balancete")
    fun exportBalancete(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val csv = exportService.exportarBalanceteCsv(dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=balancete.csv")
            .body(csv)
    }

    @GetMapping("/export/dre")
    fun exportDre(
        @RequestParam contratoId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val csv = exportService.exportarDreCsv(contratoId, dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=dre_contrato_$contratoId.csv")
            .body(csv)
    }

    @GetMapping("/export/lancamentos")
    fun exportLancamentos(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val csv = exportService.exportarLancamentosCsv(dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=lancamentos.csv")
            .body(csv)
    }

    @GetMapping("/fluxo-caixa")
    fun fluxoDeCaixa(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val fluxo = service.gerarFluxoDeCaixa(dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok(fluxo)
    }

    @PostMapping("/fechamento-mensal")
    fun fechamentoMensal(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataInicio = LocalDate.parse(inicio)
        val dataFim = LocalDate.parse(fim)

        val fechamento = service.fecharMesContabil(dataInicio, dataFim, effectiveTenant)
        return ResponseEntity.ok(fechamento)
    }

    @PostMapping("/reabrir-periodo")
    fun reabrirPeriodo(
        @RequestParam competencia: String,
        @RequestParam motivo: String,
        @RequestParam(required = false) usuario: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia)
        return ResponseEntity.ok(service.reabrirPeriodo(effectiveTenant, comp, motivo, usuario ?: "sistema"))
    }

    @PostMapping("/encerramento-exercicio")
    fun encerramentoExercicio(
        @RequestParam ano: Int,
        @RequestParam(required = false) usuario: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(encerramentoExercicioService.encerrarExercicio(effectiveTenant, ano, usuario))
    }

    @GetMapping("/conciliacao-saldos")
    fun conciliacaoSaldos(
        @RequestParam dataCorte: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(
            conciliacaoContabilService.conciliarSaldos(effectiveTenant, LocalDate.parse(dataCorte))
        )
    }
}

// DTOs simples
data class CriarContaRequest(
    val codigo: String,
    val descricao: String,
    val tipo: String,
    val natureza: String,
    val contaMaeId: String? = null,
    val nivel: Int = 1,
    val aceitaLancamento: Boolean = true
)

data class LancarRequest(
    val data: LocalDate,
    val contaDebitoId: UUID,
    val contaCreditoId: UUID,
    val valor: BigDecimal,
    val historico: String? = null,
    val origemTipo: String? = null,
    val origemId: UUID? = null,
    val contratoId: UUID? = null,
    val costCenterId: UUID? = null,
    val branchId: UUID? = null,
    val lines: List<LancarLineRequest>? = null
)

data class LancarLineRequest(
    val contaId: UUID,
    val natureza: String,
    val valor: BigDecimal,
    val historico: String? = null
)