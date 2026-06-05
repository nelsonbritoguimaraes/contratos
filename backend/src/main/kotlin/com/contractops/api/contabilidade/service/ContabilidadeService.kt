package com.contractops.api.contabilidade.service

import com.contractops.api.contabilidade.domain.AccountingPeriod
import com.contractops.api.contabilidade.domain.ContaContabil
import com.contractops.api.contabilidade.domain.LancamentoContabil
import com.contractops.api.contabilidade.repository.AccountingPeriodRepository
import com.contractops.api.contabilidade.repository.AccountingRuleRepository
import com.contractops.api.contabilidade.repository.ContaContabilRepository
import com.contractops.api.contabilidade.domain.LancamentoContabilLine
import com.contractops.api.contabilidade.repository.LancamentoContabilLineRepository
import com.contractops.api.contabilidade.repository.LancamentoContabilRepository
import com.contractops.api.contract.repository.ContractRepository
import com.contractops.api.common.events.DomainEventPublisher
import com.contractops.api.common.events.journalEntryCreatedEvent
import com.contractops.api.rh.repository.PayslipItemRepository
import com.contractops.api.rh.service.PayslipService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.*

data class LancamentoLineInput(
    val contaId: UUID,
    val natureza: String,
    val valor: BigDecimal,
    val historico: String? = null
)

