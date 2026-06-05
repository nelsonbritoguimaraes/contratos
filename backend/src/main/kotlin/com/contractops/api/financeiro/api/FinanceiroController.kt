package com.contractops.api.financeiro.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.financeiro.domain.ContaBancaria
import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import com.contractops.api.financeiro.domain.TransacaoFinanceira
import com.contractops.api.financeiro.service.FinanceiroService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Controller do Módulo Financeiro Enterprise (CFO Literal).
 * Endpoints organizados por domínio:
 * - /tesouraria (contas bancárias, conciliação)
 * - /receber (AR)
 * - /pagar (AP)
 * - /nfs (emissão e consulta de NFS-e)
 * - /fluxo-caixa (real e projetado)
 * - /dashboard/cfo (o coração do CFO)
 * - /compliance (retenções, guias, calendário)
 * - /fechamento (fechamento financeiro mensal)
 *
 * SPEC §16, §22, §25.7
 */
@RestController
@RequestMapping("/api/financeiro")
@PreAuthorize("hasAnyRole('ADMIN','GESTOR_GRUPO','FINANCEIRO','CONTADOR')")
class FinanceiroController(
    private val service: FinanceiroService,
    private val openFinanceWebhookService: com.contractops.api.financeiro.service.OpenFinanceWebhookService,
    private val workflowOrchestrator: com.contractops.api.financeiro.service.FinanceWorkflowOrchestratorService,
    private val nfsCobrancaWorkflowStarter: com.contractops.api.financeiro.temporal.NfsCobrancaWorkflowStarter,
    private val cnabRetornoImportService: com.contractops.api.financeiro.service.CnabRetornoImportService,
    private val payslipRepository: com.contractops.api.rh.repository.PayslipRepository,
    private val tenantFiscalProfileService: com.contractops.api.financeiro.service.TenantFiscalProfileService,
    private val openFinanceConsentService: com.contractops.api.financeiro.service.OpenFinanceConsentService
) {

    // ============================================================
    // PERFIL FISCAL DO TENANT
    // ============================================================

    @GetMapping("/perfil-fiscal")
    fun obterPerfilFiscal(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<TenantFiscalProfileResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(tenantFiscalProfileService.obter(effectiveTenant))
    }

    @PutMapping("/perfil-fiscal")
    fun atualizarPerfilFiscal(
        @RequestBody request: UpdateTenantFiscalProfileRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<TenantFiscalProfileResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(tenantFiscalProfileService.atualizar(effectiveTenant, request))
    }

    // ============================================================
    // TESOURARIA
    // ============================================================

    @GetMapping("/tesouraria/contas")
    fun listarContasBancarias(
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<ContaBancaria>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.listarContasBancariasAtivas(effectiveTenant))
    }

    @PostMapping("/tesouraria/contas")
    fun criarContaBancaria(
        @RequestBody request: CriarContaBancariaRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContaBancaria> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val conta = ContaBancaria(
            tenantId = effectiveTenant,
            bancoCodigo = request.bancoCodigo,
            bancoNome = request.bancoNome,
            agencia = request.agencia,
            conta = request.conta,
            tipo = request.tipo,
            contaContabilId = request.contaContabilId,
            observacoes = request.observacoes
        )

        return ResponseEntity.ok(service.criarContaBancaria(conta))
    }

    @PostMapping("/tesouraria/extrato/importar")
    fun importarExtrato(
        @RequestParam contaBancariaId: UUID,
        @RequestBody itens: List<ExtratoItemRequest>,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val entidades = itens.map {
            com.contractops.api.financeiro.domain.ExtratoBancarioItem(
                tenantId = effectiveTenant,
                contaBancariaId = contaBancariaId,
                data = it.data,
                documento = it.documento,
                historico = it.historico,
                valor = it.valor,
                tipo = it.tipo
            )
        }

        val qtd = service.importarExtrato(contaBancariaId, entidades, effectiveTenant)
        return ResponseEntity.ok(mapOf(
            "importados" to qtd,
            "totalRecebidos" to itens.size,
            "message" to "$qtd novos itens de extrato importados com sucesso"
        ))
    }

    /**
     * Upload real de arquivo de extrato bancário (OFX ou CSV).
     * Este é o endpoint de produção para importação de conciliação.
     */
    @PostMapping("/tesouraria/extrato/upload")
    fun uploadExtratoBancario(
        @RequestParam contaBancariaId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val qtd = service.importarExtratoDeArquivo(contaBancariaId, file, effectiveTenant)

        return ResponseEntity.ok(mapOf(
            "importados" to qtd,
            "arquivo" to (file.originalFilename ?: "sem-nome"),
            "message" to "Arquivo processado com sucesso. $qtd transações importadas."
        ))
    }

    // ============================================================
    // CONTAS A RECEBER (AR) - básico por enquanto
    // ============================================================

    @GetMapping("/receber")
    fun listarContasAReceber(
        @RequestParam(required = false) contratoId: UUID?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) vencimentoDe: String?,
        @RequestParam(required = false) vencimentoAte: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContasAReceberResumoResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val de = if (vencimentoDe != null) LocalDate.parse(vencimentoDe) else null
        val ate = if (vencimentoAte != null) LocalDate.parse(vencimentoAte) else null

        val resumo = service.listarContasAReceberRich(
            tenantId = effectiveTenant,
            contratoId = contratoId,
            status = status,
            vencimentoDe = de,
            vencimentoAte = ate
        )
        return ResponseEntity.ok(resumo)
    }

    @PostMapping("/receber/{contaAReceberId}/baixar")
    fun baixarContaAReceber(
        @PathVariable contaAReceberId: UUID,
        @RequestBody request: RegistrarRecebimentoRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val data = LocalDate.parse(request.data)
        val recebimento = service.registrarRecebimento(
            contaAReceberId = contaAReceberId,
            valor = request.valor,
            data = data,
            contaBancariaId = request.contaBancariaId,
            retencoesJson = request.observacao,
            tenantId = effectiveTenant
        )
        return ResponseEntity.ok(
            mapOf(
                "id" to recebimento.id,
                "contaAReceberId" to contaAReceberId,
                "valor" to recebimento.valor,
                "data" to recebimento.data,
                "message" to "Recebimento registrado e saldo bancário atualizado quando conta informada"
            )
        )
    }

    // ============================================================
    // CONTAS A PAGAR (AP)
    // ============================================================

    @GetMapping("/pagar")
    fun listarContasAPagar(
        @RequestParam(required = false) contratoId: UUID?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) origem: String?,
        @RequestParam(required = false) vencimentoDe: String?,
        @RequestParam(required = false) vencimentoAte: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<ContasAPagarResumoResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val de = vencimentoDe?.let { LocalDate.parse(it) }
        val ate = vencimentoAte?.let { LocalDate.parse(it) }
        return ResponseEntity.ok(
            service.listarContasAPagarRich(
                tenantId = effectiveTenant,
                contratoId = contratoId,
                status = status,
                origem = origem,
                vencimentoDe = de,
                vencimentoAte = ate
            )
        )
    }

    @PostMapping("/pagar/{contaAPagarId}/baixar")
    fun baixarContaAPagar(
        @PathVariable contaAPagarId: UUID,
        @RequestBody request: BaixarContaAPagarRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val pagamento = service.pagarContaAPagar(
            contaAPagarId = contaAPagarId,
            data = LocalDate.parse(request.data),
            valor = request.valor,
            contaBancariaId = request.contaBancariaId,
            formaPagamento = request.formaPagamento,
            tenantId = effectiveTenant
        )
        return ResponseEntity.ok(mapOf(
            "id" to pagamento.id,
            "contaAPagarId" to contaAPagarId,
            "valor" to pagamento.valor,
            "message" to "Pagamento registrado com sucesso"
        ))
    }

    @PostMapping("/receber/{contaAReceberId}/cobranca")
    fun gerarCobranca(
        @PathVariable contaAReceberId: UUID,
        @RequestBody request: GerarCobrancaRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val cobranca = service.emitirCobranca(effectiveTenant, contaAReceberId, request.tipo)
        return ResponseEntity.ok(cobranca)
    }

    @PostMapping("/pagamentos/import/cnab-retorno")
    fun importarCnabRetorno(
        @RequestParam contaBancariaId: UUID,
        @RequestBody conteudo: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(cnabRetornoImportService.importarRetornoCnab240(conteudo, effectiveTenant, contaBancariaId))
    }

    @GetMapping("/auditoria")
    fun listarAuditoria(
        @RequestParam(defaultValue = "100") limit: Int,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<com.contractops.api.financeiro.domain.FinancialAuditLog>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.listarAuditoriaFinanceira(effectiveTenant, limit))
    }

    @GetMapping("/relatorios/dre-contrato/{contratoId}")
    fun dreContrato(
        @PathVariable contratoId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(
            service.gerarDreContratoFinanceiro(contratoId, LocalDate.parse(inicio), LocalDate.parse(fim), effectiveTenant)
        )
    }

    @PostMapping("/provisao-glosa")
    fun provisionarGlosa(
        @RequestBody request: ProvisaoGlosaRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ar = service.provisionarGlosaFinanceira(request.measurementId, request.valorGlosa, effectiveTenant)
        return ResponseEntity.ok(ar)
    }

    // ============================================================
    // NFS-e
    // ============================================================

    @PostMapping("/nfs/emitir")
    fun emitirNfs(
        @RequestBody request: EmitirNfsRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")

        val nfs = service.emitirNfs(
            measurementId = request.measurementId,
            contratoId = request.contratoId,
            tomadorCnpj = request.tomadorCnpj,
            valorServicos = request.valorServicos,
            tenantId = effectiveTenant
        )

        return ResponseEntity.ok(nfs)
    }

    @PostMapping("/nfs/{nfsId}/cancelar")
    fun cancelarNfs(
        @PathVariable nfsId: UUID,
        @RequestParam motivo: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val nfs = service.cancelarNfs(nfsId, motivo, effectiveTenant)
        return ResponseEntity.ok(nfs)
    }

    @GetMapping("/nfs/consultar")
    fun consultarNfs(
        @RequestParam numero: String,
        @RequestParam tomadorCnpj: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val nfs = service.consultarNfs(numero, tomadorCnpj, effectiveTenant)
        return ResponseEntity.ok(nfs ?: mapOf("message" to "NFS-e não encontrada"))
    }

    // ==================== FGTS Digital e DCTFWeb (Fase 4 Polish) ====================

    @GetMapping("/fgts/evento")
    fun gerarEventoFGTS(
        @RequestParam competencia: String,
        @RequestParam totalFolha: BigDecimal,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia)
        return ResponseEntity.ok(service.gerarEventoFGTS(comp, totalFolha, effectiveTenant))
    }

    @GetMapping("/fgts/guia")
    fun gerarGuiaFGTS(
        @RequestParam competencia: String,
        @RequestParam valor: BigDecimal,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia)
        return ResponseEntity.ok(service.gerarGuiaFGTS(comp, valor, effectiveTenant))
    }

    @GetMapping("/dctfweb")
    fun gerarDCTFWeb(
        @RequestParam competencia: String,
        @RequestParam totalRetencoes: BigDecimal,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val comp = LocalDate.parse(competencia)
        return ResponseEntity.ok(service.gerarDCTFWeb(comp, totalRetencoes, effectiveTenant))
    }

    @PostMapping("/conciliacao/importar-extrato")
    fun importarExtrato(
        @RequestParam contaBancariaId: UUID,
        @RequestParam formato: String,
        @RequestBody conteudo: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val qtd = service.importarExtratoMelhorado(contaBancariaId, conteudo, formato, effectiveTenant)
        return ResponseEntity.ok(mapOf("importados" to qtd))
    }

    @PostMapping("/pagamentos/export/cnab240")
    fun exportarCnab240(
        @RequestBody contasIds: List<UUID>,
        @RequestParam agencia: String,
        @RequestParam conta: String,
        @RequestParam dv: String,
        @RequestParam cnpj: String,
        @RequestParam nomeEmpresa: String,
        @RequestParam dataPagamento: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val dataPag = LocalDate.parse(dataPagamento)
        val contas = service.listarContasAPagarPorIds(contasIds, effectiveTenant)
        val cnab = service.exportarCnab240Pagamentos(contas, agencia, conta, dv, cnpj, nomeEmpresa, dataPag)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=CNAB240_${dataPag}.txt")
            .body(cnab)
    }

    @GetMapping("/conciliacao/export/ofx")
    fun exportarOfx(
        @RequestParam contaBancariaId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam saldoFinal: BigDecimal,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ini = LocalDate.parse(inicio)
        val fimDate = LocalDate.parse(fim)

        val itens = service.listarExtratosNaoConciliadosOuPorPeriodo(contaBancariaId, ini, fimDate, effectiveTenant)
        val ofx = service.exportarOfxConciliacao(contaBancariaId.toString(), itens, ini, fimDate, saldoFinal)

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=conciliacao_${ini}_a_${fimDate}.ofx")
            .body(ofx)
    }

    @GetMapping("/relatorios/posicao-caixa")
    fun posicaoCaixa(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<String> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ini = LocalDate.parse(inicio)
        val fimDate = LocalDate.parse(fim)
        return ResponseEntity.ok(service.gerarRelatorioPosicaoCaixa(ini, fimDate, effectiveTenant))
    }

    @GetMapping("/relatorios/aging/ar")
    fun agingAR(
        @RequestParam(required = false) dataCorte: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val corte = dataCorte?.let { LocalDate.parse(it) } ?: LocalDate.now()
        return ResponseEntity.ok(service.gerarAgingContasAReceber(effectiveTenant, corte))
    }

    @GetMapping("/relatorios/aging/ap")
    fun agingAP(
        @RequestParam(required = false) dataCorte: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val corte = dataCorte?.let { LocalDate.parse(it) } ?: LocalDate.now()
        return ResponseEntity.ok(service.gerarAgingContasAPagar(effectiveTenant, corte))
    }

    @GetMapping("/relatorios/calendario-obrigacoes")
    fun calendarioObrigacoes(
        @RequestParam mes: Int,
        @RequestParam ano: Int,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.gerarCalendarioObrigacoes(mes, ano, effectiveTenant))
    }

    @GetMapping("/fechamento-mensal")
    fun listarFechamentos(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<FechamentoFinanceiroResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val list = service.listarFechamentosFinanceiros(effectiveTenant).map {
            FechamentoFinanceiroResponse(
                id = it.id,
                dataInicio = it.dataInicio,
                dataFim = it.dataFim,
                status = it.status,
                saldoCaixaFinal = it.saldoCaixaFinal,
                totalRecebimentos = it.totalRecebimentos,
                totalPagamentos = it.totalPagamentos,
                observacoes = it.observacoes,
                dataFechamento = it.dataFechamento?.toString()
            )
        }
        return ResponseEntity.ok(list)
    }

    @PostMapping("/fechamento-mensal/fechar")
    fun fecharFechamento(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) fechadoPor: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val fechamento = service.fecharMesFinanceiro(
            LocalDate.parse(inicio),
            LocalDate.parse(fim),
            effectiveTenant,
            fechadoPor ?: "cfo@contractops"
        )
        return ResponseEntity.ok(fechamento)
    }

    @PostMapping("/compliance/retencoes/calcular")
    fun calcularRetencoes(
        @RequestBody request: CalcularRetencoesRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<RetencaoCalculadaResponse>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(
            service.calcularRetencoes(
                valorServico = request.valorServico,
                municipioIbge = request.municipioIbge,
                naturezaServico = request.naturezaServico,
                tenantId = effectiveTenant,
                aplicarInss = request.aplicarInss
            )
        )
    }

    @PostMapping("/compliance/darf/{retencaoId}")
    fun gerarDarf(
        @PathVariable retencaoId: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<DarfPreviewResponse> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.gerarGuiaDarf(retencaoId, effectiveTenant))
    }

    @PostMapping("/fechamento-mensal/reabrir")
    fun reabrirFechamento(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) usuario: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ini = LocalDate.parse(inicio)
        val fimDate = LocalDate.parse(fim)
        val fechamento = service.reabrirMesFinanceiro(ini, fimDate, effectiveTenant, usuario ?: "cfo@contractops")
        return ResponseEntity.ok(fechamento)
    }

    @PostMapping("/pagamentos/{contaAPagarId}/aprovar")
    fun aprovarPagamento(
        @PathVariable contaAPagarId: UUID,
        @RequestParam nivel: Int,
        @RequestParam usuario: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val conta = service.aprovarPagamentoPendente(contaAPagarId, nivel, usuario, effectiveTenant)
        return ResponseEntity.ok(conta)
    }

    // ============================================================
    // FLUXO DE CAIXA (placeholder poderoso)
    // ============================================================

    @GetMapping("/fluxo-caixa/real")
    fun fluxoCaixaReal(
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ini = LocalDate.parse(inicio)
        val fimDate = LocalDate.parse(fim)
        val resultado = service.calcularFluxoCaixaReal(ini, fimDate, effectiveTenant)
        return ResponseEntity.ok(resultado)
    }

    // ============================================================
    // CFO DASHBOARD (o grande entregável)
    // ============================================================

    @GetMapping("/dashboard/cfo")
    fun cfoDashboard(
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.getCfoDashboard(effectiveTenant))
    }

    @PostMapping("/conciliacao")
    fun conciliar(
        @RequestParam contaBancariaId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ini = LocalDate.parse(inicio)
        val fimDate = LocalDate.parse(fim)
        val conciliacao = service.conciliarAutomatico(contaBancariaId, ini, fimDate, effectiveTenant)
        return ResponseEntity.ok(conciliacao)
    }

    @GetMapping("/conciliacao/extratos-pendentes")
    fun listarExtratosPendentes(
        @RequestParam contaBancariaId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<ExtratoBancarioItem>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ini = LocalDate.parse(inicio)
        val fimDate = LocalDate.parse(fim)
        return ResponseEntity.ok(service.listarExtratosPendentes(contaBancariaId, ini, fimDate, effectiveTenant))
    }

    @GetMapping("/conciliacao/transacoes-pendentes")
    fun listarTransacoesPendentes(
        @RequestParam contaBancariaId: UUID,
        @RequestParam inicio: String,
        @RequestParam fim: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<TransacaoFinanceira>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val ini = LocalDate.parse(inicio)
        val fimDate = LocalDate.parse(fim)
        return ResponseEntity.ok(service.listarTransacoesPendentes(contaBancariaId, ini, fimDate, effectiveTenant))
    }

    @PostMapping("/conciliacao/manual")
    fun conciliarManual(
        @RequestBody payload: Map<String, String>,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Any> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val contaId = UUID.fromString(payload["contaBancariaId"])
        val extratoId = UUID.fromString(payload["extratoId"])
        val transacaoId = UUID.fromString(payload["transacaoId"])
        val result = service.conciliarManual(contaId, extratoId, transacaoId, effectiveTenant)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/fluxo-caixa/projetado")
    fun fluxoProjetado(
        @RequestParam(defaultValue = "13") semanas: Int,
        @RequestParam(defaultValue = "BASE") cenario: String,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.calcularFluxoCaixaProjetado(semanas, cenario, effectiveTenant))
    }

    @PostMapping("/simulacao")
    fun simular(
        @RequestBody parametros: Map<String, Any>,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.simularCenario(parametros, effectiveTenant))
    }

    // ============================================================
    // INTEGRAÇÃO FOLHA → FINANCEIRO + CONTABILIDADE (Bloco Atual)
    // ============================================================

    @PostMapping("/folha/processar-fechamento")
    fun processarFechamentoFolha(
        @RequestBody payslipIds: List<UUID>,
        @RequestParam(required = false) contratoId: UUID?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val payslips = payslipRepository.findAllById(payslipIds)
            .filter { it.tenantId == effectiveTenant && it.status in listOf("APPROVED", "EXPORTED") }

        if (payslips.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf(
                "message" to "Nenhum holerite aprovado encontrado para os IDs informados",
                "payslipIdsRecebidos" to payslipIds.size
            ))
        }

        val resultado = service.processarFechamentoFolhaCompleto(
            payslipsAprovados = payslips,
            tenantId = effectiveTenant,
            contratoId = contratoId
        )
        return ResponseEntity.ok(resultado)
    }

    // ============================================================
    // Open Finance + match NFS-e ↔ extrato
    // ============================================================

    @GetMapping("/open-finance/consents")
    fun listarOpenFinanceConsents(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<com.contractops.api.financeiro.domain.OpenFinanceConsent>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(openFinanceConsentService.listar(effectiveTenant))
    }

    @PostMapping("/open-finance/consents/iniciar")
    fun iniciarOpenFinanceConsent(
        @RequestBody request: IniciarOpenFinanceConsentRequest,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<com.contractops.api.financeiro.domain.OpenFinanceConsent> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(
            openFinanceConsentService.iniciarConsentimento(
                tenantId = effectiveTenant,
                contaBancariaId = request.contaBancariaId,
                institutionId = request.institutionId,
                institutionName = request.institutionName
            )
        )
    }

    @PostMapping("/open-finance/consents/{id}/confirmar")
    fun confirmarOpenFinanceConsent(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<com.contractops.api.financeiro.domain.OpenFinanceConsent> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(openFinanceConsentService.confirmarConsentimento(id, effectiveTenant))
    }

    @PostMapping("/open-finance/consents/{id}/revogar")
    fun revogarOpenFinanceConsent(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<com.contractops.api.financeiro.domain.OpenFinanceConsent> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(openFinanceConsentService.revogarConsentimento(id, effectiveTenant))
    }

    @PostMapping("/open-finance/webhook")
    fun openFinanceWebhook(
        @RequestBody payload: Map<String, Any>,
        @RequestParam(required = false) tenantId: UUID?,
        @RequestParam(required = false) contaBancariaId: UUID?,
        @RequestHeader(name = "X-OpenFinance-Secret", required = false) secret: String?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val event = openFinanceWebhookService.processWebhook(effectiveTenant, contaBancariaId, payload, secret)
        return ResponseEntity.ok(
            mapOf<String, Any>(
                "eventId" to (event.id?.toString() ?: ""),
                "processado" to event.processado,
                "itensImportados" to event.itensImportados
            )
        )
    }

    @GetMapping("/conciliacao/sugestoes")
    fun sugestoesConciliacao(
        @RequestParam contaBancariaId: UUID,
        @RequestParam inicio: LocalDate,
        @RequestParam fim: LocalDate,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<List<com.contractops.api.financeiro.service.MatchCandidate>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        return ResponseEntity.ok(service.sugerirMatchesConciliacao(contaBancariaId, inicio, fim, effectiveTenant))
    }

    // ============================================================
    // DANFSE + workflow NFS-e (Temporal-ready)
    // ============================================================

    @PostMapping("/nfs/{id}/danfse/email")
    fun enviarDanfseEmail(
        @PathVariable id: UUID,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val msg = service.enviarDanfseOrgao(id, effectiveTenant, email)
        return ResponseEntity.ok(mapOf<String, Any>("emailId" to (msg.id?.toString() ?: ""), "classification" to (msg.classification ?: "")))
    }

    @PostMapping("/workflows/nfs-cobranca/iniciar")
    fun iniciarWorkflowNfs(
        @RequestParam notaFiscalId: UUID,
        @RequestParam(required = false) contaAReceberId: UUID?,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val wf = nfsCobrancaWorkflowStarter.iniciar(notaFiscalId, contaAReceberId, effectiveTenant)
        return ResponseEntity.ok(
            mapOf<String, Any>(
                "workflowId" to (wf.id?.toString() ?: ""),
                "estado" to wf.estadoAtual,
                "temporal" to nfsCobrancaWorkflowStarter.isTemporalEnabled()
            )
        )
    }

    @PostMapping("/workflows/{id}/avancar")
    fun avancarWorkflow(
        @PathVariable id: UUID,
        @RequestParam(required = false) tenantId: UUID?
    ): ResponseEntity<Map<String, Any>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val wf = workflowOrchestrator.avancar(id, effectiveTenant)
        return ResponseEntity.ok(
            mapOf<String, Any>(
                "workflowId" to (wf.id?.toString() ?: ""),
                "estado" to wf.estadoAtual,
                "concluido" to wf.concluido,
                "erro" to (wf.erro ?: "")
            )
        )
    }

    @GetMapping("/workflows/ativos")
    fun workflowsAtivos(@RequestParam(required = false) tenantId: UUID?): ResponseEntity<List<Map<String, Any?>>> {
        val effectiveTenant = tenantId ?: TenantContext.getCurrentTenantId() ?: throw IllegalStateException("Tenant não resolvido. Token JWT deve conter tenantId.")
        val list = workflowOrchestrator.listarAtivos(effectiveTenant).map {
            mapOf("id" to it.id, "estado" to it.estadoAtual, "notaFiscalId" to it.notaFiscalId, "concluido" to it.concluido)
        }
        return ResponseEntity.ok(list)
    }
}