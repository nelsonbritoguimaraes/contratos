SPEC v1.0 — SaaS Gestor de Contratos, Operações, Folha e Contabilidade para Empresas de Mão de Obra Exclusiva
1. Visão do produto

O sistema será um ERP SaaS especializado em empresas que vencem licitações e executam contratos de prestação de serviços com dedicação exclusiva de mão de obra.

Ele deve controlar o ciclo completo:

licitação → proposta/planilha vencedora → contrato → implantação → alocação de postos → colaboradores → ponto → cobertura diária → glosas → faturamento → nota fiscal → recebimento → folha → encargos → contabilidade → auditoria → renovação/repactuação.

O sistema deve atender empresas individuais, grupos empresariais, holdings, matriz, filiais, unidades operacionais e múltiplos CNPJs. Para o contexto de contratos públicos, o sistema deve suportar fiscalização técnica e administrativa, anexos mensais, notificações do órgão, resposta a glosas, controle de IMR e rastreabilidade documental.

A base regulatória de ponto deve considerar a Portaria nº 671/2021, que classifica os registradores em REP-C, REP-A e REP-P, conforme orientação do Ministério do Trabalho e Emprego. O cálculo de IMR deve seguir a lógica de critérios mensuráveis, objetivos e comprováveis, pois o Governo Federal define o IMR como mecanismo para estabelecer níveis esperados de qualidade e adequações de pagamento.

2. Stack técnica recomendada
2.1 Backend principal

Para um sistema desse porte, eu recomendo:

Backend principal: Kotlin + Spring Boot + Java 21 LTS

Motivo: folha, contabilidade, contratos, cálculo de glosas, auditoria, integrações fiscais e processamento financeiro exigem estabilidade, tipagem forte, testes robustos e arquitetura transacional. Kotlin/Spring é excelente para ERP SaaS crítico.

Componentes principais
Camada	Tecnologia recomendada
Backend core	Kotlin + Spring Boot
Banco principal	PostgreSQL
Multi-tenant	PostgreSQL com tenant_id, Row Level Security e opção enterprise de banco isolado por cliente grande
Mensageria	Apache Kafka ou Redpanda
Workflows longos	Temporal.io
Cache	Redis
Busca	OpenSearch
BI operacional	ClickHouse ou PostgreSQL materialized views no início
Arquivos/anexos	S3-compatible storage, como AWS S3, MinIO ou Cloudflare R2
OCR/documentos	Tesseract, Google Document AI, AWS Textract ou Azure Document Intelligence
Autenticação	Keycloak ou Auth0/WorkOS
Assinatura digital	Integração ICP-Brasil, Clicksign, ZapSign ou solução própria com certificado A1/A3
Observabilidade	OpenTelemetry + Grafana + Prometheus + Loki
Containers	Docker + Kubernetes
Infra	Terraform
API externa	REST + GraphQL para telas complexas + webhooks
Integrações fiscais	workers assíncronos com fila, retry e trilha de auditoria
2.2 Serviços auxiliares

O backend deve ser organizado em módulos de domínio, não em microserviços prematuros. A recomendação inicial é um modular monolith bem separado, evoluindo para microserviços quando houver volume.

Módulos que podem virar serviços separados depois:

Motor de cálculo de folha.
Motor contábil.
Motor de glosas/IMR.
Integração com relógios de ponto.
Agentes de IA.
Processamento de documentos/OCR.
Faturamento/NFS-e.
Sincronização eSocial/SPED/FGTS Digital.
3. Frontend e design system
3.1 Stack frontend

Frontend: React + TypeScript + Material Design 3 + AG Grid Enterprise

Você indicou o uso de Material Design 3. O Material 3 é o design system open-source do Google para diretrizes, estilos e componentes de interface.

Para as telas de dados pesados, usar:

AG Grid Enterprise com tema Quartz + Row Grouping + Pivot

A AG Grid documenta temas embutidos como ponto de partida para estilização. O Row Grouping é ativado por coluna com rowGroup, permitindo agrupamentos hierárquicos. O Pivot permite criar tabelas dinâmicas com agregações, útil para contratos, glosas, folha, custos e faturamento.

3.2 Padrão visual
Design system

Base:

Material Design 3

Tokens de cor.
Tipografia.
Elevação.
Estados de foco/hover/disabled.
Componentes acessíveis.
Layout responsivo.
Dark mode e light mode.
Densidade de tela configurável.
Data grid

Base:

AG Grid Quartz

Row Grouping.
Pivot.
Server-side row model.
Colunas fixas.
Filtros avançados.
Exportação CSV/Excel.
Layouts salvos por usuário.
Dashboards com agrupamento por contrato, órgão, filial, lote, posto, competência e colaborador.
3.3 Telas prioritárias com Pivot/Grouping
Mapa geral de contratos.
Mapa de postos por órgão/lote/unidade.
Cobertura diária de postos.
Apuração de ponto.
Glosas por contrato.
IMR por indicador.
Folha por filial/contrato/centro de custo.
DRE por contrato.
Contabilidade por CNPJ/filial.
Estoque de uniformes.
Equipamentos alocados.
Notificações por contrato.
Faturamento e notas fiscais.
4. Estrutura multiempresa, grupo e filiais

O SaaS deve suportar a seguinte hierarquia:

Tenant SaaS
 └── Grupo empresarial
      ├── Empresa / CNPJ matriz
      │    ├── Filial / estabelecimento / CNPJ filial
      │    ├── Centro de custo
      │    ├── Conta bancária
      │    ├── Certificado digital
      │    └── Plano contábil
      ├── Empresa / CNPJ 2
      └── Empresa / CNPJ 3
4.1 Entidades principais
Entidade	Descrição
Grupo empresarial	Holding ou conjunto de empresas relacionadas
Empresa	Pessoa jurídica com CNPJ próprio
Filial	Estabelecimento vinculado à empresa
Centro de custo	Unidade gerencial para contrato, filial, órgão ou operação
Unidade operacional	Local físico de execução do contrato
Certificado digital	A1/A3 por CNPJ para eSocial, NFS-e, SPED e integrações
Usuário	Pessoa que acessa o sistema
Perfil	Conjunto de permissões
Papel operacional	Fiscal interno, DP, financeiro, contador, supervisor, gestor de contrato
4.2 Permissões

O sistema deve ter RBAC + ABAC.

Exemplo:

Usuário pode ver somente contratos da filial X.
Supervisor pode ver colaboradores do contrato Y.
DP pode ver folha, mas não contabilidade.
Contador pode ver lançamentos, mas não editar ponto.
Gestor do grupo pode ver todos os CNPJs.
Cliente externo/órgão pode acessar portal restrito de documentos, se habilitado.
5. Módulo de licitações
5.1 Objetivo

Controlar licitações vencidas, propostas, lotes, planilhas, órgãos contratantes, documentos, prazos e histórico até a formalização do contrato.

O sistema deve poder integrar com dados públicos de contratações. O PNCP disponibiliza informações de compras e contratos em dados abertos, o que permite futura automação de consulta e conciliação de licitações/contratos.

5.2 Funcionalidades
Cadastro de licitação

Campos:

Número do processo.
Número do edital.
Modalidade.
Portal de origem.
Órgão.
Unidade compradora.
CNPJ do órgão.
Objeto.
Data de publicação.
Data da sessão.
Data da homologação.
Data da adjudicação.
Lotes.
Itens.
Valor estimado.
Valor vencedor.
Status.
Documentos.
Links externos.
Equipe responsável.
Garantia de proposta.
Riscos identificados.
Cadastro de lote

Campos:

Número do lote.
Descrição.
Órgão/unidade.
Itens do lote.
Quantitativo de postos.
Valor mensal.
Valor anual.
Valor global.
Prazo.
Observações de execução.
Planilha vencedora

Funcionalidades:

Importar Excel.
Versionar planilha.
Travar versão vencedora.
Comparar planilha original x reajustada x repactuada.
Armazenar memória de cálculo.
Vincular CCT/ACT.
Vincular composição de custos.
Validar erros aritméticos.
Separar custo de mão de obra, insumos, uniformes, equipamentos, tributos, administração e lucro.
6. Módulo de contratos
6.1 Cadastro do contrato

Campos:

Número do contrato.
Número do processo.
Licitação vinculada.
Órgão contratante.
CNPJ do órgão.
Gestor do órgão.
Fiscal técnico.
Fiscal administrativo.
Preposto da contratada.
Empresa contratada.
Filial responsável.
Objeto.
Vigência inicial.
Vigência final.
Valor mensal.
Valor anual.
Valor global.
Fonte de recurso.
Empenhos.
Lotes vinculados.
Garantia contratual.
Seguro garantia.
Percentual de garantia.
Prazo de pagamento.
Critérios de medição.
IMR aplicável.
Regras de glosa.
Regras de substituição.
Regras de uniforme.
Regras de equipamentos.
Regras de faturamento.
Conta vinculada, se aplicável.
Documentos obrigatórios mensais.
6.2 Aditivos e apostilamentos

O sistema deve controlar:

Prorrogação de vigência.
Acréscimo/supressão.
Repactuação.
Reajuste.
Reequilíbrio econômico-financeiro.
Alteração de postos.
Alteração de lotes.
Alteração de local.
Alteração de CCT.
Alteração de gestor/fiscal.
Alteração de garantia.
Histórico com versão.
6.3 Fiscalização contratual

O manual operacional de gestão e fiscalização contratual do Governo Federal recomenda segregação de funções em contratos com fiscalização trabalhista/previdenciária, especialmente em dedicação exclusiva de mão de obra, para reduzir riscos e aumentar transparência.

Por isso, o sistema deve separar:

Papel	Função
Fiscal técnico interno	Avalia execução, presença, qualidade e IMR
Fiscal administrativo interno	Avalia documentos trabalhistas, previdenciários e fiscais
Gestor de contrato	Valida medição, faturamento e resposta ao órgão
DP	Gera folha, encargos e documentos
Financeiro	Emite NF, acompanha recebimento e glosas
Contabilidade	Lança receitas, custos, provisões e tributos
Supervisor operacional	Garante cobertura dos postos
7. Módulo de postos, quantitativos e lotes
7.1 Cadastro de posto

Campos:

Contrato.
Lote.
Item da planilha.
Código do posto.
Nome do posto.
Função.
CBO.
Sindicato/CCT.
Local de execução.
Quantidade contratada.
Escala.
Jornada.
Horário.
Carga mensal.
Valor mensal por posto.
Valor diário.
Valor hora.
Valor plantão.
Adicionais.
Insumos vinculados.
Equipamentos vinculados.
Uniformes exigidos.
Supervisor.
Funcionário titular.
Funcionários reserva.
Volantes permitidos.
Obrigatoriedade de substituição.
Tolerância para reposição.
Regra de glosa por ausência.
Regra de IMR.
7.2 Mapa de cobertura

O sistema deve gerar automaticamente o mapa:

Contrato → Lote → Unidade → Posto → Escala → Funcionário titular → Substituto → Status do dia

Status possíveis:

Coberto.
Descoberto.
Coberto por volante.
Coberto parcialmente.
Falta sem reposição.
Atraso.
Saída antecipada.
Posto inativo.
Posto suspenso.
Posto aguardando implantação.
Posto em férias com substituição.
Afastamento com substituição.
Ausência justificada sem glosa.
Ausência com glosa.
8. Módulo de colaboradores e DP
8.1 Cadastro de colaborador

Campos:

Nome.
CPF.
RG.
PIS/NIS.
Data de nascimento.
Sexo.
Estado civil.
Endereço.
Contatos.
Dados bancários.
Dependentes.
Cargo.
CBO.
Sindicato.
CCT.
Empresa empregadora.
Filial.
Centro de custo.
Contrato alocado.
Posto titular.
Data de admissão.
Tipo de contrato.
Salário base.
Benefícios.
Jornada.
Escala.
ASO.
Treinamentos.
Uniformes recebidos.
Equipamentos recebidos.
Documentos anexos.
Status trabalhista.
8.2 Eventos de DP
Admissão.
Transferência de contrato.
Transferência de filial.
Promoção.
Alteração salarial.
Férias.
Afastamento.
Licença.
Advertência.
Suspensão.
Acidente de trabalho.
Rescisão.
Substituição temporária.
Mudança de escala.
Mudança de posto.
Exame periódico.
Entrega/devolução de uniformes.
Entrega/devolução de equipamentos.
9. Módulo de ponto e jornada
9.1 Requisitos legais e técnicos

O sistema deve tratar ponto de forma compatível com a Portaria 671/2021, que reconhece REP-C, REP-A e REP-P. Também deve gerar, quando aplicável:

AFD.
AEJ.
Espelho de ponto.
Relatório de marcações.
Registro de ajustes.
Histórico de justificativas.
Comprovante de marcação.
Trilha de auditoria.

A Portaria 671 também tornou desnecessário o cadastro do REP-C no antigo CAREP, mas fabricantes continuam com obrigações de registro de modelos e empregadores devem manter atestado técnico/termo de responsabilidade quando usarem sistemas de ponto eletrônico.

9.2 Formas de entrada de ponto

O sistema deve aceitar:

Integração direta com relógio físico.
Integração via cloud/API do fornecedor.
Importação de AFD.
Importação de AEJ.
Upload manual de arquivo.
Digitalização de folha de ponto.
Anexo de PDF/imagem.
Digitação manual com justificativa.
Ponto mobile REP-P.
Ponto web.
Ponto offline com sincronização.
Entrada por planilha.
Entrada por e-mail recebido.
Integração com sistemas de ponto de terceiros.
9.3 Fluxo de apuração
Coleta bruta
 → Normalização
 → Validação legal
 → Vinculação ao colaborador
 → Vinculação ao contrato/posto
 → Apuração diária
 → Identificação de faltas/atrasos
 → Identificação de cobertura por volante
 → Ajustes e justificativas
 → Aprovação do supervisor
 → Aprovação do DP
 → Envio para folha
 → Envio para medição/glosa
9.4 Ajustes manuais

Todo ajuste manual deve exigir:

Motivo.
Usuário responsável.
Data/hora.
Evidência.
Aprovação.
Log imutável.
Antes/depois.
Impacto na folha.
Impacto na glosa.
Impacto na medição.
10. Integração com relógios de ponto no Brasil
10.1 Estratégia geral

O sistema deve ter um framework de conectores de ponto. A ideia não é amarrar o SaaS a um único fornecedor, mas permitir integração por várias rotas:

Tipo	Quando usar
API local do equipamento	Quando o relógio está na rede do cliente
SDK/DLL do fabricante	Quando o fabricante exige biblioteca própria
API cloud	Quando o cliente usa plataforma em nuvem do fornecedor
AFD/AEJ	Quando a integração direta não está disponível
Agente local	Quando o relógio está atrás de firewall/NAT
Upload manual	Plano B para implantação rápida
E-mail parser	Quando relatórios são enviados automaticamente
SFTP	Para clientes enterprise
Webhook	Para plataformas modernas
10.2 Agente local de comunicação

Criar um componente chamado:

Clock Bridge Agent

Ele será instalado na rede do cliente quando os relógios não forem acessíveis pela internet.

Funções:

Descobrir relógios na rede.
Autenticar nos equipamentos.
Coletar marcações.
Enviar colaboradores.
Sincronizar biometria quando permitido.
Baixar AFD.
Enviar logs para o SaaS.
Trabalhar offline.
Sincronizar por HTTPS.
Usar fila local.
Suportar Windows e Linux.
Rodar como serviço.
Atualizar automaticamente.
10.3 Integrações prioritárias
1. Control iD

Modelos-alvo:

iDClass.
iDClass Bio.
iDClass Mult.
iDBlock, se usado em acesso.
RHiD, se cliente usar plataforma cloud.

Método:

API do próprio equipamento.
API local via rede.
Importação AFD.
Possível integração com RHiD.

A Control iD possui documentação oficial da API de comunicação do REP iDClass, incluindo exemplos e coleção Postman.

2. Topdata

Modelos-alvo:

Inner REP.
Inner REP Plus.
Inner REP Plus v5.
Inner Ponto 4.
Leitor Facial T4/F4, quando usado em conjunto.

Método:

SDK Inner REP.
Comunicação TCP/IP.
AFD.
Agente local.

A Topdata informa que oferece SDK Inner REP para integração customizada, permitindo comunicação direta com equipamentos, coleta de marcações, envio de cadastros de funcionários e outras operações.

3. Henry

Modelos-alvo:

Prisma Super Fácil Advanced.
Prisma SF.
Henry Orion, quando aplicável.
Relógios REP-C Henry certificados.

Método:

Comunicação TCP/IP.
Aplicativo web embarcado, quando disponível.
AFD/USB.
Integração via agente local.
Integração indireta via sistemas parceiros quando o cliente já usa Henry Ponto.

O Henry Prisma Super Fácil Advanced é descrito como REP homologado pelo MTE e certificado pelo Inmetro, com biometria, código de barras, proximidade RFID e Smart Card.

4. Dimep / Kairos

Modelos-alvo:

PrintPoint.
Smart Point.
Linha Dimep/Kairos.
Equipamentos integrados ao Kairos.

Método:

API REST Kairos quando disponível.
API REST de equipamentos em integrações existentes.
AFD.
Agente local.
Integração com plataforma Kairos.

Há documentação pública de integração REST com equipamentos Dimep em contexto de Secullum Ponto Web. A própria Dimep também informa recursos de integração API, exportação de folha e comunicação com relógios de marcas como MADIS, Control ID e Henry em suas ofertas.

5. Madis Rodbel

Modelos-alvo:

MD REP.
MD 0705.
MD 0706.
MD REP EVO.
MD REP EVO II.
MD Comune.

Método:

API.
AFD.
TCP/IP.
Agente local.
Plataforma MD Comune.

A Madis informa integração via API, integração com folha de pagamento, relatórios de compliance e sincronização em tempo real em sua solução MD Comune.

6. Secullum

Modelos-alvo:

Secullum Ponto Web.
Secullum Checkin.
Central do Funcionário.
Integração indireta com equipamentos suportados pelo Secullum.

Método:

API/integração com plataforma.
Importação de dados.
Integração indireta com equipamentos.
Exportação folha.
AFD.

A Secullum lista compatibilidade com diversos fabricantes e modelos, incluindo Madis Rodbel, Topdata, ZKTeco, Hikvision, Intelbras, Proveu, RW Tech, Secullum e Trix, com tipos de comunicação como USB, TCP/IP, online/offline e portas específicas.

7. ZKTeco

Modelos-alvo:

SpeedFace V3L.
SpeedFace V4L.
SpeedFace V5L.
Equipamentos compatíveis com ZKBioTime.
Outros modelos, desde que compatíveis com legislação aplicável no Brasil.

Método:

ZKBioTime API.
SDK ZKTeco.
Agente local.
AFD quando aplicável.
Integração por middleware.

A ZKTeco disponibiliza SDKs no seu centro de downloads. Também possui página para ZKBio Time API.

8. Outros conectores a prever

Incluir conectores ou importadores para:

Hikvision.
Intelbras.
Proveu.
RW Tech.
Trix.
Ahgora/TOTVS RH Ponto Eletrônico.
Tangerino.
Pontomais.
PontoTel.
Coalize.
Senior.
TOTVS RM.
Domínio.
Alterdata.
Fortes.
Contmatic.
Convenia.
Sólides.

Nem todos terão API pública ou integração homologada. Para esses, o sistema deve suportar importação de AFD, AEJ, CSV, XLSX, PDF, e-mail e SFTP.

11. Módulo de volantes e reposição
11.1 Objetivo

Controlar funcionários volantes/reservas para cobrir faltas, férias, afastamentos, atrasos e substituições obrigatórias.

11.2 Funcionalidades
Cadastro de volante.
Região de atuação.
Contratos habilitados.
Funções habilitadas.
Treinamentos exigidos.
Uniformes disponíveis.
Equipamentos disponíveis.
Disponibilidade por dia/horário.
Custo por acionamento.
Tempo de deslocamento.
SLA de chegada.
Histórico de acionamentos.
Ranking de eficiência.
Controle de cobertura.
11.3 Fluxo
Falta detectada no ponto
 → Sistema verifica regra de substituição
 → Busca volante compatível
 → Supervisor confirma acionamento
 → Volante registra chegada
 → Sistema vincula cobertura ao posto
 → Evita ou reduz glosa
 → Gera evidência para fiscalização
12. Módulo de implantação contratual
12.1 Objetivo

Gerir a fase entre assinatura do contrato e início efetivo da operação.

12.2 Checklist de implantação
Assinatura do contrato.
Cadastro do contrato.
Cadastro de lotes.
Importação da planilha vencedora.
Cadastro de postos.
Cadastro do órgão.
Cadastro dos fiscais.
Nomeação do preposto.
Recrutamento.
Admissão.
Exames admissionais.
Treinamentos.
Uniformes.
Equipamentos.
Crachás.
Acesso ao local.
Configuração de ponto.
Instalação de relógios.
Validação do posto.
Ordem de serviço.
Ata de início.
Plano de comunicação.
Plano de contingência.
Checklist assinado.
13. Módulo de equipamentos
13.1 Cadastro de equipamentos

Campos:

Código patrimonial.
Tipo.
Marca.
Modelo.
Número de série.
Data de compra.
Valor de aquisição.
Fornecedor.
Garantia.
Vida útil.
Contrato vinculado.
Posto vinculado.
Colaborador responsável.
Localização.
Status.
Manutenções.
Fotos.
Termo de entrega.
Termo de devolução.
13.2 Tipos de equipamentos
Relógio de ponto.
Rádio comunicador.
Celular.
Tablet.
Notebook.
Câmera.
EPI.
Ferramenta.
Máquina.
Armário.
Chaves.
Crachá.
Leitor biométrico.
Equipamento de segurança.
Veículo.
13.3 Controle
Alocação por contrato/posto/colaborador.
Manutenção preventiva.
Manutenção corretiva.
Substituição.
Devolução.
Extravio.
Depreciação.
Custo por contrato.
Evidência fotográfica.
Assinatura digital.
14. Módulo de uniformes
14.1 Estoque de uniformes

Campos:

Item.
Tipo.
Tamanho.
Gênero/modelagem.
Cor.
Quantidade em estoque.
Quantidade reservada.
Quantidade distribuída.
Custo unitário.
Fornecedor.
Contrato vinculado.
Validade/vida útil.
Lote de compra.
Local de estoque.
14.2 Distribuição por funcionário

O sistema deve registrar:

Funcionário.
Contrato.
Posto.
Itens entregues.
Quantidade.
Tamanho.
Data.
Responsável.
Assinatura.
Foto.
Termo de cautela.
Data prevista de troca.
Devolução.
Desconto, se permitido e configurado juridicamente.
Status.
14.3 Kits de uniforme

Exemplo:

Kit Vigilante:
- 2 calças
- 2 camisas
- 1 cinto
- 1 bota
- 1 crachá
- 1 jaqueta

O kit deve poder ser vinculado ao cargo, contrato, lote ou posto.

15. Módulo de notificações, e-mails e documentos
15.1 Notificações recebidas por contrato

Campos:

Contrato.
Órgão.
Número da notificação.
Data de recebimento.
Canal.
Fiscal emissor.
Assunto.
Prazo de resposta.
Gravidade.
Categoria.
Valor potencial de glosa.
Responsável interno.
Status.
Resposta.
Documentos.
Histórico.

Categorias:

Falta de colaborador.
Descumprimento de posto.
Atraso.
Não entrega de documento.
Problema de uniforme.
Problema de equipamento.
IMR.
Penalidade.
Advertência.
Glosa.
Solicitação de substituição.
Pedido de esclarecimento.
Medição.
Nota fiscal.
Repactuação.
15.2 Integração com e-mail

Conectores:

Microsoft Graph.
Gmail API.
IMAP.
SMTP.
Webhook de entrada.

Funcionalidades:

Capturar e-mails por contrato.
Associar por número do contrato, órgão, assunto ou remetente.
Extrair anexos.
Classificar com IA.
Criar tarefa automática.
Gerar prazo.
Sugerir resposta.
Guardar trilha.
Enviar resposta com protocolo.
Registrar e-mail enviado e recebido.
16. Módulo de medição, faturamento e notas fiscais
16.1 Medição mensal

Fluxo:

Competência
 → Contrato
 → Postos contratados
 → Cobertura real
 → Faltas/atrasos
 → IMR
 → Glosas sugeridas
 → Documentos trabalhistas
 → Validação do gestor
 → Pré-fatura
 → Nota fiscal
 → Envio ao órgão
 → Recebimento
 → Baixa financeira
 → Contabilização
16.2 Nota fiscal

O sistema deve controlar:

NFS-e emitida.
Número.
Série.
Município.
Prestador.
Tomador.
Contrato.
Competência.
Valor bruto.
Deduções.
Glosas.
Retenções.
ISS.
INSS retido.
IRRF.
PIS/COFINS/CSLL.
Valor líquido.
XML.
PDF.
Protocolo.
E-mail de envio.
Status de pagamento.

O Portal Nacional da NFS-e disponibiliza acesso para MEI e demais empresas, emissão web, consulta de nota e sistemas de prestador/município/cidadão.

17. Módulo de glosas
17.1 Conceito

Glosa é a diferença entre o valor faturado e o valor aceito/pago pelo contratante.

Glosa = Valor faturado - Valor aprovado/pago
% Glosa = Glosa / Valor faturado × 100
17.2 Tipos de glosa
Glosa por falta.
Glosa por atraso.
Glosa por saída antecipada.
Glosa por posto descoberto.
Glosa por cobertura parcial.
Glosa por não substituição.
Glosa por IMR.
Glosa por ausência de documento.
Glosa por uniforme.
Glosa por equipamento.
Glosa por qualidade.
Glosa por descumprimento de SLA.
Glosa por notificação não respondida.
Glosa administrativa.
Glosa financeira.
Glosa contestada.
Glosa mantida.
Glosa recuperada.
17.3 Fórmulas-base
Glosa simples
glosa = valor_faturado - valor_aprovado
Glosa por falta diária
valor_dia_posto = valor_mensal_posto / dias_base
glosa_falta = valor_dia_posto × dias_descobertos × fator_glosa
Glosa por hora descoberta
valor_hora_posto = valor_mensal_posto / horas_mensais_contratadas
glosa_hora = valor_hora_posto × horas_descobertas × fator_glosa
Glosa por atraso
glosa_atraso = valor_hora_posto × horas_atraso × fator_atraso
Glosa por IMR
glosa_imr = base_calculo_imr × percentual_deducao_imr
Glosa final
glosa_total = glosa_falta + glosa_atraso + glosa_imr + outras_glosas - glosa_recuperada
17.4 Regras importantes

