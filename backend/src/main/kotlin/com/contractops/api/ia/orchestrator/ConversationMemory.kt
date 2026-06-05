package com.contractops.api.ia.orchestrator

import org.springframework.stereotype.Service
import java.util.*

/**
 * Memória simples de conversação por tenant/sessão - Fase 5.
 * Permite que agentes mantenham contexto entre interações.
 */
@Service
class ConversationMemory {

    private val sessions = mutableMapOf<String, MutableList<String>>()

    fun addMessage(sessionId: String, message: String) {
        sessions.getOrPut(sessionId) { mutableListOf() }.add(message)
    }

    fun getHistory(sessionId: String, maxMessages: Int = 6): String {
        val history = sessions[sessionId] ?: return ""
        return history.takeLast(maxMessages).joinToString("\n")
    }

    fun clear(sessionId: String) {
        sessions.remove(sessionId)
    }
}