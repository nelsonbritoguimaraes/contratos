package com.contractops.api.common.domain

import java.util.UUID

/**
 * Interface marcadora para entidades multi-tenant.
 * Toda entidade que representa dados de negócio deve implementar isso.
 *
 * Na Fase 1 usaremos um filtro simples em nível de aplicação.
 * RLS real no Postgres virá em fases posteriores (conforme plano aprovado).
 */
interface TenantAware {
    val tenantId: UUID
}
