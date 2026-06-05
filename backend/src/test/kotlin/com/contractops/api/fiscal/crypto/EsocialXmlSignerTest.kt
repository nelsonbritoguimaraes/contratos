package com.contractops.api.fiscal.crypto

import com.contractops.api.fiscal.config.FiscalProperties
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EsocialXmlSignerTest {

    private val fiscalProperties = FiscalProperties().apply {
        esocial.certificatePath = null
    }

    private val signer = EsocialXmlSigner(IcpBrasilKeyStoreLoader(fiscalProperties))

    @Test
    fun `sign adiciona Signature stub quando certificado ausente`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtRemun/v_S_01_03_00">
              <evtRemun Id="ID123">
                <ideEvento><tpAmb>2</tpAmb></ideEvento>
              </evtRemun>
            </eSocial>
        """.trimIndent()

        val result = signer.sign(xml)
        assertTrue(result.xml.contains("<Signature"))
        assertTrue(result.mode == "STUB")
    }
}
