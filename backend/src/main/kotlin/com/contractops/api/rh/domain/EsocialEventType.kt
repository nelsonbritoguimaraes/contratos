package com.contractops.api.rh.domain

/**
 * Lista completa dos principais eventos do eSocial relevantes para o módulo de RH/Folha.
 * Baseado no leiaute oficial do eSocial (versão S-1.0+).
 */
enum class EsocialEventType(val description: String, val category: String) {
    // Não-periódicos (admissão, alterações, desligamento)
    S2200("Admissão de Trabalhador", "NAO_PERIODICO"),
    S2205("Alteração de Dados Cadastrais do Trabalhador", "NAO_PERIODICO"),
    S2206("Alteração de Contrato de Trabalho", "NAO_PERIODICO"),
    S2210("Comunicação de Acidente de Trabalho", "NAO_PERIODICO"),
    S2220("Monitoramento da Saúde do Trabalhador", "NAO_PERIODICO"),
    S2230("Afastamento Temporário", "NAO_PERIODICO"),
    S2240("Condições Ambientais do Trabalho", "NAO_PERIODICO"),
    S2299("Desligamento de Trabalhador", "NAO_PERIODICO"),
    S2300("Trabalhador sem Vínculo de Emprego", "NAO_PERIODICO"),

    // Tabelas
    S1010("Tabela de Rubricas", "TABELA"),
    S1070("Tabela de Processos", "TABELA"),

    // Periódicos
    S1200("Remuneração do Trabalhador", "PERIODICO"),
    S1210("Pagamento de Rendimentos do Trabalho", "PERIODICO"),
    S2399("Fechamento de Eventos Periódicos", "PERIODICO");

    companion object {
        fun getAll(): List<EsocialEventType> = entries.toList()
        fun getNonPeriodic(): List<EsocialEventType> = entries.filter { it.category == "NAO_PERIODICO" }
        fun getPeriodic(): List<EsocialEventType> = entries.filter { it.category == "PERIODICO" }
    }
}