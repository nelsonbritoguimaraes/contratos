package com.contractops.api.time.bridge

/**
 * Fabricantes de relógios de ponto suportados (conforme Portaria 671 e mercado brasileiro).
 * Usado pelo Clock Bridge para roteamento de adaptadores.
 */
enum class ClockVendor(val displayName: String, val supportsAFD: Boolean = true) {
    GENERIC_AFD("Genérico AFD (Portaria 671)", true),
    CONTROL_ID("Control iD", true),
    TOPDATA("Topdata", true),
    HENRY("Henry", true),
    DIMEP_KAIROS("Dimep / Kairos", true),
    MADIS("Madis", true),
    SECULLUM("Secullum", true),
    ZKTECO("ZKTeco", true),
    BIOMETRICO_OUTROS("Outros Biométricos / REP", true),
    MANUAL("Importação Manual / CSV", false);

    companion object {
        fun fromName(name: String?): ClockVendor {
            if (name.isNullOrBlank()) return GENERIC_AFD
            return entries.firstOrNull { 
                it.name.equals(name, ignoreCase = true) || 
                it.displayName.contains(name, ignoreCase = true) 
            } ?: GENERIC_AFD
        }
    }
}