package com.contractops.api.bidding

import com.contractops.api.bidding.service.BiddingWorkflowService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BiddingWorkflowServiceTest {

    private val service = BiddingWorkflowService()

    @Test
    fun `permite transicao HOMOLOGADA para ADJUDICADA`() {
        assertTrue(service.canTransition("HOMOLOGADA", "ADJUDICADA"))
    }

    @Test
    fun `bloqueia transicao PERDIDA para HOMOLOGADA`() {
        assertFalse(service.canTransition("PERDIDA", "HOMOLOGADA"))
    }

    @Test
    fun `lista statuses nao vazia`() {
        assertTrue(service.allStatuses().contains("PROSPECCAO"))
    }
}
