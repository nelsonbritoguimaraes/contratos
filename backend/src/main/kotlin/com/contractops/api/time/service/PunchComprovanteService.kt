package com.contractops.api.time.service

import com.contractops.api.time.domain.PunchComprovante
import com.contractops.api.time.domain.RawPunch
import com.contractops.api.time.repository.PunchComprovanteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

@Service
class PunchComprovanteService(
    private val repository: PunchComprovanteRepository
) {

    @Transactional
    fun emitir(raw: RawPunch, employeeId: UUID): PunchComprovante {
        val conteudo = buildString {
            appendLine("COMPROVANTE DE REGISTRO DE PONTO")
            appendLine("Portaria MTP nº 671/2021 — Art. 79")
            appendLine("Data/Hora: ${raw.punchTimestamp}")
            appendLine("Tipo: ${raw.punchType ?: "MARCAÇÃO"}")
            appendLine("NSR: ${raw.nsr ?: "—"}")
            appendLine("Canal: ${raw.sourceChannel ?: "DEVICE"}")
            if (raw.latitude != null) appendLine("Geo: ${raw.latitude}, ${raw.longitude}")
        }
        val hash = sha256(conteudo)
        return repository.save(
            PunchComprovante(
                tenantId = raw.tenantId,
                rawPunchId = raw.id,
                employeeId = employeeId,
                punchTimestamp = raw.punchTimestamp,
                hashComprovante = hash,
                conteudo = conteudo
            )
        )
    }

    fun listUltimas48h(tenantId: UUID, employeeId: UUID): List<PunchComprovante> {
        val since = LocalDateTime.now().minusHours(48)
        return repository.findByTenantIdAndEmployeeIdAndPunchTimestampAfterOrderByPunchTimestampDesc(
            tenantId, employeeId, since
        )
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
