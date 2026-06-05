package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.Recebimento
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RecebimentoRepository : JpaRepository<Recebimento, UUID>
