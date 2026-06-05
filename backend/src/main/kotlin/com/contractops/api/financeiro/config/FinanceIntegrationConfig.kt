package com.contractops.api.financeiro.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    ObligationsProperties::class,
    OpenFinanceProperties::class,
    NfsWorkflowProperties::class,
    TemporalProperties::class
)
class FinanceIntegrationConfig