O TCU esclarece que a existência de IMR em contrato de dedicação exclusiva não transforma necessariamente o contrato em contrato de resultado; o IMR implica variação de remuneração com base em desempenho previamente acordado.

Portanto, o sistema deve evitar erro comum: não pode aplicar dupla penalização sobre o mesmo fato sem regra contratual clara.

Exemplo:

Falta de colaborador gerou posto descoberto.
O mesmo fato impactou indicador IMR.
O sistema deve aplicar regra de prioridade:
glosa por falta;
IMR;
maior valor;
soma permitida;
limite máximo;
regra específica do contrato.
17.5 Motor de regras de glosa

Cada contrato deve ter uma tabela configurável:

Regra	Exemplo
Dias-base	22 dias úteis, 30 dias corridos ou escala
Base de cálculo	Valor do posto, valor do lote, valor mensal do contrato
Fator	1x, 2x, percentual fixo
Reposição	Elimina, reduz ou não reduz a glosa
Tolerância	10 min, 15 min, 30 min
Limite mensal	5%, 10%, 20%
Acúmulo com IMR	Sim/não
Precisa de notificação	Sim/não
Recurso permitido	Sim/não
Prazo de recurso	X dias
17.6 Recurso de glosa

Fluxo:

Glosa recebida
 → Classificação
 → Vinculação a evidências
 → Cálculo conferido
 → Agente de IA sugere defesa
 → Gestor revisa
 → Envio ao órgão
 → Resultado
 → Valor recuperado
 → Glosa mantida
 → Contabilização
18. Módulo de IMR
18.1 Cadastro de indicadores

Campos:

Contrato.
Nome do indicador.
Descrição.
Categoria.
Método de medição.
Periodicidade.
Fonte da evidência.
Meta.
Peso.
Faixas de desempenho.
Percentual de dedução.
Limite.
Responsável.
Evidências obrigatórias.
Regra de contestação.
18.2 Exemplos de indicadores
Indicador	Medição	Penalidade
Cobertura de postos	% postos cobertos	Dedução progressiva
Reposição em prazo	Tempo até substituição	Dedução por evento
Uso de uniforme	Auditoria/fotos	Dedução por não conformidade
Entrega de documentos	Até dia X	Dedução fixa
Qualidade do atendimento	Avaliação do fiscal	Dedução por faixa
Equipamento disponível	Checklist	Dedução por item
Resposta a notificações	Prazo	Dedução por atraso
18.3 Cálculo por faixa

Exemplo:

Resultado	Dedução
100%	0%
95% a 99,99%	1%
90% a 94,99%	3%
80% a 89,99%	5%
Abaixo de 80%	10%
resultado = entregas_conformes / entregas_exigidas × 100
deducao = buscar_faixa(resultado)
glosa_imr = valor_base × deducao
19. Módulo de folha de pagamento
19.1 Escopo

O sistema deve ser capaz de processar folha completa para empresas, filiais, contratos e centros de custo.

A documentação técnica oficial do eSocial mantém leiautes, XSDs e manuais técnicos atualizados, que devem orientar o módulo de folha e eventos trabalhistas.

19.2 Cadastros de folha
Empresa.
Filial.
Lotação tributária.
Estabelecimento.
Cargos.
Funções.
CBO.
Sindicatos.
CCT/ACT.
Rubricas.
Eventos fixos.
Eventos variáveis.
Tabelas de INSS.
Tabelas de IRRF.
FGTS.
Escalas.
Jornada.
Feriados.
Benefícios.
Dependentes.
Bancos.
Centros de custo.
Tomadores/contratos.
19.3 Eventos de folha
Proventos
Salário.
Hora extra.
Adicional noturno.
DSR.
Insalubridade.
Periculosidade.
Gratificação.
Prêmio.
Ajuda de custo.
Vale-transporte indenizado, se aplicável.
Diferença salarial.
Férias.
13º salário.
Aviso prévio.
Saldo de salário.
Descontos
INSS.
IRRF.
FGTS não é desconto do empregado, mas encargo patronal.
Vale-transporte.
Vale-alimentação/refeição.
Faltas.
Atrasos.
Adiantamento salarial.
Empréstimos/consignados.
Pensão alimentícia.
Convênio.
Desconto autorizado.
Multas permitidas somente se juridicamente configuradas.
Encargos
INSS patronal.
RAT.
Terceiros.
FGTS.
FGTS rescisório.
Provisão de férias.
Provisão de 13º.
Provisão de encargos.
Benefícios patronais.
19.4 Integração com ponto

A folha deve importar automaticamente:

Horas normais.
Horas extras.
Adicional noturno.
Atrasos.
Faltas.
DSR perdido.
Banco de horas.
Plantões.
Escalas especiais.
Substituições.
Transferências de posto.
Rateio por contrato.
19.5 Eventos eSocial

O módulo deve gerar e controlar, no mínimo:

S-1000 — Empregador.
S-1005 — Estabelecimentos.
S-1010 — Rubricas.
S-1020 — Lotações tributárias.
S-1200 — Remuneração.
S-1210 — Pagamentos.
S-1299 — Fechamento.
S-2200 — Admissão.
S-2205 — Alteração cadastral.
S-2206 — Alteração contratual.
S-2230 — Afastamento.
S-2240 — Condições ambientais.
S-2299 — Desligamento.
S-2399 — Término de trabalhador sem vínculo, quando aplicável.
S-3000 — Exclusão.
Totalizadores.
19.6 FGTS Digital

O FGTS Digital deve ser tratado como integração obrigatória no roadmap, porque usa informações prestadas via eSocial como base de dados, conforme perguntas frequentes oficiais.

Funcionalidades:

Conferir remuneração transmitida.
Conferir base de FGTS.
Gerar relatórios de conferência.
Apontar divergências entre folha, eSocial e FGTS Digital.
Guardar guias.
Controlar vencimentos.
Integrar comprovantes de pagamento.
19.7 DCTFWeb

A DCTFWeb é alimentada por eSocial, EFD-Reinf e MIT; o serviço oficial informa que a declaração deve ser elaborada com base nessas informações e que a partir de 2025 houve unificação envolvendo DCTFWeb/MIT.

O sistema deve:

Conferir débitos apurados.
Comparar folha x eSocial x DCTFWeb.
Controlar DARF.
Armazenar recibos.
Controlar retificações.
Alertar divergências.
Registrar pagamentos.
Baixar comprovantes.

A Receita Federal esclarece que a integração entre eSocial/EFD-Reinf e DCTFWeb ocorre automaticamente após envio e processamento com sucesso dos eventos de fechamento.

20. Módulo contábil
20.1 Escopo

O sistema deve ter contabilidade completa para:

Grupo empresarial.
Empresa.
Matriz.
Filial.
Centro de custo.
Contrato.
Lote.
Posto.
Competência.
20.2 Funcionalidades contábeis
Plano de contas.
Histórico padrão.
Lançamentos manuais.
Lançamentos automáticos.
Diário.
Razão.
Balancete.
Balanço patrimonial.
DRE.
DRE por contrato.
DRE por filial.
DRE por centro de custo.
Fluxo de caixa.
Conciliação bancária.
Provisões.
Depreciação.
Apropriação de custos.
Rateio.
Fechamento mensal.
Travamento de competência.
Reabertura controlada.
Auditoria.
20.3 Lançamentos automáticos

Gerar lançamentos a partir de:

Folha.
Encargos.
Benefícios.
Provisões.
Férias.
13º.
Rescisões.
Faturamento.
NFS-e.
Retenções.
Glosas.
Recuperação de glosas.
Estoque de uniformes.
Distribuição de uniformes.
Equipamentos.
Depreciação.
Reembolsos.
Pagamentos.
Recebimentos.
20.4 SPED

O sistema deve preparar ou integrar:

ECD.
ECF.
EFD-Contribuições.
EFD-Reinf.
DCTFWeb/MIT.
Relatórios auxiliares.

A Receita Federal mantém programas geradores, validadores e visualizadores do SPED, incluindo ECD, ECF e EFD-Contribuições.

20.5 Contabilidade por contrato

Cada contrato deve ter:

Receita bruta.
Glosas.
Receita líquida.
Custos diretos de folha.
Encargos.
Benefícios.
Uniformes.
Equipamentos.
Supervisão.
Deslocamento.
Tributos.
Margem bruta.
Margem líquida.
Resultado por posto.
Resultado por lote.
Resultado por órgão.
Resultado por filial.
21. Módulo fiscal e obrigações acessórias
21.1 Obrigações
NFS-e.
ISS.
Retenções federais.
INSS retido.
IRRF.
PIS/COFINS/CSLL.
EFD-Reinf.
DCTFWeb.
MIT.
SPED.
Relatórios fiscais.
Comprovantes.
21.2 EFD-Reinf

O sistema deve controlar eventos de retenção e cruzar:

Nota fiscal emitida.
Retenção informada.
Tomador.
CNPJ prestador.
Código de receita.
CPRB, quando aplicável.
DCTFWeb.
DARF.
22. Módulo financeiro
22.1 Contas a receber
Fatura.
Nota fiscal.
Contrato.
Órgão.
Competência.
Valor bruto.
Glosa.
Retenções.
Valor líquido.
Vencimento.
Status.
Recebimento.
Atraso.
Juros/multa, se aplicável.
Conciliação bancária.
22.2 Contas a pagar
Folha.
Encargos.
Benefícios.
Fornecedores.
Uniformes.
Equipamentos.
Serviços.
Tributos.
Parcelamentos.
Empréstimos.
22.3 Tesouraria
Bancos.
Contas.
Extratos.
Conciliação.
Fluxo de caixa.
Previsão de recebimento.
Previsão de folha.
Necessidade de capital de giro por contrato.
23. Agentes de IA
23.1 Provedores

O sistema deve ter uma camada de IA independente de fornecedor:

OpenAI.
Anthropic.
Gemini.
Ollama/local.
Futuramente: Mistral, Groq, AWS Bedrock, Azure OpenAI, Vertex AI.

A documentação da OpenAI indica uso da Responses API para texto, structured output, ferramentas e workflows multimodais, além de Agents SDK para orquestrar ferramentas, handoffs, aprovações, tracing e execução em containers. A Anthropic oferece Messages API, tool use, structured outputs, streaming e outros recursos para construir com Claude. O Gemini suporta function calling com structured output em modelos Gemini 3, permitindo chamadas e saídas aderentes a schema. O Ollama expõe API local por padrão em localhost:11434/api, útil para modelos locais e dados sensíveis.

23.2 Arquitetura da IA

Criar um módulo chamado:

AI Orchestrator

Funções:

Roteamento entre provedores.
Seleção de modelo por tarefa.
Controle de custo.
Controle de contexto.
RAG com documentos do contrato.
Guardrails.
Logs.
Aprovação humana.
Mascaramento de dados sensíveis.
Bloqueio de ações críticas sem confirmação.
Auditoria de prompts e respostas.
23.3 Agentes propostos
Agente	Função
Agente de Licitações	Lê edital, extrai lotes, obrigações, documentos, riscos e regras de medição
Agente de Contratos	Resume contrato, identifica cláusulas de glosa, vigência, reajuste, garantias
Agente de IMR	Interpreta indicadores, sugere fórmula e confere medição
Agente de Glosas	Calcula, explica e monta defesa de glosa
Agente de Ponto	Analisa faltas, atrasos, divergências e cobertura
Agente de DP/Folha	Confere eventos, rubricas e inconsistências
Agente Contábil	Sugere lançamentos e identifica divergências
Agente Fiscal	Confere retenções, NFS-e, EFD-Reinf e DCTFWeb
Agente de E-mails	Classifica e-mails, cria tarefas e sugere respostas
Agente de Documentos	Faz OCR, extrai informações e vincula anexos
Agente de Auditoria	Detecta riscos, documentos faltantes e inconsistências
Agente de Estoque	Prever necessidade de uniformes/equipamentos
Agente Executivo	Responde perguntas sobre resultado, margem e risco
23.4 Exemplos de uso

Perguntas que o usuário poderá fazer:

“Quais contratos terão vencimento nos próximos 90 dias?”
“Qual contrato teve maior glosa por falta no mês?”
“Esse colaborador estava escalado e marcou ponto?”
“Monte a defesa dessa glosa.”
“Quais documentos faltam para faturar o contrato X?”
“Qual a margem líquida do contrato Y?”
“Compare a planilha vencedora com a folha real.”
“Quais postos ficaram descobertos ontem?”
“Qual filial está com maior risco trabalhista?”
“Faça o resumo da notificação recebida do órgão.”
24. Segurança, LGPD e auditoria
24.1 LGPD

