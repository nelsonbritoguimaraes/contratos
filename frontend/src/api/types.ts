/**
 * TypeScript types matching the ContractOps backend DTOs (Kotlin/Spring).
 * Keep this in sync with backend DTOs as the API evolves.
 */

// ====================== COMMON ======================
export type UUID = string

export interface WinningSpreadsheetSummary {
  id: UUID
  versao: number
  isVencedora: boolean
  arquivoNome?: string
}

// ====================== CONTRACT ======================
export interface Contract {
  id: UUID
  tenantId: UUID
  companyId: UUID
  branchId?: UUID
  biddingId?: UUID
  winningSpreadsheetId?: UUID
  winningSpreadsheet?: WinningSpreadsheetSummary
  numero: string
  orgao: string
  cnpjOrgao?: string
  objeto?: string
  vigenciaInicio?: string
  vigenciaFim?: string
  valorMensal?: number
  valorGlobal?: number
  status: 'ATIVO' | 'SUSPENSO' | 'EM_IMPLANTACAO' | 'ENCERRADO' | string
  qtdPostosContratados: number
  prepostoNome?: string
  gestorOrgao?: string
  fiscalTecnico?: string
  fiscalAdministrativo?: string
  regrasGlosa?: string
  regrasSubstituicao?: string
  regrasUniforme?: string
  regrasEquipamentos?: string
  regrasFaturamento?: string
  regrasPonto?: string
  regrasMedicao?: string
  createdAt?: string
  updatedAt?: string
}

export interface CreateContractRequest {
  companyId: UUID
  branchId?: UUID
  biddingId?: UUID
  winningSpreadsheetId?: UUID
  numero: string
  orgao: string
  cnpjOrgao?: string
  objeto?: string
  vigenciaInicio?: string
  vigenciaFim?: string
  valorMensal?: number
  valorGlobal?: number
  status?: string
}

export interface UpdateContractRequest {
  numero?: string
  orgao?: string
  cnpjOrgao?: string
  objeto?: string
  vigenciaInicio?: string
  vigenciaFim?: string
  valorMensal?: number
  valorGlobal?: number
  status?: string
  prepostoNome?: string
  gestorOrgao?: string
  fiscalTecnico?: string
  fiscalAdministrativo?: string
}

// ====================== SERVICE POST ======================
export interface ServicePost {
  id: UUID
  tenantId: UUID
  contractId: UUID
  codigo?: string
  nome: string
  funcao?: string
  cbo?: string
  escala?: string
  jornadaHoras?: number
  valorMensal?: number
  valorDiario?: number
  status: string
  titularNome?: string
  createdAt?: string
}

export interface CreatePostRequest {
  nome: string
  codigo?: string
  funcao?: string
  escala?: string
  cbo?: string
  jornadaHoras?: number
  valorMensal?: number
  valorDiario?: number
  titularNome?: string
}

export interface UpdatePostRequest {
  nome?: string
  codigo?: string
  funcao?: string
  escala?: string
  valorMensal?: number
  status?: string
}

// ====================== CONTRACT LOT ======================
export interface ContractLot {
  id: UUID
  contractId: UUID
  originalBiddingLotId?: UUID
  numeroLote?: string
  descricao?: string
  quantitativoPostos: number
  valorMensal?: number
  valorGlobal?: number
}

export interface CreateContractLotRequest {
  numeroLote?: string
  descricao?: string
  quantitativoPostos?: number
  valorMensal?: number
  valorGlobal?: number
  originalBiddingLotId?: UUID
}

// ====================== BIDDING (LICITAÇÃO) ======================
export interface Bidding {
  id: UUID
  tenantId: UUID
  processoNumero?: string
  editalNumero?: string
  modalidade?: string
  portalOrigem?: string
  orgao: string
  cnpjOrgao?: string
  objeto?: string
  dataPublicacao?: string
  dataSessao?: string
  dataHomologacao?: string
  dataAdjudicacao?: string
  valorEstimado?: number
  valorVencedor?: number
  status: string
  fonteRecurso?: string
  createdAt?: string
  // Campos extras usados no BiddingDetailDialog (Edital + Empresa Vencedora)
  editalUrl?: string
  vencedorEmpresa?: string
}

