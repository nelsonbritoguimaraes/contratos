package com.contractops.api.financeiro.service

import com.contractops.api.contabilidade.service.ContabilidadeService
import com.contractops.api.common.events.DomainEventPublisher
import com.contractops.api.common.events.invoiceIssuedEvent
import com.contractops.api.financeiro.api.ContaAReceberResponse
import com.contractops.api.financeiro.api.ContaAPagarResponse
import com.contractops.api.financeiro.api.ContasAReceberResumoResponse
import com.contractops.api.financeiro.api.ContasAPagarResumoResponse
import com.contractops.api.financeiro.api.DarfPreviewResponse
import com.contractops.api.financeiro.api.RetencaoCalculadaResponse
import com.contractops.api.financeiro.domain.*
import com.contractops.api.financeiro.exception.FinanceiroBusinessException
import com.contractops.api.financeiro.config.NfsWorkflowProperties
import com.contractops.api.financeiro.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

/**
 * Motor principal do Módulo Financeiro Enterprise — CFO Literal.
 *
 * Responsável por:
 * - Emissão e ciclo de vida de NFS-e
 * - Gestão completa de Contas a Receber e Contas a Pagar
 * - Tesouraria (Contas bancárias + Conciliação)
 * - Fluxo de Caixa real + projetado (13 semanas / 12 meses)
 * - Cálculo de retenções tributárias
 * - CFO Dashboard e KPIs executivos
 * - Fechamento financeiro mensal
 *
 * Integra automaticamente com:
 * - MeasurementService (faturamento)
 * - PayslipService (folha → contas a pagar)
 * - ContabilidadeService (lançamentos automáticos)
 *
 * SPEC §16, §22, §25.7
 */
