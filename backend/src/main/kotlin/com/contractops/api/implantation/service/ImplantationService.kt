package com.contractops.api.implantation.service

import com.contractops.api.common.audit.GlobalAuditService
import com.contractops.api.implantation.domain.ContractImplantation
import com.contractops.api.implantation.domain.ImplantationChecklistItem
import com.contractops.api.implantation.repository.ContractImplantationRepository
import com.contractops.api.implantation.repository.ImplantationChecklistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
class ImplantationService(
    private val implantationRepository: ContractImplantationRepository,
    private val checklistRepository: ImplantationChecklistRepository,
    private val auditService: GlobalAuditService
) {
    private val defaultChecklist = listOf(
        "ASSINATURA" to "Assinatura do contrato",
        "CADASTRO_CONTRATO" to "Cadastro do contrato no sistema",
        "LOTES" to "Cadastro de lotes",
        "PLANILHA" to "Importação planilha vencedora",
        "POSTOS" to "Cadastro de postos",
        "ORGAO" to "Cadastro do órgão e fiscais",
        "PREPOSTO" to "Nomeação do preposto",
        "RECRUTAMENTO" to "Recrutamento e admissões",
        "TREINAMENTOS" to "Treinamentos obrigatórios",
        "UNIFORMES" to "Entrega de uniformes",
        "EQUIPAMENTOS" to "Alocação de equipamentos",
        "PONTO" to "Configuração relógios de ponto",
        "ATA_INICIO" to "Ata de início operacional"
    )

    fun listar(tenantId: UUID): List<ContractImplantation> =
        implantationRepository.findByTenantId(tenantId)

    fun buscarPorContrato(tenantId: UUID, contractId: UUID): ContractImplantation? =
        implantationRepository.findByTenantIdAndContractId(tenantId, contractId)

    fun checklist(implantationId: UUID): List<ImplantationChecklistItem> =
        checklistRepository.findByImplantationIdOrderBySortOrder(implantationId)

    @Transactional
    fun iniciar(tenantId: UUID, contractId: UUID): ContractImplantation {
        val existing = implantationRepository.findByTenantIdAndContractId(tenantId, contractId)
        if (existing != null) return existing

        val impl = implantationRepository.save(
            ContractImplantation(tenantId = tenantId, contractId = contractId, status = "EM_ANDAMENTO")
        )
        defaultChecklist.forEachIndexed { idx, (code, desc) ->
            checklistRepository.save(
                ImplantationChecklistItem(
                    tenantId = tenantId,
                    implantationId = impl.id!!,
                    code = code,
                    description = desc,
                    sortOrder = idx
                )
            )
        }
        auditService.registrar(tenantId, "IMPLANTATION", impl.id, "INICIAR", details = "Contrato $contractId")
        return impl
    }

    @Transactional
    fun concluirItem(itemId: UUID, tenantId: UUID, actor: String): ImplantationChecklistItem {
        val item = checklistRepository.findById(itemId).orElseThrow()
        require(item.tenantId == tenantId)
        item.completed = true
        item.completedAt = OffsetDateTime.now()
        item.completedBy = actor
        val saved = checklistRepository.save(item)
        val items = checklistRepository.findByImplantationIdOrderBySortOrder(item.implantationId)
        val impl = implantationRepository.findById(item.implantationId).orElseThrow()
        if (items.filter { it.required }.all { it.completed }) {
            impl.status = "PRONTO"
            implantationRepository.save(impl)
        }
        return saved
    }
}
