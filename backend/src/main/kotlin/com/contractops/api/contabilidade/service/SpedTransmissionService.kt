package com.contractops.api.contabilidade.service

import com.contractops.api.contabilidade.domain.SpedTransmission
import com.contractops.api.contabilidade.repository.SpedTransmissionRepository
import com.contractops.api.fiscal.crypto.IcpBrasilKeyStoreLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Service
class SpedTransmissionService(
    private val spedService: SpedService,
    private val validatorService: SpedValidatorService,
    private val transmissionRepository: SpedTransmissionRepository,
    private val keyStoreLoader: IcpBrasilKeyStoreLoader
) {

    fun listar(tenantId: UUID): List<SpedTransmission> =
        transmissionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)

    @Transactional
    fun validar(
        tenantId: UUID,
        tipo: String,
        inicio: LocalDate?,
        fim: LocalDate?,
        ano: Int?
    ): Map<String, Any> {
        val conteudo = gerarArquivo(tenantId, tipo, inicio, fim, ano)
        val result = when (tipo.uppercase()) {
            "ECD" -> validatorService.validarECD(conteudo)
            "ECF" -> validatorService.validarECF(conteudo)
            else -> validatorService.validarECD(conteudo)
        }

        val tx = SpedTransmission(
            tenantId = tenantId,
            tipo = tipo.uppercase(),
            competenciaInicio = inicio,
            competenciaFim = fim,
            anoCalendario = ano,
            status = if (result.valido) "VALIDADO" else "ERRO_VALIDACAO",
            arquivoHash = result.arquivoHash,
            totalRegistros = result.totalRegistros,
            errosValidacao = result.erros,
            mensagem = if (result.valido) "Arquivo aprovado na validação PVA (local)" else "Erros encontrados"
        )
        transmissionRepository.save(tx)

        return mapOf<String, Any>(
            "transmissionId" to (tx.id ?: UUID.randomUUID()),
            "valido" to result.valido,
            "erros" to result.erros,
            "avisos" to result.avisos,
            "totalRegistros" to result.totalRegistros,
            "arquivoHash" to result.arquivoHash,
            "status" to tx.status
        )
    }

    @Transactional
    fun transmitir(
        tenantId: UUID,
        transmissionId: UUID,
        aprovadoPor: String?
    ): Map<String, Any> {
        val tx = transmissionRepository.findById(transmissionId)
            .orElseThrow { IllegalArgumentException("Transmissão não encontrada") }
        if (tx.tenantId != tenantId) throw IllegalArgumentException("Transmissão não pertence ao tenant")
        if (tx.status != "VALIDADO") {
            throw IllegalStateException("Somente arquivos VALIDADOS podem ser transmitidos (status atual: ${tx.status})")
        }

        val certConfigured = keyStoreLoader.isEsocialCertificateConfigured()

        tx.aprovadoPor = aprovadoPor ?: "sistema"
        tx.transmitidoEm = OffsetDateTime.now()

        if (certConfigured) {
            tx.status = "TRANSMITIDO"
            tx.protocolo = "SPED-${tx.tipo}-${System.currentTimeMillis()}"
            tx.mensagem = "Transmitido com certificado ICP-Brasil (modo integração — protocolo simulado até API RFB)"
        } else {
            tx.status = "PENDENTE_CERTIFICADO"
            tx.protocolo = null
            tx.mensagem = "Validação OK. Configure contractops.fiscal.esocial.certificate-path para transmissão oficial à RFB."
        }

        transmissionRepository.save(tx)
        return mapOf<String, Any>(
            "transmissionId" to (tx.id ?: transmissionId),
            "status" to tx.status,
            "protocolo" to (tx.protocolo ?: ""),
            "mensagem" to (tx.mensagem ?: ""),
            "certificadoConfigurado" to certConfigured
        )
    }

    private fun gerarArquivo(tenantId: UUID, tipo: String, inicio: LocalDate?, fim: LocalDate?, ano: Int?): String =
        when (tipo.uppercase()) {
            "ECD" -> spedService.gerarSpedContabilECD(tenantId, inicio!!, fim!!)
            "ECF" -> spedService.gerarECF(tenantId, ano ?: inicio!!.year)
            "EFD-REINF" -> spedService.gerarEfdReinf(tenantId, inicio ?: LocalDate.of(ano ?: LocalDate.now().year, 1, 1))
            else -> throw IllegalArgumentException("Tipo SPED não suportado: $tipo")
        }
}
