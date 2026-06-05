package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.FinancialAuditLog
import com.contractops.api.financeiro.repository.FinancialAuditLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class FinancialAuditService(
    private val repository: FinancialAuditLogRepository
) {
    @Transactional
    fun registrar(tenantId: UUID, entidadeTipo: String, entidadeId: UUID?, acao: String, usuario: String?, detalhe: String?) {
        repository.save(
            FinancialAuditLog(
                tenantId = tenantId,
                entidadeTipo = entidadeTipo,
                entidadeId = entidadeId,
                acao = acao,
                usuario = usuario,
                detalhe = detalhe
            )
        )
    }

    fun listar(tenantId: UUID, limit: Int = 100): List<FinancialAuditLog> =
        repository.findByTenantIdOrderByCreatedAtDesc(tenantId).take(limit)
}