O sistema tratará dados pessoais, trabalhistas, biométricos, financeiros e documentos sensíveis. A LGPD é a Lei nº 13.709/2018 e dispõe sobre proteção de dados pessoais.

Medidas obrigatórias:

Criptografia em repouso.
Criptografia em trânsito.
Controle de acesso por perfil.
MFA.
Logs de auditoria.
Registro de acesso a dados sensíveis.
Minimização de dados.
Retenção configurável.
Anonimização quando aplicável.
Gestão de consentimento quando necessário.
Base legal por categoria de dado.
DPO/encarregado configurável.
Política de descarte.
Exportação de dados do titular.
Relatório de incidentes.
24.2 Auditoria

Toda ação relevante deve gerar log:

Quem fez.
Quando fez.
De onde fez.
Antes.
Depois.
Módulo.
Entidade.
Motivo.
Evidência.
IP/device.
Assinatura/hash.

Ajustes de ponto, glosas, folha, lançamentos contábeis, notas fiscais e alterações contratuais devem ser especialmente auditáveis.

25. Modelo de dados principal
25.1 Núcleo organizacional
Tenant
EnterpriseGroup
Company
Branch
CostCenter
User
Role
Permission
AuditLog
DigitalCertificate
BankAccount
25.2 Licitações e contratos
Bidding
BiddingLot
BiddingItem
WinningSpreadsheet
WinningSpreadsheetVersion
Contract
ContractAmendment
ContractLot
ContractDocument
ContractNotification
ContractManager
ContractRule
ContractGuarantee
ContractBudgetCommitment
25.3 Postos e operação
ServicePost
PostSchedule
PostAllocation
PostCoverage
Workplace
Shift
Roster
ReplacementPool
ReplacementAssignment
SupervisorVisit
OperationalChecklist
25.4 Colaboradores e ponto
Employee
EmploymentContract
EmployeeDocument
EmployeeAssignment
TimeClockDevice
RawPunch
NormalizedPunch
AttendanceDay
ManualPunchAdjustment
PunchAttachment
Absence
Delay
Leave
Vacation
Termination
25.5 Glosa e IMR
MeasurementPeriod
MeasurementLine
Glosa
GlosaType
GlosaRule
GlosaEvidence
GlosaAppeal
GlosaRecovery
IMRIndicator
IMRMeasurement
IMRRange
IMRDeduction
25.6 Uniformes e equipamentos
UniformItem
UniformStock
UniformKit
UniformDistribution
UniformReturn
Asset
AssetAllocation
AssetMaintenance
AssetReturn
25.7 Faturamento e financeiro
Invoice
InvoiceLine
Nfse
TaxRetention
AccountsReceivable
AccountsPayable
Payment
Receipt
BankTransaction
BankReconciliation
25.8 Folha
PayrollPeriod
PayrollEvent
PayrollRubric
PayrollCalculation
PayrollEmployeeResult
PayrollCompanyResult
PayrollProvision
PayrollBenefit
PayrollDeduction
ESocialEvent
FGTSDigitalRecord
DCTFWebRecord
25.9 Contabilidade
ChartOfAccounts
AccountingPeriod
JournalEntry
JournalEntryLine
Ledger
TrialBalance
FinancialStatement
CostAllocation
AccountingRule
TaxRule
26. APIs principais
26.1 API de contratos
GET /contracts
POST /contracts
GET /contracts/{id}
PUT /contracts/{id}
POST /contracts/{id}/amendments
POST /contracts/{id}/documents
POST /contracts/{id}/notifications
GET /contracts/{id}/dashboard
26.2 API de postos
GET /contracts/{id}/posts
POST /contracts/{id}/posts
PUT /posts/{id}
POST /posts/{id}/assign-employee
POST /posts/{id}/assign-replacement
GET /posts/{id}/coverage
26.3 API de ponto
POST /time-clocks
POST /time-clocks/{id}/sync
POST /punches/import-afd
POST /punches/manual
GET /attendance/periods/{periodId}
POST /attendance/{id}/approve
26.4 API de glosa
POST /measurements/{id}/calculate-glosas
GET /glosas
GET /glosas/{id}
POST /glosas/{id}/appeal
POST /glosas/{id}/recover
26.5 API de folha
POST /payroll/periods
POST /payroll/periods/{id}/calculate
POST /payroll/periods/{id}/close
POST /payroll/periods/{id}/esocial
GET /payroll/periods/{id}/results
26.6 API contábil
POST /accounting/entries
POST /accounting/periods/{id}/close
GET /accounting/trial-balance
GET /accounting/ledger
GET /accounting/dre-by-contract
27. Dashboards e KPIs
27.1 Dashboard executivo
Contratos ativos.
Contratos a vencer.
Valor mensal contratado.
Valor faturado.
Valor glosado.
Glosa recuperada.
Margem por contrato.
Postos ativos.
Postos descobertos.
Colaboradores ativos.
Turnover.
Absenteísmo.
Custo de folha.
Provisões.
Pendências documentais.
Notificações abertas.
Risco por contrato.
27.2 Dashboard operacional
Cobertura diária.
Faltas.
Atrasos.
Volantes acionados.
Postos críticos.
Supervisores.
Equipamentos pendentes.
Uniformes pendentes.
Ocorrências.
27.3 Dashboard financeiro
Faturamento previsto.
Faturamento realizado.
Recebimentos.
Atrasos.
Glosas.
Retenções.
Caixa.
Contratos deficitários.
27.4 Dashboard folha/DP
Folha por contrato.
Encargos.
Benefícios.
Férias.
Rescisões.
Afastamentos.
Pendências eSocial.
Divergências de ponto.
FGTS.
DCTFWeb.
27.5 Dashboard contábil
DRE por contrato.
DRE por filial.
Balancete.
Contas com divergência.
Provisões.
Custos não apropriados.
Receitas sem contrato.
Glosas sem contabilização.
28. Fluxos críticos
28.1 Do edital ao contrato
Cadastrar licitação
 → Importar edital
 → IA extrai obrigações
 → Cadastrar lotes
 → Importar planilha vencedora
 → Homologar proposta
 → Criar contrato
 → Criar postos
 → Criar checklist de implantação
28.2 Do ponto à glosa
Coletar ponto
 → Apurar presença
 → Comparar com escala/posto
 → Detectar ausência
 → Verificar reposição
 → Calcular impacto
 → Sugerir glosa
 → Anexar evidência
 → Medição mensal
28.3 Da medição à nota
Fechar ponto
 → Fechar documentos
 → Calcular glosas
 → Validar IMR
 → Gerar pré-fatura
 → Aprovar faturamento
 → Emitir NFS-e
 → Enviar ao órgão
 → Acompanhar pagamento
28.4 Da folha à contabilidade
Fechar ponto
 → Calcular folha
 → Gerar encargos
 → Transmitir eSocial
 → Conferir FGTS Digital
 → Conferir DCTFWeb
 → Gerar lançamentos contábeis
 → Fechar competência
29. Importações e exportações
29.1 Importações
Excel da planilha vencedora.
AFD.
AEJ.
CSV de ponto.
PDF de folha de ponto.
XML/PDF de NFS-e.
Extrato bancário OFX/CNAB.
Planilhas de uniformes.
Planilhas de colaboradores.
Documentos do edital.
E-mails.
Relatórios de relógio de ponto.
Arquivos de folha legados.
29.2 Exportações
Excel.
CSV.
PDF.
AFD/AEJ, quando aplicável.
Espelho de ponto.
Relatório de medição.
Demonstrativo de glosa.
Dossiê de faturamento.
Dossiê de contrato.
Relatórios de folha.
Relatórios contábeis.
SPED/arquivos auxiliares.
API/webhook.
30. Roadmap sugerido
Fase 1 — MVP operacional
Multiempresa.
Cadastro de licitações.
Cadastro de contratos.
Lotes.
Postos.
Planilha vencedora.
Colaboradores.
Ponto manual/importado.
Cobertura de postos.
Volantes.
Glosa básica.
IMR básico.
Faturamento manual.
Uniformes.
Equipamentos.
Documentos.
Dashboards AG Grid.
Fase 2 — Integrações de ponto e documentos
Clock Bridge Agent.
Control iD.
Topdata.
Henry.
Dimep/Kairos.
Madis.
Secullum.
ZKTeco.
Importador AFD/AEJ universal.
OCR de ponto.
Integração com e-mail.
Portal de documentos.
Fase 3 — Folha e DP
Motor de folha.
Rubricas.
Eventos.
Benefícios.
Férias.
Rescisão.
Provisões.
eSocial.
FGTS Digital.
DCTFWeb.
Relatórios de conferência.
Fase 4 — Contabilidade e fiscal
Plano de contas.
Lançamentos automáticos.
DRE por contrato.
Balancete.
SPED.
EFD-Reinf.
NFS-e.
Conciliação bancária.
Retenções.
Fase 5 — IA avançada
Agente de licitação.
Agente de contrato.
Agente de glosa.
Agente de ponto.
Agente de folha.
Agente contábil.
RAG por contrato.
Classificação automática de e-mails.
Defesa de glosa.
Auditoria inteligente.
Previsão de absenteísmo.
Previsão de margem.
31. Requisitos não funcionais
31.1 Performance
Suportar milhões de marcações de ponto.
Processar folha por empresa/filial/contrato.
Dashboards com server-side pagination.
Jobs assíncronos para cálculos pesados.
Reprocessamento por competência.
Cache de consultas frequentes.
31.2 Disponibilidade
SaaS 24/7.
Backup diário.
Backup point-in-time.
Alta disponibilidade para clientes enterprise.
Plano de desastre.
Monitoramento de jobs críticos.
31.3 Escalabilidade
Separar workers de cálculo.
Separar ingestion de ponto.
Separar IA.
Separar documentos.
Separar BI.
Permitir sharding por tenant grande.
31.4 Auditoria
Log imutável.
Hash de documentos.
Versionamento.
Trilha de alterações.
Exportação de auditoria.
Relatórios para fiscalização.
32. O que eu acrescentaria ao escopo original

Além do que você listou, eu incluiria obrigatoriamente:

Repactuação contratual por CCT/ACT.
Reajuste e reequilíbrio econômico-financeiro.
Conta vinculada, quando prevista.
Controle de garantia contratual.
Matriz de riscos por contrato.
DRE por contrato.
Custo real x planilha vencedora.
Controle de margem por posto.
Portal do fiscal/cliente, opcional.
Assinatura digital de cautelas e documentos.
OCR de notificações e folhas de ponto físicas.
Controle de CCT/ACT e sindicatos.
Alertas de vencimento de ASO, férias, contrato, garantia e documentos.
RPA/workflow de resposta a notificações.
Comparativo folha real x custo previsto na proposta.
Simulador de glosa antes do faturamento.
Simulador de repactuação.
Motor de regras configurável por contrato.
Rastreabilidade completa por competência.
Indicador de risco trabalhista/previdenciário por contrato.
33. Definição final do produto

O produto deve ser posicionado como:

ERP SaaS de Gestão de Contratos Públicos com Mão de Obra Exclusiva, Ponto, Glosas, Folha, Contabilidade e IA.

Nome conceitual:

ContractOps AI

ou

Mão de Obra 360

ou

Gestor Público Contratos Pro

Resumo da proposta de valor:

“Controle contratos públicos de mão de obra exclusiva do edital à contabilidade: postos, colaboradores, ponto, volantes, glosas, IMR, faturamento, folha, encargos, documentos, uniformes, equipamentos, auditoria e inteligência artificial em uma única plataforma SaaS.”

Abaixo está uma especificação completa inicial para o SaaS. Eu estruturaria o produto como uma plataforma de gestão operacional, trabalhista, fiscal, contábil e financeira de contratos públicos de dedicação exclusiva de mão de obra, com automação por agentes de IA e integrações oficiais sempre que possível.

1. Visão geral do produto

Nome provisório: ContractOps AI — Gestão inteligente de contratos, folha, ponto, glosas, faturamento e contabilidade.

O sistema será um SaaS multiempresa e multifilial para empresas que vencem licitações e executam contratos de prestação de serviços com dedicação exclusiva de mão de obra. Ele controlará desde a licitação vencida até a execução diária do contrato, medição, ponto, substituições, glosas, folha de pagamento, emissão automática de NFS-e, envio por e-mail ao contratante, conciliação bancária, contabilidade e obrigações acessórias.

A arquitetura deve considerar que contratos públicos de dedicação exclusiva exigem forte controle de fiscalização técnica, administrativa, previdenciária, fiscal e trabalhista. A própria IN 05/2017 trata a fiscalização administrativa como o acompanhamento dos aspectos administrativos da execução, especialmente obrigações previdenciárias, fiscais e trabalhistas em contratos com dedicação exclusiva de mão de obra.

