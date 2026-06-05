package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.api.TenantFiscalProfileResponse
import com.contractops.api.financeiro.api.UpdateTenantFiscalProfileRequest
import com.contractops.api.financeiro.domain.TenantFiscalProfile
import com.contractops.api.financeiro.exception.FinanceiroBusinessException
import com.contractops.api.financeiro.repository.TenantFiscalProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Service
class TenantFiscalProfileService(
    private val repository: TenantFiscalProfileRepository
) {
    fun obter(tenantId: UUID): TenantFiscalProfileResponse =
        toResponse(getOrCreate(tenantId))

    @Transactional
    fun atualizar(tenantId: UUID, request: UpdateTenantFiscalProfileRequest): TenantFiscalProfileResponse {
        request.cnpjPrestador?.let { cnpj ->
            val digits = cnpj.filter { it.isDigit() }
            if (digits.isNotEmpty() && digits.length != 14) {
                throw FinanceiroBusinessException("CNPJ do prestador deve ter 14 dígitos")
            }
        }
        if (request.aliquotaInssRetencao != null &&
            (request.aliquotaInssRetencao < BigDecimal.ZERO || request.aliquotaInssRetencao > BigDecimal.ONE)
        ) {
            throw FinanceiroBusinessException("Alíquota INSS retenção deve estar entre 0 e 1")
        }

        val profile = getOrCreate(tenantId)
        request.desoneracaoFolha?.let { profile.desoneracaoFolha = it }
        request.aliquotaInssRetencao?.let { profile.aliquotaInssRetencao = it }
        request.simplesNacional?.let { profile.simplesNacional = it }
        request.municipioIbgePadrao?.let { profile.municipioIbgePadrao = it }
        request.cnpjPrestador?.let { profile.cnpjPrestador = it.filter { c -> c.isDigit() }.ifBlank { null } }
        profile.updatedAt = OffsetDateTime.now()

        return toResponse(repository.save(profile))
    }

    fun getOrCreate(tenantId: UUID): TenantFiscalProfile =
        repository.findById(tenantId).orElseGet {
            repository.save(
                TenantFiscalProfile(
                    tenantId = tenantId,
                    desoneracaoFolha = false,
                    aliquotaInssRetencao = BigDecimal("0.1100"),
                    simplesNacional = false,
                    municipioIbgePadrao = "3550308"
                )
            )
        }

    fun resolveCnpjPrestador(tenantId: UUID): String {
        val cnpj = getOrCreate(tenantId).cnpjPrestador?.filter { it.isDigit() }
        return if (cnpj?.length == 14) cnpj else "00000000000000"
    }

    private fun toResponse(p: TenantFiscalProfile) = TenantFiscalProfileResponse(
        tenantId = p.tenantId,
        desoneracaoFolha = p.desoneracaoFolha,
        aliquotaInssRetencao = p.aliquotaInssRetencao,
        simplesNacional = p.simplesNacional,
        municipioIbgePadrao = p.municipioIbgePadrao,
        cnpjPrestador = p.cnpjPrestador,
        updatedAt = p.updatedAt.toString()
    )
}
