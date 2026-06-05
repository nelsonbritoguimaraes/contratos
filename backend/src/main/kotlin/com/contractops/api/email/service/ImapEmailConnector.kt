package com.contractops.api.email.service

import java.util.*

data class IncomingEmailMessage(
    val externalId: String,
    val fromAddress: String,
    val subject: String,
    val body: String,
    val attachmentNames: List<String> = emptyList()
)

/**
 * Conector IMAP stub — implementação real via JavaMail / Spring Integration (SPEC §15.2).
 */
interface ImapEmailConnector {
    fun fetchUnread(tenantId: UUID): List<IncomingEmailMessage>
    fun markProcessed(externalId: String)
}
