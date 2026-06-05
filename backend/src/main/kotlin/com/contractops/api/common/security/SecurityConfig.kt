package com.contractops.api.common.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

/**
 * Configuração básica de segurança (preparada para Keycloak).
 * Alinhado com SPEC v1.0 seções 2.1 e 24.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    @Value("\${contractops.security.permit-all-api:true}") private val permitAllApi: Boolean
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/**", "/error").permitAll()
                if (permitAllApi) {
                    auth.requestMatchers("/api/**").permitAll()
                }
                auth.anyRequest().authenticated()
            }

        if (!permitAllApi) {
            http.oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
        }

        return http.build()
    }

    /**
     * Extrai authorities do JWT (roles do Keycloak).
     * Também pode ser estendido para extrair tenant_id de claims customizadas.
     */
    @Bean
    fun jwtAuthenticationConverter(): TenantAwareJwtAuthenticationConverter {
        return TenantAwareJwtAuthenticationConverter()
    }
}
