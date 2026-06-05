package com.contractops.api.fiscal.esocial

import com.contractops.api.employee.domain.Employee
import com.contractops.api.fiscal.config.FiscalProperties
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * XML eSocial S-1.3 (estrutura alinhada ao leiaute oficial — validação XSD em produção).
 */
@Component
class EsocialXmlBuilder(
    private val fiscalProperties: FiscalProperties
) {
    private val esocial = fiscalProperties.esocial
    private val periodFmt = DateTimeFormatter.ofPattern("yyyy-MM")

    fun buildS2200(employee: Employee): String {
        val cpf = digitsOnly(employee.cpf)
        val cnpj = employerCnpjRoot()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtAdmissao/v_S_01_03_00">
              <evtAdmissao Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <trabalhador>
                  <cpfTrab>$cpf</cpfTrab>
                  <nmTrab>${escapeXml(employee.fullName)}</nmTrab>
                  <dtNascto>${employee.birthDate ?: LocalDate.of(1990, 1, 1)}</dtNascto>
                </trabalhador>
                <vinculo>
                  <matricula>${escapeXml(employee.matricula ?: cpf.take(11))}</matricula>
                  <dtAdm>${employee.admissionDate ?: LocalDate.now()}</dtAdm>
                  <tpRegTrab>1</tpRegTrab>
                  <tpRegPrev>1</tpRegPrev>
                  <cargo>
                    <nmCargo>${escapeXml(employee.cargo ?: "Empregado")}</nmCargo>
                    <CBOCargo>${employee.cbo ?: "517110"}</CBOCargo>
                  </cargo>
                  <remuneracao>
                    <vrSalFx>${employee.salaryBase ?: "0.00"}</vrSalFx>
                  </remuneracao>
                </vinculo>
              </evtAdmissao>
            </eSocial>
        """.trimIndent()
    }

    fun buildS2205(employee: Employee): String {
        val cpf = digitsOnly(employee.cpf)
        val cnpj = employerCnpjRoot()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtAltCadastral/v_S_01_03_00">
              <evtAltCadastral Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <ideTrabalhador><cpfTrab>$cpf</cpfTrab></ideTrabalhador>
                <alteracao>
                  <dtAlteracao>${LocalDate.now()}</dtAlteracao>
                  <dadosTrabalhador>
                    <nmTrab>${escapeXml(employee.fullName)}</nmTrab>
                    <sexo>${mapGender(employee.gender)}</sexo>
                    <dtNascto>${employee.birthDate ?: LocalDate.of(1990, 1, 1)}</dtNascto>
                  </dadosTrabalhador>
                </alteracao>
              </evtAltCadastral>
            </eSocial>
        """.trimIndent()
    }

    fun buildS2206(employee: Employee, novoSalario: String, novaFuncao: String?, dataAlteracao: LocalDate = LocalDate.now()): String {
        val cpf = digitsOnly(employee.cpf)
        val cnpj = employerCnpjRoot()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtAltContratual/v_S_01_03_00">
              <evtAltContratual Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <ideVinculo>
                  <cpfTrab>$cpf</cpfTrab>
                  <matricula>${escapeXml(employee.matricula ?: cpf.take(11))}</matricula>
                </ideVinculo>
                <altContratual>
                  <dtAlteracao>$dataAlteracao</dtAlteracao>
                  <cargo>
                    <nmCargo>${escapeXml(novaFuncao ?: employee.cargo ?: "Empregado")}</nmCargo>
                    <CBOCargo>${employee.cbo ?: "517110"}</CBOCargo>
                  </cargo>
                  <remuneracao><vrSalFx>$novoSalario</vrSalFx></remuneracao>
                </altContratual>
              </evtAltContratual>
            </eSocial>
        """.trimIndent()
    }

    fun buildS2230(employee: Employee, tipoAfastamento: String, dataInicio: LocalDate, dataFim: LocalDate?): String {
        val cpf = digitsOnly(employee.cpf)
        val cnpj = employerCnpjRoot()
        val fimTag = dataFim?.let { "<dtTermAfast>$it</dtTermAfast>" } ?: ""
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtAfastTemp/v_S_01_03_00">
              <evtAfastTemp Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <ideVinculo>
                  <cpfTrab>$cpf</cpfTrab>
                  <matricula>${escapeXml(employee.matricula ?: cpf.take(11))}</matricula>
                </ideVinculo>
                <infoAfastamento>
                  <dtIniAfast>$dataInicio</dtIniAfast>
                  $fimTag
                  <codMotAfast>$tipoAfastamento</codMotAfast>
                </infoAfastamento>
              </evtAfastTemp>
            </eSocial>
        """.trimIndent()
    }

    fun buildS2240(employee: Employee, tipoCondicao: String, grau: String, dataInicio: LocalDate): String {
        val cpf = digitsOnly(employee.cpf)
        val cnpj = employerCnpjRoot()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtExpRisco/v_S_01_03_00">
              <evtExpRisco Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <ideVinculo>
                  <cpfTrab>$cpf</cpfTrab>
                  <matricula>${escapeXml(employee.matricula ?: cpf.take(11))}</matricula>
                </ideVinculo>
                <infoExpRisco>
                  <dtIniCondicao>$dataInicio</dtIniCondicao>
                  <infoAmb>
                    <localAmb>1</localAmb>
                    <dscAmb>${escapeXml(tipoCondicao)}</dscAmb>
                  </infoAmb>
                  <agNoc>
                    <codAgNoc>${mapGrauCodigo(grau)}</codAgNoc>
                    <dscAgNoc>${escapeXml("$tipoCondicao - $grau")}</dscAgNoc>
                  </agNoc>
                </infoExpRisco>
              </evtExpRisco>
            </eSocial>
        """.trimIndent()
    }

    fun buildS2299(employee: Employee, dataDesligamento: LocalDate, motivo: String): String {
        val cpf = digitsOnly(employee.cpf)
        val cnpj = employerCnpjRoot()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtDeslig/v_S_01_03_00">
              <evtDeslig Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <ideVinculo>
                  <cpfTrab>$cpf</cpfTrab>
                  <matricula>${escapeXml(employee.matricula ?: cpf.take(11))}</matricula>
                </ideVinculo>
                <infoDeslig>
                  <dtDeslig>$dataDesligamento</dtDeslig>
                  <mtvDeslig>$motivo</mtvDeslig>
                  <pensAlim>0</pensAlim>
                  <percAliment>0</percAliment>
                </infoDeslig>
              </evtDeslig>
            </eSocial>
        """.trimIndent()
    }

    fun buildS1200(cpf: String, competencia: LocalDate, rubricas: List<Pair<String, String>>): String {
        val cnpj = employerCnpjRoot()
        val itens = rubricas.joinToString("\n") { (cod, valor) ->
            """<itensRemun><codRubr>$cod</codRubr><vrRubr>$valor</vrRubr></itensRemun>"""
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtRemun/v_S_01_03_00">
              <evtRemun Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <ideTrabalhador><cpfTrab>${digitsOnly(cpf)}</cpfTrab></ideTrabalhador>
                <dmDev>
                  <ideDmDev>1</ideDmDev>
                  <codCateg>101</codCateg>
                  <infoPerApur>
                    <idePeriodo><perApur>${competencia.format(periodFmt)}</perApur></idePeriodo>
                    $itens
                  </infoPerApur>
                </dmDev>
              </evtRemun>
            </eSocial>
        """.trimIndent()
    }

    fun buildS1210(cpf: String, competencia: LocalDate, valorLiquido: String, dataPagamento: LocalDate): String {
        val cnpj = employerCnpjRoot()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtPgtos/v_S_01_03_00">
              <evtPgtos Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <ideBenef>${digitsOnly(cpf)}</ideBenef>
                <idePgto>
                  <perApur>${competencia.format(periodFmt)}</perApur>
                  <dtPgto>$dataPagamento</dtPgto>
                  <vrLiq>$valorLiquido</vrLiq>
                </idePgto>
              </evtPgtos>
            </eSocial>
        """.trimIndent()
    }

    fun buildS1299(competencia: LocalDate, indApuracao: String = "1"): String {
        val cnpj = employerCnpjRoot()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtFechaEvPer/v_S_01_03_00">
              <evtFechaEvPer Id="ID${System.currentTimeMillis()}">
                ${ideEventoBlock()}
                <ideEmpregador>
                  <tpInsc>1</tpInsc>
                  <nrInsc>$cnpj</nrInsc>
                </ideEmpregador>
                <infoFech>
                  <perApur>${competencia.format(periodFmt)}</perApur>
                  <indApuracao>$indApuracao</indApuracao>
                </infoFech>
              </evtFechaEvPer>
            </eSocial>
        """.trimIndent()
    }

    private fun ideEventoBlock() = """
                <ideEvento>
                  <tpAmb>${esocial.tpAmb}</tpAmb>
                  <procEmi>1</procEmi>
                  <verProc>ContractOps-1.0</verProc>
                </ideEvento>
    """.trimIndent()

    private fun employerCnpjRoot() = digitsOnly(esocial.employerCnpj).padStart(14, '0').take(8)

    private fun mapGender(gender: String?) = when (gender?.uppercase()) {
        "M", "MASCULINO" -> "M"
        "F", "FEMININO" -> "F"
        else -> "M"
    }

    private fun mapGrauCodigo(grau: String) = when (grau.uppercase()) {
        "MINIMO", "MÍNIMO" -> "01"
        "MEDIO", "MÉDIO" -> "02"
        "MAXIMO", "MÁXIMO" -> "03"
        else -> "02"
    }

    private fun digitsOnly(s: String) = s.replace(Regex("[^0-9]"), "")
    private fun escapeXml(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