A proposta é que o sistema funcione em modo autopiloto, mas com trilha de auditoria, validação por regras, aprovações configuráveis e registro de responsabilidade técnica. Ou seja: o sistema calcula, gera, classifica, concilia, emite e envia automaticamente quando a empresa permitir; para atos críticos, como fechamento de folha, transmissão fiscal, contabilidade oficial e emissão de documentos fiscais, o sistema deve permitir aprovação humana obrigatória ou aprovação automática por política.

2. Decisões técnicas principais
Backend recomendado

Eu usaria uma arquitetura baseada em:

Kotlin + Spring Boot + Java 21/25

Excelente para aplicações empresariais, fiscais, contábeis, trabalhistas e integrações SOAP/XML/REST.
Ecossistema maduro para segurança, transações, mensageria, auditoria, certificados digitais, XML, NF, eSocial, integrações bancárias e APIs governamentais.
Spring Boot é indicado para aplicações Java de produção, com baixa configuração inicial e bom ecossistema corporativo.

Componentes centrais

PostgreSQL como banco principal.
Redis para cache, filas leves e controle de sessão.
Kafka ou Redpanda para eventos.
Temporal.io para workflows longos: folha, NFS-e, cobrança, conciliação, notificações, fechamento mensal.
OpenSearch para busca textual e auditoria pesquisável.
ClickHouse para analytics, pivôs, BI e grandes consultas operacionais.
MinIO/S3 para arquivos, XMLs, PDFs, anexos, e-mails, comprovantes, espelhos de ponto, holerites e documentos.
Keycloak ou Ory para autenticação, SSO, MFA, RBAC e ABAC.
Vault para segredos, certificados digitais e chaves de API.
Kubernetes para deploy SaaS escalável.
Workers especializados em Python para OCR, extração documental e IA, quando necessário.
Frontend recomendado

React + TypeScript + Material Design 3 + AG Grid Enterprise

O usuário definiu o uso do Material Design 3. O Material 3 é o design system open source do Google, com diretrizes, estilos e componentes para interfaces modernas.

Para tabelas, relatórios operacionais e visão gerencial:

AG Grid Enterprise

Tema Quartz como base.
Row Grouping para agrupar por contrato, órgão, lote, posto, competência, colaborador, filial, centro de custo.
Pivot para análises de glosa, folha, custo, faturamento, ponto, margem e inadimplência.
Server-Side Row Model para bases grandes.
Exportação Excel.
Master/detail para abrir detalhes de contrato, nota, funcionário ou medição dentro da própria grade.

O AG Grid documenta o tema Quartz como tema padrão em versões modernas e também documenta recursos de row grouping e pivoting; pivoting e recursos avançados são recursos Enterprise.

3. Estrutura SaaS multiempresa
Modelo de tenant

O tenant será a organização contratante do SaaS, que pode ser:

Empresa única.
Grupo empresarial.
Matriz com filiais.
Várias empresas do mesmo grupo econômico.
Empresas com CNPJs diferentes, inscrições municipais diferentes e certificados digitais diferentes.
Hierarquia
Tenant
 └── Grupo Empresarial
      ├── Empresa / Matriz
      │    ├── Filial 1
      │    ├── Filial 2
      │    └── Filial N
      ├── Plano de contas
      ├── Centros de custo
      ├── Contratos
      ├── Colaboradores
      ├── Certificados digitais
      └── Obrigações fiscais/trabalhistas
Isolamento de dados

Recomendação:

PostgreSQL com tenant_id obrigatório em todas as tabelas.
Row Level Security no banco.
Criptografia por tenant para dados sensíveis.
Opção enterprise: schema ou database por tenant grande.
Logs e auditoria separados por tenant.
Política de retenção de documentos por tipo: folha, fiscal, contábil, trabalhista, contrato, ponto e e-mail.
4. Módulos do sistema
4.1. Módulo de licitações vencidas
Objetivo

Cadastrar, importar e controlar licitações vencidas, propostas, lotes, planilhas, atas, contratos e documentos de habilitação.

Funcionalidades
Cadastro da licitação.
Número do processo.
Modalidade: pregão, concorrência, dispensa, inexigibilidade etc.
Regime legal: Lei 14.133/2021, lei anterior quando aplicável, contratos legados.
Órgão contratante.
Unidade gestora.
Portal de origem.
PNCP.
Compras.gov.br.
Número do edital.
Número do contrato.
Número da ata, quando houver.
Lotes vencidos.
Itens vencidos.
Planilha vencedora.
Proposta comercial.
Termo de referência.
Estudo técnico preliminar.
Edital.
Ata da sessão.
Homologação.
Adjudicação.
Contrato assinado.
Aditivos.
Apostilamentos.
Termos de repactuação.
Garantia contratual.
Conta vinculada, quando exigida.
Documentos de habilitação.
Certidões.
Vigência.
Prazos de renovação.
Prazo de implantação.
Obrigações contratuais.
Cláusulas de IMR/SLA/glosa.
Cláusulas de reajuste, repactuação e reequilíbrio.
Integrações recomendadas
PNCP para consulta de contratações, atas e contratos. O manual da API do PNCP descreve consultas REST/JSON para dados de contratações, atas e contratos no âmbito da Lei 14.133.
Compras.gov.br para dados de licitações federais e integração com histórico de compras.
Consulta automática de certidões: Receita Federal, FGTS/CRF, CNDT, CADIN, SICAF, quando houver viabilidade técnica e jurídica.
4.2. Módulo de contratos
Cadastro do contrato

Campos principais:

Empresa executora.
Filial executora.
Órgão contratante.
CNPJ do órgão.
Unidade administrativa.
Fiscal técnico.
Fiscal administrativo.
Gestor do contrato.
Preposto da contratada.
Número do contrato.
Processo administrativo.
Licitação vinculada.
Data de assinatura.
Início da vigência.
Fim da vigência.
Prazo de execução.
Data de implantação.
Valor global.
Valor mensal estimado.
Índice de reajuste.
Data-base da categoria.
Sindicato/CCT aplicável.
Natureza do serviço.
Regime de dedicação exclusiva.
Município de prestação.
Locais de prestação.
Regras de faturamento.
Regras de retenção.
Regras de glosa.
Regras de substituição.
Regras de medição.
Regras de ponto.
Regras de uniforme.
Regras de equipamentos.
Regras de documentação mensal.
Controle de vigência
Alertas de vencimento.
Renovação.
Aditivo de prazo.
Aditivo de valor.
Supressão.
Acréscimo.
Apostilamento.
Repactuação.
Reajuste.
Reequilíbrio econômico-financeiro.
Encerramento.
Rescisão.
Desmobilização.
Painel do contrato

Cada contrato deve ter uma visão 360°:

Licitação de origem.
Lotes.
Postos.
Funcionários alocados.
Volantes.
Escalas.
Ponto.
Faltas.
Substituições.
IMR.
Glosas.
Medições.
Notas fiscais.
E-mails enviados.
Notificações recebidas.
Documentos pendentes.
Uniformes.
Equipamentos.
Custos.
Margem.
Folha vinculada.
Provisões.
Recebimentos.
Pendências fiscais/trabalhistas.
4.3. Módulo de lotes, itens e postos
Cadastro de lote
Licitação vinculada.
Contrato vinculado.
Número do lote.
Descrição do lote.
Valor global do lote.
Valor mensal do lote.
Itens do lote.
Quantitativo contratado.
Quantitativo executado.
Quantitativo ativo.
Quantitativo suspenso.
Quantitativo glosado.
Cadastro de posto

Campos principais:

Contrato.
Lote.
Item.
Código do posto.
Nome do posto.
Função.
CBO.
Município.
Local físico.
Setor.
Turno.
Escala.
Jornada.
Horário previsto.
Quantidade contratada.
Quantidade implantada.
Quantidade ocupada.
Quantidade vaga.
Tipo de posto: fixo, volante, reserva, cobertura, supervisão.
Valor mensal por posto.
Valor diário.
Valor hora.
Valor adicional noturno.
Valor hora extra.
Valor de benefícios.
Valor de encargos.
Valor de insumos.
Valor de uniforme.
Valor de equipamento.
Margem prevista.
Regra de glosa por ausência.
Regra de glosa por atraso.
Regra de medição.
Regra de substituição.
Planilha vencedora

O sistema deve importar e versionar a planilha vencedora:

Excel.
PDF.
CSV.
Planilha estruturada manualmente.
Memória de cálculo.
Composição de custos.
Salários.
Encargos.
Benefícios.
Uniformes.
EPIs.
Equipamentos.
Materiais.
BDI.
Lucro.
Tributos.
Custos indiretos.
Reserva técnica.
Custos de reposição.
Férias, 13º, rescisões e provisões.
Comparativo entre preço licitado, custo real e margem real.
4.4. Módulo de implantação do contrato
Objetivo

Controlar a fase entre a assinatura do contrato e o início efetivo da operação.

Checklist de implantação
Recebimento do contrato assinado.
Cadastro do órgão contratante.
Cadastro dos fiscais.
Cadastro dos locais.
Cadastro dos postos.
Definição de escalas.
Definição de colaboradores.
Contratação de novos funcionários.
Transferência de funcionários.
Exames admissionais.
ASO.
Integração.
Treinamentos obrigatórios.
NR aplicável.
Entrega de uniforme.
Entrega de EPI.
Entrega de crachá.
Entrega de equipamento.
Instalação de relógio de ponto.
Parametrização de ponto.
Parametrização de folha.
Parametrização de faturamento.
Parametrização de glosa.
Cadastro de e-mails oficiais.
Cadastro de documentos mensais exigidos.
Aprovação de início pelo gestor interno.
Termo de início.
Evidências fotográficas.
Relatório de mobilização.
Status
Planejado.
Em implantação.
Parcialmente implantado.
Implantado.
Em pendência.
Suspenso.
Cancelado.
4.5. Módulo de colaboradores
Cadastro completo
Nome.
CPF.
RG.
Data de nascimento.
Sexo/gênero, quando necessário.
PIS/NIS.
CTPS digital.
Matrícula interna.
Matrícula eSocial.
Empresa.
Filial.
Contrato.
Posto.
Cargo.
CBO.
Sindicato.
CCT.
Data de admissão.
Tipo de contrato.
Salário base.
Jornada.
Escala.
Banco de horas.
Benefícios.
Dependentes.
Dados bancários.
Endereço.
Contatos.
Documentos pessoais.
ASO.
Certificados.
Treinamentos.
Uniformes recebidos.
EPIs recebidos.
Equipamentos sob guarda.
Histórico de alocações.
Histórico salarial.
Histórico de afastamentos.
Histórico de férias.
Histórico de advertências.
Histórico de substituições.
Histórico de ponto.
Histórico de glosas relacionadas.
Alocação

Um colaborador poderá estar:

Fixo em um posto.
Volante.
Reserva técnica.
Em treinamento.
Em férias.
Afastado.
Em cobertura temporária.
Em transição entre contratos.
Desmobilizado.
Desligado.
4.6. Módulo de funcionários volantes e reposição
Objetivo

Garantir cobertura de faltas, férias, afastamentos, licenças e postos vagos.

Funcionalidades
Cadastro de banco de volantes.
Região de atuação.
Habilidades.
Funções habilitadas.
Contratos que pode atender.
Disponibilidade.
Escala flexível.
Custo por acionamento.
Tempo de deslocamento.
Documentos obrigatórios.
Uniformes disponíveis.
Equipamentos disponíveis.
Histórico de coberturas.
Ranking por confiabilidade.
Alerta automático de falta sem cobertura.
Sugestão automática de substituto.
Aceite via aplicativo.
Registro de chegada.
Evidência de cobertura.
Cálculo de glosa evitada.
Cálculo de custo da reposição.
Regra automática

Quando o sistema detectar falta, atraso crítico ou posto descoberto:

Verifica se há volante disponível.
Calcula distância, função, escala, custo e conformidade documental.
Sugere ou aciona substituto.
Registra cobertura.
Atualiza medição.
Evita ou reduz glosa.
Gera evidência para o contratante.
4.7. Módulo de ponto eletrônico

Este é um dos módulos mais importantes.

A Portaria 671 trata tipos de sistema de registro eletrônico de ponto: REP-C, REP-A e REP-P. O FAQ oficial do Ministério do Trabalho explica que o SREP pode ser convencional, alternativo ou via programa, e diferencia REP-C, REP-A e REP-P.

Tipos de entrada de ponto

O sistema deve aceitar:

Relógio físico REP-C.
Sistema alternativo REP-A, quando permitido por CCT/acordo.
Sistema via programa REP-P.
Aplicativo mobile.
Web.
Arquivo AFD.
Arquivo AEJ.
Importação manual.
Digitalização de cartão de ponto.
Upload de PDF.
Upload de imagem.
Digitação manual.
Integração API.
Integração SDK.
Integração TCP/IP.
Integração por e-mail.
Integração offline por arquivo.
Tratamento de ponto
Importação de marcações.
Identificação de colaborador.
Identificação de posto.
Associação com escala.
Tratamento de entrada.
Tratamento de saída.
Intervalo.
Hora extra.
Adicional noturno.
Atraso.
Saída antecipada.
Falta.
Falta parcial.
Intervalo não cumprido.
Intrajornada.
Interjornada.
DSR.
Banco de horas.
Feriados.
Escalas 12x36.
Escalas 5x2.
Escalas 6x1.
Plantões.
Sobreaviso, se aplicável.
Abonos.
Justificativas.
Atestados.
Férias.
Afastamentos.
Licenças.
Ponto por exceção, quando juridicamente permitido.
Espelho de ponto.
Assinatura do espelho.
Aprovação pelo supervisor.
Envio para folha.
Arquivos oficiais

