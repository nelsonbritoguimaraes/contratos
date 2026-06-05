package com.contractops.api.contabilidade

import com.contractops.api.contabilidade.domain.AccountingPeriod
import com.contractops.api.contabilidade.domain.ContaContabil
import com.contractops.api.contabilidade.domain.LancamentoContabil
import com.contractops.api.contabilidade.repository.AccountingPeriodRepository
import com.contractops.api.contabilidade.repository.AccountingRuleRepository
import com.contractops.api.contabilidade.repository.ContaContabilRepository
import com.contractops.api.contabilidade.repository.LancamentoContabilLineRepository
import com.contractops.api.contabilidade.repository.LancamentoContabilRepository
import com.contractops.api.contabilidade.service.ContabilidadeService
import com.contractops.api.rh.repository.PayslipItemRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class ContabilidadeServiceTest {

    @Mock lateinit var contaRepository: ContaContabilRepository
    @Mock lateinit var lancamentoRepository: LancamentoContabilRepository
    @Mock lateinit var lancamentoLineRepository: LancamentoContabilLineRepository
    @Mock lateinit var accountingPeriodRepository: AccountingPeriodRepository
    @Mock lateinit var accountingRuleRepository: AccountingRuleRepository
    @Mock lateinit var payslipItemRepository: PayslipItemRepository

    private lateinit var service: ContabilidadeService
    private val tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @BeforeEach
    fun setup() {
        service = ContabilidadeService(
            contaRepository,
            lancamentoRepository,
            lancamentoLineRepository,
            accountingPeriodRepository,
            accountingRuleRepository,
            payslipItemRepository,
            null,
            null,
            null
        )
    }

    @Test
    fun `lancarMedicaoAprovada usa conta clientes 1_1_02 e receita 4_1_01`() {
        val clientes = conta("1.1.02", "ATIVO", "DEVEDORA")
        val receita = conta("4.1.01", "RECEITA", "CREDORA")
        whenever(contaRepository.findByTenantIdAndCodigo(tenantId, "4.1.01")).thenReturn(receita)
        whenever(contaRepository.findByTenantIdAndCodigo(tenantId, "1.1.02")).thenReturn(clientes)
        whenever(contaRepository.findById(clientes.id!!)).thenReturn(Optional.of(clientes))
        whenever(contaRepository.findById(receita.id!!)).thenReturn(Optional.of(receita))
        whenever(lancamentoRepository.save(any())).thenAnswer { it.arguments[0] as LancamentoContabil }

        val measurementId = UUID.randomUUID()
        val contratoId = UUID.randomUUID()
        service.lancarMedicaoAprovada(measurementId, tenantId, contratoId, BigDecimal("5000.00"))

        val captor = ArgumentCaptor.forClass(LancamentoContabil::class.java)
        verify(lancamentoRepository).save(captor.capture())
        assertEquals(clientes.id, captor.value.contaDebito.id)
        assertEquals(receita.id, captor.value.contaCredito.id)
        assertEquals("MEASUREMENT", captor.value.origemTipo)
    }

    @Test
    fun `lancar bloqueia periodo fechado`() {
        val competencia = YearMonthHelper.firstDay(LocalDate.now())
        whenever(accountingPeriodRepository.findByTenantIdAndCompetencia(tenantId, competencia))
            .thenReturn(AccountingPeriod(tenantId = tenantId, competencia = competencia, status = "FECHADO"))

        assertThrows(IllegalStateException::class.java) {
            service.lancar(
                tenantId, LocalDate.now(),
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, "teste"
            )
        }
    }

    @Test
    fun `fecharMesContabil persiste periodo FECHADO`() {
        val inicio = LocalDate.of(2025, 6, 1)
        val fim = LocalDate.of(2025, 6, 30)
        whenever(accountingPeriodRepository.findByTenantIdAndCompetencia(tenantId, inicio)).thenReturn(null)
        whenever(contaRepository.findByTenantIdAndAtivaTrue(tenantId)).thenReturn(emptyList())
        whenever(lancamentoRepository.findByTenantIdAndDataBetween(tenantId, inicio, fim)).thenReturn(emptyList())
        whenever(accountingPeriodRepository.save(any())).thenAnswer { it.arguments[0] as AccountingPeriod }

        val result = service.fecharMesContabil(inicio, fim, tenantId, "tester")

        assertEquals("FECHADO", result["status"])
        verify(accountingPeriodRepository).save(check { assertEquals("FECHADO", it.status) })
    }

    private fun conta(codigo: String, tipo: String, natureza: String): ContaContabil =
        ContaContabil(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            codigo = codigo,
            descricao = codigo,
            tipo = tipo,
            natureza = natureza,
            aceitaLancamento = true
        )
}

private object YearMonthHelper {
    fun firstDay(date: LocalDate) = date.withDayOfMonth(1)
}
