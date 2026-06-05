package com.contractops.api.financeiro.service

import com.contractops.api.email.domain.EmailMessage
import com.contractops.api.email.repository.EmailMessageRepository
import com.contractops.api.financeiro.config.NfsWorkflowProperties
import com.contractops.api.financeiro.domain.NotaFiscalServico
import com.contractops.api.financeiro.repository.NotaFiscalServicoRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

enum class NfsEmailTemplate { EMISSAO, COBRANCA, CANCELAMENTO }

@Service
class NfsOrgaoEmailService(
    private val mailSender: ObjectProvider<JavaMailSender>,
    private val emailRepository: EmailMessageRepository,
    private val notaFiscalRepository: NotaFiscalServicoRepository,
    private val danfseService: NfsDanfseService,
    private val workflowProperties: NfsWorkflowProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun enviarDanfseAoOrgao(
        nfId: UUID,
        tenantId: UUID,
        destinatario: String? = null,
        template: NfsEmailTemplate = NfsEmailTemplate.EMISSAO
    ): EmailMessage {
        val nf = notaFiscalRepository.findById(nfId).orElseThrow { IllegalArgumentException("NFS-e não encontrada") }
        require(nf.tenantId == tenantId) { "NFS-e de outro tenant" }

        val html = nf.danfseHtml ?: danfseService.gerarDanfseHtml(nf).also {
            nf.danfseHtml = it
            notaFiscalRepository.save(nf)
        }

        val to = destinatario ?: nf.tomadorEmail ?: workflowProperties.orgaoEmailDefault
        val content = renderTemplate(template, nf)
        val subject = content.first
        val body = content.second

        if (mailSender.getIfAvailable() != null) {
            try {
                val sender = mailSender.getObject()
                val message = sender.createMimeMessage()
                MimeMessageHelper(message, true, "UTF-8").apply {
                    setTo(to)
                    setSubject(subject)
                    setText(body, true)
                    addAttachment("DANFSE-${nf.numero}.html", ByteArrayResource(html.toByteArray(Charsets.UTF_8)), "text/html")
                }
                sender.send(message)
                log.info("DANFSE NFS-e {} enviado para {} ({})", nf.numero, to, template)
            } catch (ex: Exception) {
                log.warn("Falha SMTP; registrando e-mail localmente: {}", ex.message)
            }
        }

        val email = EmailMessage(
            tenantId = tenantId,
            fromAddress = "noreply@contractops.local",
            subject = subject,
            body = "$body\n\n[DANFSE HTML anexo — ${html.length} bytes]",
            classification = "NFSE_ORGAO_${template.name}"
        )
        val saved = emailRepository.save(email)
        nf.emailOrgaoEnviadoEm = OffsetDateTime.now()
        nf.tomadorEmail = to
        notaFiscalRepository.save(nf)
        return saved
    }

    private fun renderTemplate(template: NfsEmailTemplate, nf: NotaFiscalServico): Pair<String, String> {
        val dt = nf.dataEmissao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        return when (template) {
            NfsEmailTemplate.EMISSAO -> "NFS-e nº ${nf.numero} — Prestação de serviços" to """
                <p>Prezado(a) responsável,</p>
                <p>Informamos a emissão da <strong>NFS-e nº ${nf.numero}</strong>, série ${nf.serie}, em $dt.</p>
                <ul>
                  <li>Tomador: ${nf.tomadorRazaoSocial ?: nf.tomadorCnpj}</li>
                  <li>Valor dos serviços: R$ ${nf.valorServicos}</li>
                  <li>Valor líquido: R$ ${nf.valorLiquido}</li>
                </ul>
                <p>Segue em anexo o DANFSE para conferência e processamento.</p>
                <p>Atenciosamente,<br/>ContractOps — Faturamento</p>
            """.trimIndent()

            NfsEmailTemplate.COBRANCA -> "Cobrança — NFS-e nº ${nf.numero}" to """
                <p>Prezado(a),</p>
                <p>Encaminhamos cobrança referente à NFS-e <strong>${nf.numero}</strong> ($dt).</p>
                <p>Valor líquido: <strong>R$ ${nf.valorLiquido}</strong>.</p>
                <p>DANFSE em anexo. Solicitamos confirmação do recebimento.</p>
            """.trimIndent()

            NfsEmailTemplate.CANCELAMENTO -> "Cancelamento — NFS-e nº ${nf.numero}" to """
                <p>Prezado(a),</p>
                <p>Comunicamos o <strong>cancelamento</strong> da NFS-e nº ${nf.numero} emitida em $dt.</p>
                <p>Favor desconsiderar documentos anteriores.</p>
            """.trimIndent()
        }
    }
}
