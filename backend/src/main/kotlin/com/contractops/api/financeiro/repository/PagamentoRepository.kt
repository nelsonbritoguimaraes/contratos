package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.Pagamento
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PagamentoRepository : JpaRepository<Pagamento, UUID>
