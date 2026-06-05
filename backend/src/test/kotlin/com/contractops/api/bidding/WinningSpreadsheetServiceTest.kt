package com.contractops.api.bidding

import com.contractops.api.bidding.domain.Bidding
import com.contractops.api.bidding.domain.WinningSpreadsheet
import com.contractops.api.bidding.repository.WinningSpreadsheetRepository
import com.contractops.api.bidding.service.BiddingService
import com.contractops.api.bidding.service.WinningSpreadsheetService
import com.contractops.api.common.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.*

@ExtendWith(MockitoExtension::class)
class WinningSpreadsheetServiceTest {

    @Mock lateinit var winningSpreadsheetRepository: WinningSpreadsheetRepository
    @Mock lateinit var biddingService: BiddingService

    @InjectMocks lateinit var service: WinningSpreadsheetService

    @Test
    fun `setVencedora marca planilha e desmarca anteriores`() {
        val tenantId = UUID.randomUUID()
        val biddingId = UUID.randomUUID()
        val sheetId = UUID.randomUUID()
        val bidding = mock<Bidding> { on { id } doReturn biddingId }
        val ws1 = WinningSpreadsheet(tenantId = tenantId, bidding = bidding, versao = 1, isVencedora = true)
        val ws2 = WinningSpreadsheet(tenantId = tenantId, bidding = bidding, versao = 2, isVencedora = false)

        whenever(winningSpreadsheetRepository.findById(sheetId)).thenReturn(Optional.of(ws2))
        whenever(winningSpreadsheetRepository.findByBiddingId(biddingId)).thenReturn(listOf(ws1, ws2))
        whenever(winningSpreadsheetRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = service.setVencedora(sheetId, tenantId)
        assertTrue(result.isVencedora)
        assertTrue(ws2.isVencedora)
        assertTrue(!ws1.isVencedora)
    }

    @Test
    fun `setVencedora falha se planilha inexistente`() {
        whenever(winningSpreadsheetRepository.findById(any())).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> {
            service.setVencedora(UUID.randomUUID(), UUID.randomUUID())
        }
    }
}
