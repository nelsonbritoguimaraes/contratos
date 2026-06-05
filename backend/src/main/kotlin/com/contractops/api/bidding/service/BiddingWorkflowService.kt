package com.contractops.api.bidding.service

import org.springframework.stereotype.Component

@Component
class BiddingWorkflowService {

    private val transitions = mapOf(
        "PROSPECCAO" to setOf("EM_ANALISE", "DESISTENCIA"),
        "EM_ANALISE" to setOf("PROPOSTA_EM_ELABORACAO", "DESISTENCIA"),
        "PROPOSTA_EM_ELABORACAO" to setOf("PARTICIPANDO", "DESISTENCIA"),
        "PARTICIPANDO" to setOf("SESSAO_PUBLICA", "PERDIDA", "DESISTENCIA"),
        "SESSAO_PUBLICA" to setOf("RECURSO", "HOMOLOGADA", "PERDIDA"),
        "RECURSO" to setOf("HOMOLOGADA", "PERDIDA"),
        "HOMOLOGADA" to setOf("ADJUDICADA", "CONTRATO_ASSINADO"),
        "ADJUDICADA" to setOf("CONTRATO_ASSINADO"),
        "CONTRATO_ASSINADO" to emptySet(),
        "PERDIDA" to emptySet(),
        "DESISTENCIA" to emptySet()
    )

    fun canTransition(from: String, to: String): Boolean {
        val allowed = transitions[from.uppercase()] ?: emptySet()
        return to.uppercase() in allowed || from.equals(to, ignoreCase = true)
    }

    fun validateTransition(from: String, to: String) {
        require(canTransition(from, to)) {
            "Transição inválida: $from → $to"
        }
    }

    fun allStatuses(): List<String> = transitions.keys.sorted()
}
