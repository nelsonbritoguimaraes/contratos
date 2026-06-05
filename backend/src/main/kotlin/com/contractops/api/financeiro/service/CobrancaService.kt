package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.Cobranca
import com.contractops.api.financeiro.exception.FinanceiroBusinessException
import com.contractops.api.financeiro.repository.CobrancaRepository
import com.contractops.api.financeiro.repository.ContaAReceberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class CobrancaService(
    private val cobrancaRepository: CobrancaRepository,
    private val contaAReceberRepository: ContaAReceberRepository,
    private val auditService: FinancialAuditService
) {
    @Transactional
    fun emitirCobranca(
        tenantId: UUID,
        contaAReceberId: UUID,
        tipo: String,
        chavePixRecebedor: String = "00020126580014br.gov.bcb.pix"
    ): Cobranca {
        val ar = contaAReceberRepository.findById(contaAReceberId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { FinanceiroBusinessException("Conta a receber não encontrada") }

        val valor = ar.valorLiquido
        val venc = ar.vencimento

        val cobranca = when (tipo.uppercase()) {
            "PIX" -> {
                val payload = gerarPayloadPixCopiaECola(valor, venc, chavePixRecebedor, ar.id.toString())
                Cobranca(
                    tenantId = tenantId,
                    contaAReceberId = contaAReceberId,
                    tipo = "PIX",
                    codigoPix = payload,
                    qrCodePayload = payload,
                    valor = valor,
                    vencimento = venc,
                    status = "EMITIDA"
                )
            }
            "BOLETO" -> {
                val linha = gerarLinhaDigitavelBoleto(valor, venc, ar.id.toString())
                Cobranca(
                    tenantId = tenantId,
                    contaAReceberId = contaAReceberId,
                    tipo = "BOLETO",
                    linhaDigitavel = linha,
                    nossoNumero = ar.id.toString().replace("-", "").take(20),
                    valor = valor,
                    vencimento = venc,
                    status = "EMITIDA"
                )
            }
            else -> throw FinanceiroBusinessException("Tipo de cobrança inválido: $tipo")
        }

        val saved = cobrancaRepository.save(cobranca)
        auditService.registrar(tenantId, "COBRANCA", saved.id, "EMITIR_${tipo.uppercase()}", "sistema", "AR $contaAReceberId")
        return saved
    }

    private fun gerarPayloadPixCopiaECola(valor: BigDecimal, vencimento: LocalDate, chave: String, txId: String): String {
        val v = valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        return "00020126${chave.length.toString().padStart(2, '0')}$chave" +
            "5204000053039865802BR5925CONTRATOS LTDA6009SAO PAULO" +
            "62070503***6304${crc16("CONTRATOS$v$txId")}"
    }

    private fun gerarLinhaDigitavelBoleto(valor: BigDecimal, vencimento: LocalDate, ref: String): String {
        val cents = valor.multiply(BigDecimal(100)).toLong().toString().padStart(10, '0')
        val fator = vencimento.toEpochDay().toString().takeLast(4).padStart(4, '0')
        return "23793.38128 60000.000003 00000.000${ref.take(3)} 1 $fator$cents"
    }

    private fun crc16(input: String): String {
        var crc = 0xFFFF
        input.forEach { b ->
            crc = crc xor (b.code shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
            }
        }
        return (crc and 0xFFFF).toString(16).uppercase().padStart(4, '0')
    }
}
