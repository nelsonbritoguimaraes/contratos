package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.config.OpenFinanceProperties
import com.contractops.api.financeiro.domain.ExtratoBancarioItem
import com.contractops.api.financeiro.domain.OpenFinanceWebhookEvent
import com.contractops.api.financeiro.repository.OpenFinanceWebhookEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class OpenFinanceWebhookService(
    private val eventRepository: OpenFinanceWebhookEventRepository,
    @Lazy private val financeiroService: FinanceiroService,
    private val objectMapper: ObjectMapper,
    private val openFinanceProperties: OpenFinanceProperties
) {

  @Transactional
  fun processWebhook(
      tenantId: UUID,
      contaBancariaId: UUID?,
      payload: Map<String, Any>,
      secret: String? = null
  ): OpenFinanceWebhookEvent {
      if (!openFinanceProperties.enabled) {
          throw IllegalStateException("Open Finance desabilitado")
      }
      val expectedSecret = openFinanceProperties.webhookSecret
      if (!expectedSecret.isNullOrBlank() && secret != expectedSecret) {
          throw IllegalArgumentException("X-OpenFinance-Secret inválido ou ausente")
      }
      val event = OpenFinanceWebhookEvent(
          tenantId = tenantId,
          contaBancariaId = contaBancariaId,
          payloadJson = objectMapper.writeValueAsString(payload)
      )
      val saved = eventRepository.save(event)

      val contaId = contaBancariaId ?: (payload["contaBancariaId"] as? String)?.let { UUID.fromString(it) }
      if (contaId != null) {
          val transacoes = payload["transacoes"] as? List<*> ?: emptyList<Any>()
          val itens = transacoes.mapNotNull { row ->
              val map = row as? Map<*, *> ?: return@mapNotNull null
              ExtratoBancarioItem(
                  tenantId = tenantId,
                  contaBancariaId = contaId,
                  data = LocalDate.parse(map["data"].toString()),
                  documento = map["documento"]?.toString(),
                  historico = map["historico"]?.toString() ?: "Open Finance",
                  valor = BigDecimal(map["valor"].toString()),
                  tipo = map["tipo"]?.toString()?.uppercase() ?: "CREDITO"
              )
          }
          if (itens.isNotEmpty()) {
              val importados = financeiroService.importarExtrato(contaId, itens, tenantId)
              saved.itensImportados = importados
              val inicio = itens.minOf { it.data }
              val fim = itens.maxOf { it.data }
              financeiroService.conciliarAutomatico(contaId, inicio, fim, tenantId)
          }
      }

      saved.processado = true
      return eventRepository.save(saved)
  }
}