export interface CreateBiddingRequest {
  processoNumero?: string
  editalNumero?: string
  modalidade?: string
  portalOrigem?: string
  orgao: string
  cnpjOrgao?: string
  objeto?: string
  dataPublicacao?: string
  dataSessao?: string
  dataHomologacao?: string
  dataAdjudicacao?: string
  valorEstimado?: number
  valorVencedor?: number
  status?: string
  fonteRecurso?: string
}

export interface BiddingListItem {
  id: UUID
  editalNumero?: string
  orgao: string
  status: string
  valorVencedor?: number
  dataHomologacao?: string
}

// ====================== BIDDING LOT ======================
export interface BiddingLot {
  id: UUID
  biddingId: UUID
  numeroLote?: string
  descricao?: string
  quantitativoPostos: number
  valorMensal?: number
  valorAnual?: number
  valorGlobal?: number
  prazoMeses?: number
}

// Posto Planejado dentro de uma Licitação (antes de virar contrato)
// Essencial para empresa de terceirização
export interface BiddingPosto {
  id?: UUID
  biddingLotId: UUID
  codigo?: string
  nome: string
  funcao?: string
  cbo?: string
  escala?: string
  jornadaHoras?: number
  valorMensal?: number
  localExecucao?: string          // Local de Trabalho
  municipioExecucao?: string      // Município de Trabalho (crítico para eSocial)
  quantidade?: number
}

export interface CreateBiddingLotRequest {
  numeroLote?: string
  descricao?: string
  quantitativoPostos?: number
  valorMensal?: number
  valorAnual?: number
  valorGlobal?: number
  prazoMeses?: number
}

// ====================== WINNING SPREADSHEET ======================
export interface WinningSpreadsheet {
  id: UUID
  biddingId?: UUID
  contractId?: UUID
  versao: number
  arquivoNome?: string
  arquivoUrl?: string
  memoriaCalculo?: string
  isVencedora: boolean
  createdAt?: string
}

export interface CreateWinningSpreadsheetRequest {
  biddingId?: UUID
  contractId?: UUID
  versao?: number
  arquivoNome?: string
  arquivoUrl?: string
  memoriaCalculo?: string
  isVencedora?: boolean
}

// ====================== PAYSLIP / FOLHA (Integração RH → Financeiro → Contabilidade) ======================
export interface PayslipCloseResponse {
  contractId?: string
  competence: string
  holeritesAprovados: number
  message: string
}

export interface GlobalCloseCompetenceResponse {
  competence: string
  holeritesAprovados: number
  message: string
}

export interface EncargosSummaryResponse {
  contractId: string
  competence: string
  totalHoleritesAprovados: number
  encargosEstimados: {
    INSS: number
    FGTS: number
    totalEncargos: number
  }
  observacao?: string
}

// ====================== CONTRACT AMENDMENT (Aditivos / Repactuação) ======================
export interface ContractAmendment {
  id: UUID
  tenantId: UUID
  contractId: UUID
  tipo: 'PRORROGACAO' | 'ACRESCIMO' | 'REPCTUACAO' | 'REAJUSTE' | 'REEQUILIBRIO' | string
  numero?: string
  dataAssinatura?: string
  vigenciaAnteriorFim?: string
  vigenciaNovaFim?: string
  valorAnterior?: number
  valorNovo?: number
  descricao?: string
  justificativa?: string
  status: string
  createdAt?: string
}

export interface CreateAmendmentRequest {
  tipo: string
  numero?: string
  dataAssinatura?: string
  vigenciaNovaFim?: string
  valorNovo?: number
  descricao?: string
  justificativa?: string
}

// ============================================================
// FINANCEIRO ENTERPRISE (CFO LITERAL) — Alinhado 100% com FinanceiroController + Dtos
// ============================================================

export interface ContaBancaria {
  id: UUID
  tenantId: UUID
  bancoCodigo: string
  bancoNome: string
  agencia: string
  conta: string
  tipo: 'CORRENTE' | 'POUPANCA' | 'APLICACAO' | string
  saldoAtual: number
  ativa: boolean
  contaContabilId?: UUID
  observacoes?: string
}

