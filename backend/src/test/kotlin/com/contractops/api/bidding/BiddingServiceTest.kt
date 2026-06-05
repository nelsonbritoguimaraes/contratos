package com.contractops.api.bidding

import com.contractops.api.bidding.api.BiddingRequest
import com.contractops.api.bidding.domain.Bidding
import com.contractops.api.bidding.repository.BiddingRepository
import com.contractops.api.bidding.service.BiddingService
import com.contractops.api.bidding.service.BiddingWorkflowService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
class BiddingServiceTest {

    @Mock lateinit var biddingRepository: BiddingRepository
    @Mock lateinit var workflowService: BiddingWorkflowService

    @InjectMocks lateinit var service: BiddingService

    @Test
    fun `createBidding persiste campos extendidos`() {
        val tenantId = UUID.randomUUID()
        whenever(biddingRepository.save(any())).thenAnswer { inv ->
            inv.getArgument<Bidding>(0).apply { /* id simulado via reflection não necessário */ }
        }

        val result = service.createBidding(
            tenantId,
            BiddingRequest(
                orgao = "Prefeitura",
                editalUrl = "https://pncp.gov.br/edital/1",
                vencedorEmpresa = "ContractOps Ltda"
            )
        )

        assertEquals("Prefeitura", result.orgao)
        assertEquals("https://pncp.gov.br/edital/1", result.editalUrl)
        assertEquals("ContractOps Ltda", result.vencedorEmpresa)
    }
}
