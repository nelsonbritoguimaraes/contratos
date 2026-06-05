package com.contractops.api.email.service

import com.contractops.api.email.domain.EmailAttachment
import com.contractops.api.email.domain.EmailMessage
import com.contractops.api.email.repository.EmailAttachmentRepository
import com.contractops.api.email.repository.EmailMessageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Starter de Integração de E-mail (Fase 2).
 * Classificação simples de e-mails de órgãos, glosas, etc.
 */
@Service
class EmailIntegrationService(
    private val repository: EmailMessageRepository,
    private val attachmentRepository: EmailAttachmentRepository,
    private val imapConnector: ImapEmailConnector
) {

    fun receiveEmail(from: String, subject: String, body: String, tenantId: UUID): EmailMessage {
        val classification = classify(subject, body)

        val email = EmailMessage(
            tenantId = tenantId,
            fromAddress = from,
            subject = subject,
            body = body,
            receivedAt = LocalDateTime.now(),
            classification = classification,
            status = "CLASSIFICADO"
        )
        return repository.save(email)
    }

    @Transactional
    fun syncFromImap(tenantId: UUID): List<EmailMessage> {
        val incoming = imapConnector.fetchUnread(tenantId)
        return incoming.map { msg ->
            val saved = repository.save(
                EmailMessage(
                    tenantId = tenantId,
                    fromAddress = msg.fromAddress,
                    subject = msg.subject,
                    body = msg.body,
                    receivedAt = LocalDateTime.now(),
                    classification = classify(msg.subject, msg.body),
                    status = "RECEBIDO"
                )
            )
            msg.attachmentNames.forEach { name ->
                attachmentRepository.save(
                    EmailAttachment(
                        tenantId = tenantId,
                        emailId = saved.id!!,
                        fileName = name
                    )
                )
            }
            imapConnector.markProcessed(msg.externalId)
            saved
        }
    }

    fun findAttachments(emailId: UUID, tenantId: UUID): List<EmailAttachment> =
        attachmentRepository.findByTenantIdAndEmailId(tenantId, emailId)

    private fun classify(subject: String, body: String): String {
        val text = (subject + " " + body).lowercase()

        return when {
            text.contains("glosa") || text.contains("desconto") -> "GLOSA"
            text.contains("notificação") || text.contains("fiscal") || text.contains("órgão") -> "NOTIFICACAO_ORGAO"
            text.contains("medição") || text.contains("faturamento") -> "MEDICAO"
            text.contains("contrato") || text.contains("aditivo") -> "CONTRATO"
            else -> "OUTRO"
        }
    }

    fun findByClassification(classification: String, tenantId: UUID) =
        repository.findByTenantIdAndClassification(tenantId, classification)
}