export interface CriarContaBancariaRequest {
  bancoCodigo: string
  bancoNome: string
  agencia: string
  conta: string
  tipo?: string
  contaContabilId?: UUID
  observacoes?: string
}

export interface ContaAReceber {
  id: UUID
  tenantId: UUID
  contratoId?: UUID
  measurementId?: UUID
  nfsId?: UUID
  valorBruto: number
  valorLiquido: number
  vencimento: string
  status: 'ABERTO' | 'PARCIAL' | 'PAGO' | 'VENCIDO' | string
  diasAtraso: number
  jurosMulta?: number
  observacoes?: string
}

export interface ContaAPagar {
  id: UUID
  tenantId: UUID
  origem: 'PAYSLIP' | 'TRIBUTO' | 'FORNECEDOR' | 'UNIFORME' | string
  contratoId?: UUID
  valor: number
  vencimento: string
  status: 'PENDENTE' | 'APROVADO' | 'PAGO' | 'CANCELADO' | string
  dataPagamento?: string
  formaPagamento?: string
  nivelAprovacao?: number
}

export interface NotaFiscalServico {
  id: UUID
  tenantId: UUID
  numero: string
  serie?: string
  dataEmissao: string
  tomadorCnpj: string
  valorServicos: number
  valorLiquido: number
  issRetido: number
  outrasRetencoes: number
  status: 'EMITIDA' | 'CANCELADA' | 'SUBSTITUIDA' | string
  xml?: string
  measurementId?: UUID
  contratoId: UUID
}

export interface EmitirNfsRequest {
  measurementId: UUID
  contratoId: UUID
  tomadorCnpj: string
  valorServicos: number
}

export interface CfoDashboardResponse {
  dataCorte: string
  posicaoCaixa: Record<string, any>
  kpis: Record<string, any>
  alertas: string[]
  agingAR?: Record<string, number>
  agingAP?: Record<string, number>
  proximosVencimentos?: any[]
}

export interface FluxoCaixaProjetadoResponse {
  horizonteSemanas: number
  cenario: string
  dataInicio: string
  dataFim: string
  entradasProjetadas: number
  saidasProjetadas: number
  saldoProjetado: number
  previsoes: Array<{
    data: string
    tipo: string
    valor: number
    probabilidade?: number
  }>
}

export interface SimulacaoRequest {
  atrasoMedioRecebimento?: number
  aumentoFolha?: number
  reducaoFaturamento?: number
  [key: string]: any
}

export interface AgingReport {
  buckets: Record<string, { count: number; valor: number }>
  totalAberto: number
}

// Real DTOs returned by /financeiro/relatorios/aging/{ar,ap} (backend AgingReportService.AgingReport)
export interface AgingBucket {
  faixa: string
  quantidade: number
  valor: number
}

export interface AgingReportDTO {
  tipo: string
  dataCorte: string
  buckets: AgingBucket[]
  total: number
}

// ============================================================
// IA — 10 AGENTS + ORCHESTRATOR + /ask (Alinhado com IaController)
// ============================================================

export interface IaAskResponse {
  question: string
  routed_agents: string[]
  answers: Record<string, string>
}

export interface IaCallLog {
  id?: UUID
  timestamp: string
  provider: string
  promptPreview: string
  responsePreview?: string
  tenantId?: UUID
  routedAgents?: string[]
  costEstimate?: number
}

export interface IaDashboardSummary {
  totalCalls: number
  callsByAgent: Record<string, number>
  costEstimate: number
  guardrailBlocks: number
}

// ============================================================
// EMPLOYEES + ASSIGNMENTS (Alinhado com EmployeeController + EmployeeAssignment)
// ============================================================

export interface Employee {
  id: UUID
  tenantId: UUID
  companyId: UUID
  branchId?: UUID

  // === DADOS PESSOAIS (S-2200 eSocial) ===
  fullName: string
  cpf: string
  rg?: string
  pisNis?: string
  email?: string
  phone?: string
  dataNascimento?: string
  sexo?: 'M' | 'F' | string
  estadoCivil?: string
  nacionalidade?: string

