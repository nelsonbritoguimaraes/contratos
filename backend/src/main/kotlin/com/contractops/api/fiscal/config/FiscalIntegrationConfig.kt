package com.contractops.api.fiscal.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(FiscalProperties::class)
class FiscalIntegrationConfig
