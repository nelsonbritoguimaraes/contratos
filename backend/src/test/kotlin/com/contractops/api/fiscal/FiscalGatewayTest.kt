package com.contractops.api.fiscal

import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.crypto.IcpBrasilKeyStoreLoader
import com.contractops.api.fiscal.esocial.EsocialGatewayRouter
import com.contractops.api.fiscal.esocial.HttpEsocialGateway
import com.contractops.api.fiscal.esocial.SandboxEsocialGateway
import com.contractops.api.fiscal.esocial.StubEsocialGateway
import com.contractops.api.fiscal.nfse.NfseGatewayRouter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.web.client.RestTemplateBuilder

class FiscalGatewayTest {

    @Test
    fun `sandbox esocial aceita xml valido`() {
        val props = FiscalProperties(mode = "sandbox")
        val router = EsocialGatewayRouter(
            props,
            StubEsocialGateway(),
            SandboxEsocialGateway(),
            HttpEsocialGateway(props, IcpBrasilKeyStoreLoader(props), RestTemplateBuilder())
        )
        val xml = """<?xml version="1.0"?><eSocial><evtAdmissao/><Signature/></eSocial>"""
        val result = router.transmit("S2200", xml)
        assertTrue(result.success)
        assertTrue(result.protocolNumber!!.startsWith("SANDBOX"))
    }

    @Test
    fun `sandbox esocial rejeita xml sem assinatura`() {
        val gateway = SandboxEsocialGateway()
        val xml = """<?xml version="1.0"?><eSocial><evtAdmissao/></eSocial>"""
        assertThrows<IllegalArgumentException> { gateway.transmit("S2200", xml) }
    }

    @Test
    fun `nfse sandbox retorna protocolo`() {
        val props = FiscalProperties(mode = "sandbox")
        val router = NfseGatewayRouter(props, RestTemplateBuilder())
        val result = router.emit("<Rps/>", "NFS-001")
        assertTrue(result.success)
    }
}
