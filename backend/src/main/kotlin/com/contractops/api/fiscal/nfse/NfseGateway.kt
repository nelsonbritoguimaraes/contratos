package com.contractops.api.fiscal.nfse

import com.contractops.api.fiscal.model.FiscalTransmitResult

interface NfseGateway {
    fun emit(xml: String, numero: String): FiscalTransmitResult
}
