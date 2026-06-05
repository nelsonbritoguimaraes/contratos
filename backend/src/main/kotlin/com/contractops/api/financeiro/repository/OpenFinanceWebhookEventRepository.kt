package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.OpenFinanceWebhookEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OpenFinanceWebhookEventRepository : JpaRepository<OpenFinanceWebhookEvent, UUID>
