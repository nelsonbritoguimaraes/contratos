package com.contractops.api.glosa.service

import com.contractops.api.glosa.domain.Glosa
import com.contractops.api.glosa.repository.GlosaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class GlosaService(
    private val glosaRepository: GlosaRepository
) {

    fun findById(id: UUID, tenantId: UUID): Glosa? =
        glosaRepository.findById(id).orElse(null)?.takeIf { it.tenantId == tenantId }

    @Transactional
    fun updateStatus(
        id: UUID,
        tenantId: UUID,
        status: String,
        description: String? = null,
        evidenceUrl: String? = null
    ): Glosa {
        val glosa = findById(id, tenantId)
            ?: throw IllegalArgumentException("Glosa não encontrada")

        val allowed = setOf("APURADA", "CONTESTADA", "MANTIDA", "RECUPERADA", "CANCELADA")
        if (status.uppercase() !in allowed) {
            throw IllegalArgumentException("Status inválido: $status")
        }

        glosa.status = status.uppercase()
        description?.let { glosa.description = it }
        evidenceUrl?.let { glosa.evidenceUrl = it }

        return glosaRepository.save(glosa)
    }

    @Transactional
    fun appeal(id: UUID, tenantId: UUID, motivo: String, evidenceUrl: String? = null): Glosa {
        val glosa = findById(id, tenantId)
            ?: throw IllegalArgumentException("Glosa não encontrada")
        glosa.status = "CONTESTADA"
        glosa.description = (glosa.description ?: "") + " | Recurso: $motivo"
        evidenceUrl?.let { glosa.evidenceUrl = it }
        return glosaRepository.save(glosa)
    }
}
