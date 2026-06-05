package com.contractops.api.post.api

import java.math.BigDecimal

/**
 * DTOs para Postos de Serviço (ServicePost).
 * Usados pelo ContractController nos endpoints aninhados /contracts/{id}/posts.
 *
 * Futuro: mover endpoints de posto para um PostController dedicado quando o módulo crescer.
 */

/** Request para criação de Posto dentro de um Contrato */
data class CreatePostRequest(
    val nome: String,
    val codigo: String? = null,
    val funcao: String? = null,
    val escala: String? = null,
    val cbo: String? = null,
    val jornadaHoras: Int? = null,
    val valorMensal: BigDecimal? = null,
    val valorDiario: BigDecimal? = null,
    val titularNome: String? = null
)

/** Request para atualização parcial de Posto */
data class UpdatePostRequest(
    val nome: String? = null,
    val codigo: String? = null,
    val funcao: String? = null,
    val escala: String? = null,
    val cbo: String? = null,
    val jornadaHoras: Int? = null,
    val valorMensal: BigDecimal? = null,
    val valorDiario: BigDecimal? = null,
    val status: String? = null,
    val titularNome: String? = null
)
