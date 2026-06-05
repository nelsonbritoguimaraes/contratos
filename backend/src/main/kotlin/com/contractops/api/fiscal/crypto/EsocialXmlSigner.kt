package com.contractops.api.fiscal.crypto

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Base64
import javax.xml.crypto.dsig.*
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfo
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory
import javax.xml.crypto.dsig.keyinfo.X509Data
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec
import javax.xml.crypto.dsig.spec.TransformParameterSpec
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Assinatura XML-DSig para eventos eSocial (ICP-Brasil, procEmi=1).
 * Quando certificado não está configurado, insere bloco Signature stub para sandbox/dev.
 */
@Component
class EsocialXmlSigner(
    private val keyStoreLoader: IcpBrasilKeyStoreLoader
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class SignResult(
        val xml: String,
        val signed: Boolean,
        val mode: String,
        val message: String
    )

    fun sign(xml: String): SignResult {
        val material = keyStoreLoader.loadEsocialKeyMaterial()
        return if (material != null) {
            try {
                SignResult(
                    xml = signWithXmlDsig(xml, material.privateKey, material.certificate),
                    signed = true,
                    mode = "ICP_BRASIL",
                    message = "Assinado com certificado ${material.certificate.subjectX500Principal.name}"
                )
            } catch (ex: Exception) {
                log.error("Falha assinatura XML-DSig: {}", ex.message)
                SignResult(
                    xml = appendStubSignature(xml),
                    signed = false,
                    mode = "FALLBACK_STUB",
                    message = "Assinatura real falhou; stub aplicado: ${ex.message}"
                )
            }
        } else {
            SignResult(
                xml = appendStubSignature(xml),
                signed = false,
                mode = "STUB",
                message = "Certificado não configurado — configure contractops.fiscal.esocial.certificate-path"
            )
        }
    }

    private fun signWithXmlDsig(xml: String, privateKey: PrivateKey, certificate: X509Certificate): String {
        val dbf = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val doc = dbf.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

        val targetId = findSignableId(doc.documentElement)
            ?: throw IllegalArgumentException("Elemento com atributo Id não encontrado no XML eSocial")

        val fac = XMLSignatureFactory.getInstance("DOM")
        val digestMethod = fac.newDigestMethod(DigestMethod.SHA256, null)
        val transformList = listOf(
            fac.newTransform(Transform.ENVELOPED, null as TransformParameterSpec?),
            fac.newTransform(CanonicalizationMethod.INCLUSIVE, null as TransformParameterSpec?)
        )
        val reference = fac.newReference("#$targetId", digestMethod, transformList, null, null)
        val signedInfo = fac.newSignedInfo(
            fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, null as C14NMethodParameterSpec?),
            fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
            listOf(reference)
        )

        val kif = fac.getKeyInfoFactory()
        val x509Data: X509Data = kif.newX509Data(listOf(certificate))
        val keyInfo: KeyInfo = kif.newKeyInfo(listOf(x509Data))

        val signature = fac.newXMLSignature(signedInfo, keyInfo)
        val signContext = DOMSignContext(privateKey, doc.documentElement)
        signContext.uriDereferencer = fac.uriDereferencer
        signature.sign(signContext)

        return documentToString(doc)
    }

    private fun findSignableId(root: Element): String? {
        if (root.hasAttribute("Id")) return root.getAttribute("Id")
        for (i in 0 until root.childNodes.length) {
            val node = root.childNodes.item(i)
            if (node is Element) {
                findSignableId(node)?.let { return it }
            }
        }
        return null
    }

    private fun appendStubSignature(xml: String): String {
        log.warn("Using stub XML signature — NOT valid for production")
        if (xml.contains("<Signature") || xml.contains("xmlns:ds=")) return xml
        val stub = """
            <!-- ContractOps STUB Signature — substituir por ICP-Brasil em produção -->
            <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
              <SignedInfo>
                <CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
                <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
                <Reference URI="">
                  <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
                  <DigestValue>${Base64.getEncoder().encodeToString(xml.toByteArray().copyOf(32))}</DigestValue>
                </Reference>
              </SignedInfo>
              <SignatureValue>STUB-CONTRACTOPS-NOT-FOR-PRODUCTION</SignatureValue>
            </Signature>
        """.trimIndent()
        return if (xml.contains("</eSocial>")) {
            xml.replace("</eSocial>", "$stub\n</eSocial>")
        } else {
            "$xml\n$stub"
        }
    }

    private fun documentToString(doc: Document): String {
        val tf = TransformerFactory.newInstance()
        val transformer = tf.newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "no")
        }
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }
}
