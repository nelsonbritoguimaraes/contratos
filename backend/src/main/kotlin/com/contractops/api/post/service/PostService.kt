package com.contractops.api.post.service

import com.contractops.api.post.domain.ServicePost
import com.contractops.api.post.repository.ServicePostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class PostService(
    private val postRepository: ServicePostRepository
) {

    fun findByContract(contractId: UUID): List<ServicePost> =
        postRepository.findByContractId(contractId)

    @Transactional
    fun createPost(
        tenantId: UUID,
        contractId: UUID,
        nome: String,
        codigo: String?,
        funcao: String?,
        escala: String?,
        cbo: String? = null,
        jornadaHoras: Int? = null,
        valorMensal: BigDecimal?,
        valorDiario: BigDecimal? = null,
        titularNome: String? = null,
        status: String = "ATIVO"
    ): ServicePost {

        val post = ServicePost(
            tenantId = tenantId,
            contractId = contractId,
            codigo = codigo,
            nome = nome,
            funcao = funcao,
            escala = escala,
            cbo = cbo,
            jornadaHoras = jornadaHoras,
            valorMensal = valorMensal,
            valorDiario = valorDiario,
            titularNome = titularNome,
            status = status
        )

        return postRepository.save(post)
    }

    fun findById(id: UUID): ServicePost? =
        postRepository.findById(id).orElse(null)

    @Transactional
    fun updatePost(
        id: UUID,
        tenantId: UUID,
        nome: String? = null,
        codigo: String? = null,
        funcao: String? = null,
        escala: String? = null,
        cbo: String? = null,
        jornadaHoras: Int? = null,
        valorMensal: BigDecimal? = null,
        valorDiario: BigDecimal? = null,
        titularNome: String? = null,
        status: String? = null
    ): ServicePost? {

        val existing = postRepository.findById(id).orElse(null) ?: return null

        if (existing.tenantId != tenantId) {
            throw IllegalArgumentException("Posto não pertence ao tenant")
        }

        nome?.let { existing.nome = it }
        codigo?.let { existing.codigo = it }
        funcao?.let { existing.funcao = it }
        escala?.let { existing.escala = it }
        cbo?.let { existing.cbo = it }
        jornadaHoras?.let { existing.jornadaHoras = it }
        valorMensal?.let { existing.valorMensal = it }
        valorDiario?.let { existing.valorDiario = it }
        titularNome?.let { existing.titularNome = it }
        status?.let { existing.status = it }

        return postRepository.save(existing)
    }
}
