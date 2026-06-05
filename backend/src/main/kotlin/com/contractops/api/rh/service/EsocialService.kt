package com.contractops.api.rh.service

import com.contractops.api.employee.repository.EmployeeRepository
import com.contractops.api.fiscal.crypto.EsocialXmlSigner
import com.contractops.api.fiscal.esocial.EsocialGatewayRouter
import com.contractops.api.fiscal.esocial.EsocialXmlBuilder
import com.contractops.api.rh.domain.EsocialEvent
import com.contractops.api.rh.repository.EsocialEventRepository
import com.contractops.api.rh.repository.PayslipItemRepository
import com.contractops.api.rh.repository.PayslipRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

/**
 * Geração e transmissão eSocial S-1.3 (modo configurável: stub | sandbox | production).
 */
@Service
class EsocialService(
    private val repository: EsocialEventRepository,
    private val employeeRepository: EmployeeRepository,
    private val payslipRepository: PayslipRepository,
    private val payslipItemRepository: PayslipItemRepository,
    private val esocialXmlBuilder: EsocialXmlBuilder,
    private val esocialGatewayRouter: EsocialGatewayRouter,
    private val esocialXmlSigner: EsocialXmlSigner,
    private val objectMapper: ObjectMapper = ObjectMapper()
) {

    fun findPendingByTenant(tenantId: UUID): List<EsocialEvent> =
        repository.findByTenantIdAndStatus(tenantId, "PENDING")

    @Transactional
    fun simulateSend(eventId: UUID, tenantId: UUID): EsocialEvent =
        transmitEvent(eventId, tenantId)

    @Transactional
    fun transmitEvent(eventId: UUID, tenantId: UUID): EsocialEvent {
        val event = repository.findById(eventId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Evento eSocial não encontrado") }

        if (event.status !in listOf("GENERATED", "PENDING")) {
            throw IllegalStateException("Evento não pode ser transmitido no status ${event.status}")
        }

        val xml = extractXml(event.payload) ?: event.payload.orEmpty()
        val signResult = esocialXmlSigner.sign(xml)
        val result = esocialGatewayRouter.transmit(event.eventType ?: "UNKNOWN", signResult.xml)

        event.status = if (result.success) "SENT" else "ERROR"
        event.receiptNumber = result.receiptNumber ?: result.protocolNumber

        val jsonPart = event.payload?.substringBefore("| XML: ")?.trim()
            ?.removePrefix("JSON:")?.trim().orEmpty().ifBlank { "{}" }
        event.payload = buildString {
            append("JSON: $jsonPart | XML: ${signResult.xml}")
            append("\n\n--- ASSINATURA ---\n")
            append("mode=${signResult.mode} signed=${signResult.signed}\n")
            append(signResult.message)
            append("\n\n--- TRANSMISSÃO ---\n")
            append(result.message)
            append("\nprotocolo=${result.protocolNumber}")
        }

        return repository.save(event)
    }

    private fun extractXml(payload: String?): String? {
        if (payload.isNullOrBlank()) return null
        val marker = "| XML: "
        val idx = payload.indexOf(marker)
        if (idx >= 0) return payload.substring(idx + marker.length).trim()
        return if (payload.trimStart().startsWith("<?xml")) payload else null
    }

    @Transactional
    fun generateS2200Admissao(employeeId: UUID, tenantId: UUID): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        val payload = mapOf(
            "evento" to "S-2200 - Admissão de Trabalhador",
            "matricula" to (employee.matricula ?: "N/A"),
            "cpf" to employee.cpf,
            "nome" to employee.fullName,
            "dataNascimento" to employee.birthDate?.toString(),
            "dataAdmissao" to employee.admissionDate?.toString(),
            "cargo" to employee.cargo,
            "cbo" to employee.cbo,
            "salarioBase" to employee.salaryBase,
            "tipoContrato" to (employee.contractType ?: "CLT"),
            "pis" to employee.pisNis,
            "ctps" to null,
            "geradoEm" to OffsetDateTime.now().toString(),
            "observacao" to "Stub gerado pelo ContractOps AI - dados sujeitos a complementação"
        )

        val jsonPayload = objectMapper.writeValueAsString(payload)
        val xmlPayload = esocialXmlBuilder.buildS2200(employee)

        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S2200",
            competence = LocalDate.now().withDayOfMonth(1),
            payload = "JSON: $jsonPayload | XML: $xmlPayload",
            status = "GENERATED",
            generatedAt = OffsetDateTime.now()
        )

        return repository.save(event)
    }

    @Transactional
    fun generateS2299Desligamento(employeeId: UUID, tenantId: UUID, dataDesligamento: LocalDate, motivo: String): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        val payload = mapOf(
            "evento" to "S-2299 - Desligamento de Trabalhador",
            "matricula" to (employee.matricula ?: "N/A"),
            "cpf" to employee.cpf,
            "nome" to employee.fullName,
            "dataDesligamento" to dataDesligamento.toString(),
            "motivoDesligamento" to motivo,
            "ultimoSalario" to employee.salaryBase,
            "geradoEm" to OffsetDateTime.now().toString(),
            "observacao" to "Stub gerado pelo ContractOps AI"
        )

        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S2299",
            competence = dataDesligamento.withDayOfMonth(1),
            payload = "JSON: ${objectMapper.writeValueAsString(payload)} | XML: ${esocialXmlBuilder.buildS2299(employee, dataDesligamento, motivo)}",
            status = "GENERATED",
            generatedAt = OffsetDateTime.now()
        )

        return repository.save(event)
    }

    @Transactional
    fun generateS1200Remuneracao(employeeId: UUID, tenantId: UUID, competencia: LocalDate): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        val payslip = payslipRepository.findByTenantIdAndEmployeeIdAndCompetence(tenantId, employeeId, competencia)
        val rubricas = if (payslip?.id != null) {
            payslipItemRepository.findByPayslipId(payslip.id!!).map { item ->
                item.rubric.code to item.totalValue.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            }
        } else {
            listOf("SALARIO_BASE" to (employee.salaryBase?.toPlainString() ?: "0.00"))
        }

        val payload = mapOf(
            "evento" to "S-1200",
            "cpf" to employee.cpf,
            "matricula" to employee.matricula,
            "competencia" to competencia.toString(),
            "rubricas" to rubricas,
            "payslipId" to payslip?.id?.toString(),
            "geradoEm" to OffsetDateTime.now().toString()
        )

        val jsonPayload = objectMapper.writeValueAsString(payload)
        val xmlPayload = esocialXmlBuilder.buildS1200(employee.cpf, competencia, rubricas)

        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S1200",
            competence = competencia.withDayOfMonth(1),
            payload = "JSON: $jsonPayload | XML: $xmlPayload",
            status = "GENERATED",
            generatedAt = OffsetDateTime.now()
        )

        return repository.save(event)
    }

    @Transactional
    fun generateS1299FechamentoPeriodico(competence: LocalDate, tenantId: UUID): EsocialEvent {
        val comp = competence.withDayOfMonth(1)
        val payload = mapOf(
            "evento" to "S-1299 - Fechamento dos Eventos Periódicos",
            "competencia" to comp.toString(),
            "prazoLegal" to "Até dia 15 do mês subsequente (MOS eSocial S-1.3)",
            "geradoEm" to OffsetDateTime.now().toString()
        )
        val xml = esocialXmlBuilder.buildS1299(comp)
        val event = EsocialEvent(
            tenantId = tenantId,
            eventType = "S1299",
            competence = comp,
            payload = "JSON: ${objectMapper.writeValueAsString(payload)} | XML: $xml",
            status = "GENERATED",
            generatedAt = OffsetDateTime.now()
        )
        return repository.save(event)
    }

    @Transactional
    fun generateS1210PagamentoColaborador(
        employeeId: UUID,
        tenantId: UUID,
        competencia: LocalDate,
        valorLiquido: java.math.BigDecimal,
        dataPagamento: LocalDate
    ): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }
        val comp = competencia.withDayOfMonth(1)
        val xml = esocialXmlBuilder.buildS1210(
            employee.cpf,
            comp,
            valorLiquido.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
            dataPagamento
        )
        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S1210",
            competence = comp,
            payload = "JSON: {\"evento\":\"S-1210\",\"valorLiquido\":$valorLiquido} | XML: $xml",
            status = "GENERATED",
            generatedAt = OffsetDateTime.now()
        )
        return repository.save(event)
    }

    private fun generateSimpleS2200Xml(data: Map<String, Any?>): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtAdmissao/v_S_01_00_00">
                <evtAdmissao Id="ID1">
                    <ideEvento>
                        <tpAmb>1</tpAmb>
                        <procEmi>1</procEmi>
                        <verProc>1.0.0</verProc>
                    </ideEvento>
                    <ideEmpregador>
                        <tpInsc>1</tpInsc>
                        <nrInsc>00000000000000</nrInsc>
                    </ideEmpregador>
                    <trabalhador>
                        <cpfTrab>${data["cpf"]}</cpfTrab>
                        <nmTrab>${data["nome"]}</nmTrab>
                        <dtNascto>${data["dataNascimento"] ?: "1990-01-01"}</dtNascto>
                    </trabalhador>
                    <vinculo>
                        <matAEnt>${data["matricula"]}</matAEnt>
                        <dtAdm>${data["dataAdmissao"]}</dtAdm>
                        <cargo>
                            <nmCargo>${data["cargo"]}</nmCargo>
                            <cbocargo>${data["cbo"]}</cbocargo>
                        </cargo>
                        <remuneracao>
                            <vrSalFx>${data["salarioBase"]}</vrSalFx>
                        </remuneracao>
                    </vinculo>
                </evtAdmissao>
            </eSocial>
        """.trimIndent()
    }

    // Gerador genérico melhorado para diferentes tipos de eventos
    fun generateStructuredXml(eventType: String, data: Map<String, Any?>): String {
        val base = """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/${eventType.lowercase()}/v_S_01_00_00">
                <${eventType} Id="ID${System.currentTimeMillis()}">
                    <ideEvento>
                        <tpAmb>1</tpAmb>
                        <procEmi>1</procEmi>
                        <verProc>1.0.0</verProc>
                    </ideEvento>
                    <ideEmpregador>
                        <tpInsc>1</tpInsc>
                        <nrInsc>00000000000000</nrInsc>
                    </ideEmpregador>
        """.trimIndent()

        val body = when (eventType) {
            "S2200", "S2205", "S2206" -> """
                        <trabalhador>
                            <cpfTrab>${data["cpf"]}</cpfTrab>
                            <nmTrab>${data["nome"]}</nmTrab>
                        </trabalhador>
                        <vinculo>
                            <matAEnt>${data["matricula"]}</matAEnt>
                        </vinculo>
            """.trimIndent()
            "S2399" -> """
                        <infoFech>
                            <competencia>${data["competencia"]}</competencia>
                        </infoFech>
            """.trimIndent()
            else -> "<dados>${data}</dados>"
        }

        return "$base$body</${eventType}></eSocial>"
    }

    @Transactional
    fun generateS1010Rubricas(tenantId: UUID): EsocialEvent {
        val payload = mapOf(
            "evento" to "S-1010 - Tabela de Rubricas",
            "observacao" to "Geração de rubricas do tenant (stub)",
            "geradoEm" to java.time.OffsetDateTime.now().toString()
        )

        val event = EsocialEvent(
            tenantId = tenantId,
            eventType = "S1010",
            competence = java.time.LocalDate.now().withDayOfMonth(1),
            payload = objectMapper.writeValueAsString(payload),
            status = "GENERATED",
            generatedAt = java.time.OffsetDateTime.now()
        )

        return repository.save(event)
    }

    // ==================== NOVOS EVENTOS COMPLETOS ====================

    @Transactional
    fun generateS2205AlteracaoCadastral(employeeId: UUID, tenantId: UUID): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        val payload = mapOf(
            "evento" to "S-2205 - Alteração de Dados Cadastrais",
            "cpf" to employee.cpf,
            "alteracoes" to mapOf(
                "nome" to employee.fullName,
                "dataNascimento" to employee.birthDate?.toString(),
                "sexo" to employee.gender
            ),
            "geradoEm" to java.time.OffsetDateTime.now().toString()
        )

        val xmlPayload = esocialXmlBuilder.buildS2205(employee)
        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S2205",
            competence = java.time.LocalDate.now().withDayOfMonth(1),
            payload = "JSON: ${objectMapper.writeValueAsString(payload)} | XML: $xmlPayload",
            status = "GENERATED",
            generatedAt = java.time.OffsetDateTime.now()
        )
        return repository.save(event)
    }

    @Transactional
    fun generateS2206AlteracaoContrato(employeeId: UUID, tenantId: UUID, novoSalario: java.math.BigDecimal, novaFuncao: String?): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        val payload = mapOf(
            "evento" to "S-2206 - Alteração de Contrato de Trabalho",
            "cpf" to employee.cpf,
            "matricula" to employee.matricula,
            "novaRemuneracao" to novoSalario,
            "novaFuncao" to (novaFuncao ?: employee.cargo),
            "dataAlteracao" to java.time.LocalDate.now().toString(),
            "geradoEm" to java.time.OffsetDateTime.now().toString()
        )

        val xmlPayload = esocialXmlBuilder.buildS2206(employee, novoSalario.toPlainString(), novaFuncao)
        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S2206",
            competence = java.time.LocalDate.now().withDayOfMonth(1),
            payload = "JSON: ${objectMapper.writeValueAsString(payload)} | XML: $xmlPayload",
            status = "GENERATED",
            generatedAt = java.time.OffsetDateTime.now()
        )
        return repository.save(event)
    }

    @Transactional
    fun generateS2230Afastamento(employeeId: UUID, tenantId: UUID, tipoAfastamento: String, dataInicio: java.time.LocalDate, dataFim: java.time.LocalDate?): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        val payload = mapOf(
            "evento" to "S-2230 - Afastamento Temporário",
            "cpf" to employee.cpf,
            "matricula" to employee.matricula,
            "tipoAfastamento" to tipoAfastamento, // Ex: 01 - Acidente, 02 - Licença Médica, etc.
            "dataInicio" to dataInicio.toString(),
            "dataFim" to dataFim?.toString(),
            "geradoEm" to java.time.OffsetDateTime.now().toString()
        )

        val xmlPayload = esocialXmlBuilder.buildS2230(employee, tipoAfastamento, dataInicio, dataFim)
        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S2230",
            competence = dataInicio.withDayOfMonth(1),
            payload = "JSON: ${objectMapper.writeValueAsString(payload)} | XML: $xmlPayload",
            status = "GENERATED",
            generatedAt = java.time.OffsetDateTime.now()
        )
        return repository.save(event)
    }

    @Transactional
    fun generateS2399FechamentoPeriodico(competence: java.time.LocalDate, tenantId: UUID): EsocialEvent {
        // S-2399 = término sem vínculo; fechamento periódico correto é S-1299
        return generateS1299FechamentoPeriodico(competence, tenantId)
    }

    @Transactional
    fun generateS1210Pagamento(competence: LocalDate, tenantId: UUID): EsocialEvent {
        val comp = competence.withDayOfMonth(1)
        val payload = mapOf(
            "evento" to "S-1210 - Pagamento de Rendimentos (consolidado)",
            "competencia" to comp.toString(),
            "observacao" to "Use generateS1210PagamentoColaborador para detalhe por colaborador",
            "geradoEm" to OffsetDateTime.now().toString()
        )
        val event = EsocialEvent(
            tenantId = tenantId,
            eventType = "S1210",
            competence = comp,
            payload = objectMapper.writeValueAsString(payload),
            status = "GENERATED",
            generatedAt = OffsetDateTime.now()
        )
        return repository.save(event)
    }

    @Transactional
    fun generateS2240CondicoesAmbientais(employeeId: UUID, tenantId: UUID, tipo: String, grau: String, dataInicio: java.time.LocalDate): EsocialEvent {
        val employee = employeeRepository.findById(employeeId)
            .filter { it.tenantId == tenantId }
            .orElseThrow { IllegalArgumentException("Colaborador não encontrado") }

        val payload = mapOf(
            "evento" to "S-2240 - Condições Ambientais do Trabalho",
            "cpf" to employee.cpf,
            "matricula" to employee.matricula,
            "tipoCondicao" to tipo, // INSALUBRIDADE, PERICULOSIDADE, etc.
            "grau" to grau,         // MÍNIMO, MÉDIO, MÁXIMO
            "dataInicio" to dataInicio.toString(),
            "geradoEm" to java.time.OffsetDateTime.now().toString()
        )

        val xmlPayload = esocialXmlBuilder.buildS2240(employee, tipo, grau, dataInicio)
        val event = EsocialEvent(
            tenantId = tenantId,
            employeeId = employeeId,
            eventType = "S2240",
            competence = dataInicio.withDayOfMonth(1),
            payload = "JSON: ${objectMapper.writeValueAsString(payload)} | XML: $xmlPayload",
            status = "GENERATED",
            generatedAt = java.time.OffsetDateTime.now()
        )
        return repository.save(event)
    }
}