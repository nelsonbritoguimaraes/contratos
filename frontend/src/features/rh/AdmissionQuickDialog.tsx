/**
 * Admissão Rápida - Fluxo guiado para cadastrar colaborador + registrar evento de ADMISSION automaticamente.
 * Essencial para eSocial (S-2200).
 */
import { useState } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button,
  TextField, Stack, Stepper, Step, StepLabel, Alert
} from '@mui/material'
import { useCreateEmployee } from '../../api/hooks/useEmployees'
import { useNotification } from '../../components/NotificationProvider'
import { useTenant } from '../../api/hooks/useTenant'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess?: () => void
}

export default function AdmissionQuickDialog({ open, onClose, onSuccess }: Props) {
  const { showNotification } = useNotification()
  const createEmployee = useCreateEmployee()
  const { tenantId } = useTenant()

  const [activeStep, setActiveStep] = useState(0)
  const [employeeData, setEmployeeData] = useState({
    fullName: '',
    cpf: '',
    pisNis: '',
    dataNascimento: '',
    cargo: '',
    salarioBase: '',
    admissionDate: new Date().toISOString().slice(0, 10),
  })

  const steps = ['Dados Pessoais', 'Dados Contratuais', 'Confirmar Admissão'];

  const handleNext = () => setActiveStep((prev) => prev + 1)
  const handleBack = () => setActiveStep((prev) => prev - 1)

  const handleCreateAndAdmit = async () => {
    try {
      await createEmployee.mutateAsync({
        companyId: tenantId || '',
        fullName: employeeData.fullName,
        cpf: employeeData.cpf,
        pisNis: employeeData.pisNis || undefined,
        dataNascimento: employeeData.dataNascimento || undefined,
        cargo: employeeData.cargo || undefined,
        salarioBase: employeeData.salarioBase ? parseFloat(employeeData.salarioBase) : undefined,
        admissionDate: employeeData.admissionDate,
      })

      showNotification('Colaborador criado! Evento de ADMISSÃO registrado automaticamente para eSocial.', 'success')

      onSuccess?.()
      onClose()
      setActiveStep(0)
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro ao criar colaborador'
      showNotification(message, 'error')
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth PaperProps={{ sx: { maxHeight: '92dvh' } }}>
      <DialogTitle>Admissão Rápida de Colaborador</DialogTitle>
      <DialogContent sx={{ overflowY: 'auto' }}>
        <Stepper activeStep={activeStep} sx={{ mb: 2.5 }}>
          {steps.map((label) => <Step key={label}><StepLabel>{label}</StepLabel></Step>)}
        </Stepper>

        {activeStep === 0 && (
          <Stack spacing={2}>
            <TextField label="Nome Completo *" value={employeeData.fullName} onChange={e => setEmployeeData({...employeeData, fullName: e.target.value})} />
            <Stack direction="row" spacing={2}>
              <TextField label="CPF *" value={employeeData.cpf} onChange={e => setEmployeeData({...employeeData, cpf: e.target.value})} />
              <TextField label="PIS/NIS" value={employeeData.pisNis} onChange={e => setEmployeeData({...employeeData, pisNis: e.target.value})} />
            </Stack>
            <TextField label="Data de Nascimento" type="date" value={employeeData.dataNascimento} onChange={e => setEmployeeData({...employeeData, dataNascimento: e.target.value})} InputLabelProps={{ shrink: true }} />
          </Stack>
        )}

        {activeStep === 1 && (
          <Stack spacing={2}>
            <TextField label="Cargo" value={employeeData.cargo} onChange={e => setEmployeeData({...employeeData, cargo: e.target.value})} />
            <TextField label="Salário Base" type="number" value={employeeData.salarioBase} onChange={e => setEmployeeData({...employeeData, salarioBase: e.target.value})} />
            <TextField label="Data de Admissão *" type="date" value={employeeData.admissionDate} onChange={e => setEmployeeData({...employeeData, admissionDate: e.target.value})} InputLabelProps={{ shrink: true }} />
          </Stack>
        )}

        {activeStep === 2 && (
          <Alert severity="info">
            Ao confirmar, o sistema vai:<br />
            1. Criar o cadastro do colaborador<br />
            2. Registrar automaticamente o evento de <strong>ADMISSÃO</strong> (S-2200 para eSocial)
          </Alert>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        {activeStep > 0 && <Button onClick={handleBack}>Voltar</Button>}
        {activeStep < 2 && <Button variant="contained" onClick={handleNext}>Avançar</Button>}
        {activeStep === 2 && (
          <Button variant="contained" color="success" onClick={handleCreateAndAdmit} disabled={createEmployee.isPending}>
            Confirmar Admissão e Gerar eSocial
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}
