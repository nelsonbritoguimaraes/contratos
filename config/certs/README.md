# Certificados ICP-Brasil (A1)

Coloque os arquivos **fora do Git** nesta pasta:

| Arquivo | Uso |
|---------|-----|
| `esocial.pfx` | Transmissão eSocial (`contractops.fiscal.esocial.certificate-path`) |
| `nfse.pfx` | Emissão NFS-e Nacional (`contractops.fiscal.nfse.certificate-path`) |

Pode ser o **mesmo** certificado da empresa nos dois arquivos (cópia) se o CNPJ for o mesmo.

## Gerar / exportar

1. Exporte o certificado A1 do e-CNPJ ou token como `.pfx` (PKCS#12).
2. Copie para esta pasta com os nomes acima.
3. Defina a senha em `config/fiscal.env` (não commitar).

## Caminhos no Windows (dev local)

```
FISCAL_ESOCIAL_CERT_PATH=./config/certs/esocial.pfx
FISCAL_NFSE_CERT_PATH=./config/certs/nfse.pfx
```

Com `SPRING_PROFILES_ACTIVE=local` o backend resolve caminhos relativos à **raiz do repositório** (`Contratos/`).
