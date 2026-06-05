package com.contractops.api.common.config

import com.contractops.api.common.tenant.TenantRlsInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val tenantRlsInterceptor: TenantRlsInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tenantRlsInterceptor).addPathPatterns("/api/**")
    }
}
