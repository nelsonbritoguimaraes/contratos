package com.contractops.api.financeiro.repository

import com.contractops.api.financeiro.domain.TenantFiscalProfile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TenantFiscalProfileRepository : JpaRepository<TenantFiscalProfile, UUID>
