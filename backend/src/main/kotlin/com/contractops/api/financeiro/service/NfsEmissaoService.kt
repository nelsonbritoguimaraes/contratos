package com.contractops.api.financeiro.service

import com.contractops.api.fiscal.config.FiscalProperties
import com.contractops.api.fiscal.model.FiscalTransmitResult
import com.contractops.api.fiscal.nfse.NfseGatewayRouter
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Emissão NFS-e (ABRASF / Nacional) com transmissão via NfseGatewayRouter.
 */
@Service
class NfsEmissaoService(
    private val fiscalProperties: FiscalProperties,
    private val nfseGatewayRouter: NfseGatewayRouter
) {

    fun gerarXmlNfs(
        numero: String,
        tomadorCnpj: String,
        tomadorRazaoSocial: String = "ÓRGÃO PÚBLICO CONTRATANTE",
        valorServicos: BigDecimal,
        iss: BigDecimal,
        outrasRetencoes: BigDecimal,
        dataEmissao: LocalDate,
        prestadorCnpj: String? = null,
        prestadorRazaoSocial: String = "CONTRATOPS - PRESTADOR DE SERVIÇOS",
        itemListaServico: String = "1402",
        codigoMunicipio: String? = null,
        naturezaOperacao: String = "1"
    ): String {
        val cnpjPrestador = prestadorCnpj ?: fiscalProperties.nfse.prestadorCnpj
        val codMun = codigoMunicipio ?: fiscalProperties.nfse.municipioIbge

        val idRps = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val reformaTributaria = incluirCbsIbsReforma(dataEmissao)
        val blocoCbsIbs = if (reformaTributaria) {
            val base = valorServicos
            val aliqCbs = BigDecimal("0.009")
            val aliqIbs = BigDecimal("0.001")
            val vCbs = base.multiply(aliqCbs).setScale(2, java.math.RoundingMode.HALF_UP)
            val vIbs = base.multiply(aliqIbs).setScale(2, java.math.RoundingMode.HALF_UP)
            """
                        <IBSCBS xmlns="http://www.abrasf.org.br/nfse/reforma/2026">
                            <vBC>${base}</vBC>
                            <gIBSCBS>
                                <CST>000</CST>
                                <cClassTrib>000001</cClassTrib>
                                <gIBSUF>
                                    <pIBSUF>0.10</pIBSUF>
                                    <vIBSUF>${vIbs}</vIBSUF>
                                </gIBSUF>
                                <gCBS>
                                    <pCBS>0.90</pCBS>
                                    <vCBS>${vCbs}</vCBS>
                                </gCBS>
                            </gIBSCBS>
                        </IBSCBS>
            """.trimIndent()
        } else ""

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Rps xmlns="http://www.abrasf.org.br/ABRASF/arquivos/nfse.xsd">
                <InfRps Id="RPS${idRps}">
                    <IdentificacaoRps>
                        <Numero>${numero}</Numero>
                        <Serie>1</Serie>
                        <Tipo>1</Tipo>
                    </IdentificacaoRps>
                    <DataEmissao>${dataEmissao}</DataEmissao>
                    <NaturezaOperacao>${naturezaOperacao}</NaturezaOperacao>
                    <OptanteSimplesNacional>2</OptanteSimplesNacional>
                    <IncentivadorCultural>2</IncentivadorCultural>
                    <Status>1</Status>
                    <Servico>
                        <Valores>
                            <ValorServicos>${valorServicos}</ValorServicos>
                            <ValorDeducoes>0.00</ValorDeducoes>
                            <ValorPis>0.00</ValorPis>
                            <ValorCofins>0.00</ValorCofins>
                            <ValorInss>0.00</ValorInss>
                            <ValorIr>0.00</ValorIr>
                            <ValorCsll>0.00</ValorCsll>
                            <IssRetido>1</IssRetido>
                            <ValorIss>${iss}</ValorIss>
                            <BaseCalculo>${valorServicos}</BaseCalculo>
                            <Aliquota>0.05</Aliquota>
                            <ValorLiquidoNfse>${valorServicos.subtract(iss).subtract(outrasRetencoes)}</ValorLiquidoNfse>
                        </Valores>
                        <ItemListaServico>${itemListaServico}</ItemListaServico>
                        <CodigoCnae>8011-4/00</CodigoCnae>
                        <CodigoTributacaoMunicipio>1402</CodigoTributacaoMunicipio>
                        <Discriminacao>Prestação de serviços de mão de obra exclusiva - Contrato de vigilância e limpeza</Discriminacao>
                        <CodigoMunicipio>${codMun}</CodigoMunicipio>
                    </Servico>
                    <Prestador>
                        <Cnpj>${cnpjPrestador}</Cnpj>
                        <InscricaoMunicipal>00000001</InscricaoMunicipal>
                    </Prestador>
                    <Tomador>
                        <IdentificacaoTomador>
                            <CpfCnpj>
                                <Cnpj>${tomadorCnpj}</Cnpj>
                            </CpfCnpj>
                        </IdentificacaoTomador>
                        <RazaoSocial>${tomadorRazaoSocial}</RazaoSocial>
                    </Tomador>
                    $blocoCbsIbs
                </InfRps>
            </Rps>
        """.trimIndent()
    }

    /**
     * Reforma tributária — CBS/IBS obrigatórios em ambiente de testes a partir de 01/08/2026 (SPEC).
     */
    fun incluirCbsIbsReforma(dataEmissao: LocalDate): Boolean =
        !dataEmissao.isBefore(LocalDate.of(2026, 8, 1))

    fun transmitir(xml: String, numero: String): FiscalTransmitResult =
        nfseGatewayRouter.emit(xml, numero)

    fun transmitirCancelamento(xml: String, numero: String): FiscalTransmitResult =
        nfseGatewayRouter.cancel(xml, numero)

    /**
     * Gera XML/Comando de cancelamento de NFS-e (stub realista).
     */
    fun gerarCancelamentoNfs(
        numeroNfse: String,
        codigoVerificacao: String,
        motivo: String,
        prestadorCnpj: String = "00000000000000"
    ): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <CancelarNfseEnvio xmlns="http://www.abrasf.org.br/ABRASF/arquivos/nfse.xsd">
                <Pedido>
                    <InfPedidoCancelamento Id="CANCEL${numeroNfse}">
                        <IdentificacaoNfse>
                            <Numero>${numeroNfse}</Numero>
                            <Cnpj>${prestadorCnpj}</Cnpj>
                            <InscricaoMunicipal>00000001</InscricaoMunicipal>
                            <CodigoVerificacao>${codigoVerificacao}</CodigoVerificacao>
                        </IdentificacaoNfse>
                        <CodigoCancelamento>1</CodigoCancelamento>
                        <MotivoCancelamento>${motivo}</MotivoCancelamento>
                    </InfPedidoCancelamento>
                </Pedido>
            </CancelarNfseEnvio>
        """.trimIndent()
    }
}