O sistema deve suportar AFD e AEJ. O próprio material oficial da Portaria 671 menciona que AFDT e ACJEF foram substituídos pelo AEJ, arquivo de pós-processamento conforme o Anexo VI.

Integração com relógios de ponto no Brasil

A estratégia correta não é depender de um único fornecedor, mas criar uma camada universal de integração de ponto:

Clock Integration Gateway
 ├── AFD Parser
 ├── AEJ Parser
 ├── REST Connectors
 ├── SOAP Connectors
 ├── SDK Connectors
 ├── TCP/IP Connectors
 ├── SFTP/File Import
 ├── Email Import
 └── Manual/Digitalização/OCR
Marcas/conectores prioritários
1. Control iD

A Control iD disponibiliza documentação de API para REP iDClass, com chamadas adaptadas à Portaria 671 usando parâmetro de modo e comunicação REST/HTTPS/JSON.

Integração:

REST API.
HTTPS.
JSON.
Cadastro de colaboradores.
Coleta de marcações.
Sincronização de data/hora.
Consulta de dispositivo.
Coleta de AFD.
Modo Portaria 671.
2. Topdata

A Topdata disponibiliza SDK para Inner Rep/Inner Ponto, com funções de cadastro, exclusão de funcionários, biometria, sincronização, TCP/IP, leitura de registros e montagem de AFD.

Integração:

SDK Windows.
DLL.
TCP/IP.
Exemplos C#, Delphi e Java.
Importação AFD.
Worker local para clientes que usam rede interna.
3. Dimep / PrintPoint / Smart Point

Há integrações de mercado com Dimep via API REST, além de comunicação TCP/IP e coleta de registros. A integração deve tratar Dimep como conector específico e também aceitar importação AFD quando não houver API disponível.

Integração:

API REST quando disponível.
TCP/IP.
AFD.
Software intermediário.
Worker local.
4. Henry

A linha Henry Prisma aparece como REP homologado/certificado em materiais comerciais e é amplamente usada no Brasil. Como a API pública pode variar por modelo/revenda, o sistema deve suportar Henry por AFD, coleta local, software intermediário e conector específico quando houver documentação do cliente.

5. Madis / Rodbel

Madis/Rodbel aparecem na base de REP e no mercado com soluções de ponto e softwares próprios. O conector deve começar por AFD/AEJ e evoluir para API quando contratada ou documentada pelo fornecedor.

6. RWTech

RWTech deve ser suportado por AFD/AEJ e integração com software de gestão de ponto quando disponível. Resultados públicos indicam exportação de AFD/AEJ em conformidade com a Portaria 671.

7. Secullum, Ahgora, Pontomais, Tangerino, Oitchau e outros REP-P/REP-A

Esses fornecedores devem ser tratados como integrações de software:

API REST.
Webhook.
Exportação AFD/AEJ.
Importação de espelho.
Importação CSV.
Integração por arquivo.
Integração por e-mail.
Conciliação com escala e posto.
Ponto offline

Quando o relógio não tiver internet:

Instalar agente local.
Ler relógio via rede local.
Coletar AFD.
Sincronizar com nuvem.
Validar duplicidade.
Validar NSR.
Aplicar hash.
Registrar evidência.
Mostrar status do último sync.
Alertar relógio sem comunicação.
Permitir upload manual do AFD.
4.8. Módulo de escala
Funcionalidades
Escala por contrato.
Escala por posto.
Escala por colaborador.
Turnos fixos.
Turnos rotativos.
12x36.
6x1.
5x2.
Plantão.
Feriados.
Revezamento.
Troca de escala.
Cobertura.
Dobra.
Banco de horas.
Planejamento de férias.
Escala de volantes.
Simulação de custo por escala.
Conflitos de jornada.
Alertas de interjornada.
Alertas de excesso de horas.
Alertas de ausência de intervalo.
Integração com ponto.
Integração com folha.
Integração com glosa.
4.9. Módulo de medição mensal
Objetivo

Gerar a medição do contrato antes da emissão da nota fiscal.

Dados usados
Postos contratados.
Postos efetivamente cobertos.
Frequência.
Faltas.
Atrasos.
Substituições.
Ocorrências.
IMR.
SLA.
Glosas.
Reajustes.
Repactuações.
Adicionais.
Descontos.
Retenções.
Documentos entregues.
Aceite do fiscal.
Histórico de e-mails.
Notificações recebidas.
Resultado
Valor bruto da medição.
Glosas.
Descontos.
Retenções.
Valor líquido a faturar.
Memória de cálculo.
Relatório para o órgão.
Arquivo PDF.
Planilha Excel.
Aprovação interna.
Aprovação do contratante.
Geração automática da NFS-e.
4.10. Módulo de glosas
Tipos de glosa
Glosa por falta.
Glosa por atraso.
Glosa por posto descoberto.
Glosa por substituição fora do prazo.
Glosa por uniforme inadequado.
Glosa por equipamento ausente.
Glosa por documentação pendente.
Glosa por descumprimento de IMR.
Glosa por não conformidade operacional.
Glosa por reincidência.
Glosa manual.
Glosa contestada.
Glosa revertida em recurso.
Glosa mantida.
Glosa recuperada.
Fórmula base
Valor da glosa = Valor faturado - Valor aprovado/pago
Glosa por falta

Exemplo de fórmula parametrizável:

Glosa por falta = Valor diário do posto × quantidade de dias/turnos descobertos × fator contratual

Ou, por hora:

Glosa por falta = Valor hora do posto × horas descobertas × fator contratual
Glosa por IMR

O IMR precisa ser totalmente configurável por contrato:

Glosa IMR = Valor base do contrato × percentual de penalidade por indicador

ou

Glosa IMR = Peso do indicador × pontuação perdida × valor de referência

Exemplos de indicadores:

Posto descoberto.
Atraso de reposição.
Falha de supervisão.
Falta de uniforme.
Falta de EPI.
Reclamação procedente.
Não entrega de documento mensal.
Não atendimento de notificação.
Não conformidade em vistoria.
Descumprimento de prazo.
Workflow de glosa
Sistema detecta evento.
Calcula glosa preliminar.
Verifica se houve cobertura por volante.
Verifica justificativa.
Verifica regra contratual.
Gera evidência.
Submete ao responsável.
Lança na medição.
Aplica na NFS-e, se aprovada.
Controla recurso administrativo.
Controla recuperação da glosa.
Atualiza margem do contrato.
4.11. Módulo de notificações recebidas
Cadastro
Contrato.
Órgão.
Fiscal emissor.
Data de recebimento.
Meio de recebimento.
E-mail vinculado.
Ofício.
Número do processo.
Tipo de notificação.
Gravidade.
Prazo de resposta.
Responsável interno.
Status.
Anexos.
Resposta enviada.
Evidências.
Risco de glosa.
Risco de penalidade.
Risco de rescisão.
IA sugerindo resposta.
Controle de protocolo.
Tipos
Notificação de falta.
Notificação de descumprimento.
Notificação trabalhista.
Notificação documental.
Notificação de IMR.
Notificação de glosa.
Notificação de penalidade.
Solicitação de esclarecimento.
Solicitação de substituição.
Solicitação de documentos.
Advertência.
Multa.
Processo administrativo sancionador.
4.12. Módulo de e-mails
Funcionalidades
Integração IMAP/SMTP.
Microsoft Graph.
Gmail API.
Caixas compartilhadas.
E-mails por contrato.
Envio automático de NFS-e.
Envio automático de medição.
Envio automático de folha documental.
Envio automático de resposta.
Captura de anexos.
Classificação por IA.
Vinculação automática ao contrato.
Vinculação automática à notificação.
Vinculação automática à NFS-e.
Controle de thread.
Protocolo de envio.
Confirmação de leitura, quando disponível.
Assinatura padrão.
Templates.
SLA de resposta.
Auditoria.
Agente de e-mail

O agente deve:

Ler e-mails recebidos.
Identificar contrato pelo número, CNPJ, órgão, assunto ou anexos.
Classificar urgência.
Extrair prazos.
Criar tarefas.
Anexar documentos.
Sugerir resposta.
Alertar responsável.
Enviar automaticamente documentos recorrentes quando autorizado.
4.13. Módulo de notas fiscais de serviço — Curitiba

O sistema deve emitir NFS-e de serviço para Curitiba e enviar automaticamente ao contratante.

Ponto importante: Curitiba está migrando para o Emissor Nacional da NFS-e. O guia oficial de migração informa que Curitiba deixará o emissor próprio e passará a usar o Emissor Nacional, com necessidade de cadastro e integração via API para emissão em lotes. O cronograma oficial indica obrigatoriedade em fases: ISS Fixo em 01/10/2025, optantes do Simples Nacional em 01/11/2025 e demais contribuintes em 01/01/2026.

Estratégia correta

Implementar dois caminhos:

Caminho 1 — NFS-e Nacional

Obrigatório para Curitiba conforme cronograma atual.

Funcionalidades:

Cadastro no ambiente nacional.
Certificado digital.
Integração API.
Emissão.
Consulta.
Cancelamento.
Substituição.
Download XML.
Download DANFSE.
Armazenamento.
Envio por e-mail.
Protocolo.
Conciliação com contas a receber.
Lançamento contábil.
Lançamento fiscal.

A documentação nacional possui APIs e materiais técnicos de produção e homologação, incluindo manuais de API, esquemas XSD, anexos de domínio e layouts.

Caminho 2 — Legado Curitiba

Manter suporte legado para histórico, contingência, clientes com documentos antigos ou ambientes que ainda precisem consultar documentos anteriores.

A documentação antiga de Curitiba usa WebService SOAP/XML, SSL e certificado digital ICP-Brasil A1 ou A3.

Workflow automático da NFS-e
Medição aprovada
 → cálculo de glosas
 → cálculo de retenções
 → valor líquido/faturável
 → geração do RPS/DPS
 → emissão NFS-e
 → consulta status
 → download XML
 → download DANFSE
 → lançamento fiscal
 → lançamento contábil
 → contas a receber
 → envio automático por e-mail ao contratante
 → arquivamento do protocolo
E-mail automático ao contratante
Template por órgão.
Assunto padronizado.
Corpo com número do contrato, competência, valor, número da NFS-e.
Anexo DANFSE.
Anexo XML.
Anexo medição.
Anexo memória de cálculo.
Anexo documentos exigidos.
Cópia para fiscal técnico.
Cópia para fiscal administrativo.
Cópia interna.
Registro de envio.
Reenvio automático se falhar.
4.14. Módulo de folha de pagamento

O sistema deve calcular a folha completa com base em ponto, escala, CCT, salários, benefícios, adicionais e eventos trabalhistas.

A integração com eSocial precisa seguir os leiautes oficiais. A documentação do eSocial mantém leiautes, XSD, Manual de Orientação, Manual do Desenvolvedor, mensagens do sistema e pacote de comunicação.

Cadastro trabalhista
Empregador.
Estabelecimentos.
Lotação tributária.
Rubricas.
Cargos.
Funções.
Horários.
Escalas.
Sindicatos.
CCT.
Colaboradores.
Dependentes.
Benefícios.
Afastamentos.
Férias.
Rescisões.
Processos trabalhistas, se aplicável.
Cálculo da folha
Salário base.
Horas normais.
Horas extras.
Adicional noturno.
Periculosidade.
Insalubridade.
DSR.
Faltas.
Atrasos.
Saídas antecipadas.
Banco de horas.
Férias.
13º salário.
Adiantamento.
Rescisão.
Aviso prévio.
VT.
VA.
VR.
Plano de saúde.
Plano odontológico.
Coparticipações.
Descontos autorizados.
Pensão alimentícia.
INSS.
IRRF.
FGTS.
FGTS rescisório.
Provisões.
Encargos patronais.
Custo por contrato.
Custo por posto.
Custo por filial.
Custo por centro de custo.
Integração eSocial

Eventos a suportar:

S-1000 — empregador.
S-1005 — estabelecimentos.
S-1010 — rubricas.
S-1020 — lotações.
S-2190 — admissão preliminar.
S-2200 — admissão.
S-2205 — alteração cadastral.
S-2206 — alteração contratual.
S-2230 — afastamento.
S-2299 — desligamento.
S-1200 — remuneração.
S-1210 — pagamentos.
S-1280 — informações complementares.
S-1298 — reabertura.
S-1299 — fechamento.

Os leiautes do eSocial trazem regras específicas para eventos periódicos como S-1200 e S-1210, incluindo identificação por CPF/período, validações de remuneração, pagamento e fechamento.

Fechamento da folha

Workflow:

Ponto tratado
 → eventos variáveis
 → CCT aplicada
 → cálculo da folha
 → conferência automática
 → validação de divergências
 → provisões
 → encargos
 → holerites
 → eSocial
 → FGTS Digital
 → DCTFWeb/MIT
 → lançamentos contábeis
 → contas a pagar
