package com.contractops.api.email.service

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Component
@Profile("local", "prod")
class StubImapEmailConnector : ImapEmailConnector {
    override fun fetchUnread(tenantId: UUID): List<IncomingEmailMessage> = emptyList()

    override fun markProcessed(externalId: String) {
        // no-op stub
    }
}