@Service
class FinanceiroService(
    private val contaBancariaRepository: ContaBancariaRepository,
    private val contaAReceberRepository: ContaAReceberRepository,
    private val contaAPagarRepository: ContaAPagarRepository,
    private val transacaoFinanceiraRepository: TransacaoFinanceiraRepository,
    private val notaFiscalRepository: NotaFiscalServicoRepository,
    private val retencaoRepository: RetencaoTributariaRepository,
    private val conciliacaoRepository: ConciliacaoBancariaRepository,
    private val extratoRepository: ExtratoBancarioItemRepository,
    private val previsaoRepository: PrevisaoFinanceiraRepository,
    private val pagamentoRepository: PagamentoRepository,
    private val recebimentoRepository: RecebimentoRepository,
    private val fechamentoRepository: FinanceiroPeriodoFechamentoRepository,

    // Integrações com outros módulos (injeção opcional para não quebrar builds intermediários)
    private val contabilidadeService: ContabilidadeService? = null,
    private val nfsEmissaoService: NfsEmissaoService? = null,
    private val fgtsDigitalService: FGTSDigitalService? = null,
    private val dctfWebService: DCTFWebService? = null,
    private val cnabExportService: CnabExportService? = null,
    private val ofxExportService: OfxExportService? = null,
    private val financialReportService: FinancialReportService? = null,
    private val agingReportService: AgingReportService? = null,
    private val taxCalendarService: TaxCalendarService? = null,
    private val bankStatementParser: BankStatementParserService? = null,
    private val nfseExtratoMatchService: NfseExtratoMatchService? = null,
    private val nfsDanfseService: NfsDanfseService? = null,
    private val nfsOrgaoEmailService: NfsOrgaoEmailService? = null,
    private val nfsCobrancaWorkflowStarter: com.contractops.api.financeiro.temporal.NfsCobrancaWorkflowStarter? = null,
    private val fgtsGateway: FgtsDigitalGateway? = null,
    private val dctfGateway: DctfWebGateway? = null,
    private val contractRepository: com.contractops.api.contract.repository.ContractRepository? = null,
    private val cobrancaService: CobrancaService? = null,
    private val reinfNfsIntegrationService: ReinfNfsIntegrationService? = null,
    private val auditService: FinancialAuditService? = null,
    private val tenantFiscalProfileRepository: TenantFiscalProfileRepository? = null,
    private val tenantFiscalProfileService: TenantFiscalProfileService? = null,
    private val workflowProperties: NfsWorkflowProperties,
    private val domainEventPublisher: DomainEventPublisher? = null
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FinanceiroService::class.java)
    }

    // ============================================================
    // TESOURARIA - Contas Bancárias
    // ============================================================

    fun listarContasBancariasAtivas(tenantId: UUID): List<ContaBancaria> =
        contaBancariaRepository.findByTenantIdAndAtivaTrue(tenantId)

    fun listarContasAPagarPorIds(ids: List<UUID>, tenantId: UUID): List<ContaAPagar> {
        return contaAPagarRepository.findAllById(ids).filter { it.tenantId == tenantId }
    }

    fun listarExtratosNaoConciliadosOuPorPeriodo(contaBancariaId: UUID, inicio: LocalDate, fim: LocalDate, tenantId: UUID): List<ExtratoBancarioItem> {
        return extratoRepository.findByTenantIdAndContaBancariaId(tenantId, contaBancariaId)
            .filter { it.data in inicio..fim }
    }

    @Transactional
    fun criarContaBancaria(conta: ContaBancaria): ContaBancaria {
        val existing = contaBancariaRepository.findByTenantId(conta.tenantId)
            .any { it.bancoNome == conta.bancoNome && it.conta == conta.conta }
        if (existing) {
            throw IllegalStateException("Conta bancária já existe para este tenant")
        }
        return contaBancariaRepository.save(conta)
    }

    // ============================================================
    // CONTAS A RECEBER (AR)
    // ============================================================

    @Transactional
    fun criarContaAReceberDaMedicao(
        measurementId: UUID,
        contratoId: UUID,
        valorBruto: BigDecimal,
        valorLiquido: BigDecimal,
        vencimento: LocalDate,
        tenantId: UUID,
        glosaProvisao: BigDecimal = BigDecimal.ZERO,
        tomadorCnpj: String? = null
    ): ContaAReceber {
        validarPeriodoAberto(tenantId, vencimento)

        val existing = contaAReceberRepository.findByTenantIdAndMeasurementId(tenantId, measurementId)
        if (existing.isNotEmpty()) {
            return existing.first()
        }

        val ar = ContaAReceber(
            tenantId = tenantId,
            contratoId = contratoId,
            measurementId = measurementId,
            valorBruto = valorBruto,
            valorLiquido = valorLiquido.subtract(glosaProvisao).max(BigDecimal.ZERO),
            vencimento = vencimento,
            status = "ABERTO",
            glosaProvisao = glosaProvisao,
            tomadorCnpj = tomadorCnpj
        )
        contractRepository?.findById(contratoId)?.ifPresent { c ->
            ar.branchId = c.branchId
        }
        val saved = contaAReceberRepository.save(ar)
        auditService?.registrar(tenantId, "CONTA_A_RECEBER", saved.id, "CRIAR_MEDICAO", "sistema", "Medição $measurementId")
        return saved
    }

    @Transactional
    fun registrarRecebimento(
        contaAReceberId: UUID,
        valor: BigDecimal,
        data: LocalDate,
        contaBancariaId: UUID?,
        retencoesJson: String? = null,
        tenantId: UUID
    ): Recebimento {
        val ar = contaAReceberRepository.findById(contaAReceberId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Conta a Receber não encontrada") }

        validarPeriodoAberto(tenantId, data)

        if (ar.status == "PAGO") {
            throw FinanceiroBusinessException("Conta a Receber já está paga")
        }

        // Registra o recebimento
        val recebimento = Recebimento(
            tenantId = tenantId,
            contaAReceberId = contaAReceberId,
            data = data,
            valor = valor,
            contaBancariaId = contaBancariaId,
            retencoesAplicadas = retencoesJson
        )
        val savedRecebimento = recebimentoRepository.save(recebimento)

        // Atualiza a Conta a Receber
        val totalRecebido = (ar.valorRecebido ?: BigDecimal.ZERO).add(valor)
        ar.valorRecebido = totalRecebido
        ar.dataRecebimento = data

        if (totalRecebido >= ar.valorLiquido) {
            ar.status = "PAGO"
        } else {
            ar.status = "PARCIAL"
        }
        contaAReceberRepository.save(ar)

        // Cria Transação Financeira (entrada no caixa)
        if (contaBancariaId != null) {
            val transacao = TransacaoFinanceira(
                tenantId = tenantId,
                data = data,
                contaBancariaId = contaBancariaId,
                tipo = "ENTRADA",
                valor = valor,
                origemTipo = "RECEBIMENTO",
                origemId = savedRecebimento.id,
                historico = "Recebimento de AR #${ar.id} - Medição ${ar.measurementId}",
                conciliado = false
            )
            transacaoFinanceiraRepository.save(transacao)

            // Atualiza saldo da conta bancária
            atualizarSaldoBancario(contaBancariaId, valor, "ENTRADA")
        }

        auditService?.registrar(tenantId, "CONTA_A_RECEBER", contaAReceberId, "RECEBER", "sistema", "Recebimento R$ $valor")

        // Lança baixa na contabilidade automaticamente (melhorado - Bloco Atual)
        try {
            contabilidadeService?.let { contab ->
                val banco = contab.findContaPorCodigo("1.1.01", tenantId)  // Caixa e Bancos
                val clientes = contab.findContaPorCodigo("1.1.02", tenantId) // Clientes

                if (banco != null && clientes != null && contaBancariaId != null) {
                    contab.lancar(
                        tenantId = tenantId,
                        data = data,
                        contaDebitoId = banco.id!!,
                        contaCreditoId = clientes.id!!,
                        valor = valor,
                        historico = "Baixa de Conta a Receber - AR #${ar.id} (Medição ${ar.measurementId})",
                        origemTipo = "RECEBIMENTO",
                        origemId = savedRecebimento.id,
                        contratoId = ar.contratoId
                    )
                } else {
                    logger.warn("Contas contábeis padrão (1.1.01 / 1.1.02) não encontradas para lançamento de recebimento")
                }
            }
        } catch (e: Exception) {
            logger.warn("Falha no lançamento contábil do recebimento: ${e.message}")
        }

        return savedRecebimento
    }

    // ============================================================
    // NFS-e (Emissão e ciclo de vida) - Versão avançada Fase 4 Polish
    // ============================================================

    @Transactional
    fun emitirNfs(
        measurementId: UUID,
        contratoId: UUID,
        tomadorCnpj: String,
        valorServicos: BigDecimal,
        tenantId: UUID
    ): NotaFiscalServico {
        validarPeriodoAberto(tenantId, LocalDate.now())

        // Idempotência: se já existe NFS-e para esta medição, retorna
        val existing = notaFiscalRepository.findByTenantIdAndMeasurementId(tenantId, measurementId)
        if (existing.isNotEmpty()) {
            return existing.first()
        }

        val contract = contractRepository?.findById(contratoId)?.orElse(null)
        val fiscalProfile = tenantFiscalProfileRepository?.findById(tenantId)?.orElse(null)
        val municipioIbge = fiscalProfile?.municipioIbgePadrao ?: "3550308"

        val previewsRetencoes = calcularRetencoes(
            valorServico = valorServicos,
            municipioIbge = municipioIbge,
            naturezaServico = "TERCEIRIZACAO",
            tenantId = tenantId,
            tomadorCnpj = tomadorCnpj
        )
        val iss = previewsRetencoes.filter { it.tipo == "ISS" }.sumOf { it.valorRetido }
        val federais = previewsRetencoes.filter { it.tipo != "ISS" }.sumOf { it.valorRetido }
        val totalRetencoes = previewsRetencoes.sumOf { it.valorRetido }
        val valorLiquido = valorServicos.subtract(totalRetencoes)

        // Gera número simples (em produção viria de sequência ou prefeitura)
        val numero = "NFS-" + System.currentTimeMillis().toString().takeLast(8)
        val dataEmissao = LocalDate.now()

        val nfs = NotaFiscalServico(
            tenantId = tenantId,
            numero = numero,
            serie = "1",
            dataEmissao = dataEmissao,
            tomadorCnpj = tomadorCnpj,
            tomadorRazaoSocial = contract?.orgao,
            contratoId = contratoId,
            measurementId = measurementId,
            valorServicos = valorServicos,
            valorLiquido = valorLiquido,
            issRetido = iss,
            outrasRetencoes = federais,
            status = "EMITIDA",
            xml = nfsEmissaoService?.gerarXmlNfs(
                numero = numero,
                tomadorCnpj = tomadorCnpj,
                tomadorRazaoSocial = contract?.orgao ?: "ÓRGÃO PÚBLICO",
                valorServicos = valorServicos,
                iss = iss,
                outrasRetencoes = federais,
                dataEmissao = dataEmissao,
                prestadorCnpj = resolveCnpjPrestador(tenantId),
                codigoMunicipio = municipioIbge
            ) ?: gerarXmlNfsStub(numero, tomadorCnpj, valorServicos, iss, federais, dataEmissao, resolveCnpjPrestador(tenantId))
        )

        val savedNfs = notaFiscalRepository.save(nfs)

        nfsEmissaoService?.transmitir(savedNfs.xml ?: "", savedNfs.numero)

        nfsDanfseService?.gerarDanfseHtml(savedNfs)?.let { html ->
            savedNfs.danfseHtml = html
            notaFiscalRepository.save(savedNfs)
        }

        persistirRetencoesNfs(savedNfs.id!!, previewsRetencoes, tenantId)

        val valorInss = previewsRetencoes.filter { it.tipo == "INSS" }.sumOf { it.valorRetido }
        reinfNfsIntegrationService?.transmitirRetencoesNfs(
            competencia = dataEmissao.withDayOfMonth(1),
            cnpjPrestador = resolveCnpjPrestador(tenantId),
            cnpjTomador = tomadorCnpj,
            valorServicos = valorServicos,
            valorRetencaoInss = valorInss
        )?.let { protocolo ->
            savedNfs.reinfProtocolo = protocolo
            notaFiscalRepository.save(savedNfs)
        }

        auditService?.registrar(tenantId, "NFS_E", savedNfs.id, "EMITIR", "sistema", "Medição $measurementId contrato $contratoId")

        // Atualiza a Conta a Receber (se já existir) - Fase 4 Polish
        val ars = contaAReceberRepository.findByTenantIdAndMeasurementId(tenantId, measurementId)
        ars.forEach { ar ->
            ar.notaFiscalId = savedNfs.id
            ar.valorLiquido = valorLiquido
            ar.status = "FATURADO"   // Fecha o fluxo financeiro
            contaAReceberRepository.save(ar)
        }

        try {
            contabilidadeService?.lancarNfsEmitida(
                tenantId = tenantId,
                nfsId = savedNfs.id!!,
                measurementId = measurementId,
                contratoId = contratoId,
                valorServicos = valorServicos,
                valorLiquido = valorLiquido,
                totalRetencoes = totalRetencoes,
                data = dataEmissao,
                numeroNfs = numero
            )
        } catch (e: Exception) {
            logger.warn("Falha ao lançar NFS-e na contabilidade: ${e.message}")
        }

        if (workflowProperties.autoEmailOnEmit) {
            try {
                nfsOrgaoEmailService?.enviarDanfseAoOrgao(savedNfs.id!!, tenantId)
            } catch (e: Exception) {
                logger.warn("Falha ao enviar DANFSE ao órgão: ${e.message}")
            }
        }

        ars.firstOrNull()?.id?.let { arId ->
            nfsCobrancaWorkflowStarter?.iniciar(savedNfs.id!!, arId, tenantId)
        } ?: nfsCobrancaWorkflowStarter?.iniciar(savedNfs.id!!, null, tenantId)

        domainEventPublisher?.publish(
            invoiceIssuedEvent(
                tenantId = tenantId,
                nfsId = savedNfs.id!!,
                contractId = contratoId,
                numero = numero,
                valor = valorServicos
            )
        )

        return savedNfs
    }

    fun enviarDanfseOrgao(nfId: UUID, tenantId: UUID, email: String? = null) =
        nfsOrgaoEmailService?.enviarDanfseAoOrgao(nfId, tenantId, email)
            ?: throw FinanceiroBusinessException("Serviço de e-mail NFS-e indisponível")

    fun sugerirMatchesConciliacao(
        contaBancariaId: UUID,
        periodoInicio: LocalDate,
        periodoFim: LocalDate,
        tenantId: UUID
    ): List<MatchCandidate> {
        val matchService = nfseExtratoMatchService ?: return emptyList()
        val extratos = extratoRepository.findByTenantIdAndContaBancariaIdAndConciliadoFalse(tenantId, contaBancariaId)
            .filter { it.data in periodoInicio..periodoFim && it.tipo == "CREDITO" }
        val transacoes = transacaoFinanceiraRepository.findByTenantIdAndContaBancariaId(tenantId, contaBancariaId)
            .filter { !it.conciliado && it.data in periodoInicio..periodoFim }
        val nfs = notaFiscalRepository.findByTenantId(tenantId)
            .filter { it.status == "EMITIDA" || it.status == "AUTORIZADA" }

        val sugestoes = mutableListOf<MatchCandidate>()
        extratos.forEach { ext ->
            transacoes.forEach { tr ->
                val m = matchService.matchExtratoToTransacao(ext, tr)
                if (m.confidence >= BigDecimal("55.00")) sugestoes += m
            }
            matchService.matchExtratoToNfse(ext, nfs)?.let { sugestoes += it }
        }
        return sugestoes.sortedByDescending { it.confidence }
    }

    @Transactional
    fun cancelarNfs(nfsId: UUID, motivo: String, tenantId: UUID): NotaFiscalServico {
        val nfs = notaFiscalRepository.findById(nfsId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("NFS-e não encontrada") }

        if (nfs.status != "EMITIDA") {
            throw FinanceiroBusinessException("Só é possível cancelar NFS-e com status EMITIDA")
        }

        nfs.status = "CANCELADA"
        nfs.observacoes = (nfs.observacoes ?: "") + " | Cancelada: $motivo"

        // Reverte status da Conta a Receber, se existir
        if (nfs.measurementId != null) {
            val ars = contaAReceberRepository.findByTenantIdAndMeasurementId(tenantId, nfs.measurementId!!)
            ars.forEach { ar ->
                if (ar.notaFiscalId == nfs.id) {
                    ar.status = "ABERTO"
                    ar.notaFiscalId = null
                    contaAReceberRepository.save(ar)
                }
            }
        }

        // Reverte lançamento contábil automaticamente (melhorado - Bloco Atual)
        try {
            contabilidadeService?.let { contab ->
                val receita = contab.findContaPorCodigo("4.1.01", tenantId)   // Receita
                val clientes = contab.findContaPorCodigo("1.1.02", tenantId)  // Clientes

                if (receita != null && clientes != null) {
                    contab.lancar(
                        tenantId = tenantId,
                        data = LocalDate.now(),
                        contaDebitoId = receita.id!!,     // Estorna receita (débito na receita)
                        contaCreditoId = clientes.id!!,   // Estorna cliente
                        valor = nfs.valorLiquido,
                        historico = "Cancelamento NFS-e #${nfs.numero} - Motivo: $motivo",
                        origemTipo = "CANCELAMENTO_NFS_E",
                        origemId = nfs.id,
                        contratoId = nfs.contratoId
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Falha ao reverter lançamento contábil do cancelamento de NFS-e: ${e.message}")
        }

        // Gera e transmite XML de cancelamento
        val xmlCancel = nfsEmissaoService?.gerarCancelamentoNfs(
            numeroNfse = nfs.numero,
            codigoVerificacao = nfs.codigoVerificacao ?: "N/A",
            motivo = motivo,
            prestadorCnpj = resolveCnpjPrestador(tenantId)
        )
        xmlCancel?.let { xml ->
            val result = nfsEmissaoService?.transmitirCancelamento(xml, nfs.numero)
            nfs.observacoes = (nfs.observacoes ?: "") + " | Cancel protocol: ${result?.protocolNumber} (${result?.message})"
            auditService?.registrar(tenantId, "NFS_E", nfs.id, "CANCELAR", "sistema", result?.message ?: motivo)
        }

        return notaFiscalRepository.save(nfs)
    }

    fun consultarNfs(numero: String, tomadorCnpj: String, tenantId: UUID): NotaFiscalServico? {
        val nfs = notaFiscalRepository.findByTenantIdAndNumeroAndSerie(tenantId, numero, "1")
        return nfs?.takeIf { it.tomadorCnpj == tomadorCnpj || tomadorCnpj.isBlank() }
    }

    // ==================== FGTS Digital e DCTFWeb (Fase 4 Polish) ====================

    fun gerarEventoFGTS(competencia: LocalDate, totalFolha: BigDecimal, tenantId: UUID): String {
        val totalFgts = totalFolha.multiply(BigDecimal("0.08"))
        val cnpj = resolveCnpjPrestador(tenantId)
        return fgtsDigitalService?.gerarEventoS1210(competencia, totalFolha, totalFgts, cnpj)
            ?: "FGTS Digital Service não disponível"
    }

    fun gerarGuiaFGTS(competencia: LocalDate, valorFgts: BigDecimal, tenantId: UUID): String {
        return fgtsDigitalService?.gerarGuiaFGTS(competencia, valorFgts, resolveCnpjPrestador(tenantId))
            ?: "FGTS Digital Service não disponível"
    }

    fun gerarDCTFWeb(competencia: LocalDate, totalRetencoes: BigDecimal, tenantId: UUID): String {
        return dctfWebService?.gerarDCTFWeb(competencia, resolveCnpjPrestador(tenantId), totalRetencoes)
            ?: "DCTFWeb Service não disponível"
    }

    /**
     * Exporta CNAB 240 para pagamento de várias Contas a Pagar.
     */
    fun exportarCnab240Pagamentos(
        contas: List<ContaAPagar>,
        agencia: String,
        conta: String,
        dv: String,
        cnpj: String,
        nomeEmpresa: String,
        dataPagamento: LocalDate
    ): String {
        return cnabExportService?.gerarCnab240Pagamentos(
            contas, agencia, conta, dv, cnpj, nomeEmpresa, dataPagamento
        ) ?: "CNAB Export Service não disponível"
    }

    fun exportarOfxConciliacao(
        contaBancariaId: String,
        itens: List<ExtratoBancarioItem>,
        dataInicio: LocalDate,
        dataFim: LocalDate,
        saldoFinal: BigDecimal
    ): String {
        return ofxExportService?.gerarOfxExtrato(
            contaBancariaId, itens, dataInicio, dataFim, saldoFinal
        ) ?: "OFX Export Service não disponível"
    }

    fun gerarRelatorioPosicaoCaixa(inicio: LocalDate, fim: LocalDate, tenantId: UUID): String {
        val transacoes = transacaoFinanceiraRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
        return financialReportService?.gerarPosicaoCaixaPorPeriodo(transacoes, inicio, fim)
            ?: "Financial Report Service não disponível"
    }

    fun gerarAgingContasAReceber(tenantId: UUID, dataCorte: LocalDate = LocalDate.now()): AgingReportService.AgingReport {
        val contas = contaAReceberRepository.findByTenantIdAndStatus(tenantId, "ABERTO")
        return agingReportService?.gerarAgingContasAReceber(contas, dataCorte)
            ?: AgingReportService.AgingReport("CONTAS_A_RECEBER", dataCorte, emptyList(), BigDecimal.ZERO)
    }

    fun gerarAgingContasAPagar(tenantId: UUID, dataCorte: LocalDate = LocalDate.now()): AgingReportService.AgingReport {
        val contas = contaAPagarRepository.findByTenantIdAndStatus(tenantId, "ABERTO")
        return agingReportService?.gerarAgingContasAPagar(contas, dataCorte)
            ?: AgingReportService.AgingReport("CONTAS_A_PAGAR", dataCorte, emptyList(), BigDecimal.ZERO)
    }

    fun gerarCalendarioObrigacoes(mes: Int, ano: Int, tenantId: UUID): List<TaxCalendarService.Obrigacao> {
        val retencoes = retencaoRepository.findByTenantIdAndDataVencimentoBetween(
            tenantId,
            LocalDate.of(ano, mes, 1),
            LocalDate.of(ano, mes, LocalDate.of(ano, mes, 1).lengthOfMonth())
        )
        return taxCalendarService?.gerarCalendario(retencoes, mes, ano) ?: emptyList()
    }

    // ============================================================
    // GERAÇÃO DE CONTAS A PAGAR A PARTIR DA FOLHA (Curto Prazo)
    // ============================================================

    @Transactional
    fun gerarContasAPagarDaFolha(
        payslipsAprovados: List<com.contractops.api.rh.domain.Payslip>,
        tenantId: UUID,
        vencimentoPadrao: LocalDate = LocalDate.now().plusDays(5)
    ): List<ContaAPagar> {
        val criadas = mutableListOf<ContaAPagar>()

        payslipsAprovados.filter { it.status == "APPROVED" || it.status == "EXPORTED" }.forEach { payslip ->
            // Evita duplicar
            val jaExiste = contaAPagarRepository.findByTenantIdAndOrigemAndOrigemId(tenantId, "PAYSLIP", payslip.id!!).isNotEmpty()
            if (jaExiste) return@forEach

            val conta = ContaAPagar(
                tenantId = tenantId,
                origem = "PAYSLIP",
                origemId = payslip.id,
                contratoId = payslip.contractId,
                valor = payslip.netAmount,
                vencimento = vencimentoPadrao,
                status = "ABERTO",
                observacoes = "Folha ${payslip.competence} - colaborador ${payslip.employeeId}"
            )
            payslip.contractId?.let { cid ->
                contractRepository?.findById(cid)?.ifPresent { c -> conta.branchId = c.branchId }
            }
            criadas += contaAPagarRepository.save(conta)
        }

        return criadas
    }

    /**
     * Orquestra o fechamento completo da folha:
     * 1. Gera Contas a Pagar (se ainda não existirem)
     * 2. Dispara lançamentos contábeis via ContabilidadeService (se disponível)
     *
     * Este é o principal hook de integração RH → Financeiro → Contabilidade.
     */
    @Transactional
    fun processarFechamentoFolhaCompleto(
        payslipsAprovados: List<com.contractops.api.rh.domain.Payslip>,
        tenantId: UUID,
        contratoId: UUID? = null,
        vencimentoPadrao: LocalDate = LocalDate.now().plusDays(5)
    ): Map<String, Any> {
        // 1. Gera Contas a Pagar
        val contasAPagar = gerarContasAPagarDaFolha(payslipsAprovados, tenantId, vencimentoPadrao)

        // 2. Lança contabilmente (usa o método rico já existente no ContabilidadeService)
        var lancamentosRealizados = 0
        payslipsAprovados.forEach { payslip ->
            try {
                contabilidadeService?.lancarFolhaAprovada(
                    payslipId = payslip.id!!,
                    tenantId = tenantId,
                    contratoId = payslip.contractId ?: contratoId ?: UUID.randomUUID(),
                    valorTotal = payslip.netAmount
                )
                lancamentosRealizados++
            } catch (e: Exception) {
                logger.warn("Falha ao lançar folha ${payslip.id} na contabilidade: ${e.message}")
            }
        }

        return mapOf(
            "contasAPagarGeradas" to contasAPagar.size,
            "lancamentosContabeis" to lancamentosRealizados,
            "totalPayslipsProcessados" to payslipsAprovados.size
        )
    }

    /**
     * Realiza pagamento de Conta a Pagar e cria TransacaoFinanceira automaticamente.
     */
    @Transactional
    fun pagarContaAPagar(
        contaAPagarId: UUID,
        data: LocalDate,
        valor: BigDecimal,
        contaBancariaId: UUID,
        formaPagamento: String,
        tenantId: UUID
    ): Pagamento {
        validarPeriodoAberto(tenantId, data)

        val contaPagar = contaAPagarRepository.findById(contaAPagarId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Conta a Pagar não encontrada") }

        if (contaPagar.status == "PAGO") {
            throw FinanceiroBusinessException("Conta a Pagar já está paga")
        }

        val pagamento = Pagamento(
            tenantId = tenantId,
            contaAPagarId = contaAPagarId,
            data = data,
            valor = valor,
            contaBancariaId = contaBancariaId,
            formaPagamento = formaPagamento
        )
        val savedPagamento = pagamentoRepository.save(pagamento)

        contaPagar.status = "PAGO"
        contaPagar.dataPagamento = data
        contaPagar.valorPago = valor
        contaAPagarRepository.save(contaPagar)

        // Cria Transação Financeira (SAÍDA) automaticamente
        val transacao = TransacaoFinanceira(
            tenantId = tenantId,
            data = data,
            contaBancariaId = contaBancariaId,
            tipo = "SAIDA",
            valor = valor,
            origemTipo = "PAGAMENTO",
            origemId = savedPagamento.id,
            historico = "Pagamento ${contaPagar.origem} - ${contaAPagarId}",
            conciliado = false
        )
        transacaoFinanceiraRepository.save(transacao)

        // Atualiza saldo bancário
        atualizarSaldoBancario(contaBancariaId, valor, "SAIDA")

        auditService?.registrar(tenantId, "CONTA_A_PAGAR", contaAPagarId, "BAIXAR", "sistema", "Pagamento $formaPagamento R$ $valor")

        // Dispara lançamento contábil automático (melhorado - Bloco Atual)
        try {
            contabilidadeService?.let { contab ->
                val banco = contab.findContaPorCodigo("1.1.01", tenantId)                    // Caixa e Bancos
                val salariosPagar = contab.findContaPorCodigo("2.1.01", tenantId)           // Salários a Pagar (ou fornecedor dependendo da origem)

                if (banco != null && salariosPagar != null) {
                    contab.lancar(
                        tenantId = tenantId,
                        data = data,
                        contaDebitoId = salariosPagar.id!!,   // Baixa do passivo
                        contaCreditoId = banco.id!!,          // Saída do banco
                        valor = valor,
                        historico = "Pagamento ${contaPagar.origem} - ${contaPagar.observacoes}",
                        origemTipo = "PAGAMENTO",
                        origemId = savedPagamento.id,
                        contratoId = contaPagar.contratoId
                    )
                } else {
                    logger.warn("Contas (1.1.01 Banco ou 2.1.01 Salários a Pagar) não encontradas para pagamento de AP")
                }
            }
        } catch (e: Exception) {
            logger.warn("Falha ao lançar contabilmente o pagamento: ${e.message}")
        }

        return savedPagamento
    }

    /**
     * Simulação de aprovação multi-nível para pagamentos de alto valor (Fase 4 Polish).
     */
    @Transactional
    fun aprovarPagamentoPendente(contaAPagarId: UUID, nivel: Int, usuario: String, tenantId: UUID): ContaAPagar {
        val conta = contaAPagarRepository.findById(contaAPagarId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Conta a Pagar não encontrada") }

        if (conta.valor > BigDecimal("50000") && nivel < 2) {
            throw FinanceiroBusinessException("Pagamento acima de R$ 50.000 exige aprovação de nível 2")
        }

        // Simples: marca como APROVADO se nível suficiente
        if (conta.status == "ABERTO") {
            conta.status = "APROVADO"
        }

        // Em produção teria histórico de aprovações
        return contaAPagarRepository.save(conta)
    }

    /** INTEGRATION_STUB: SPEC §16 — NFS-e municipal / Nacional; XML simulado, sem envio à prefeitura. */
    private fun gerarXmlNfsStub(
        numero: String,
        tomadorCnpj: String,
        valorServicos: BigDecimal,
        iss: BigDecimal,
        outrasRetencoes: BigDecimal,
        dataEmissao: LocalDate,
        prestadorCnpj: String = "00000000000000"
    ): String {
        logger.warn("Generating stub NFS-e XML — real NFSe gateway not configured")
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                <infNFe Id="NFS$numero">
                    <ide>
                        <cNF>$numero</cNF>
                        <natOp>Prestação de Serviços</natOp>
                        <dEmi>${dataEmissao}</dEmi>
                    </ide>
                    <emit>
                        <CNPJ>$prestadorCnpj</CNPJ>
                        <xNome>CONTRATOPS DEMO LTDA</xNome>
                    </emit>
                    <dest>
                        <CNPJ>$tomadorCnpj</CNPJ>
                    </dest>
                    <det>
                        <prod>
                            <vProd>$valorServicos</vProd>
                        </prod>
                    </det>
                    <total>
                        <ISSQN>
                            <vISS>$iss</vISS>
                        </ISSQN>
                        <retencoes>
                            <vRetOutras>$outrasRetencoes</vRetOutras>
                        </retencoes>
                    </total>
                </infNFe>
            </NFe>
        """.trimIndent()
    }


    // ============================================================
    // CONTAS A PAGAR (AP) - especialmente vindas da Folha
    // ============================================================

    @Transactional
    fun criarContasAPagarDaFolhaAprovada(
        payslipId: UUID,
        tenantId: UUID,
        contratoId: UUID,
        breakdown: Map<String, BigDecimal>
    ): List<ContaAPagar> {
        val contas = mutableListOf<ContaAPagar>()
        val dataBase = LocalDate.now().plusDays(5)

        // Líquido a pagar aos colaboradores
        breakdown["liquido"]?.let { valor ->
            if (valor > BigDecimal.ZERO) {
                val cap = ContaAPagar(
                    tenantId = tenantId,
                    origem = "PAYSLIP",
                    origemId = payslipId,
                    contratoId = contratoId,
                    valor = valor,
                    vencimento = dataBase,
                    status = "ABERTO",
                    observacoes = "Líquido da folha - Payslip $payslipId"
                )
                contas.add(contaAPagarRepository.save(cap))
            }
        }

        // INSS (parte patronal + retida) - vence dia 20
        breakdown["inss"]?.let { valor ->
            if (valor > BigDecimal.ZERO) {
                val cap = ContaAPagar(
                    tenantId = tenantId,
                    origem = "RETENCAO_TRIBUTARIA",
                    origemId = payslipId,
                    contratoId = contratoId,
                    valor = valor,
                    vencimento = dataBase.plusDays(15),
                    status = "ABERTO",
                    observacoes = "INSS - Folha $payslipId"
                )
                val saved = contaAPagarRepository.save(cap)
                contas.add(saved)

                // Cria retenção/GPS
                val ret = RetencaoTributaria(
                    tenantId = tenantId,
                    contaAPagarId = saved.id,
                    tipo = "INSS",
                    aliquota = BigDecimal("0.08"),
                    baseCalculo = valor,
                    valorRetido = valor,
                    codigoReceita = "2990",
                    dataVencimento = saved.vencimento,
                    status = "PENDENTE"
                )
                retencaoRepository.save(ret)
            }
        }

        // FGTS - vence dia 7 do mês seguinte
        breakdown["fgts"]?.let { valor ->
            if (valor > BigDecimal.ZERO) {
                val cap = ContaAPagar(
                    tenantId = tenantId,
                    origem = "RETENCAO_TRIBUTARIA",
                    origemId = payslipId,
                    contratoId = contratoId,
                    valor = valor,
                    vencimento = dataBase.plusDays(2),
                    status = "ABERTO",
                    observacoes = "FGTS - Folha $payslipId"
                )
                contas.add(contaAPagarRepository.save(cap))
            }
        }

        return contas
    }

    // ============================================================
    // TESOURARIA + CONCILIAÇÃO
    // ============================================================

    @Transactional
    fun importarExtrato(
        contaBancariaId: UUID,
        itens: List<ExtratoBancarioItem>,
        tenantId: UUID
    ): Int {
        // Validação básica
        val conta = contaBancariaRepository.findById(contaBancariaId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Conta bancária não encontrada para este tenant") }

        var importados = 0
        val salvos = mutableListOf<ExtratoBancarioItem>()

        for (item in itens) {
            // Evita duplicação simples (mesma data + documento + valor)
            val jaExiste = extratoRepository.findByTenantIdAndContaBancariaId(tenantId, contaBancariaId)
                .any { it.data == item.data && it.documento == item.documento && it.valor.compareTo(item.valor) == 0 }

            if (!jaExiste) {
                val novo = ExtratoBancarioItem(
                    tenantId = tenantId,
                    contaBancariaId = contaBancariaId,
                    data = item.data,
                    documento = item.documento,
                    historico = item.historico,
                    valor = item.valor,
                    tipo = item.tipo.uppercase()
                )
                salvos += extratoRepository.save(novo)
                importados++
            }
        }

        // Opcional: atualizar saldo da conta com base no extrato (não obrigatório, mas útil)
        if (salvos.isNotEmpty()) {
            val saldoExtrato = salvos.sumOf { if (it.tipo == "CREDITO") it.valor else it.valor.negate() }
            // Não sobrescreve automaticamente o saldo_atual (o usuário decide na conciliação)
        }

        return importados
    }

    @Transactional
    fun conciliarAutomatico(
        contaBancariaId: UUID,
        periodoInicio: LocalDate,
        periodoFim: LocalDate,
        tenantId: UUID
    ): ConciliacaoBancaria {
        val extratosPendentes = extratoRepository.findByTenantIdAndContaBancariaIdAndConciliadoFalse(tenantId, contaBancariaId)
            .filter { it.data in periodoInicio..periodoFim }

        val transacoesPendentes = transacaoFinanceiraRepository.findByTenantIdAndContaBancariaId(tenantId, contaBancariaId)
            .filter { !it.conciliado && it.data in periodoInicio..periodoFim }
            .toMutableList()

        var conciliados = 0
        val conciliadosExtratoIds = mutableListOf<UUID>()

        // === Motor de Conciliação Avançado (Curto Prazo / Fase 3) ===
        // Prioridade: 1) Documento  2) Valor + data (±2 dias)  3) Valor + palavras-chave
        for (extrato in extratosPendentes) {
            var match = transacoesPendentes.find { trans ->
                !trans.conciliado &&
                extrato.documento != null &&
                trans.historico.contains(extrato.documento!!, ignoreCase = true)
            }

            if (match == null) {
                match = transacoesPendentes.find { trans ->
                    !trans.conciliado &&
                    trans.valor.compareTo(extrato.valor) == 0 &&
                    (trans.data == extrato.data || trans.data == extrato.data.plusDays(1) || trans.data == extrato.data.minusDays(1) ||
                     trans.data == extrato.data.plusDays(2) || trans.data == extrato.data.minusDays(2))
                }
            }

            if (match == null) {
                val chaves = extrato.historico.uppercase().split(Regex("[\\s\\-\\.\\,/]")).filter { it.length >= 4 }.take(3)
                match = transacoesPendentes.find { trans ->
                    !trans.conciliado &&
                    trans.valor.compareTo(extrato.valor) == 0 &&
                    chaves.any { chave -> trans.historico.uppercase().contains(chave) }
                }
            }

            if (match == null && extrato.tipo == "CREDITO" && nfseExtratoMatchService != null) {
                val nfs = notaFiscalRepository.findByTenantId(tenantId)
                val nfMatch = nfseExtratoMatchService.matchExtratoToNfse(extrato, nfs)
                if (nfMatch != null) {
                    extrato.notaFiscalId = nfMatch.notaFiscalId
                    extrato.matchConfidence = nfMatch.confidence
                    extrato.matchMetodo = nfMatch.metodo
                    val nf = nfs.find { it.id == nfMatch.notaFiscalId }
                    match = transacoesPendentes.find { trans ->
                        !trans.conciliado &&
                        nf != null &&
                        (trans.historico.contains(nf.numero, ignoreCase = true) ||
                            trans.valor.compareTo(extrato.valor) == 0)
                    }
                    if (match == null && nfMatch.confidence >= BigDecimal("75.00")) {
                        extrato.conciliado = true
                        extratoRepository.save(extrato)
                        conciliados++
                        conciliadosExtratoIds += extrato.id!!
                        continue
                    }
                }
            }

            if (match != null) {
                val scored = nfseExtratoMatchService?.matchExtratoToTransacao(extrato, match)
                extrato.conciliado = true
                extrato.transacaoFinanceiraId = match.id
                scored?.let {
                    extrato.matchConfidence = it.confidence
                    extrato.matchMetodo = it.metodo
                }
                extratoRepository.save(extrato)

                match.conciliado = true
                transacaoFinanceiraRepository.save(match)

                conciliados++
                conciliadosExtratoIds += extrato.id!!
                transacoesPendentes.remove(match)
            }
        }

        val saldoExtrato = extratosPendentes.sumOf { if (it.tipo == "CREDITO") it.valor else it.valor.negate() }
        val saldoSistemaNaoConciliado = transacoesPendentes.filter { !it.conciliado }.sumOf { if (it.tipo == "ENTRADA") it.valor else it.valor.negate() }
        val diferenca = saldoExtrato.subtract(saldoSistemaNaoConciliado)

        val conciliacao = ConciliacaoBancaria(
            tenantId = tenantId,
            contaBancariaId = contaBancariaId,
            dataInicio = periodoInicio,
            dataFim = periodoFim,
            saldoExtrato = saldoExtrato,
            saldoSistema = saldoSistemaNaoConciliado,
            diferenca = diferenca,
            status = when {
                conciliados == 0 && extratosPendentes.isEmpty() -> "SEM_MOVIMENTO"
                conciliados == extratosPendentes.size -> "CONCILIADO"
                conciliados > 0 -> "PARCIAL"
                else -> "DIVERGENTE"
            },
            observacoes = "Conciliação automática de alta precisão — $conciliados de ${extratosPendentes.size} itens conciliados. Diferença: R$ ${diferenca.setScale(2)}"
        )

        val saved = conciliacaoRepository.save(conciliacao)

        extratosPendentes.filter { it.conciliado }.forEach {
            it.conciliacaoId = saved.id
            extratoRepository.save(it)
        }

        return saved
    }

    // ============================================================
    // CONCILIAÇÃO — LISTAGEM DE PENDENTES + MANUAL (para UI real)
    // ============================================================

    fun listarExtratosPendentes(
        contaBancariaId: UUID,
        inicio: LocalDate,
        fim: LocalDate,
        tenantId: UUID
    ): List<ExtratoBancarioItem> {
        return extratoRepository.findByTenantIdAndContaBancariaIdAndConciliadoFalse(tenantId, contaBancariaId)
            .filter { it.data in inicio..fim }
    }

    fun listarTransacoesPendentes(
        contaBancariaId: UUID,
        inicio: LocalDate,
        fim: LocalDate,
        tenantId: UUID
    ): List<TransacaoFinanceira> {
        return transacaoFinanceiraRepository.findByTenantIdAndContaBancariaId(tenantId, contaBancariaId)
            .filter { !it.conciliado && it.data in inicio..fim }
    }

    @Transactional
    fun conciliarManual(
        contaBancariaId: UUID,
        extratoId: UUID,
        transacaoId: UUID,
        tenantId: UUID
    ): ConciliacaoBancaria {
        val extrato = extratoRepository.findById(extratoId)
            .filter { it.tenantId == tenantId && it.contaBancariaId == contaBancariaId && !it.conciliado }
            .orElseThrow { FinanceiroBusinessException("Extrato não encontrado ou já conciliado") }

        val transacao = transacaoFinanceiraRepository.findById(transacaoId)
            .filter { it.tenantId == tenantId && it.contaBancariaId == contaBancariaId && !it.conciliado }
            .orElseThrow { FinanceiroBusinessException("Transação não encontrada ou já conciliada") }

        // Link e marca
        extrato.conciliado = true
        extrato.transacaoFinanceiraId = transacao.id
        extratoRepository.save(extrato)

        transacao.conciliado = true
        transacaoFinanceiraRepository.save(transacao)

        // Registra resumo de conciliação
        val conciliacao = ConciliacaoBancaria(
            tenantId = tenantId,
            contaBancariaId = contaBancariaId,
            dataInicio = extrato.data,
            dataFim = extrato.data,
            saldoExtrato = if (extrato.tipo == "CREDITO") extrato.valor else extrato.valor.negate(),
            saldoSistema = if (transacao.tipo == "ENTRADA") transacao.valor else transacao.valor.negate(),
            diferenca = BigDecimal.ZERO,
            status = "CONCILIADO",
            observacoes = "Conciliação manual: ${extrato.historico} ↔ ${transacao.historico}"
        )
        val saved = conciliacaoRepository.save(conciliacao)

        extrato.conciliacaoId = saved.id
        extratoRepository.save(extrato)

        return saved
    }

    /**
     * Importa extrato bancário (suporte básico a CSV e OFX-like).
     * Fase 4 Polish.
     */
    @Transactional
    fun importarExtratoMelhorado(
        contaBancariaId: UUID,
        conteudo: String,
        formato: String, // "CSV" ou "OFX"
        tenantId: UUID
    ): Int {
        val linhas = conteudo.lines().filter { it.isNotBlank() }
        var importados = 0

        linhas.forEach { linha ->
            try {
                val item = when (formato.uppercase()) {
                    "CSV" -> parseCsvLinha(linha, contaBancariaId, tenantId)
                    "OFX" -> parseOfxLikeLinha(linha, contaBancariaId, tenantId)
                    else -> null
                }

                if (item != null) {
                    extratoRepository.save(item)
                    importados++
                }
            } catch (e: Exception) {
                // Ignora linhas ruins
            }
        }

        return importados
    }

    private fun parseCsvLinha(linha: String, contaId: UUID, tenantId: UUID): ExtratoBancarioItem? {
        val partes = linha.split(",")
        if (partes.size < 4) return null

        return ExtratoBancarioItem(
            tenantId = tenantId,
            contaBancariaId = contaId,
            data = LocalDate.parse(partes[0].trim()),
            documento = partes.getOrNull(1)?.trim(),
            historico = partes.getOrNull(2)?.trim() ?: "Importado",
            valor = BigDecimal(partes.getOrNull(3)?.trim() ?: "0"),
            tipo = if ((partes.getOrNull(3)?.toDoubleOrNull() ?: 0.0) >= 0) "CREDITO" else "DEBITO"
        )
    }

    private fun parseOfxLikeLinha(linha: String, contaId: UUID, tenantId: UUID): ExtratoBancarioItem? {
        // Parsing simples para formato OFX-like (STMTTRN)
        if (!linha.contains("STMTTRN")) return null

        return ExtratoBancarioItem(
            tenantId = tenantId,
            contaBancariaId = contaId,
            data = LocalDate.now(),
            historico = "Importado via OFX",
            valor = BigDecimal("0"),
            tipo = "CREDITO"
        )
    }

    // ============================================================
    // FLUXO DE CAIXA REAL + PROJETADO (Coração do CFO)
    // ============================================================

    fun calcularFluxoCaixaReal(inicio: LocalDate, fim: LocalDate, tenantId: UUID): Map<String, Any> {
        val transacoes = transacaoFinanceiraRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)

        val entradas = transacoes.filter { it.tipo == "ENTRADA" }.sumOf { it.valor }
        val saidas = transacoes.filter { it.tipo == "SAIDA" }.sumOf { it.valor }

        val saldo = entradas.subtract(saidas)

        return mapOf(
            "periodo" to "$inicio a $fim",
            "entradas" to entradas,
            "saidas" to saidas,
            "saldoLiquido" to saldo,
            "totalTransacoes" to transacoes.size
        )
    }

    fun calcularFluxoCaixaProjetado(
        horizonteSemanas: Int = 13,
        cenario: String = "BASE",
        tenantId: UUID
    ): Map<String, Any> {
        val hoje = LocalDate.now()
        val fim = hoje.plusWeeks(horizonteSemanas.toLong())

        // Busca previsões já existentes
        val previsoes = previsaoRepository.findByTenantIdAndDataBetween(tenantId, hoje, fim)
            .filter { it.cenario == cenario }

        // Se não tiver muitas previsões, gera projeção básica inteligente
        val projecoes = if (previsoes.size < 5) {
            gerarProjecoesAutomaticas(hoje, fim, horizonteSemanas, cenario, tenantId)
        } else {
            previsoes
        }

        val entradasProjetadas = projecoes.filter { it.tipo.contains("RECEBIMENTO") || it.tipo.contains("ENTRADA") }.sumOf { it.valor }
        val saidasProjetadas = projecoes.filter { it.tipo.contains("PAGAMENTO") || it.tipo.contains("SAIDA") || it.tipo.contains("FOLHA") || it.tipo.contains("TRIBUTO") }.sumOf { it.valor }

        return mapOf(
            "horizonteSemanas" to horizonteSemanas,
            "cenario" to cenario,
            "dataInicio" to hoje.toString(),
            "dataFim" to fim.toString(),
            "entradasProjetadas" to entradasProjetadas,
            "saidasProjetadas" to saidasProjetadas,
            "saldoProjetado" to entradasProjetadas.subtract(saidasProjetadas),
            "previsoes" to projecoes.map { mapOf(
                "data" to it.data,
                "tipo" to it.tipo,
                "valor" to it.valor,
                "probabilidade" to it.probabilidade
            )}
        )
    }

    private fun gerarProjecoesAutomaticas(hoje: LocalDate, fim: LocalDate, semanas: Int, cenario: String, tenantId: UUID): List<PrevisaoFinanceira> {
        val lista = mutableListOf<PrevisaoFinanceira>()

        contaAReceberRepository.findByTenantIdAndStatusIn(tenantId, listOf("ABERTO", "FATURADO", "PARCIAL"))
            .filter { it.vencimento in hoje..fim }
            .forEach { ar ->
                lista.add(
                    PrevisaoFinanceira(
                        tenantId = tenantId,
                        data = ar.vencimento,
                        tipo = "RECEBIMENTO_PROJETADO",
                        valor = ar.valorLiquido.subtract(ar.valorRecebido ?: BigDecimal.ZERO).max(BigDecimal.ZERO),
                        probabilidade = if (cenario == "OTIMISTA") 92 else if (cenario == "PESSIMISTA") 65 else 82,
                        cenario = cenario,
                        descricao = "AR contrato ${ar.contratoId} venc. ${ar.vencimento}",
                        origem = "CONTAS_A_RECEBER"
                    )
                )
            }

        listOf("ABERTO", "APROVADO").flatMap { status ->
            contaAPagarRepository.findByTenantIdAndStatus(tenantId, status)
        }.filter { it.vencimento in hoje..fim }.forEach { ap ->
            lista.add(
                PrevisaoFinanceira(
                    tenantId = tenantId,
                    data = ap.vencimento,
                    tipo = "PAGAMENTO_${ap.origem}",
                    valor = ap.valor.subtract(ap.valorPago ?: BigDecimal.ZERO).max(BigDecimal.ZERO),
                    probabilidade = 95,
                    cenario = cenario,
                    descricao = "AP ${ap.origem} - ${ap.observacoes?.take(40) ?: ap.id}",
                    origem = ap.origem
                )
            )
        }

        if (lista.size >= 5) return lista

        var data = hoje
        for (i in 1..semanas) {
            val valorReceb = BigDecimal(38000 + (i * 1200))
            lista.add(PrevisaoFinanceira(
                tenantId = tenantId,
                data = data,
                tipo = "RECEBIMENTO_PROJETADO",
                valor = valorReceb,
                probabilidade = if (cenario == "OTIMISTA") 92 else if (cenario == "PESSIMISTA") 65 else 82,
                cenario = cenario,
                descricao = "Recebimento projetado - Semana $i",
                origem = "SISTEMA"
            ))
            data = data.plusDays(7)
        }

        data = hoje.withDayOfMonth(5).plusMonths(1)
        for (i in 0..3) {
            lista.add(PrevisaoFinanceira(
                tenantId = tenantId,
                data = data,
                tipo = "PAGAMENTO_FOLHA",
                valor = BigDecimal(29500),
                probabilidade = 100,
                cenario = cenario,
                descricao = "Folha projetada",
                origem = "RH"
            ))
            data = data.plusMonths(1)
        }

        return lista
    }

    fun simularCenario(
        parametros: Map<String, Any>,
        tenantId: UUID
    ): Map<String, Any> {
        val atrasoRecebimento = (parametros["atrasoMedioRecebimento"] as? Number)?.toInt() ?: 25
        val aumentoFolha = (parametros["aumentoFolha"] as? Number)?.toDouble() ?: 0.0
        val reducaoFaturamento = (parametros["reducaoFaturamento"] as? Number)?.toDouble() ?: 0.0

        val projecaoBase = calcularFluxoCaixaProjetado(13, "BASE", tenantId)
        val entradasBase = projecaoBase["entradasProjetadas"] as BigDecimal

        val entradasAjustadas = entradasBase
            .multiply(BigDecimal(1 - (reducaoFaturamento / 100)))
            .multiply(BigDecimal(1 - (atrasoRecebimento - 25) * 0.01))

        val saidasAjustadas = (projecaoBase["saidasProjetadas"] as BigDecimal)
            .multiply(BigDecimal(1 + (aumentoFolha / 100)))

        return mapOf(
            "cenario" to "SIMULADO",
            "parametros" to parametros,
            "entradasAjustadas" to entradasAjustadas,
            "saidasAjustadas" to saidasAjustadas,
            "saldoFinalSimulado" to entradasAjustadas.subtract(saidasAjustadas),
            "impactoVsBase" to entradasAjustadas.subtract(entradasBase)
        )
    }

    // ============================================================
    // CFO DASHBOARD & INTELIGÊNCIA
    // ============================================================

    fun getCfoDashboard(tenantId: UUID, dataCorte: LocalDate = LocalDate.now()): Map<String, Any> {
        val contas = contaBancariaRepository.findByTenantIdAndAtivaTrue(tenantId)
        val posicaoCaixa = contas.associate { it.bancoNome to it.saldoAtual }
        val totalCaixa = contas.sumOf { it.saldoAtual }

        // AR
        val arsAbertos = contaAReceberRepository.findByTenantIdAndStatus(tenantId, "ABERTO")
        val arsVencidos = arsAbertos.filter { it.vencimento.isBefore(dataCorte) }
        val totalAR = arsAbertos.sumOf { it.valorLiquido }

        // AP
        val apsAbertos = contaAPagarRepository.findByTenantIdAndStatus(tenantId, "ABERTO")
        val totalAP = apsAbertos.sumOf { it.valor }

        // DSO simples (média de dias de recebimento)
        val dso = if (arsAbertos.isNotEmpty()) arsAbertos.map { it.diasAtraso }.average() else 28.0

        // Runway (dias de caixa com burn médio)
        val burnMedioSemanal = totalAP.divide(BigDecimal("4"), 2, RoundingMode.HALF_UP)
        val runwayDias = if (burnMedioSemanal > BigDecimal.ZERO) {
            totalCaixa.divide(burnMedioSemanal, 0, RoundingMode.DOWN).toInt() * 7
        } else 999

        // Aging AR (já existia)
        val agingAR = mapOf(
            "0-30" to arsAbertos.filter { it.diasAtraso <= 30 }.sumOf { it.valorLiquido },
            "31-60" to arsAbertos.filter { it.diasAtraso in 31..60 }.sumOf { it.valorLiquido },
            "61-90" to arsAbertos.filter { it.diasAtraso in 61..90 }.sumOf { it.valorLiquido },
            ">90" to arsAbertos.filter { it.diasAtraso > 90 }.sumOf { it.valorLiquido }
        )

        // === MELHORIAS DO BLOCO B - CFO LITERAL ===
        // Aging AP
        val agingAP = mapOf(
            "0-30" to apsAbertos.filter { it.vencimento.isBefore(dataCorte.plusDays(30)) }.sumOf { it.valor },
            "31-60" to apsAbertos.filter { it.vencimento.isAfter(dataCorte.plusDays(30)) && it.vencimento.isBefore(dataCorte.plusDays(60)) }.sumOf { it.valor },
            "61-90" to apsAbertos.filter { it.vencimento.isAfter(dataCorte.plusDays(60)) && it.vencimento.isBefore(dataCorte.plusDays(90)) }.sumOf { it.valor },
            ">90" to apsAbertos.filter { it.vencimento.isAfter(dataCorte.plusDays(90)) }.sumOf { it.valor }
        )

        // Conciliações pendentes (tesouraria)
        val conciliacoesPendentes = try {
            extratoRepository.findAll().filter { !it.conciliado && it.tenantId == tenantId }.size
        } catch (e: Exception) { 0 }

        // Fluxo de caixa projetado simples próximos 30 dias (usa o motor existente)
        val fluxo30Dias = try {
            calcularFluxoCaixaProjetado(4, "BASE", tenantId)
        } catch (e: Exception) {
            mapOf("entradasProjetadas" to BigDecimal.ZERO, "saidasProjetadas" to BigDecimal.ZERO)
        }

        val entradas30 = (fluxo30Dias["entradasProjetadas"] as? BigDecimal) ?: BigDecimal.ZERO
        val saidas30 = (fluxo30Dias["saidasProjetadas"] as? BigDecimal) ?: BigDecimal.ZERO

        // Alertas expandidos (CFO grade)
        val alertas = mutableListOf<String>()
        if (runwayDias < 30) alertas.add("CRÍTICO: Runway menor que 30 dias!")
        if (arsVencidos.size > 3) alertas.add("ALERTA: ${arsVencidos.size} contas a receber vencidas")
        if (totalCaixa < BigDecimal("50000")) alertas.add("ALERTA: Posição de caixa baixa")
        if (conciliacoesPendentes > 20) alertas.add("ALERTA: $conciliacoesPendentes itens de extrato pendentes de conciliação")
        if (apsAbertos.any { it.vencimento.isBefore(dataCorte) }) alertas.add("ALERTA: Existem Contas a Pagar vencidas")
        if (entradas30 < saidas30) alertas.add("ALERTA: Fluxo projetado dos próximos 30 dias negativo")

        // Próximos vencimentos AP + AR combinados
        val proximosVencimentos = (apsAbertos + arsAbertos.map { ap ->
            // Conversão simples para mostrar AR como "vencimentos"
            ContaAPagar(
                tenantId = ap.tenantId,
                origem = "RECEBER",
                origemId = ap.id,
                valor = ap.valorLiquido.subtract(ap.valorRecebido ?: BigDecimal.ZERO),
                vencimento = ap.vencimento,
                status = ap.status
            )
        }).sortedBy { it.vencimento }.take(8).map {
            mapOf(
                "vencimento" to it.vencimento,
                "valor" to it.valor,
                "tipo" to it.origem,
                "status" to it.status
            )
        }

        return mapOf(
            "dataCorte" to dataCorte.toString(),
            "posicaoCaixa" to posicaoCaixa,
            "totalCaixa" to totalCaixa,
            "totalContasAReceber" to totalAR,
            "totalContasAPagar" to totalAP,
            "kpis" to mapOf(
                "cashRunwayDias" to runwayDias,
                "dsoMedio" to dso,
                "totalARAberto" to totalAR,
                "totalAPAberto" to totalAP,
                "quantidadeARVencida" to arsVencidos.size,
                "conciliacoesPendentes" to conciliacoesPendentes,
                "fluxo30Dias_entradas" to entradas30,
                "fluxo30Dias_saidas" to saidas30
            ),
            "agingAR" to agingAR,
            "agingAP" to agingAP,
            "alertas" to alertas,
            "proximosVencimentos" to proximosVencimentos,
            "resumoTesouraria" to mapOf(
                "conciliacoesPendentes" to conciliacoesPendentes,
                "contasBancariasAtivas" to contas.size
            )
        )
    }

    // ============================================================
    // COMPLIANCE TRIBUTÁRIO-FINANCEIRO
    // ============================================================

    /**
     * Retenções federais/municipais típicas de terceirização (SPEC §21–22).
     * IRRF 1,5% | PIS/COFINS/CSLL (4,65%) | INSS 11% (cessão MO) | ISS conforme município.
     * Produção: integrar perfil tributário por contrato/CCT.
     */
    fun calcularRetencoes(
        valorServico: BigDecimal,
        municipioIbge: String,
        naturezaServico: String,
        tenantId: UUID,
        aplicarInss: Boolean = true,
        tomadorCnpj: String? = null
    ): List<RetencaoCalculadaResponse> {
        if (valorServico <= BigDecimal.ZERO) {
            throw FinanceiroBusinessException("Valor do serviço deve ser positivo para cálculo de retenções")
        }
        val vencimento = proximoDiaUtil(LocalDate.now().plusDays(20))
        val natureza = naturezaServico.uppercase()
        val retencoes = mutableListOf<RetencaoCalculadaResponse>()
        val profile = tenantFiscalProfileRepository?.findById(tenantId)?.orElse(null)
        val simples = profile?.simplesNacional == true
        val desonerada = profile?.desoneracaoFolha == true
        val aliquotaInss = profile?.aliquotaInssRetencao ?: BigDecimal("0.1100")

        if (!simples) {
            retencoes.add(
                montarRetencaoPreview("IRRF", BigDecimal("0.0150"), valorServico, "0561", vencimento,
                    "IRRF — serviços (1,5%): competência documental (DCTFWeb)")
            )
            retencoes.add(
                montarRetencaoPreview("PIS", BigDecimal("0.0065"), valorServico, "5952", vencimento,
                    "PIS — CSRF/PIS-COFINS-CSLL")
            )
            retencoes.add(
                montarRetencaoPreview("COFINS", BigDecimal("0.0300"), valorServico, "5952", vencimento,
                    "COFINS — CSRF/PIS-COFINS-CSLL")
            )
            retencoes.add(
                montarRetencaoPreview("CSLL", BigDecimal("0.0100"), valorServico, "5952", vencimento,
                    "CSLL — CSRF/PIS-COFINS-CSLL (4,65% no total)")
            )
        }

        if (aplicarInss && (natureza.contains("TERCEIR") || natureza.contains("MO") || natureza.contains("LIMPEZA") || natureza.contains("SEGURANCA"))) {
            if (desonerada) {
                val aliqCprb = if (LocalDate.now().year >= 2026) BigDecimal("0.0270") else BigDecimal("0.0360")
                retencoes.add(
                    montarRetencaoPreview("CPRB", aliqCprb, valorServico, "2985", vencimento,
                        "CPRB — desoneração folha (${aliqCprb.multiply(BigDecimal(100))}%)")
                )
            } else {
                retencoes.add(
                    montarRetencaoPreview("INSS", aliquotaInss, valorServico, "2631", vencimento,
                        "INSS — retenção ${aliquotaInss.multiply(BigDecimal(100))}% cessão de mão de obra (EFD-Reinf R-2010)")
                )
            }
        }

        val aliquotaIss = when {
            municipioIbge == "3550308" -> BigDecimal("0.0500")
            municipioIbge.startsWith("41") -> BigDecimal("0.0500")
            else -> BigDecimal("0.0200")
        }
        retencoes.add(
            montarRetencaoPreview("ISS", aliquotaIss, valorServico, "ISS", vencimento,
                "ISS — município $municipioIbge (tomador ${tomadorCnpj?.takeLast(4) ?: "—"})")
        )

        return retencoes
    }

    @Transactional
    fun persistirRetencoesNfs(
        notaFiscalId: UUID,
        previews: List<RetencaoCalculadaResponse>,
        tenantId: UUID
    ): List<RetencaoTributaria> {
        retencaoRepository.findByTenantIdAndNotaFiscalId(tenantId, notaFiscalId).forEach {
            if (it.status == "PENDENTE") retencaoRepository.delete(it)
        }
        return previews.map { p ->
            retencaoRepository.save(
                RetencaoTributaria(
                    tenantId = tenantId,
                    notaFiscalId = notaFiscalId,
                    tipo = p.tipo,
                    aliquota = p.aliquota,
                    baseCalculo = p.baseCalculo,
                    valorRetido = p.valorRetido,
                    codigoReceita = p.codigoReceita,
                    dataVencimento = LocalDate.parse(p.dataVencimento),
                    status = "PENDENTE"
                )
            )
        }
    }

    @Transactional
    fun gerarGuiaDarf(retencaoId: UUID, tenantId: UUID): DarfPreviewResponse {
        val ret = retencaoRepository.findById(retencaoId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Retenção não encontrada") }

        val competencia = LocalDate.now().withDayOfMonth(1)
        val darfTexto = """
            |=== DARF — Prévia ContractOps (não substitui emissão no Sicalc/DCTFWeb) ===
            |Tipo: ${ret.tipo}
            |Código receita: ${ret.codigoReceita ?: "—"}
            |Competência: ${competencia}
            |Base de cálculo: R$ ${ret.baseCalculo}
            |Alíquota: ${ret.aliquota.multiply(BigDecimal(100))}%
            |Valor principal: R$ ${ret.valorRetido}
            |Vencimento sugerido: ${ret.dataVencimento}
            |---
            |Reforma tributária (Ago/2026): incluir CBS/IBS nos DFEs quando parametrizado.
        """.trimMargin()

        ret.darfGerado = darfTexto
        ret.status = "GUIA_GERADA"
        retencaoRepository.save(ret)

        return DarfPreviewResponse(
            retencaoId = retencaoId,
            tipo = ret.tipo,
            codigoReceita = ret.codigoReceita,
            valor = ret.valorRetido,
            competencia = competencia.toString(),
            darfTexto = darfTexto,
            avisoReforma2026 = "A partir de 01/08/2026 os DFEs devem contemplar campos CBS/IBS (Ato Conjunto RFB/CGIBS nº 1/2025)."
        )
    }

    private fun montarRetencaoPreview(
        tipo: String,
        aliquota: BigDecimal,
        base: BigDecimal,
        codigoReceita: String,
        vencimento: LocalDate,
        observacao: String
    ): RetencaoCalculadaResponse {
        val valor = base.multiply(aliquota).setScale(2, RoundingMode.HALF_UP)
        return RetencaoCalculadaResponse(
            tipo = tipo,
            aliquota = aliquota,
            baseCalculo = base,
            valorRetido = valor,
            codigoReceita = codigoReceita,
            dataVencimento = vencimento.toString(),
            observacao = observacao
        )
    }

    private fun proximoDiaUtil(data: LocalDate): LocalDate {
        var d = data
        while (d.dayOfWeek.value >= 6) d = d.plusDays(1)
        return d
    }

    // ============================================================
    // FECHAMENTO FINANCEIRO MENSAL
    // ============================================================

    @Transactional
    fun fecharMesFinanceiro(inicio: LocalDate, fim: LocalDate, tenantId: UUID, fechadoPor: String? = null): FinanceiroPeriodoFechamento {
        if (fim.isBefore(inicio)) {
            throw FinanceiroBusinessException("Data fim deve ser posterior à data início")
        }

        val existente = fechamentoRepository.findByTenantIdAndDataInicioAndDataFim(tenantId, inicio, fim)
        if (existente?.status == "FECHADO") {
            throw FinanceiroBusinessException("Período $inicio a $fim já está fechado")
        }

        val transacoes = transacaoFinanceiraRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
        val totalEntradas = transacoes.filter { it.tipo == "ENTRADA" }.sumOf { it.valor }
        val totalSaidas = transacoes.filter { it.tipo == "SAIDA" }.sumOf { it.valor }

        val nfsEmitidas = notaFiscalRepository.findByTenantIdAndDataEmissaoBetween(tenantId, inicio, fim)
        val totalFaturado = nfsEmitidas.sumOf { it.valorLiquido }

        val apsPagos = contaAPagarRepository.findByTenantIdAndStatus(tenantId, "PAGO")
            .filter { ap -> ap.dataPagamento?.let { d -> d in inicio..fim } == true }
        val totalPagamentos = apsPagos.sumOf { it.valorPago ?: BigDecimal.ZERO }

        val arsRecebidos = contaAReceberRepository.findByTenantId(tenantId)
            .filter { ar ->
                ar.status == "PAGO" && ar.dataRecebimento?.let { d -> d in inicio..fim } == true
            }
        val totalRecebido = arsRecebidos.sumOf { it.valorRecebido ?: BigDecimal.ZERO }

        val saldoFinalCaixa = contaBancariaRepository.findByTenantIdAndAtivaTrue(tenantId)
            .sumOf { it.saldoAtual }

        val fechamento = existente ?: FinanceiroPeriodoFechamento(
            tenantId = tenantId,
            dataInicio = inicio,
            dataFim = fim
        )

        fechamento.status = "FECHADO"
        fechamento.saldoCaixaInicial = saldoFinalCaixa.subtract(totalEntradas).add(totalSaidas)
        fechamento.saldoCaixaFinal = saldoFinalCaixa
        fechamento.totalRecebimentos = totalRecebido
        fechamento.totalPagamentos = totalPagamentos
        fechamento.totalRetencoes = totalFaturado.subtract(totalRecebido)
        fechamento.observacoes = "Fechamento com ${nfsEmitidas.size} NFS-e e ${apsPagos.size} pagamentos. Período travado — movimentações bloqueadas."
        fechamento.fechadoPor = fechadoPor
        fechamento.dataFechamento = java.time.OffsetDateTime.now()

        val saved = fechamentoRepository.save(fechamento)
        auditService?.registrar(tenantId, "FECHAMENTO_FINANCEIRO", saved.id, "FECHAR", fechadoPor, "Período $inicio a $fim")
        return saved
    }

    @Transactional
    fun reabrirMesFinanceiro(inicio: LocalDate, fim: LocalDate, tenantId: UUID, usuario: String? = null): FinanceiroPeriodoFechamento {
        val fechamento = fechamentoRepository.findByTenantIdAndDataInicioAndDataFim(tenantId, inicio, fim)
            ?: throw FinanceiroBusinessException("Fechamento não encontrado para o período")

        if (fechamento.status != "FECHADO") {
            throw FinanceiroBusinessException("Período não está fechado")
        }

        fechamento.status = "REABERTO"
        fechamento.observacoes = (fechamento.observacoes ?: "") + " | Reaberto em ${LocalDate.now()} por ${usuario ?: "sistema"}"
        val saved = fechamentoRepository.save(fechamento)
        auditService?.registrar(tenantId, "FECHAMENTO_FINANCEIRO", saved.id, "REABRIR", usuario, "Período $inicio a $fim")
        return saved
    }

    fun listarFechamentosFinanceiros(tenantId: UUID): List<FinanceiroPeriodoFechamento> =
        fechamentoRepository.findByTenantIdOrderByDataInicioDesc(tenantId)

    private fun validarPeriodoAberto(tenantId: UUID, data: LocalDate) {
        val fechados = fechamentoRepository.findByTenantIdAndStatus(tenantId, "FECHADO")
        val bloqueado = fechados.any { data in it.dataInicio..it.dataFim }
        if (bloqueado) {
            throw FinanceiroBusinessException(
                "Período financeiro fechado para a data $data. Reabra o mês em Fechamento Financeiro antes de movimentar."
            )
        }
    }

    private fun resolveCnpjPrestador(tenantId: UUID): String =
        tenantFiscalProfileService?.resolveCnpjPrestador(tenantId)
            ?: tenantFiscalProfileRepository?.findById(tenantId)?.map { it.cnpjPrestador?.filter { c -> c.isDigit() }?.takeIf { d -> d.length == 14 } ?: "00000000000000" }?.orElse("00000000000000")
            ?: "00000000000000"

    // ============================================================
    // HELPERS INTERNOS (serão preenchidos conforme as fases)
    // ============================================================

    private fun atualizarSaldoBancario(contaBancariaId: UUID, valor: BigDecimal, tipo: String) {
        contaBancariaRepository.findById(contaBancariaId).ifPresent { conta ->
            conta.saldoAtual = when (tipo.uppercase()) {
                "ENTRADA" -> conta.saldoAtual.add(valor)
                "SAIDA" -> conta.saldoAtual.subtract(valor)
                else -> conta.saldoAtual
            }
            contaBancariaRepository.save(conta)
        }
    }

    fun emitirCobranca(tenantId: UUID, contaAReceberId: UUID, tipo: String): Cobranca {
        val ar = contaAReceberRepository.findById(contaAReceberId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Conta a receber não encontrada") }
        validarPeriodoAberto(tenantId, ar.vencimento)
        return cobrancaService?.emitirCobranca(tenantId, contaAReceberId, tipo)
            ?: throw FinanceiroBusinessException("Serviço de cobrança indisponível")
    }

    @Transactional
    fun provisionarGlosaFinanceira(measurementId: UUID, valorGlosa: BigDecimal, tenantId: UUID): ContaAReceber {
        val ars = contaAReceberRepository.findByTenantIdAndMeasurementId(tenantId, measurementId)
        val ar = ars.firstOrNull() ?: throw FinanceiroBusinessException("Conta a receber não encontrada para medição $measurementId")
        ar.glosaProvisao = valorGlosa
        ar.valorLiquido = ar.valorBruto.subtract(valorGlosa).max(BigDecimal.ZERO)
        val saved = contaAReceberRepository.save(ar)
        auditService?.registrar(tenantId, "CONTA_A_RECEBER", saved.id, "PROVISAO_GLOSA", "sistema", "Glosa R$ $valorGlosa")
        return saved
    }

    fun gerarDreContratoFinanceiro(contratoId: UUID, inicio: LocalDate, fim: LocalDate, tenantId: UUID): Map<String, Any> {
        val nfs = notaFiscalRepository.findByTenantIdAndContratoId(tenantId, contratoId)
            .filter { it.dataEmissao in inicio..fim && it.status != "CANCELADA" }
        val receitaBruta = nfs.sumOf { it.valorServicos }
        val retencoes = nfs.flatMap { nf ->
            nf.id?.let { retencaoRepository.findByTenantIdAndNotaFiscalId(tenantId, it) } ?: emptyList()
        }.sumOf { it.valorRetido }
        val custos = contaAPagarRepository.findByTenantIdAndContratoId(tenantId, contratoId)
            .filter { it.vencimento in inicio..fim && it.status != "CANCELADO" }
            .sumOf { it.valor }
        val glosas = contaAReceberRepository.findByTenantIdAndContratoId(tenantId, contratoId)
            .sumOf { it.glosaProvisao ?: BigDecimal.ZERO }
        val receitaLiquida = receitaBruta.subtract(retencoes)
        val margem = receitaLiquida.subtract(custos).subtract(glosas)
        return mapOf(
            "contratoId" to contratoId,
            "periodo" to mapOf("inicio" to inicio.toString(), "fim" to fim.toString()),
            "receitaBruta" to receitaBruta,
            "retencoes" to retencoes,
            "receitaLiquida" to receitaLiquida,
            "custosDiretos" to custos,
            "glosaProvisao" to glosas,
            "margemOperacional" to margem,
            "nfsEmitidas" to nfs.size
        )
    }

    fun listarAuditoriaFinanceira(tenantId: UUID, limit: Int = 100): List<FinancialAuditLog> =
        auditService?.listar(tenantId, limit) ?: emptyList()

    /**
     * Importa extrato a partir de arquivo real (OFX ou CSV).
     * Método de alto nível para uso via Controller.
     */
    @Transactional
    fun importarExtratoDeArquivo(
        contaBancariaId: UUID,
        file: org.springframework.web.multipart.MultipartFile,
        tenantId: UUID
    ): Int {
        val parser = bankStatementParser ?: throw FinanceiroBusinessException("Parser de extrato não disponível")

        val itens = parser.parse(file, contaBancariaId, tenantId)
        if (itens.isEmpty()) {
            throw FinanceiroBusinessException("Nenhum item válido encontrado no arquivo de extrato")
        }

        return importarExtrato(contaBancariaId, itens, tenantId)
    }

    // ============================================================
    // CONTAS A RECEBER - LISTAGEM RICA (Prioridade Imediata)
    // Substitui o endpoint vazio /receber com dados reais + aging + resumos
    // ============================================================

    fun listarContasAReceberRich(
        tenantId: UUID,
        contratoId: UUID? = null,
        status: String? = null,
        vencimentoDe: LocalDate? = null,
        vencimentoAte: LocalDate? = null
    ): ContasAReceberResumoResponse {
        val contas = contaAReceberRepository.findRich(
            tenantId = tenantId,
            contratoId = contratoId,
            status = status,
            vencimentoDe = vencimentoDe,
            vencimentoAte = vencimentoAte
        )

        val hoje = LocalDate.now()

        val responses = contas.map { ar ->
            // Recalcula dias de atraso em tempo real
            val dias = if (ar.vencimento.isBefore(hoje)) hoje.toEpochDay() - ar.vencimento.toEpochDay() else 0L

            val bucket = when {
                dias <= 30 -> "0-30"
                dias <= 60 -> "31-60"
                dias <= 90 -> "61-90"
                else -> "90+"
            }

            val saldoAberto = ar.valorLiquido.subtract(ar.valorRecebido ?: BigDecimal.ZERO)

            ContaAReceberResponse(
                id = ar.id,
                contratoId = ar.contratoId,
                measurementId = ar.measurementId,
                notaFiscalId = ar.notaFiscalId,
                valorBruto = ar.valorBruto,
                valorLiquido = ar.valorLiquido,
                vencimento = ar.vencimento,
                status = ar.status,
                diasAtraso = dias.toInt(),
                jurosMulta = ar.jurosMulta,
                dataRecebimento = ar.dataRecebimento,
                valorRecebido = ar.valorRecebido,
                agingBucket = bucket,
                saldoAberto = saldoAberto.coerceAtLeast(BigDecimal.ZERO)
            )
        }

        // KPIs executivos
        val valorTotalAberto = responses
            .filter { it.status in listOf("ABERTO", "PARCIAL", "FATURADO") }
            .sumOf { it.saldoAberto }

        val valorTotalVencido = responses
            .filter { it.diasAtraso > 0 }
            .sumOf { it.saldoAberto }

        val porStatus = responses.groupingBy { it.status }.eachCount()
        val porAging = responses.groupBy { it.agingBucket }
            .mapValues { (_, list) -> list.sumOf { it.saldoAberto }.setScale(2, RoundingMode.HALF_UP) }

        return ContasAReceberResumoResponse(
            total = responses.size,
            valorTotalAberto = valorTotalAberto.setScale(2, RoundingMode.HALF_UP),
            valorTotalVencido = valorTotalVencido.setScale(2, RoundingMode.HALF_UP),
            porStatus = porStatus,
            porAging = porAging,
            contas = responses.sortedBy { it.vencimento }
        )
    }

    fun listarContasAPagarRich(
        tenantId: UUID,
        contratoId: UUID? = null,
        status: String? = null,
        origem: String? = null,
        vencimentoDe: LocalDate? = null,
        vencimentoAte: LocalDate? = null
    ): ContasAPagarResumoResponse {
        val contas = contaAPagarRepository.findRich(
            tenantId = tenantId,
            contratoId = contratoId,
            status = status,
            origem = origem,
            vencimentoDe = vencimentoDe,
            vencimentoAte = vencimentoAte
        )

        val hoje = LocalDate.now()
        val responses = contas.map { ap ->
            val dias = if (ap.vencimento.isBefore(hoje)) hoje.toEpochDay() - ap.vencimento.toEpochDay() else 0L
            val bucket = when {
                dias <= 30 -> "0-30"
                dias <= 60 -> "31-60"
                dias <= 90 -> "61-90"
                else -> "90+"
            }
            val saldo = when (ap.status) {
                "PAGO" -> BigDecimal.ZERO
                else -> ap.valor.subtract(ap.valorPago ?: BigDecimal.ZERO).coerceAtLeast(BigDecimal.ZERO)
            }
            ContaAPagarResponse(
                id = ap.id,
                origem = ap.origem,
                origemId = ap.origemId,
                contratoId = ap.contratoId,
                valor = ap.valor,
                vencimento = ap.vencimento,
                status = ap.status,
                dataPagamento = ap.dataPagamento,
                valorPago = ap.valorPago,
                formaPagamento = ap.formaPagamento,
                observacoes = ap.observacoes,
                diasAtraso = dias.toInt(),
                agingBucket = bucket,
                saldoAberto = saldo
            )
        }

        val valorTotalAberto = responses
            .filter { it.status in listOf("ABERTO", "APROVADO") }
            .sumOf { it.saldoAberto }

        val valorTotalVencido = responses
            .filter { it.diasAtraso > 0 && it.status != "PAGO" }
            .sumOf { it.saldoAberto }

        return ContasAPagarResumoResponse(
            total = responses.size,
            valorTotalAberto = valorTotalAberto.setScale(2, RoundingMode.HALF_UP),
            valorTotalVencido = valorTotalVencido.setScale(2, RoundingMode.HALF_UP),
            porStatus = responses.groupingBy { it.status }.eachCount(),
            porOrigem = responses.groupingBy { it.origem }.eachCount(),
            porAging = responses.groupBy { it.agingBucket }
                .mapValues { (_, list) -> list.sumOf { it.saldoAberto }.setScale(2, RoundingMode.HALF_UP) },
            contas = responses.sortedBy { it.vencimento }
        )
    }
}