/**
 * Employees / Colaboradores - Cadastro completo + Eventos de DP (para eSocial)
 * 
 * Agora muito mais robusto:
 * - Dados pessoais completos (CPF, RG, PIS, endereço, etc.)
 * - Data de admissão
 * - Histórico de eventos DP (Admissão, Demissão, Alteração salarial, Férias, etc.)
 */
import { useState } from 'react'
import {
  Box, Typography, Alert, Button, Stack, TextField, Paper,
  Tabs, Tab, Divider, Chip
} from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import {
  useEmployees,
  useCreateEmployee,
  useAssignEmployee,
  useEmployeeEvents,
  useRegisterEmployeeEvent
} from '../../api/hooks/useEmployees'
import { useContracts } from '../../api/hooks/useContracts'
import { useNotification } from '../../components/NotificationProvider'
import AdmissionQuickDialog from './AdmissionQuickDialog'
import { ColDef } from 'ag-grid-community'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { employeeSchema, type EmployeeForm } from '../../schemas/employeeSchema'
import { maskCPF, unmask } from '../../utils/masks'

const employeeColumns: ColDef[] = [
  { headerName: 'Nome', field: 'fullName', flex: 1 },
  { headerName: 'CPF', field: 'cpf' },
  { headerName: 'Cargo', field: 'cargo' },
  { headerName: 'Admissão', field: 'admissionDate' },
  { headerName: 'Status', field: 'status' },
]