  // Endereço completo
  cep?: string
  logradouro?: string
  numero?: string
  complemento?: string
  bairro?: string
  cidade?: string
  uf?: string

  // === DADOS CONTRATUAIS ===
  cargo?: string
  cbo?: string
  salarioBase?: number
  admissionDate?: string
  contractType?: string // CLT, PJ, ESTAGIARIO, etc.
  jornadaSemanal?: number

  // Dados bancários
  banco?: string
  agencia?: string
  conta?: string
  tipoConta?: string

  // === DADOS DE SAÚDE E SEGURANÇA (importante para eSocial) ===
  asoAdmissionalDate?: string
  asoPeriodicoDate?: string
  dataUltimoExame?: string

  // Dependentes (simplificado para início)
  qtdDependentes?: number

  status: string
  createdAt?: string
  updatedAt?: string

  // Controle de Jornada (essencial para terceirização)
  maxJornadaPercentual?: number;   // ex: 100% por padrão, pode ser menor para alguns
  jornadaAtualPercentual?: number; // calculado a partir das alocações
}

export interface EmployeeAssignment {
  id: UUID
  tenantId: UUID
  employeeId: UUID
  contractId: UUID
  postId?: UUID
  role: 'TITULAR' | 'VOLANTE' | 'RESERVA' | 'SUPERVISOR' | string
  // Backend DTO uses startDate / endDate / isActive (from EmployeeAssignmentResponse)
  startDate?: string
  endDate?: string
  isActive?: boolean
  // Legacy/compat aliases (some UI code may use)
  dataInicio?: string
  dataFim?: string
  ativo?: boolean
}

export interface CreateEmployeeRequest {
  companyId: UUID
  branchId?: UUID
  fullName: string
  cpf: string
  rg?: string
  pisNis?: string
  email?: string
  phone?: string
  dataNascimento?: string
  sexo?: string
  estadoCivil?: string
  nacionalidade?: string

  // Endereço
  cep?: string
  logradouro?: string
  numero?: string
  complemento?: string
  bairro?: string
  cidade?: string
  uf?: string

  cargo?: string
  cbo?: string
  salarioBase?: number
  admissionDate?: string
  contractType?: string
  jornadaSemanal?: number

  // Saúde
  asoAdmissionalDate?: string
}

export interface UpdateEmployeeRequest extends Partial<CreateEmployeeRequest> {}

export interface AssignEmployeeRequest {
  employeeId: UUID
  contractId: UUID
  postId?: UUID
  role: string
  // Accept either; hook normalizes to backend startDate
  dataInicio?: string
  startDate?: string
}

// ============================================================
// EMPLOYEE EVENTS - DP (Admissão, Demissão, Férias, Afastamento, etc.)
// Essencial para eSocial
// ============================================================

export interface EmployeeEvent {
  id: UUID
  tenantId: UUID
  employeeId: UUID
  contractId?: UUID
  postId?: UUID // Link to specific Posto/Lote from Bidding
  localTrabalho?: string
  municipioTrabalho?: string

  eventType: 
    | 'ADMISSION' 
    | 'TERMINATION' 
    | 'SALARY_CHANGE' 
    | 'PROMOTION' 
    | 'VACATION_START' 
    | 'VACATION_END' 
    | 'LEAVE' 
    | 'RETURN_FROM_LEAVE' 
    | 'SUSPENSION'
    | 'RESCISION'
    | string

  eventDate: string
  description?: string
  previousValue?: number
  newValue?: number
  reason?: string
  documentReference?: string
  affectsPayrollFrom?: string

  // Campos específicos por tipo de evento (para eSocial e terceirização)
  // Para TERMINATION / RESCISION
  motivoDemissao?: string
  tipoAvisoPrevio?: 'TRABALHADO' | 'INDENIZADO' | 'NAO_APLICAVEL'
  dataHomologacao?: string

  // Para ADMISSION
  tipoAdmissao?: string
  categoriaTrabalhador?: string

  // Geral para terceirização
  substituiFuncionarioId?: UUID
  observacaoSubstituicao?: string
}

