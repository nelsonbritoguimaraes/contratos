package com.contractops.api.financeiro.service

import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.financeiro.domain.ContaAPagar
import com.contractops.api.financeiro.repository.FornecedorRepository
import com.contractops.api.financeiro.repository.LancamentoFinanceiroRepository
import com.contractops.api.rh.repository.PayslipRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Serviço para geração de arquivos CNAB 240 para pagamentos em lote.
 */
@Service
class CnabExportService(
    private val payslipRepository: PayslipRepository? = null,
    private val employeeRepository: EmployeeRepository? = null,
    private val lancamentoRepository: LancamentoFinanceiroRepository? = null,
    private val fornecedorRepository: FornecedorRepository? = null
) {

    private val formatter = DateTimeFormatter.ofPattern("ddMMyyyy")

    /**
     * Gera um arquivo CNAB 240 (layout FEBRABAN) para pagamento de várias Contas a Pagar.
     */
    fun gerarCnab240Pagamentos(
        contas: List<ContaAPagar>,
        contaBancariaAgencia: String,
        contaBancariaConta: String,
        contaBancariaDv: String,
        empresaCnpj: String,
        empresaNome: String,
        dataPagamento: LocalDate
    ): String {
        val sb = StringBuilder()

        // Header de Arquivo (Tipo 0) - CNAB 240 FEBRABAN
        sb.appendLine(gerarHeaderArquivo(empresaCnpj, empresaNome, dataPagamento))

        // Header de Lote (Tipo 1)
        sb.appendLine(gerarHeaderLote(empresaCnpj, empresaNome))

        var sequencial = 1
        var valorTotal = BigDecimal.ZERO

        contas.forEach { conta ->
            sb.appendLine(gerarSegmentoA(conta, sequencial, dataPagamento, contaBancariaAgencia, contaBancariaConta, contaBancariaDv))
            sequencial++
            valorTotal = valorTotal.add(conta.valor)
        }

        sb.appendLine(gerarTraillerLote(sequencial, valorTotal))
        sb.appendLine(gerarTraillerArquivo(sequencial + 2))

        return sb.toString()
    }

    private fun gerarHeaderArquivo(cnpj: String, nome: String, data: LocalDate): String {
        // Layout CNAB 240 FEBRABAN mais próximo do real (Tipo 0 - Header de Arquivo)
        val dataGeracao = LocalDate.now().format(formatter)
        return "0" + "1" + " " + cnpj.padEnd(14) + " " + nome.take(30).padEnd(30) +
                dataGeracao + "000001".padStart(6, '0') + " ".repeat(20) + "REMESSA-PAGAMENTO"
    }

    private fun gerarHeaderLote(cnpj: String, nome: String): String {
        // Tipo 1 - Header de Lote (Pagamentos)
        return "1" + "C" + " " + cnpj.padEnd(14) + " " + nome.take(30).padEnd(30) +
                "000001".padStart(6, '0') + "LOTE PAGAMENTOS - FOLHA / FORNECEDORES"
    }

    private fun gerarSegmentoA(
        conta: ContaAPagar,
        sequencial: Int,
        dataPag: LocalDate,
        agencia: String,
        contaNum: String,
        dv: String
    ): String {
        val valorFormatado = conta.valor.toString().replace(".", "").padStart(15, '0')
        val dataFormatada = dataPag.format(formatter)
        val nossoNumero = (conta.id?.toString()?.replace("-", "") ?: "").padEnd(20).take(20)

        // Segmento A — nosso número (pos 20-40) = ID da AP para matching no retorno
        val segmentoA = "2" + "A" +
                agencia.padEnd(5) +
                contaNum.padEnd(12) + dv +
                nossoNumero +
                valorFormatado +
                dataFormatada +
                sequencial.toString().padStart(6, '0')

        // Segmento B - Dados do Favorecido (melhorado)
        val favorecido = (conta.observacoes ?: "COLABORADOR / FORNECEDOR").take(40).padEnd(40)
        val segmentoB = "2" + "B" +
                resolveFavorecidoDocumento(conta).padEnd(14) +
                favorecido +
                sequencial.toString().padStart(6, '0')

        // Segmento C - Informações complementares
        val historico = "Pagamento ${conta.origem} - ${conta.observacoes ?: ""}".take(40).padEnd(40)
        val segmentoC = "2" + "C" + historico + sequencial.toString().padStart(6, '0')

        return "$segmentoA\n$segmentoB\n$segmentoC"
    }

    private fun gerarTraillerLote(qtdRegistros: Int, valorTotal: BigDecimal): String {
        val valor = valorTotal.toString().replace(".", "").padStart(15, '0')
        return "5" + qtdRegistros.toString().padStart(6, '0') + valor + " ".repeat(20) + "TRAILLER LOTE"
    }

    private fun gerarTraillerArquivo(qtdLotes: Int): String {
        return "9" + qtdLotes.toString().padStart(6, '0') + " ".repeat(30) + "FIM ARQUIVO CNAB240"
    }

    private fun resolveFavorecidoDocumento(conta: ContaAPagar): String {
        return when (conta.origem.uppercase()) {
            "PAYSLIP" -> {
                val payslipId = conta.origemId ?: return "00000000000000"
                val payslip = payslipRepository?.findById(payslipId)?.orElse(null) ?: return "00000000000000"
                val employee = employeeRepository?.findById(payslip.employeeId)?.orElse(null)
                employee?.cpf?.filter { it.isDigit() }?.padStart(11, '0') ?: "00000000000000"
            }
            "FORNECEDOR" -> {
                val lancId = conta.origemId ?: return "00000000000000"
                val lanc = lancamentoRepository?.findById(lancId)?.orElse(null) ?: return "00000000000000"
                val fornId = lanc.fornecedorId ?: return "00000000000000"
                val forn = fornecedorRepository?.findById(fornId)?.orElse(null)
                forn?.cnpj?.filter { it.isDigit() }?.padStart(14, '0') ?: "00000000000000"
            }
            else -> "00000000000000"
        }
    }
}