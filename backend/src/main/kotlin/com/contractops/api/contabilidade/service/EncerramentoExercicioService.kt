package com.contractops.api.contabilidade.service

import com.contractops.api.contabilidade.repository.ContaContabilRepository
import com.contractops.api.contabilidade.repository.LancamentoContabilRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class EncerramentoExercicioService(
    private val contabilidadeService: ContabilidadeService,
    private val lancamentoRepository: LancamentoContabilRepository,
    private val contaRepository: ContaContabilRepository
) {
    @Transactional
    fun encerrarExercicio(tenantId: UUID, ano: Int, usuario: String? = null): Map<String, Any> {
        val inicio = LocalDate.of(ano, 1, 1)
        val fim = LocalDate.of(ano, 12, 31)

        val lancamentos = lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)
        val receitas = lancamentos.filter { it.contaCredito.tipo == "RECEITA" }.sumOf { it.valor }
        val despesas = lancamentos.filter { it.contaDebito.tipo == "DESPESA" }.sumOf { it.valor }
        val resultado = receitas.subtract(despesas)

        val lucroPrejuizo = contabilidadeService.findContaPorCodigo("3.9.01", tenantId)
            ?: contabilidadeService.criarConta(
                com.contractops.api.contabilidade.domain.ContaContabil(
                    tenantId = tenantId,
                    codigo = "3.9.01",
                    descricao = "Resultado do Exercício",
                    tipo = "PATRIMONIO_LIQUIDO",
                    natureza = "CREDORA",
                    nivel = 2,
                    aceitaLancamento = true
                )
            )

        val patrimonio = contabilidadeService.findContaPorCodigo("5.1.01", tenantId)
            ?: contabilidadeService.findContaPorCodigo("2.3.01", tenantId)

        val lancamentosEncerramento = mutableListOf<UUID>()

        if (resultado.abs() > BigDecimal.ZERO && patrimonio != null) {
            if (resultado > BigDecimal.ZERO) {
                val receitaContas = contaRepository.findByTenantIdAndAtivaTrue(tenantId)
                    .filter { it.tipo == "RECEITA" }
                receitaContas.forEach { conta ->
                    val saldo = lancamentos.filter { it.contaCredito.id == conta.id }.sumOf { it.valor }
                        .subtract(lancamentos.filter { it.contaDebito.id == conta.id }.sumOf { it.valor })
                    if (saldo > BigDecimal.ZERO) {
                        val l = contabilidadeService.lancar(
                            tenantId = tenantId,
                            data = fim,
                            contaDebitoId = conta.id!!,
                            contaCreditoId = lucroPrejuizo.id!!,
                            valor = saldo,
                            historico = "Encerramento $ano — zeragem receita ${conta.codigo}",
                            origemTipo = "ENCERRAMENTO",
                            origemId = UUID.nameUUIDFromBytes("ENC-$ano-$tenantId".toByteArray())
                        )
                        lancamentosEncerramento.add(l.id!!)
                    }
                }
            }

            val lResultado = contabilidadeService.lancar(
                tenantId = tenantId,
                data = fim,
                contaDebitoId = if (resultado >= BigDecimal.ZERO) lucroPrejuizo.id!! else patrimonio.id!!,
                contaCreditoId = if (resultado >= BigDecimal.ZERO) patrimonio.id!! else lucroPrejuizo.id!!,
                valor = resultado.abs(),
                historico = "Apuração resultado exercício $ano",
                origemTipo = "ENCERRAMENTO",
                origemId = UUID.nameUUIDFromBytes("RES-$ano-$tenantId".toByteArray())
            )
            lancamentosEncerramento.add(lResultado.id!!)
        }

        contabilidadeService.fecharMesContabil(fim.withDayOfMonth(1), fim, tenantId, usuario ?: "encerramento-$ano")

        return mapOf(
            "ano" to ano,
            "receitas" to receitas,
            "despesas" to despesas,
            "resultado" to resultado,
            "lancamentosGerados" to lancamentosEncerramento.size,
            "status" to "ENCERRADO"
        )
    }
}