4.15. Upload e leitura automática de CCT
Objetivo

O usuário faz upload da Convenção Coletiva de Trabalho e o sistema atualiza parâmetros automaticamente.

Entrada
PDF.
DOC/DOCX.
Texto.
Link.
CCT anterior.
Termo aditivo.
Acordo coletivo.
Extração por IA

O agente deve extrair:

Sindicato patronal.
Sindicato laboral.
Base territorial.
Vigência.
Data-base.
Pisos salariais.
Funções.
CBO, quando houver.
Reajuste.
Retroativo.
VA/VR.
VT.
Cesta básica.
Plano de saúde.
Seguro de vida.
Adicional noturno.
Horas extras.
DSR.
Banco de horas.
Jornada.
Escala 12x36.
Multas convencionais.
Homologação.
Estabilidade.
Benefícios obrigatórios.
Contribuições sindicais/assistenciais, quando aplicável.
Regras específicas por município.
Aplicação

O sistema não deve simplesmente alterar a folha sem controle. O fluxo correto:

Upload da CCT
 → extração por IA
 → comparação com CCT anterior
 → identificação de mudanças
 → simulação de impacto
 → sugestão de novos parâmetros
 → aprovação
 → atualização da folha
 → cálculo retroativo
 → pacote de repactuação contratual
Saídas
Resumo da CCT.
Tabela de pisos.
Tabela de benefícios.
Impacto financeiro por contrato.
Impacto por posto.
Memória de cálculo.
Minuta de pedido de repactuação.
Alerta de contratos afetados.
Atualização da planilha de custos.
4.16. Módulo de contabilidade automática

O sistema deve fazer a contabilidade da empresa cadastrada, inclusive grupos e filiais.

Escopo
Plano de contas.
Centro de custo.
Filiais.
Contratos.
Lançamentos automáticos.
Contas a pagar.
Contas a receber.
Conciliação bancária.
Folha contabilizada.
Provisões trabalhistas.
Depreciação de equipamentos.
Estoque de uniformes.
Receitas por NFS-e.
Retenções.
Tributos.
Encargos.
Rateios.
DRE.
Balancete.
Razão.
Diário.
Fluxo de caixa.
Demonstrativo por contrato.
Demonstrativo por filial.
Demonstrativo consolidado do grupo.
Ingestão de documentos
XML de NFS-e.
XML de NF-e.
XML de NFC-e, se aplicável.
CT-e, se aplicável.
PDFs.
Boletos.
Extratos bancários.
OFX.
CNAB.
Comprovantes.
Recibos.
Folha.
Guias.
DARF.
FGTS.
Documentos de fornecedores.
Motor contábil

O sistema deve ter regras como:

NFS-e emitida
 → D: Clientes / Contas a receber
 → C: Receita de serviços
 → C/D: Tributos e retenções conforme regra
Folha calculada
 → D: Despesa de salários por contrato
 → C: Salários a pagar
 → C: Encargos a recolher
 → C: Provisões
Recebimento bancário conciliado
 → D: Banco
 → C: Clientes
Compra de uniforme
 → D: Estoque de uniformes
 → C: Fornecedores
Entrega de uniforme ao funcionário
 → D: Despesa/insumo do contrato
 → C: Estoque de uniformes
Obrigações e integrações

O sistema deve preparar dados para:

DCTFWeb/MIT.
EFD-Reinf.
EFD-Contribuições.
ECD.
ECF.
eSocial.
FGTS Digital.
SPED contábil/fiscal conforme regime.

A Receita Federal informa que DCTF e DCTFWeb foram unificadas e que, desde 01/01/2025, a antiga DCTF PGD passou a ser substituída pela DCTFWeb mensal, alimentada por eSocial, EFD-Reinf e MIT.

Conciliação bancária automática

Entradas:

Open Finance, se disponível.
OFX.
CNAB.
Extrato CSV.
API bancária.
Webhook bancário.
Manual.

Processo:

Extrato importado
 → normalização
 → identificação por valor/data/documento
 → match com NFS-e/contas a receber
 → match com fornecedores
 → match com folha
 → match com tributos
 → sugestão de baixa
 → baixa automática se confiança alta
 → pendência se confiança baixa
4.17. Módulo fiscal e tributário
Funcionalidades
Cadastro de regime tributário.
Simples Nacional.
Lucro Presumido.
Lucro Real.
ISS por município.
Retenções federais.
INSS sobre cessão de mão de obra, quando aplicável.
IRRF.
PIS.
COFINS.
CSLL.
Controle de tomador.
Controle de município de incidência.
Parametrização por serviço.
CNAE.
Código de serviço municipal.
NBS, se aplicável.
Alíquotas.
Regras de retenção por contrato.
Apuração.
Guias.
Lançamento contábil.
Relatórios.
4.18. Módulo de equipamentos
Cadastro
Tipo.
Marca.
Modelo.
Número de série.
Patrimônio.
Contrato.
Posto.
Funcionário responsável.
Data de entrega.
Data de devolução.
Estado de conservação.
Valor.
Garantia.
Manutenção.
Fotos.
Termo de responsabilidade.
Histórico.
Tipos
Relógio de ponto.
Computador.
Celular.
Rádio.
Equipamento operacional.
Ferramenta.
EPI.
Crachá.
Chave.
Veículo, se aplicável.
Workflows
Compra.
Entrada em estoque.
Alocação.
Entrega.
Assinatura.
Manutenção.
Substituição.
Perda.
Dano.
Devolução.
Baixa.
Lançamento contábil.
4.19. Módulo de uniformes
Cadastro de estoque
Tipo de peça.
Tamanho.
Gênero/modelagem, se necessário.
Cor.
Marca.
Quantidade.
Custo unitário.
Lote.
Fornecedor.
Data de compra.
Contrato.
Filial.
Almoxarifado.
Distribuição por funcionário
Funcionário.
Contrato.
Posto.
Itens entregues.
Quantidade.
Tamanho.
Data.
Validade/troca prevista.
Assinatura.
Foto.
Termo.
Devolução.
Desconto por extravio, se permitido e documentado.
Histórico de entregas.
Alertas
Funcionário sem uniforme.
Uniforme vencido.
Troca programada.
Estoque baixo.
Contrato sem kit mínimo.
Falta de assinatura de recebimento.
Possível glosa por uniforme.
4.20. Módulo documental
Tipos de documentos
Edital.
Contrato.
Aditivo.
Apostilamento.
Planilha vencedora.
Medição.
Nota fiscal.
XML.
E-mail.
Ofício.
Notificação.
ASO.
Treinamento.
Certidão.
Holerite.
Guia FGTS.
INSS.
Folha analítica.
Ponto.
Espelho de ponto.
Recibo de uniforme.
Recibo de EPI.
Termo de equipamento.
CCT.
Relatório de fiscalização.
Evidência fotográfica.
Recursos
OCR.
Classificação por IA.
Extração de campos.
Versionamento.
Assinatura eletrônica.
Trilha de auditoria.
Validade.
Alerta de vencimento.
Associação a contrato, posto, colaborador ou nota.
Busca semântica.
Busca por texto.
Tags.
Permissões.
4.21. Módulo de agentes de IA
Provedores

O sistema deve ter um AI Gateway próprio para integrar:

OpenAI.
Anthropic.
Google Gemini.
Ollama local.
Modelos futuros.

OpenAI recomenda a Responses API para novos projetos, com suporte a ferramentas, multimodalidade e fluxos agentic. A Anthropic documenta a Messages API como superfície principal de acesso direto aos modelos Claude. A Gemini API possui documentação oficial e referência para geração de conteúdo. O Ollama deve ser tratado como provedor local/self-hosted, com APIs próprias e compatibilidade parcial/variável com padrões OpenAI conforme evolução do projeto.

Arquitetura do AI Gateway
AI Gateway
 ├── Provider: OpenAI
 ├── Provider: Anthropic
 ├── Provider: Gemini
 ├── Provider: Ollama
 ├── Model Router
 ├── Prompt Registry
 ├── Tool Registry
 ├── RAG Engine
 ├── Policy Engine
 ├── Audit Log
 ├── Cost Control
 ├── PII Redaction
 └── Human Approval Layer
Agentes principais
1. Agente de licitação
Lê edital.
Extrai lotes.
Extrai postos.
Extrai exigências.
Extrai regras de glosa.
Extrai documentos mensais.
Compara com planilha vencedora.
Gera checklist de implantação.
Alerta riscos.
2. Agente de contrato
Lê contrato.
Identifica obrigações.
Cria prazos.
Cria regras de medição.
Cria regras de faturamento.
Cria regras de IMR.
Cria alertas de vigência.
Sugere aditivos/repactuações.
3. Agente de CCT
Lê CCT.
Extrai pisos.
Extrai benefícios.
Extrai adicionais.
Calcula impacto.
Propõe atualização da folha.
Propõe repactuação.
4. Agente de ponto
Detecta inconsistências.
Sugere abonos.
Identifica faltas.
Identifica atrasos.
Aciona volante.
Calcula risco de glosa.
Gera espelho.
5. Agente de glosa
Calcula glosas.
Identifica glosas indevidas.
Gera defesa/recurso.
Junta evidências.
Calcula recuperação possível.
6. Agente fiscal/NFS-e
Gera nota.
Valida código de serviço.
Valida retenções.
Emite NFS-e.
Envia e-mail.
Monitora falhas.
7. Agente contábil
Classifica XML.
Classifica extrato.
Sugere lançamentos.
Concilia banco.
Identifica divergências.
Fecha competência.
Gera DRE por contrato.
8. Agente de e-mail
Classifica mensagens.
Anexa ao contrato.
Identifica prazo.
Gera resposta.
Envia documentos recorrentes.
Alerta risco.
9. Agente de compliance
Monitora documentos vencidos.
Monitora certidões.
Monitora alterações legais.
Monitora APIs.
Monitora CCTs.
Monitora contratos vencendo.
4.22. Módulo de BI, pivot e analytics
Visões principais
Receita por contrato.
Receita por órgão.
Receita por filial.
Custo por contrato.
Custo por posto.
Margem por contrato.
Margem por lote.
Glosa por motivo.
Glosa por órgão.
Glosa por fiscal.
Glosa recuperada.
Faltas por colaborador.
Faltas por posto.
Cobertura por volante.
Horas extras.
Custo de folha.
Benefícios.
Uniformes.
Equipamentos.
Notificações.
Prazos críticos.
Inadimplência.
DRE por contrato.
DRE consolidada.
AG Grid

Todas as telas analíticas devem permitir:

Agrupamento de linhas.
Pivot.
Filtros.
Colunas configuráveis.
Estado salvo por usuário.
Exportação Excel.
Drill-down.
Master/detail.
Totais e subtotais.
Permissões por coluna.
Dados em tempo real quando necessário.
5. Modelo de dados principal
Entidades centrais
Tenant
BusinessGroup
LegalEntity
Branch
User
Role
Permission
AuditLog

Bid
BidLot
BidItem
WinningSpreadsheet
BidDocument

Contract
ContractAddendum
ContractLocation
ContractOfficer
ContractRule
ContractIMR
ContractBillingRule
ContractRetentionRule

Post
PostSchedule
PostPrice
PostRequirement
PostOccupancy

Employee
EmployeeContract
EmployeeAllocation
EmployeeDocument
EmployeeBenefit
EmployeePayrollProfile

FloatingEmployee
ReplacementAssignment

TimeClockDevice
TimeClockIntegration
TimeRecord
TimeSheet
TimeAdjustment
TimeApproval
Absence
Delay
Overtime
BankHours

Measurement
MeasurementItem
Glosa
GlosaRule
GlosaEvidence
IMRIndicator
IMRCalculation

Invoice
ServiceInvoice
InvoiceXML
InvoiceEmail
TaxRetention

EmailAccount
EmailMessage
EmailAttachment
Notification
Task
Deadline

PayrollPeriod
PayrollEvent
PayrollCalculation
Payslip
SocialSecurityEvent
FGTSEvent

CCT
CCTClause
CCTParameter
SalaryTable
BenefitRule

AccountingAccount
JournalEntry
JournalEntryLine
CostCenter
BankAccount
BankStatement
BankTransaction
ReconciliationMatch

Asset
AssetAssignment
UniformStock
UniformDelivery
EPIStock
EPIDelivery

Document
DocumentVersion
DocumentExtraction
DocumentSignature
6. Workflows automáticos principais
6.1. Do edital ao contrato implantado
Upload edital/contrato/planilha
 → IA extrai lotes/postos/regras
 → sistema cria contrato
 → sistema cria postos
 → sistema importa planilha vencedora
 → sistema cria checklist de implantação
 → RH aloca colaboradores
 → estoque entrega uniformes/EPIs
 → equipamentos são entregues
 → ponto é configurado
 → contrato entra em operação
6.2. Do ponto à folha
Coleta ponto
 → trata marcações
 → identifica faltas/atrasos
 → aplica escala
 → calcula adicionais
 → aprova espelho
 → gera eventos variáveis
 → calcula folha
 → gera holerite
 → transmite/gera eSocial
 → gera encargos
 → contabiliza