@Service
class ContabilidadeService(
    private val contaRepository: ContaContabilRepository,
    private val lancamentoRepository: LancamentoContabilRepository,
    private val lancamentoLineRepository: LancamentoContabilLineRepository,
    private val accountingPeriodRepository: AccountingPeriodRepository,
    private val accountingRuleRepository: AccountingRuleRepository,
    private val payslipItemRepository: PayslipItemRepository,
    private val contractRepository: ContractRepository? = null,
    private val payslipService: PayslipService? = null,
    private val domainEventPublisher: DomainEventPublisher? = null
) {

    // ==================== PLANO DE CONTAS ====================

    fun findContasAtivas(tenantId: UUID): List<ContaContabil> =
        contaRepository.findByTenantIdAndAtivaTrue(tenantId)

    fun findContaPorCodigo(codigo: String, tenantId: UUID): ContaContabil? =
        contaRepository.findByTenantIdAndCodigo(tenantId, codigo)

    @Transactional
    fun criarConta(conta: ContaContabil): ContaContabil {
        if (contaRepository.findByTenantIdAndCodigo(conta.tenantId, conta.codigo) != null) {
            throw IllegalArgumentException("Já existe conta com o código ${conta.codigo} neste tenant")
        }
        return contaRepository.save(conta)
    }

    // ==================== PERÍODOS CONTÁBEIS ====================

    fun listarPeriodos(tenantId: UUID): List<AccountingPeriod> =
        accountingPeriodRepository.findByTenantIdOrderByCompetenciaDesc(tenantId)

    private fun isPeriodoFechado(tenantId: UUID, data: LocalDate): Boolean {
        val competencia = YearMonth.from(data).atDay(1)
        val periodo = accountingPeriodRepository.findByTenantIdAndCompetencia(tenantId, competencia)
        return periodo?.status == "FECHADO"
    }

    private fun validarPeriodoAberto(tenantId: UUID, data: LocalDate) {
        if (isPeriodoFechado(tenantId, data)) {
            throw IllegalStateException("Período contábil ${YearMonth.from(data)} está fechado. Reabra ou use outra competência.")
        }
    }

    // ==================== LANÇAMENTOS ====================

    @Transactional
    fun lancar(
        tenantId: UUID,
        data: LocalDate,
        contaDebitoId: UUID,
        contaCreditoId: UUID,
        valor: BigDecimal,
        historico: String?,
        origemTipo: String? = null,
        origemId: UUID? = null,
        contratoId: UUID? = null,
        costCenterId: UUID? = null,
        branchId: UUID? = null,
        lines: List<LancamentoLineInput>? = null
    ): LancamentoContabil {
        validarPeriodoAberto(tenantId, data)

        if (origemTipo != null && origemId != null) {
            val existentes = lancamentoRepository.findByTenantIdAndOrigemTipoAndOrigemId(tenantId, origemTipo, origemId)
            if (existentes.isNotEmpty() && origemTipo !in listOf("PAYSLIP", "PROVISAO_RH", "NFS_RETENCAO")) {
                return existentes.first()
            }
        }

        val ctx = resolverContextoContrato(contratoId, costCenterId, branchId)

        if (!lines.isNullOrEmpty() && lines.size >= 2) {
            return lancarComposto(
                tenantId, data, historico, origemTipo, origemId, contratoId,
                ctx.first, ctx.second, lines
            )
        }

        val contaDebito = contaRepository.findById(contaDebitoId)
            .orElseThrow { IllegalArgumentException("Conta de débito não encontrada") }

        val contaCredito = contaRepository.findById(contaCreditoId)
            .orElseThrow { IllegalArgumentException("Conta de crédito não encontrada") }

        if (valor <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Valor do lançamento deve ser maior que zero")
        }

        val lancamento = LancamentoContabil(
            tenantId = tenantId,
            data = data,
            contaDebito = contaDebito,
            contaCredito = contaCredito,
            valor = valor,
            historico = historico,
            origemTipo = origemTipo,
            origemId = origemId,
            contratoId = contratoId,
            costCenterId = ctx.first,
            branchId = ctx.second,
            composto = false
        )

        return lancamentoRepository.save(lancamento).also { saved ->
            domainEventPublisher?.publish(
                journalEntryCreatedEvent(
                    tenantId = tenantId,
                    lancamentoId = saved.id!!,
                    contratoId = contratoId,
                    origemTipo = origemTipo,
                    valor = valor
                )
            )
        }
    }

    @Transactional
    fun lancarComposto(
        tenantId: UUID,
        data: LocalDate,
        historico: String?,
        origemTipo: String?,
        origemId: UUID?,
        contratoId: UUID?,
        costCenterId: UUID?,
        branchId: UUID?,
        lines: List<LancamentoLineInput>
    ): LancamentoContabil {
        val totalDebito = lines.filter { it.natureza.uppercase() == "D" }.sumOf { it.valor }
        val totalCredito = lines.filter { it.natureza.uppercase() == "C" }.sumOf { it.valor }
        if (totalDebito.compareTo(totalCredito) != 0) {
            throw IllegalArgumentException("Lançamento composto desbalanceado: D=$totalDebito C=$totalCredito")
        }

        val primeiraD = lines.first { it.natureza.uppercase() == "D" }
        val primeiraC = lines.first { it.natureza.uppercase() == "C" }
        val contaDebito = contaRepository.findById(primeiraD.contaId).orElseThrow()
        val contaCredito = contaRepository.findById(primeiraC.contaId).orElseThrow()

        val header = LancamentoContabil(
            tenantId = tenantId,
            data = data,
            contaDebito = contaDebito,
            contaCredito = contaCredito,
            valor = totalDebito,
            historico = historico,
            origemTipo = origemTipo,
            origemId = origemId,
            contratoId = contratoId,
            costCenterId = costCenterId,
            branchId = branchId,
            composto = true
        )
        val saved = lancamentoRepository.save(header)

        lines.sortedBy { it.natureza }.forEachIndexed { idx, line ->
            val conta = contaRepository.findById(line.contaId).orElseThrow()
            lancamentoLineRepository.save(
                LancamentoContabilLine(
                    tenantId = tenantId,
                    lancamentoId = saved.id!!,
                    linhaOrdem = idx + 1,
                    conta = conta,
                    naturezaLinha = line.natureza.uppercase().take(1),
                    valor = line.valor,
                    historicoLinha = line.historico,
                    costCenterId = costCenterId
                )
            )
        }
        return saved
    }

    fun existeLancamentoOrigem(tenantId: UUID, origemTipo: String, origemId: UUID): Boolean =
        lancamentoRepository.findByTenantIdAndOrigemTipoAndOrigemId(tenantId, origemTipo, origemId).isNotEmpty()

    @Transactional
    fun lancarNfsEmitida(
        tenantId: UUID,
        nfsId: UUID,
        measurementId: UUID?,
        contratoId: UUID,
        valorServicos: BigDecimal,
        valorLiquido: BigDecimal,
        totalRetencoes: BigDecimal,
        data: LocalDate,
        numeroNfs: String
    ) {
        val ctx = resolverContextoContrato(contratoId, null, null)
        val medicaoJaLancada = measurementId?.let {
            existeLancamentoOrigem(tenantId, "MEASUREMENT", it)
        } ?: false

        if (!medicaoJaLancada) {
            val clientes = requireConta(tenantId, "1.1.02", "Clientes a Receber")
            val receita = requireConta(tenantId, "4.1.01", "Receita de Serviços")
            lancar(
                tenantId = tenantId,
                data = data,
                contaDebitoId = clientes.id!!,
                contaCreditoId = receita.id!!,
                valor = valorServicos,
                historico = "Emissão NFS-e #$numeroNfs",
                origemTipo = "NFS_E",
                origemId = nfsId,
                contratoId = contratoId,
                costCenterId = ctx.first,
                branchId = ctx.second
            )
        }

        if (totalRetencoes > BigDecimal.ZERO) {
            lancarRetencoesNfs(tenantId, nfsId, contratoId, totalRetencoes, data, numeroNfs, ctx.first, ctx.second)
        }
    }

    @Transactional
    fun lancarRetencoesNfs(
        tenantId: UUID,
        nfsId: UUID,
        contratoId: UUID,
        totalRetencoes: BigDecimal,
        data: LocalDate,
        numeroNfs: String,
        costCenterId: UUID? = null,
        branchId: UUID? = null
    ) {
        if (totalRetencoes <= BigDecimal.ZERO) return
        val clientes = requireConta(tenantId, "1.1.02", "Clientes a Receber")
        val retRecolher = requireConta(tenantId, "2.1.06", "Retenções Tributárias a Recolher")

        lancar(
            tenantId = tenantId,
            data = data,
            contaDebitoId = clientes.id!!,
            contaCreditoId = retRecolher.id!!,
            valor = totalRetencoes,
            historico = "Retenções NFS-e #$numeroNfs",
            origemTipo = "NFS_RETENCAO",
            origemId = nfsId,
            contratoId = contratoId,
            costCenterId = costCenterId,
            branchId = branchId
        )
    }

    private fun resolverContextoContrato(
        contratoId: UUID?,
        costCenterId: UUID?,
        branchId: UUID?
    ): Pair<UUID?, UUID?> {
        if (contratoId == null) return costCenterId to branchId
        val contract = contractRepository?.findById(contratoId)?.orElse(null)
        return (costCenterId ?: null) to (branchId ?: contract?.branchId)
    }

    fun buscarLancamentosPorPeriodo(
        tenantId: UUID,
        inicio: LocalDate,
        fim: LocalDate,
        origemTipo: String? = null,
        origemId: UUID? = null
    ): List<LancamentoContabil> {
        val base = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
        return base.filter { lanc ->
            (origemTipo == null || lanc.origemTipo.equals(origemTipo, ignoreCase = true) ||
                (origemTipo.equals("FOLHA", ignoreCase = true) && lanc.origemTipo.equals("PAYSLIP", ignoreCase = true))) &&
                (origemId == null || lanc.origemId == origemId)
        }
    }

    fun buscarLancamentosPorOrigem(tenantId: UUID, origemTipo: String, origemId: UUID): List<LancamentoContabil> =
        lancamentoRepository.findByTenantIdAndOrigemTipoAndOrigemId(tenantId, origemTipo, origemId)

    // ==================== LANÇAMENTOS AUTOMÁTICOS ====================

    @Transactional
    fun lancarFolhaAprovada(payslipId: UUID, tenantId: UUID, contratoId: UUID, valorTotal: BigDecimal) {
        val data = LocalDate.now()
        validarPeriodoAberto(tenantId, data)

        val items = payslipItemRepository.findByPayslipId(payslipId)
        val rubricRules = accountingRuleRepository.findByTenantIdAndOrigemTipoAndAtivaTrue(tenantId, "PAYSLIP")
            .filter { !it.rubricCode.isNullOrBlank() }
            .associateBy { it.rubricCode!!.uppercase() }

        if (rubricRules.isNotEmpty() && items.isNotEmpty()) {
            lancarFolhaPorRubricas(payslipId, tenantId, contratoId, data, items, rubricRules)
            return
        }

        lancarFolhaAgregada(payslipId, tenantId, contratoId, valorTotal, data, items)
    }

    private fun lancarFolhaPorRubricas(
        payslipId: UUID,
        tenantId: UUID,
        contratoId: UUID,
        data: LocalDate,
        items: List<com.contractops.api.rh.domain.PayslipItem>,
        rubricRules: Map<String, com.contractops.api.contabilidade.domain.AccountingRule>
    ) {
        items.forEach { item ->
            if (item.totalValue <= BigDecimal.ZERO) return@forEach
            val rule = rubricRules[item.rubric.code.uppercase()] ?: return@forEach
            val debito = requireConta(tenantId, rule.contaDebitoCodigo, rule.contaDebitoCodigo)
            val credito = requireConta(tenantId, rule.contaCreditoCodigo, rule.contaCreditoCodigo)
            lancar(
                tenantId = tenantId,
                data = data,
                contaDebitoId = debito.id!!,
                contaCreditoId = credito.id!!,
                valor = item.totalValue,
                historico = rule.historicoPadrao ?: "${item.rubric.code} — Payslip #$payslipId",
                origemTipo = "PAYSLIP",
                origemId = payslipId,
                contratoId = contratoId
            )
        }
    }

    private fun lancarFolhaAgregada(
        payslipId: UUID,
        tenantId: UUID,
        contratoId: UUID,
        valorTotal: BigDecimal,
        data: LocalDate,
        items: List<com.contractops.api.rh.domain.PayslipItem>
    ) {
        val despesaPessoal = requireConta(tenantId, "3.1.01", "Despesa com Pessoal")
        val encargosSociais = requireConta(tenantId, "3.1.02", "Encargos Sociais")
        val inssRecolher = requireConta(tenantId, "2.1.03", "INSS a Recolher")
        val fgtsRecolher = requireConta(tenantId, "2.1.02", "FGTS a Recolher")
        val irrfRecolher = requireConta(tenantId, "2.1.04", "IRRF a Recolher")
        val liquidosPagar = requireConta(tenantId, "2.1.01", "Salários a Pagar")

        val inss = sumRubric(items, "INSS") ?: valorTotal.multiply(BigDecimal("0.11")).setScale(2, RoundingMode.HALF_UP)
        val irrf = sumRubric(items, "IRRF") ?: valorTotal.multiply(BigDecimal("0.04")).setScale(2, RoundingMode.HALF_UP)
        val fgts = sumRubric(items, "FGTS") ?: valorTotal.multiply(BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP)
        val liquido = items.filter { it.type == "PROVENTO" }.sumOf { it.totalValue }
            .subtract(items.filter { it.type == "DESCONTO" }.sumOf { it.totalValue })
            .takeIf { it > BigDecimal.ZERO } ?: valorTotal.subtract(inss).subtract(irrf)

        lancar(
            tenantId = tenantId, data = data,
            contaDebitoId = despesaPessoal.id!!,
            contaCreditoId = liquidosPagar.id!!,
            valor = liquido,
            historico = "Folha de pagamento (Líquido) - Payslip #$payslipId",
            origemTipo = "PAYSLIP", origemId = payslipId, contratoId = contratoId
        )

        if (inss > BigDecimal.ZERO) {
            lancar(
                tenantId = tenantId, data = data,
                contaDebitoId = encargosSociais.id!!,
                contaCreditoId = inssRecolher.id!!,
                valor = inss,
                historico = "Provisão INSS - Payslip #$payslipId",
                origemTipo = "PAYSLIP", origemId = payslipId, contratoId = contratoId
            )
        }

        if (fgts > BigDecimal.ZERO) {
            lancar(
                tenantId = tenantId, data = data,
                contaDebitoId = encargosSociais.id!!,
                contaCreditoId = fgtsRecolher.id!!,
                valor = fgts,
                historico = "Provisão FGTS - Payslip #$payslipId",
                origemTipo = "PAYSLIP", origemId = payslipId, contratoId = contratoId
            )
        }

        if (irrf > BigDecimal.ZERO) {
            lancar(
                tenantId = tenantId, data = data,
                contaDebitoId = despesaPessoal.id!!,
                contaCreditoId = irrfRecolher.id!!,
                valor = irrf,
                historico = "IRRF retido na fonte - Payslip #$payslipId",
                origemTipo = "PAYSLIP", origemId = payslipId, contratoId = contratoId
            )
        }
    }

    @Transactional
    fun lancarMedicaoAprovada(measurementId: UUID, tenantId: UUID, contratoId: UUID, valorLiquido: BigDecimal) {
        val data = LocalDate.now()
        val receitaServicos = requireConta(tenantId, "4.1.01", "Receita de Serviços")
        val clientes = requireConta(tenantId, "1.1.02", "Clientes a Receber")
        val ctx = resolverContextoContrato(contratoId, null, null)

        lancar(
            tenantId = tenantId,
            data = data,
            contaDebitoId = clientes.id!!,
            contaCreditoId = receitaServicos.id!!,
            valor = valorLiquido,
            historico = "Faturamento - Medição #$measurementId",
            origemTipo = "MEASUREMENT",
            origemId = measurementId,
            contratoId = contratoId,
            costCenterId = ctx.first,
            branchId = ctx.second
        )
    }

    @Transactional
    fun lancarGlosaAplicada(glosaId: UUID, tenantId: UUID, contratoId: UUID, valor: BigDecimal, competencia: LocalDate) {
        val glosaDespesa = requireConta(tenantId, "3.2.01", "Glosas Contratuais")
        val clientes = requireConta(tenantId, "1.1.02", "Clientes a Receber")

        lancar(
            tenantId = tenantId,
            data = competencia,
            contaDebitoId = glosaDespesa.id!!,
            contaCreditoId = clientes.id!!,
            valor = valor,
            historico = "Glosa contratual aplicada — #$glosaId",
            origemTipo = "GLOSA",
            origemId = glosaId,
            contratoId = contratoId
        )
    }

    @Transactional
    fun lancarProvisaoRH(
        tenantId: UUID,
        contratoId: UUID,
        tipoProvisao: String,
        valor: BigDecimal,
        competencia: LocalDate,
        costCenterId: UUID? = null,
        branchId: UUID? = null
    ) {
        val despesaProvisao = requireConta(tenantId, "3.1.03", "Provisão de Férias/13º")
        val provisaoPagar = requireConta(tenantId, "2.1.05", "Provisões Trabalhistas a Pagar")
        val ctx = resolverContextoContrato(contratoId, costCenterId, branchId)

        lancar(
            tenantId = tenantId,
            data = competencia,
            contaDebitoId = despesaProvisao.id!!,
            contaCreditoId = provisaoPagar.id!!,
            valor = valor,
            historico = "Provisão $tipoProvisao - RH - $competencia",
            origemTipo = "PROVISAO_RH",
            contratoId = contratoId,
            costCenterId = ctx.first,
            branchId = ctx.second
        )
    }

    // ==================== RELATÓRIOS ====================

    fun gerarDreContrato(contratoId: UUID, inicio: LocalDate, fim: LocalDate, tenantId: UUID): Map<String, Any> {
        val lancamentos = lancamentoRepository.findByTenantIdAndContratoId(tenantId, contratoId)
            .filter { it.data in inicio..fim }

        val receitaServicos = sumCreditoPorCodigo(lancamentos, "4.1.01")
        val glosas = sumDebitoPorCodigo(lancamentos, "3.2.01")
        val despesaPessoal = sumDebitoPorCodigo(lancamentos, "3.1.01")
        val encargos = sumDebitoPorCodigo(lancamentos, "3.1.02")
        val provisoes = sumDebitoPorCodigo(lancamentos, "3.1.03")

        val receitaLiquida = receitaServicos.subtract(glosas)
        val despesas = despesaPessoal.add(encargos).add(provisoes)
        val lucro = receitaLiquida.subtract(despesas)

        return mapOf(
            "contratoId" to contratoId,
            "periodo" to "$inicio a $fim",
            "linhas" to listOf(
                mapOf("conta" to "4.1.01", "descricao" to "Receita de Serviços", "valor" to receitaServicos),
                mapOf("conta" to "3.2.01", "descricao" to "(-) Glosas Contratuais", "valor" to glosas.negate()),
                mapOf("conta" to "", "descricao" to "Receita Líquida", "valor" to receitaLiquida),
                mapOf("conta" to "3.1.01", "descricao" to "Despesa com Pessoal", "valor" to despesaPessoal.negate()),
                mapOf("conta" to "3.1.02", "descricao" to "Encargos Sociais", "valor" to encargos.negate()),
                mapOf("conta" to "3.1.03", "descricao" to "Provisões RH", "valor" to provisoes.negate()),
                mapOf("conta" to "", "descricao" to "Resultado do Período", "valor" to lucro)
            ),
            "receitas" to receitaServicos,
            "receitaLiquida" to receitaLiquida,
            "despesas" to despesas,
            "lucroPrejuizo" to lucro,
            "totalLancamentos" to lancamentos.size
        )
    }

    fun gerarRazao(contaId: UUID, inicio: LocalDate, fim: LocalDate, tenantId: UUID): Map<String, Any> {
        val conta = contaRepository.findById(contaId).orElseThrow { IllegalArgumentException("Conta não encontrada") }

        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
            .filter { it.contaDebito.id == contaId || it.contaCredito.id == contaId }
            .sortedBy { it.data }

        val movimentos = lancamentos.map { lanc ->
            val tipo = if (lanc.contaDebito.id == contaId) "DÉBITO" else "CRÉDITO"
            mapOf(
                "data" to lanc.data,
                "historico" to (lanc.historico ?: "Lançamento #${lanc.id}"),
                "tipo" to tipo,
                "valor" to lanc.valor,
                "contraPartida" to if (tipo == "DÉBITO") lanc.contaCredito.codigo else lanc.contaDebito.codigo,
                "origemTipo" to lanc.origemTipo
            )
        }

        val totalDebito = lancamentos.filter { it.contaDebito.id == contaId }.sumOf { it.valor }
        val totalCredito = lancamentos.filter { it.contaCredito.id == contaId }.sumOf { it.valor }

        return mapOf(
            "conta" to "${conta.codigo} - ${conta.descricao}",
            "contaId" to contaId,
            "periodo" to "$inicio a $fim",
            "movimentos" to movimentos,
            "saldoDevedor" to totalDebito,
            "saldoCredor" to totalCredito,
            "saldoFinal" to if (conta.natureza == "DEVEDORA") totalDebito.subtract(totalCredito) else totalCredito.subtract(totalDebito)
        )
    }

    fun gerarBalancete(inicio: LocalDate, fim: LocalDate, tenantId: UUID): List<Map<String, Any>> {
        val contas = contaRepository.findByTenantIdAndAtivaTrue(tenantId)
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)

        return contas.map { conta ->
            val debitos = lancamentos.filter { it.contaDebito.id == conta.id }.sumOf { it.valor }
            val creditos = lancamentos.filter { it.contaCredito.id == conta.id }.sumOf { it.valor }
            val saldo = if (conta.natureza == "DEVEDORA") debitos.subtract(creditos) else creditos.subtract(debitos)

            mapOf(
                "codigo" to conta.codigo,
                "descricao" to conta.descricao,
                "tipo" to conta.tipo,
                "natureza" to conta.natureza,
                "totalDebito" to debitos,
                "totalCredito" to creditos,
                "saldo" to saldo
            )
        }.filter { (it["totalDebito"] as BigDecimal) > BigDecimal.ZERO || (it["totalCredito"] as BigDecimal) > BigDecimal.ZERO }
    }

    fun gerarBalancoPatrimonial(data: LocalDate, tenantId: UUID): Map<String, Any> {
        val inicio = LocalDate.of(2020, 1, 1)
        val contas = contaRepository.findByTenantIdAndAtivaTrue(tenantId)
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, data)

        fun saldoConta(conta: ContaContabil): BigDecimal {
            val debitos = lancamentos.filter { it.contaDebito.id == conta.id }.sumOf { it.valor }
            val creditos = lancamentos.filter { it.contaCredito.id == conta.id }.sumOf { it.valor }
            return if (conta.natureza == "DEVEDORA") debitos.subtract(creditos) else creditos.subtract(debitos)
        }

        val ativo = contas.filter { it.tipo == "ATIVO" }.map { saldoConta(it) to it }.filter { it.first > BigDecimal.ZERO }
        val passivo = contas.filter { it.tipo == "PASSIVO" }.map { saldoConta(it) to it }.filter { it.first > BigDecimal.ZERO }
        val pl = contas.filter { it.tipo == "PATRIMONIO_LIQUIDO" }.map { saldoConta(it) to it }

        val totalAtivo = ativo.sumOf { it.first }
        val totalPassivo = passivo.sumOf { it.first }
        val totalPl = pl.sumOf { it.first }

        return mapOf(
            "data" to data.toString(),
            "ativo" to totalAtivo,
            "passivo" to totalPassivo,
            "patrimonioLiquido" to totalPl,
            "detalheAtivo" to ativo.map { mapOf("codigo" to it.second.codigo, "descricao" to it.second.descricao, "saldo" to it.first) },
            "detalhePassivo" to passivo.map { mapOf("codigo" to it.second.codigo, "descricao" to it.second.descricao, "saldo" to it.first) },
            "equilibrio" to (totalAtivo.compareTo(totalPassivo.add(totalPl)) == 0)
        )
    }

    fun gerarFluxoDeCaixa(inicio: LocalDate, fim: LocalDate, tenantId: UUID): Map<String, Any> {
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
        val caixaCodigos = setOf("1.1.01")

        val entradas = lancamentos
            .filter { it.contaDebito.codigo in caixaCodigos }
            .sumOf { it.valor }

        val saidas = lancamentos
            .filter { it.contaCredito.codigo in caixaCodigos }
            .sumOf { it.valor }

        return mapOf(
            "periodo" to "$inicio a $fim",
            "atividadesOperacionais" to mapOf("entradas" to entradas, "saidas" to saidas),
            "saldoLiquido" to entradas.subtract(saidas)
        )
    }

    @Transactional
    fun fecharMesContabil(inicio: LocalDate, fim: LocalDate, tenantId: UUID, fechadoPor: String? = null): Map<String, Any> {
        val competencia = YearMonth.from(inicio).atDay(1)
        val existente = accountingPeriodRepository.findByTenantIdAndCompetencia(tenantId, competencia)
        if (existente?.status == "FECHADO") {
            throw IllegalStateException("Competência $competencia já está fechada")
        }

        val balancete = gerarBalancete(inicio, fim, tenantId)
        val dreConsolidada = gerarDreConsolidada(inicio, fim, tenantId)

        val periodo = existente ?: AccountingPeriod(tenantId = tenantId, competencia = competencia)
        periodo.status = "FECHADO"
        periodo.fechadoEm = OffsetDateTime.now()
        periodo.fechadoPor = fechadoPor ?: "sistema"
        periodo.observacao = "Fechamento mensal — ${balancete.size} contas com movimento"
        accountingPeriodRepository.save(periodo)

        return mapOf(
            "periodo" to "$inicio a $fim",
            "competencia" to competencia.toString(),
            "status" to "FECHADO",
            "balancete" to balancete,
            "dreConsolidada" to dreConsolidada,
            "contasComMovimento" to balancete.size
        )
    }

    @Transactional
    fun reabrirPeriodo(tenantId: UUID, competencia: LocalDate, motivo: String, usuario: String): Map<String, Any> {
        val periodo = accountingPeriodRepository.findByTenantIdAndCompetencia(tenantId, competencia)
            ?: throw IllegalArgumentException("Período $competencia não encontrado")
        if (periodo.status != "FECHADO") {
            throw IllegalStateException("Período não está fechado")
        }
        periodo.status = "REABERTO"
        periodo.observacao = "Reaberto por $usuario: $motivo"
        periodo.fechadoEm = null
        periodo.fechadoPor = null
        accountingPeriodRepository.save(periodo)
        return mapOf(
            "competencia" to competencia.toString(),
            "status" to "REABERTO",
            "motivo" to motivo,
            "usuario" to usuario
        )
    }

    private fun gerarDreConsolidada(inicio: LocalDate, fim: LocalDate, tenantId: UUID): Map<String, Any> {
        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
        val receitas = lancamentos.filter { it.contaCredito.tipo == "RECEITA" }.sumOf { it.valor }
        val despesas = lancamentos.filter { it.contaDebito.tipo == "DESPESA" }.sumOf { it.valor }
        return mapOf("receitas" to receitas, "despesas" to despesas, "resultado" to receitas.subtract(despesas))
    }

    private fun requireConta(tenantId: UUID, codigo: String, nome: String): ContaContabil =
        contaRepository.findByTenantIdAndCodigo(tenantId, codigo)
            ?: throw IllegalStateException("Conta $nome ($codigo) não encontrada")

    private fun sumRubric(items: List<com.contractops.api.rh.domain.PayslipItem>, code: String): BigDecimal? {
        val filtered = items.filter { it.rubric.code.equals(code, ignoreCase = true) }
        return filtered.takeIf { it.isNotEmpty() }?.sumOf { it.totalValue }
    }

    private fun sumCreditoPorCodigo(lancamentos: List<LancamentoContabil>, codigo: String): BigDecimal =
        lancamentos.filter { it.contaCredito.codigo == codigo }.sumOf { it.valor }

    private fun sumDebitoPorCodigo(lancamentos: List<LancamentoContabil>, codigo: String): BigDecimal =
        lancamentos.filter { it.contaDebito.codigo == codigo }.sumOf { it.valor }
}
