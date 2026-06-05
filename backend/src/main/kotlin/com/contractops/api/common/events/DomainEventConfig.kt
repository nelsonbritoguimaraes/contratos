package com.contractops.api.common.events

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class DomainEventConfig {

    // Method names must differ from the component bean names
    // (kafkaDomainEventPublisher / inMemoryDomainEventPublisher) to avoid a
    // BeanDefinitionOverrideException. These just expose the chosen publisher
    // as the @Primary DomainEventPublisher.
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "contractops.kafka", name = ["enabled"], havingValue = "true")
    fun primaryKafkaDomainEventPublisher(
        kafkaPublisher: KafkaDomainEventPublisher
    ): DomainEventPublisher = kafkaPublisher

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "contractops.kafka", name = ["enabled"], havingValue = "false", matchIfMissing = true)
    fun primaryInMemoryDomainEventPublisher(
        publisher: InMemoryDomainEventPublisher
    ): DomainEventPublisher = publisher
}