6.3. Da operação à nota fiscal
Postos executados
 → medição mensal
 → glosas calculadas
 → IMR aplicado
 → retenções calculadas
 → NFS-e emitida
 → e-mail enviado ao contratante
 → contas a receber criado
 → contabilidade lançada
 → conciliação bancária
6.4. Da CCT à repactuação
Upload CCT
 → IA extrai cláusulas
 → compara com parâmetros atuais
 → calcula impacto na folha
 → calcula impacto por contrato
 → atualiza folha após aprovação
 → gera memória de repactuação
 → gera minuta de ofício
 → controla protocolo no órgão
6.5. Da notificação à defesa
E-mail/ofício recebido
 → IA identifica contrato
 → cria notificação
 → extrai prazo
 → busca evidências
 → calcula risco
 → sugere resposta
 → responsável aprova
 → sistema envia e-mail
 → protocolo é arquivado
7. Regras de automação e segurança
Autopiloto configurável

Cada empresa poderá definir níveis de automação:

Processo	Manual	Assistido	Automático com aprovação	Totalmente automático
Tratamento de ponto	Sim	Sim	Sim	Sim
Acionamento de volante	Sim	Sim	Sim	Sim
Cálculo de glosa	Sim	Sim	Sim	Sim
Medição	Sim	Sim	Sim	Sim
Emissão de NFS-e	Sim	Sim	Sim	Sim
Envio de e-mail	Sim	Sim	Sim	Sim
Cálculo de folha	Sim	Sim	Sim	Parcial
Transmissão eSocial	Sim	Sim	Sim	Com política rígida
Contabilidade	Sim	Sim	Sim	Sim
Obrigações fiscais	Sim	Sim	Sim	Com política rígida
Regras obrigatórias
Nada crítico sem log.
Nada fiscal sem rastreabilidade.
Nada trabalhista sem memória de cálculo.
Nada contábil sem partida dobrada.
Nada emitido sem vínculo com empresa/filial/competência.
Nada enviado por IA sem registro do prompt, resposta, usuário, data e versão do modelo.
Toda alteração automática deve gerar evento auditável.
Toda classificação por IA deve ter nível de confiança.
Baixa confiança gera fila de revisão.
8. Segurança, LGPD e auditoria
Segurança
MFA.
SSO.
RBAC.
ABAC.
Permissão por contrato.
Permissão por filial.
Permissão por módulo.
Permissão por documento.
Criptografia em repouso.
Criptografia em trânsito.
Cofre de certificados digitais.
Segregação de funções.
Logs imutáveis.
Assinatura de eventos críticos.
Backup.
Disaster recovery.
Monitoramento.
Alertas de acesso suspeito.
LGPD
Minimização de dados.
Base legal por finalidade.
Registro de tratamento.
Controle de acesso a dados pessoais.
Mascaramento de CPF.
Mascaramento de dados bancários.
Retenção por tipo documental.
Exportação de dados.
Exclusão/anomização quando juridicamente possível.
Relatório de acesso.
DPA para clientes enterprise.
9. Integrações externas
Governo e fiscal
NFS-e Nacional.
NFS-e Curitiba legado.
eSocial.
FGTS Digital.
DCTFWeb/MIT.
EFD-Reinf.
SPED ECD/ECF.
EFD-Contribuições.
PNCP.
Compras.gov.br.
Receita Federal.
Consulta CNPJ.
Certidões.
SICAF, quando viável.
Portais municipais, conforme necessidade.
Ponto
Control iD.
Topdata.
Dimep.
Henry.
Madis/Rodbel.
RWTech.
Secullum.
Ahgora.
Pontomais.
Tangerino.
Oitchau.
Outros via AFD/AEJ/API/arquivo.
Bancos
OFX.
CNAB.
APIs bancárias.
Open Finance, quando disponível.
Webhooks.
Boletos.
PIX.
Extratos.
E-mail
Microsoft 365 Graph.
Gmail API.
IMAP.
SMTP.
Caixas compartilhadas.
DKIM/SPF/DMARC para envio confiável.
IA
OpenAI.
Anthropic.
Gemini.
Ollama.
Provedores OpenAI-compatible, quando necessário.
Vetores/embeddings.
RAG.
OCR.
Classificação documental.
10. Telas principais
Dashboard executivo
Contratos ativos.
Receita mensal.
Margem mensal.
Glosas do mês.
Glosas recuperadas.
Faltas críticas.
Postos descobertos.
NFS-e pendentes.
Folha em fechamento.
Notificações abertas.
E-mails críticos.
Certidões vencendo.
Contratos vencendo.
CCTs vencendo.
Pendências de implantação.
Contratos
Grid com row grouping por órgão, filial, status e vigência.
Detalhe do contrato.
Lotes.
Postos.
Funcionários.
Medições.
Glosas.
Notificações.
Documentos.
NFS-e.
Folha vinculada.
Margem.
Postos
Postos contratados x ocupados.
Valor por posto.
Escala.
Funcionário atual.
Volante vinculado.
Histórico.
Faltas.
Glosas.
Equipamentos.
Uniformes.
Ponto
Marcações.
Inconsistências.
Faltas.
Atrasos.
Espelho.
Aprovação.
Importação AFD/AEJ.
Relógios.
Status de sincronização.
Folha
Competências.
Eventos.
Holerites.
Encargos.
eSocial.
FGTS.
Provisões.
Custo por contrato.
Custo por posto.
Contabilidade
Lançamentos.
Conciliação bancária.
Plano de contas.
Balancete.
DRE.
Razão.
Diário.
Custos por contrato.
Obrigações.
NFS-e
Pré-faturamento.
Medições aprovadas.
Emissão.
XML.
DANFSE.
Envio por e-mail.
Status.
Cancelamento.
Substituição.
Documentos
Biblioteca.
OCR.
Classificação.
Vencimentos.
Pendências.
Assinaturas.
Busca semântica.
11. APIs internas
Exemplos de endpoints
POST /contracts
GET /contracts/{id}
POST /contracts/{id}/posts
POST /contracts/{id}/measurements
POST /contracts/{id}/invoices/issue
POST /contracts/{id}/emails/send-invoice

POST /time-clocks/{id}/sync
POST /time-records/import/afd
POST /time-records/import/aej
POST /timesheets/{id}/approve

POST /payroll/periods
POST /payroll/periods/{id}/calculate
POST /payroll/periods/{id}/close

POST /accounting/documents/import-xml
POST /accounting/reconciliation/run

POST /cct/upload
POST /cct/{id}/apply-parameters

POST /ai/agents/contract-analyze
POST /ai/agents/cct-analyze
POST /ai/agents/glosa-calculate
12. Eventos de domínio
BidWon
ContractCreated
ContractImplantationStarted
PostCreated
EmployeeAllocated
TimeClockSynced
TimeRecordImported
AbsenceDetected
ReplacementAssigned
GlosaCalculated
MeasurementClosed
InvoiceIssued
InvoiceEmailed
PayrollCalculated
PayrollClosed
CCTUploaded
CCTParametersSuggested
BankStatementImported
BankTransactionReconciled
JournalEntryCreated
NotificationReceived
EmailLinkedToContract

Esses eventos alimentam:

Auditoria.
BI.
Agentes de IA.
Notificações.
Workflows.
Integrações.
13. O que estava faltando e deve entrar no escopo

Com base no tipo de contrato e nas integrações pesquisadas, eu incluiria também:

Contratos e fiscalização
Controle de fiscal técnico, fiscal administrativo e gestor do contrato.
Livro de ocorrências.
Recebimento provisório e definitivo.
Gestão de preposto.
Relatório mensal de execução.
Controle de documentos mensais exigidos pelo órgão.
Controle de conta vinculada ou garantia trabalhista, quando exigida.
Controle de repactuação.
Controle de reajuste.
Controle de reequilíbrio.
Controle de aditivos.
Controle de apostilamentos.
Gestão de risco de sanções.
Trabalhista
ASO.
Treinamentos obrigatórios.
EPIs.
Férias planejadas.
Cobertura de férias.
Afastamentos.
Estabilidades.
Substituição obrigatória.
CCT por município/função.
Retroativo de CCT.
Provisões trabalhistas.
Documentos para fiscalização administrativa.
Fiscal e financeiro
Retenções por órgão.
Contas a receber por contrato.
Controle de inadimplência pública.
Protocolo de cobrança.
Conciliação por NFS-e.
Controle de restos a pagar, quando aplicável.
Relatório de margem real.
Operação
App mobile para supervisores.
App ou portal para colaborador.
Assinatura de recibos.
Fotos de evidência.
Geolocalização, quando juridicamente permitida.
QR code de posto.
Checklist de ronda.
Checklist de qualidade.
Ocorrências operacionais.
Plano de ação.
Compliance técnico
Monitor de mudanças legais.
Monitor de mudanças nas APIs.
Monitor de validade de certificado digital.
Monitor de falhas de relógio de ponto.
Monitor de falhas de emissão NFS-e.
Monitor de eventos eSocial rejeitados.
14. MVP recomendado
MVP 1 — Núcleo contratual e operacional
Multiempresa/multifilial.
Cadastro de licitação.
Cadastro de contrato.
Cadastro de lotes/postos.
Cadastro de colaboradores.
Planilha vencedora.
Implantação.
Documentos.
E-mails.
Notificações.
Uniformes.
Equipamentos.
Glosas básicas.
Dashboard.
MVP 2 — Ponto e medição
Integração AFD/AEJ.
Control iD.
Topdata.
Importação manual.
Tratamento de ponto.
Volantes.
Medição mensal.
Glosa por falta.
Glosa por IMR.
Relatórios.
MVP 3 — NFS-e Curitiba/Nacional
Pré-faturamento.
Retenções.
NFS-e Nacional.
Curitiba legado se necessário.
Envio automático por e-mail.
Contas a receber.
Conciliação básica.
MVP 4 — Folha
CCT.
Eventos variáveis.
Cálculo de folha.
Holerite.
Encargos.
eSocial.
Custo por contrato/posto.
MVP 5 — Contabilidade
XML.
Extrato bancário.
Conciliação.
Lançamentos automáticos.
Plano de contas.
DRE.
Balancete.
Obrigações.
MVP 6 — IA avançada
Agente de edital.
Agente de contrato.
Agente de CCT.
Agente de glosa.
Agente fiscal.
Agente contábil.
Agente de e-mail.
RAG documental.
Autopiloto com aprovação.
15. Critérios de aceite do sistema

O sistema estará funcionalmente maduro quando conseguir:

Cadastrar uma licitação vencida com lotes, itens, planilha e contrato.
Criar postos com quantitativo, valor e escala.
Alocar colaboradores fixos e volantes.
Integrar ponto por AFD/AEJ e pelo menos dois conectores diretos.
Detectar faltas, atrasos e postos descobertos.
Acionar volante ou sugerir substituição.
Calcular glosa por falta e IMR.
Gerar medição mensal.
Emitir NFS-e de Curitiba via NFS-e Nacional.
Enviar a nota e anexos automaticamente por e-mail.
Calcular folha com base no ponto e CCT.
Gerar custo por contrato e posto.
Ingerir XMLs/notas e extratos.
Conciliar banco automaticamente.
Gerar lançamentos contábeis.
Produzir DRE por contrato, filial e grupo.
Registrar auditoria completa.
Permitir automação por IA com aprovação e rastreabilidade.
Usar Material Design 3 no frontend.
Usar AG Grid Quartz com Row Grouping e Pivot nas telas operacionais e analíticas.
16. Resumo da arquitetura final
Frontend
 ├── React + TypeScript
 ├── Material Design 3
 ├── AG Grid Enterprise Quartz
 ├── Row Grouping
 ├── Pivot
 └── PWA / Mobile supervisor

Backend
 ├── Kotlin + Spring Boot
 ├── PostgreSQL
 ├── Redis
 ├── Kafka/Redpanda
 ├── Temporal
 ├── OpenSearch
 ├── ClickHouse
 ├── MinIO/S3
 ├── Keycloak/Ory
 └── Vault

Integrações
 ├── NFS-e Nacional / Curitiba
 ├── eSocial
 ├── FGTS Digital
 ├── DCTFWeb/MIT
 ├── PNCP
 ├── Compras.gov.br
 ├── Relógios de ponto
 ├── Bancos
 ├── E-mail
 └── APIs de IA

IA
 ├── OpenAI
 ├── Anthropic
 ├── Gemini
 ├── Ollama
 ├── RAG
 ├── OCR
 ├── Agentes
 └── Human-in-the-loop configurável

Minha recomendação final: construir isso como uma plataforma modular, não como um ERP monolítico tradicional. O núcleo deve ser contrato → posto → colaborador → ponto → medição → glosa → NFS-e → folha → contabilidade. Esse encadeamento é o coração do produto.  ------ salve esse arquivo como spec.md na raiz do projeto e comece a implementação, crie um arquivo para colocar tudo que foi feito e as pendencias e implemente tudo comecando pelo backend e apis, por ultimo o frontend
</user_query>