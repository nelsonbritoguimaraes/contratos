package com.contractops.api.cct.service

import com.contractops.api.cct.domain.Cct
import com.contractops.api.cct.repository.CctRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.*

@Service
class CctService(
    private val repository: CctRepository
) {

    fun findAllByTenant(tenantId: UUID): List<Cct> = repository.findByTenantId(tenantId)

    fun findById(id: UUID, tenantId: UUID): Cct? =
        repository.findById(id).filter { it.tenantId == tenantId }.orElse(null)

    @Transactional
    fun uploadCct(
        tenantId: UUID,
        contractId: UUID?,
        file: MultipartFile,
        sindicato: String? = null,
        vigenciaInicio: LocalDate? = null,
        vigenciaFim: LocalDate? = null
    ): Cct {
        val content = file.inputStream.bufferedReader().use { reader ->
            val sb = StringBuilder()
            val buf = CharArray(8192)
            var total = 0
            val maxBytes = 500000
            var n = reader.read(buf)
            while (total < maxBytes && n != -1) {
                val toAppend = kotlin.math.min(n, maxBytes - total)
                sb.append(buf, 0, toAppend)
                total += toAppend
                n = reader.read(buf)
            }
            sb.toString()
        }

        // Extração básica por palavras-chave (stub para futuro agente de IA)
        val extracted = extractBasicClauses(content)

        val cct = Cct(
            tenantId = tenantId,
            contractId = contractId,
            sindicato = sindicato,
            vigenciaInicio = vigenciaInicio,
            vigenciaFim = vigenciaFim,
            arquivoNome = file.originalFilename,
            rawText = content.take(500000), // limite razoável
            extractedData = extracted,
            status = "ATIVO"
        )

        return repository.save(cct)
    }

    private fun extractBasicClauses(text: String): String {
        val keywords = listOf(
            "piso salarial", "salário", "vale refeição", "vale transporte",
            "uniforme", "hora extra", "adicional", "férias", "13º", "rescisão"
        )
        val found = keywords.filter { text.contains(it, ignoreCase = true) }
        return if (found.isNotEmpty()) {
            "Cláusulas identificadas (extração simples): ${found.joinToString(", ")}"
        } else {
            "Nenhuma cláusula chave identificada automaticamente. Envie para extração por IA."
        }
    }
}