export interface CreateEmployeeEventRequest {
  eventType: string
  eventDate: string
  contractId?: UUID
  postId?: UUID
  localTrabalho?: string
  municipioTrabalho?: string
  description?: string
  previousValue?: number
  newValue?: number
  reason?: string
  documentReference?: string
  affectsPayrollFrom?: string

  // Campos específicos
  motivoDemissao?: string
  tipoAvisoPrevio?: string
  dataHomologacao?: string
  tipoAdmissao?: string
  categoriaTrabalhador?: string
  substituiFuncionarioId?: UUID
  observacaoSubstituicao?: string
}

// ============================================================
// RH / FOLHA (Alinhado com PayslipController, EsocialController, RhDashboard)
// ============================================================

export interface Payslip {
  id: UUID
  employeeId: UUID
  contractId: UUID
  competence: string
  baseSalary: number
  totalEarnings: number
  totalDeductions: number
  netAmount: number
  status: 'DRAFT' | 'CALCULATED' | 'APPROVED' | 'CLOSED' | string
}

export interface RhDashboardResponse {
  competence: string
  resumoFolha: {
    totalHolerites: number
    totalProventos: number
    totalDescontos: number
    totalLiquido: number
    holeritesPorStatus: Record<string, number>
  }
  eSocial: {
    pendentes: number
    gerados: number
    enviados: number
  }
  rubricasAtivas: number
}

export interface EsocialEvent {
  id: UUID
  tenantId: UUID
  employeeId: UUID
  eventType: string
  competencia?: string
  status: 'PENDING' | 'SENT' | 'ACCEPTED' | 'REJECTED' | string
  xml?: string
  createdAt: string
}

// ============================================================
// MEDIÇÃO + GLOSA (já parcialmente mapeado, reforçado)
// ============================================================

export interface Measurement {
  id: UUID
  contractId: UUID
  period: string
  baseValue: number
  glosaTotal: number
  finalAmount: number
  status: 'DRAFT' | 'CALCULATED' | 'APPROVED' | 'INVOICED' | string
  notes?: string
}

export interface Glosa {
  id: UUID
  contractId: UUID
  measurementPeriod: string
  glosaType: string
  description: string
  glosaAmount: number
  status: string
}

// ============================================================
// CONTABILIDADE (Alinhado com ContabilidadeController + Sped)
// ============================================================

export interface ContaContabil {
  id: UUID
  tenantId: UUID
  codigo: string
  descricao: string
  tipo: 'ATIVO' | 'PASSIVO' | 'PATRIMONIO_LIQUIDO' | 'RECEITA' | 'DESPESA' | string
  natureza: 'DEVEDORA' | 'CREDORA' | string
  nivel: number
  aceitaLancamento: boolean
  contaMaeId?: UUID
}

export interface LancamentoContabil {
  id: UUID
  tenantId: UUID
  data: string
  contaDebitoId: UUID
  contaCreditoId: UUID
  valor: number
  historico?: string
  origemTipo?: string
  origemId?: UUID
  contratoId?: UUID
}

export interface DreResponse {
  contratoId: UUID
  periodo: string
  receitaBruta: number
  deducoes: number
  receitaLiquida: number
  custos: number
  lucroBruto: number
  despesas: number
  lucroOperacional: number
  outros: number
  lucroLiquido: number
  margem: number
}

// ============================================================
// COMUNS / UTILITÁRIOS
// ============================================================

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
}

// Rubrica avançada com regras condicionais (usada no módulo Folha)
export interface Rubrica {
  id: string
  descricao: string
  tipo: 'PROVENTO' | 'DESCONTO'
  percentual: number
  base: 'SALARIO_BASE' | 'TOTAL_PROVENTOS'
  posto: string
  regra: string
  condicao?: 'SEMPRE' | 'ESCALA_NOTURNA' | 'TEMPO_MINIMO_3M' | 'INSALUBRE' | 'PERICULOSIDADE' | 'ADICIONAL_FUNCAO' | 'CATEGORIA_ESOCIAL'
}

export type CenarioFluxo = 'BASE' | 'OTIMISTA' | 'PESSIMISTA'
export type StatusAprovacao = 'PENDENTE' | 'APROVADO_NIVEL_1' | 'APROVADO_NIVEL_2' | 'REJEITADO'