export default function EmployeesPage() {
  const { showNotification } = useNotification()
  const { data: employees = [], isLoading, refetch } = useEmployees()
  const { data: contracts = [] } = useContracts()

  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('')
  const [activeTab, setActiveTab] = useState<'dados' | 'eventos' | 'alocacoes'>('dados')
  const [admissionDialogOpen, setAdmissionDialogOpen] = useState(false)

  const createEmployee = useCreateEmployee()
  const assignEmployee = useAssignEmployee()
  const registerEventMutation = useRegisterEmployeeEvent(selectedEmployeeId)

  // Formulário validado com react-hook-form + Zod para campos do employeeSchema
  const { register: regEmp, handleSubmit: handleSubmitEmp, control: controlEmp, reset: resetEmp, formState: { errors: empErrors } } = useForm<EmployeeForm>({
    resolver: zodResolver(employeeSchema),
    defaultValues: {
      fullName: '',
      cpf: '',
      admissionDate: '',
      cargo: '',
    },
  })

  // Campos extras não validados pelo schema mas necessários no formulário
  const [extraEmp, setExtraEmp] = useState({
    rg: '',
    pisNis: '',
    email: '',
    phone: '',
    dataNascimento: '',
    sexo: '',
    estadoCivil: '',
    nacionalidade: 'Brasileiro',

    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    bairro: '',
    cidade: '',
    uf: '',

    cbo: '',
    salarioBase: '',
    contractType: 'CLT',
    jornadaSemanal: '44',

    asoAdmissionalDate: '',
  })

  // Eventos de DP
  const { data: events = [], refetch: refetchEvents } = useEmployeeEvents(selectedEmployeeId || undefined)
  const [newEvent, setNewEvent] = useState({
    eventType: 'ADMISSION',
    eventDate: '',
    reason: '',
    newValue: '',
  })

  // Alocação (com Local e Município - essencial para terceirização e eSocial)
  const [assignForm, setAssignForm] = useState({ 
    contractId: '', 
    role: 'TITULAR',
    localTrabalho: '',
    municipioTrabalho: ''
  })

  const selectedEmployee = employees.find((e: any) => e.id === selectedEmployeeId)

  const onCreateEmployee = async (data: EmployeeForm) => {
    try {
      await createEmployee.mutateAsync({
        companyId: '',
        fullName: data.fullName,
        cpf: unmask(data.cpf),
        rg: extraEmp.rg || undefined,
        pisNis: extraEmp.pisNis || undefined,
        email: extraEmp.email || undefined,
        phone: extraEmp.phone || undefined,
        dataNascimento: extraEmp.dataNascimento || undefined,
        sexo: extraEmp.sexo || undefined,
        estadoCivil: extraEmp.estadoCivil || undefined,
        nacionalidade: extraEmp.nacionalidade || undefined,

        cep: extraEmp.cep || undefined,
        logradouro: extraEmp.logradouro || undefined,
        numero: extraEmp.numero || undefined,
        complemento: extraEmp.complemento || undefined,
        bairro: extraEmp.bairro || undefined,
        cidade: extraEmp.cidade || undefined,
        uf: extraEmp.uf || undefined,

        cargo: data.cargo || undefined,
        cbo: extraEmp.cbo || undefined,
        salarioBase: extraEmp.salarioBase ? parseFloat(extraEmp.salarioBase) : undefined,
        admissionDate: data.admissionDate || undefined,
        contractType: extraEmp.contractType || undefined,
        jornadaSemanal: extraEmp.jornadaSemanal ? parseInt(extraEmp.jornadaSemanal) : undefined,

        asoAdmissionalDate: extraEmp.asoAdmissionalDate || undefined,
      })
      showNotification('Colaborador cadastrado com sucesso! (pronto para eSocial)', 'success')
      resetEmp()
      setExtraEmp({
        rg: '', pisNis: '', email: '', phone: '', dataNascimento: '', sexo: '', estadoCivil: '', nacionalidade: 'Brasileiro',
        cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', uf: '',
        cbo: '', salarioBase: '', contractType: 'CLT', jornadaSemanal: '44',
        asoAdmissionalDate: ''
      })
      refetch()
    } catch (e: any) {
      showNotification(`Erro ao cadastrar: ${e.message}`, 'error')
    }
  }

  const handleRegisterEvent = async () => {
    if (!selectedEmployeeId || !newEvent.eventDate) return
    try {
      await registerEventMutation.mutateAsync({
        eventType: newEvent.eventType,
        eventDate: newEvent.eventDate,
        reason: newEvent.reason || undefined,
        newValue: newEvent.newValue ? parseFloat(newEvent.newValue) : undefined,
      })
      showNotification('Evento de DP registrado!', 'success')
      setNewEvent({ eventType: 'ADMISSION', eventDate: '', reason: '', newValue: '' })
      refetchEvents()
    } catch (e: any) {
      showNotification(e.message, 'error')
    }
  }

  const handleAssign = async () => {
    if (!selectedEmployeeId || !assignForm.contractId) return
    try {
      await assignEmployee.mutateAsync({
        employeeId: selectedEmployeeId,
        contractId: assignForm.contractId,
        role: assignForm.role,
        dataInicio: new Date().toISOString().slice(0, 10),
      })
      showNotification('Alocação realizada!', 'success')
    } catch (e: any) {
      showNotification(e.message, 'error')
    }
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom sx={{ mb: 0.5 }}>Colaboradores (DP)</Typography>
      <Typography color="text.secondary" mb={2}>
        Cadastro completo com eventos de Departamento Pessoal (essencial para eSocial S-2200, S-2205, S-2399...)
      </Typography>

      <Alert severity="warning" sx={{ mb: 2.25 }}>
        <strong>Atenção para eSocial:</strong> Preencha CPF, PIS, data de nascimento, admissão e registre eventos de admissão/demissão corretamente.
      </Alert>

      <Button 
        variant="contained" 
        sx={{ mb: 2 }}
        onClick={() => setAdmissionDialogOpen(true)}
      >
        + Admissão Rápida (Fluxo Guiado para eSocial)
      </Button>

      <AdmissionQuickDialog
        open={admissionDialogOpen}
        onClose={() => setAdmissionDialogOpen(false)}
        onSuccess={() => {
          refetch()
          setAdmissionDialogOpen(false)
        }}
      />

      <EnterpriseDataGrid
        title="Colaboradores"
        rowData={employees}
        columnDefs={employeeColumns}
        onRefresh={() => refetch()}
        loading={isLoading}
        onRowClicked={(params: any) => {
          if (params?.data?.id) {
            setSelectedEmployeeId(params.data.id)
            setActiveTab('dados')
          }
        }}
      />

      {/* Formulário de cadastro rico */}
      <Paper sx={{ p: { xs: 2, sm: 2.25 }, mt: 2, borderRadius: 2.5 }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>Cadastrar Novo Colaborador</Typography>
        {/* Formulário rico com seções */}
        <Typography variant="subtitle2" sx={{ mt: 0.5, color: 'primary.main', fontWeight: 600 }}>Dados Pessoais</Typography>
        <Stack direction="row" spacing={{ xs: 1.5, sm: 2 }} flexWrap="wrap" sx={{ mb: 1.5 }}>
          <TextField label="Nome Completo *" {...regEmp('fullName')} error={!!empErrors.fullName} helperText={empErrors.fullName?.message} sx={{ minWidth: 280 }} />
          <Controller
            name="cpf"
            control={controlEmp}
            render={({ field }) => (
              <TextField
                label="CPF *"
                value={maskCPF(field.value)}
                onChange={(e) => field.onChange(maskCPF(e.target.value))}
                error={!!empErrors.cpf}
                helperText={empErrors.cpf?.message}
                sx={{ width: 170 }}
              />
            )}
          />
          <TextField label="RG" value={extraEmp.rg} onChange={e => setExtraEmp({...extraEmp, rg: e.target.value})} sx={{ width: 150 }} />
          <TextField label="PIS/NIS" value={extraEmp.pisNis} onChange={e => setExtraEmp({...extraEmp, pisNis: e.target.value})} sx={{ width: 170 }} />
          <TextField label="Nascimento" type="date" value={extraEmp.dataNascimento} onChange={e => setExtraEmp({...extraEmp, dataNascimento: e.target.value})} InputLabelProps={{ shrink: true }} />
          <TextField label="Sexo" value={extraEmp.sexo} onChange={e => setExtraEmp({...extraEmp, sexo: e.target.value})} sx={{ width: 90 }} />
        </Stack>

        <Typography variant="subtitle2" sx={{ mt: 0.75, color: 'primary.main', fontWeight: 600 }}>Endereço</Typography>
        <Stack direction="row" spacing={{ xs: 1.5, sm: 2 }} flexWrap="wrap" sx={{ mb: 1.25 }}>
          <TextField label="CEP" value={extraEmp.cep} onChange={e => setExtraEmp({...extraEmp, cep: e.target.value})} sx={{ width: 130 }} />
          <TextField label="Logradouro" value={extraEmp.logradouro} onChange={e => setExtraEmp({...extraEmp, logradouro: e.target.value})} sx={{ minWidth: 220 }} />
          <TextField label="Nº" value={extraEmp.numero} onChange={e => setExtraEmp({...extraEmp, numero: e.target.value})} sx={{ width: 80 }} />
          <TextField label="Cidade" value={extraEmp.cidade} onChange={e => setExtraEmp({...extraEmp, cidade: e.target.value})} sx={{ width: 160 }} />
          <TextField label="UF" value={extraEmp.uf} onChange={e => setExtraEmp({...extraEmp, uf: e.target.value})} sx={{ width: 70 }} />
        </Stack>

        <Typography variant="subtitle2" sx={{ mt: 0.75, color: 'primary.main', fontWeight: 600 }}>Contrato + Saúde Ocupacional</Typography>
        <Stack direction="row" spacing={{ xs: 1.5, sm: 2 }} flexWrap="wrap">
          <TextField label="Cargo" {...regEmp('cargo')} error={!!empErrors.cargo} helperText={empErrors.cargo?.message} />
          <TextField label="CBO" value={extraEmp.cbo} onChange={e => setExtraEmp({...extraEmp, cbo: e.target.value})} />
          <TextField label="Salário Base" type="number" value={extraEmp.salarioBase} onChange={e => setExtraEmp({...extraEmp, salarioBase: e.target.value})} />
          <TextField label="Data Admissão *" type="date" {...regEmp('admissionDate')} InputLabelProps={{ shrink: true }} error={!!empErrors.admissionDate} helperText={empErrors.admissionDate?.message} />
          <TextField label="ASO Admissional" type="date" value={extraEmp.asoAdmissionalDate} onChange={e => setExtraEmp({...extraEmp, asoAdmissionalDate: e.target.value})} InputLabelProps={{ shrink: true }} />
          <Button variant="contained" onClick={handleSubmitEmp(onCreateEmployee)} disabled={createEmployee.isPending}>
            Cadastrar + Gerar Admissão eSocial
          </Button>
        </Stack>
      </Paper>

      {/* Detalhe do colaborador selecionado */}
      {!selectedEmployeeId && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Clique em uma linha da tabela acima para ver os detalhes completos do colaborador, eventos e alocações.
        </Alert>
      )}

      {selectedEmployeeId && selectedEmployee && (
        <Paper sx={{ p: { xs: 2, sm: 2.25 }, mt: 2, borderRadius: 2.5 }}>
          <Typography variant="h6" gutterBottom>
            {selectedEmployee.fullName} <Chip label={selectedEmployee.status} size="small" sx={{ ml: 1 }} />
          </Typography>

          <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} sx={{ mb: 1.5 }}>
            <Tab label="Dados Pessoais" value="dados" />
            <Tab label="Eventos DP (eSocial)" value="eventos" />
            <Tab label="Alocações" value="alocacoes" />
          </Tabs>

          {activeTab === 'dados' && (
            <Stack spacing={1.25}>
              <Typography variant="body2"><strong>CPF:</strong> {selectedEmployee.cpf} &nbsp;&nbsp;&nbsp; <strong>PIS:</strong> {selectedEmployee.pisNis || '—'}</Typography>
              <Typography variant="body2"><strong>Nascimento:</strong> {selectedEmployee.dataNascimento || '—'} &nbsp;&nbsp;&nbsp; <strong>Sexo/Estado Civil:</strong> {selectedEmployee.sexo || '—'} / {selectedEmployee.estadoCivil || '—'}</Typography>
              <Typography variant="body2"><strong>Endereço:</strong> {selectedEmployee.logradouro || ''} {selectedEmployee.numero || ''} — {selectedEmployee.cidade || ''}/{selectedEmployee.uf || ''}</Typography>
              <Typography variant="body2"><strong>Cargo / CBO:</strong> {selectedEmployee.cargo || '—'} ({selectedEmployee.cbo || '—'})</Typography>
              <Typography variant="body2"><strong>Admissão:</strong> {selectedEmployee.admissionDate || '—'} &nbsp;&nbsp;&nbsp; <strong>Salário Base:</strong> {selectedEmployee.salarioBase ? `R$ ${selectedEmployee.salarioBase.toLocaleString('pt-BR')}` : '—'}</Typography>
              <Typography variant="body2"><strong>ASO Admissional:</strong> {selectedEmployee.asoAdmissionalDate || '—'}</Typography>
            </Stack>
          )}

          {activeTab === 'eventos' && (
            <Box>
              <Typography variant="subtitle2" gutterBottom>Histórico de Eventos de DP</Typography>
              <EnterpriseDataGrid
                title=""
                rowData={events}
                columnDefs={[
                  { headerName: 'Data', field: 'eventDate' },
                  { headerName: 'Tipo', field: 'eventType' },
                  { headerName: 'Motivo', field: 'reason' },
                  { headerName: 'Valor Anterior → Novo', field: 'newValue' },
                ]}
                height={220}
              />

              <Divider sx={{ my: 2 }} />

              <Typography variant="subtitle2" gutterBottom>Registrar Novo Evento de DP</Typography>
              <Stack direction="row" spacing={2} flexWrap="wrap" alignItems="flex-end">
                <TextField 
                  select 
                  label="Tipo de Evento *" 
                  value={newEvent.eventType} 
                  onChange={e => setNewEvent({...newEvent, eventType: e.target.value})}
                  sx={{ minWidth: 200 }}
                >
                  {[
                    'ADMISSION', 
                    'TERMINATION', 
                    'SALARY_CHANGE', 
                    'PROMOTION',
                    'VACATION_START', 
                    'VACATION_END', 
                    'LEAVE', 
                    'RETURN_FROM_LEAVE',
                    'SUSPENSION',
                    'RESCISION'
                  ].map(t => <option key={t} value={t}>{t}</option>)}
                </TextField>

                <TextField label="Data do Evento *" type="date" value={newEvent.eventDate} onChange={e => setNewEvent({...newEvent, eventDate: e.target.value})} InputLabelProps={{ shrink: true }} />

                {/* Campos condicionais por tipo de evento */}
                {newEvent.eventType === 'TERMINATION' && (
                  <TextField label="Motivo da Demissão" value={newEvent.reason} onChange={e => setNewEvent({...newEvent, reason: e.target.value})} sx={{ minWidth: 220 }} />
                )}
                {newEvent.eventType === 'SALARY_CHANGE' && (
                  <TextField label="Novo Salário" type="number" value={newEvent.newValue} onChange={e => setNewEvent({...newEvent, newValue: e.target.value})} sx={{ width: 140 }} />
                )}
                {newEvent.eventType === 'VACATION_START' && (
                  <TextField label="Observação / Período" value={newEvent.reason} onChange={e => setNewEvent({...newEvent, reason: e.target.value})} sx={{ minWidth: 220 }} />
                )}

                <TextField label="Descrição / Motivo" value={newEvent.reason} onChange={e => setNewEvent({...newEvent, reason: e.target.value})} sx={{ minWidth: 260 }} />
                <Button variant="contained" onClick={handleRegisterEvent}>Registrar Evento</Button>
              </Stack>
              <Typography variant="caption" color="text.secondary">
                Eventos de ADMISSION e TERMINATION são críticos para os eventos eSocial (S-2200 / S-2299).
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
                Eventos de admissão e demissão são críticos para geração correta dos eventos eSocial.
              </Typography>
            </Box>
          )}

          {activeTab === 'alocacoes' && (
            <Stack spacing={2}>
              <Typography variant="subtitle2">Alocar em Contrato / Posto</Typography>
              <Stack direction="row" spacing={2}>
                <TextField select label="Contrato / Lote" value={assignForm.contractId} onChange={e => setAssignForm({...assignForm, contractId: e.target.value})} sx={{ minWidth: 280 }}>
                  {contracts.map((c: any) => <option key={c.id} value={c.id}>{c.numero} — {c.orgao}</option>)}
                </TextField>
                <TextField select label="Função" value={assignForm.role} onChange={e => setAssignForm({...assignForm, role: e.target.value})}>
                  <option value="TITULAR">TITULAR</option>
                  <option value="VOLANTE">VOLANTE</option>
                  <option value="RESERVA">RESERVA</option>
                </TextField>
                <TextField label="Local de Trabalho" value={assignForm.localTrabalho} onChange={e => setAssignForm({...assignForm, localTrabalho: e.target.value})} sx={{ minWidth: 200 }} />
                <TextField label="Município de Trabalho" value={assignForm.municipioTrabalho} onChange={e => setAssignForm({...assignForm, municipioTrabalho: e.target.value})} sx={{ minWidth: 180 }} />
                <Button variant="contained" onClick={handleAssign}>Alocar no Posto/Lote</Button>
              </Stack>
            </Stack>
          )}
        </Paper>
      )}
    </Box>
  )
}
