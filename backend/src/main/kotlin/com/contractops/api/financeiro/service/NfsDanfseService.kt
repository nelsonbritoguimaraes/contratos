package com.contractops.api.financeiro.service

import com.contractops.api.financeiro.domain.NotaFiscalServico
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class NfsDanfseService {

    fun gerarDanfseHtml(nf: NotaFiscalServico): String {
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"/><title>DANFSE ${nf.numero}</title>
            <style>body{font-family:Arial,sans-serif;margin:24px}h1{color:#1a237e}
            table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:8px}</style>
            </head>
            <body>
            <h1>Documento Auxiliar da NFS-e (DANFSE)</h1>
            <p><strong>Número:</strong> ${nf.numero} &nbsp; <strong>Série:</strong> ${nf.serie}</p>
            <p><strong>Emissão:</strong> ${nf.dataEmissao.format(fmt)} &nbsp;
            <strong>Protocolo:</strong> ${nf.protocolo ?: "—"}</p>
            <p><strong>Tomador:</strong> ${nf.tomadorRazaoSocial ?: "Órgão contratante"} — CNPJ ${nf.tomadorCnpj}</p>
            <table>
            <tr><th>Valor serviços</th><th>ISS retido</th><th>Outras retenções</th><th>Líquido</th></tr>
            <tr>
            <td>R$ ${nf.valorServicos}</td>
            <td>R$ ${nf.issRetido}</td>
            <td>R$ ${nf.outrasRetencoes}</td>
            <td>R$ ${nf.valorLiquido}</td>
            </tr>
            </table>
            <p><em>Gerado pelo ContractOps — SPEC §16 (workflow NFS-e)</em></p>
            </body></html>
        """.trimIndent()
    }
}
