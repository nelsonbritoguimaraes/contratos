package com.contractops.api.ia.api

import com.contractops.api.common.tenant.TenantContext
import com.contractops.api.ia.agents.*
import com.contractops.api.ia.orchestrator.AgentRouter
import com.contractops.api.ia.orchestrator.AiOrchestrator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/ia")
class IaController(
    private val orchestrator: AiOrchestrator,
    private val glosaAgent: GlosaAgent,
    private val contractAgent: ContractAgent,
    private val pontoAgent: PontoAgent,
    private val folhaAgent: FolhaAgent,
    private val fiscalAgent: FiscalAgent,
    private val documentAgent: DocumentAgent,
    private val contabilAgent: ContabilAgent,
    private val estoqueAgent: EstoqueAgent,
    private val executivoAgent: ExecutivoAgent,
    private val licitacoesAgent: LicitacoesAgent,
    private val agentRouter: AgentRouter
) {

    @PostMapping("/prompt")
    fun prompt(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val prompt = request["prompt"] as? String ?: return ResponseEntity.badRequest().build()
        val response = orchestrator.execute(prompt)
        return ResponseEntity.ok(mapOf("response" to response))
    }

    @PostMapping("/agente/glosa")
    fun analisarGlosa(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        val descricao = body["descricao"] ?: ""
        val contexto = body["contexto"] ?: ""
        val attendance = body["attendance"] ?: ""

        val resultado = glosaAgent.analisarGlosa(descricao, contexto, attendance)
        return ResponseEntity.ok(resultado)
    }

    @PostMapping("/agente/contrato/resumo")
    fun resumirContrato(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        val texto = body["texto"] ?: ""
        return ResponseEntity.ok(contractAgent.resumirContrato(texto))
    }

    @GetMapping("/providers")
    fun providers(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(orchestrator.listAvailableProviders())
    }

    // ==================== NOVOS AGENTES MVP ====================

    @PostMapping("/agente/ponto")
    fun analisarPonto(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        val resumo = body["resumo"] ?: ""
        val meta = body["meta"]?.toDoubleOrNull() ?: 95.0
        return ResponseEntity.ok(pontoAgent.analisarCobertura(body["contrato"] ?: "", resumo, meta))
    }

    @PostMapping("/agente/folha")
    fun analisarFolha(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        val eventos = body["eventos"] ?: ""
        val rubricas = body["rubricas"] ?: ""
        return ResponseEntity.ok(folhaAgent.analisarEventos(body["colaborador"] ?: "", eventos, rubricas))
    }

    @PostMapping("/agente/fiscal")
    fun analisarFiscal(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        return ResponseEntity.ok(
            fiscalAgent.analisarRetencoes(
                body["valor"] ?: "",
                body["retencoes"] ?: "",
                body["municipio"] ?: ""
            )
        )
    }

    @PostMapping("/agente/documento")
    fun analisarDocumento(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        return ResponseEntity.ok(
            documentAgent.analisarDocumento(
                body["texto"] ?: "",
                body["tipo"] ?: "genérico"
            )
        )
    }

    @PostMapping("/agente/contabil")
    fun sugerirLancamentos(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        return ResponseEntity.ok(
            contabilAgent.sugerirLancamentos(
                body["descricao"] ?: "",
                body["valor"] ?: "",
                body["contrato"] ?: ""
            )
        )
    }

    @PostMapping("/agente/estoque")
    fun preverEstoque(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        return ResponseEntity.ok(
            estoqueAgent.preverNecessidade(
                body["contrato"] ?: "",
                body["rotatividade"] ?: "",
                body["admissoes"] ?: ""
            )
        )
    }

    @PostMapping("/agente/executivo")
    fun perguntaExecutiva(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        return ResponseEntity.ok(
            executivoAgent.responderPerguntaExecutiva(
                body["pergunta"] ?: "",
                body["contexto"] ?: ""
            )
        )
    }

    @PostMapping("/agente/licitacao")
    fun analisarLicitacao(@RequestBody body: Map<String, String>): ResponseEntity<String> {
        return ResponseEntity.ok(licitacoesAgent.analisarEdital(body["edital"] ?: ""))
    }

    @PostMapping("/ask")
    fun smartAsk(@RequestBody body: Map<String, Any>): ResponseEntity<Any> {
        val question = body["question"] as? String ?: return ResponseEntity.badRequest().build()
        val context = body.filterKeys { it != "question" }
        val tenantId = (body["tenantId"] as? String)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

        val results = agentRouter.routeAndExecute(question, context)
        val routed = results.map { it.agentName }
        val answersMap = results.associate { it.agentName to it.response }
        val combinedResponse = answersMap.values.joinToString("\n\n")

        // Registra no histórico real (fortalece /ia/calls com agentes e custo estimado básico)
        orchestrator.recordIaCall(
            promptPreview = question,
            responsePreview = combinedResponse,
            provider = "router",
            tenantId = tenantId,
            routedAgents = routed,
            costEstimate = 0.0015 * (1 + routed.size) // estimativa simples por agente
        )

        return ResponseEntity.ok(mapOf(
            "question" to question,
            "routed_agents" to routed,
            "answers" to answersMap
        ))
    }

    @GetMapping("/calls")
    fun recentCalls(@RequestParam(defaultValue = "10") limit: Int): ResponseEntity<Any> {
        return ResponseEntity.ok(orchestrator.getRecentCalls(limit))
    }

    @GetMapping("/dashboard")
    fun iaDashboard(): ResponseEntity<Any> {
        return ResponseEntity.ok(orchestrator.getDashboardSummary())
    }
}