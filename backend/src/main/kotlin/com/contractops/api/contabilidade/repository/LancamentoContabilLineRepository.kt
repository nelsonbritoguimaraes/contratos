package com.contractops.api.contabilidade.repository

import com.contractops.api.contabilidade.domain.LancamentoContabilLine
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface LancamentoContabilLineRepository : JpaRepository<LancamentoContabilLine, UUID> {
    fun findByLancamentoIdOrderByLinhaOrdemAsc(lancamentoId: UUID): List<LancamentoContabilLine>
    fun deleteByLancamentoId(lancamentoId: UUID)
}
