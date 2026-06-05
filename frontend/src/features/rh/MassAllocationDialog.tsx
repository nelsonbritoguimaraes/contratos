/**
 * MassAllocationDialog — Ligação em massa de Funcionários aos Postos de uma Licitação
 * 
 * Pensado para empresa de terceirização de mão de obra:
 * - Seleciona uma Licitação + Lote
 * - Mostra os Postos planejados daquela licitação (com Local e Município)
 * - Lista funcionários disponíveis (filtrados por função/CBO quando possível)
 * - Permite alocação em massa (assign multiple employees to multiple posts)
 */
import { useState, useMemo } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography, Stack, 
  TextField, Paper, Alert, Chip, Box, LinearProgress, MenuItem
} from '@mui/material'
import { X, UserPlus, CheckCircle2, AlertTriangle } from 'lucide-react'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import { useContracts } from '../../api/hooks/useContracts'
import { useBiddingPostos } from '../../api/hooks/useBiddings'
import { useEmployees, useAssignEmployee } from '../../api/hooks/useEmployees'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess?: () => void
  biddingId?: string
}

export default function MassAllocationDialog({ open, onClose, onSuccess, biddingId }: Props) {
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()
  const { data: apiPostos = [] } = useBiddingPostos(biddingId)
  const { data: allEmployees = [] } = useEmployees()
  const assignEmployee = useAssignEmployee()

  const [selectedBiddingId, setSelectedBiddingId] = useState('')
  const [selectedLotId, setSelectedLotId] = useState('')
  const [allocationType, setAllocationType] = useState<'TITULAR' | 'VOLANTE'>('TITULAR')
  const [filterFuncao, setFilterFuncao] = useState('')
  const [selectedEmployeeIds, setSelectedEmployeeIds] = useState<string[]>([])

  // === MODELO ROBUSTO: Alocação INDIVIDUAL por Posto específico ===
  // assignmentsByPosto = { "p1": ["emp-123", "emp-456"], ... }
  const [assignmentsByPosto, setAssignmentsByPosto] = useState<Record<string, string[]>>({})

  // Jornada temporária desta sessão (para preview em tempo real)
  const [tempJornada, setTempJornada] = useState<Record<string, number>>({})

  const plannedPosts = useMemo(() => {
    if (apiPostos.length > 0) {
      return apiPostos.map((p: any) => ({
        id: p.id,
        nome: p.nome,
        funcao: p.funcao,
        cbo: p.cbo,
        localExecucao: p.localExecucao,
        municipioExecucao: p.municipioExecucao,
        valorMensal: p.valorMensal,
        escala: p.escala,
      }))
    }
    return []
  }, [apiPostos])

  // Enriquecer funcionários com jornada (campos reais + fallback local para preview)
  const employees = useMemo(() => allEmployees.map((emp: any) => ({
    ...emp,
    jornadaAlocada: emp.jornadaAtualPercentual ?? emp.jornadaAlocada ?? 0,
    maxJornadaPercentual: emp.maxJornadaPercentual ?? 100,
    cargo: emp.cargo || emp.funcao || 'Operacional',
  })), [allEmployees])

  // Funcionários disponíveis (jornada atual < max)
  const availableEmployees = useMemo(() => 
    employees.filter((emp: any) => (emp.jornadaAlocada || 0) < (emp.maxJornadaPercentual || 100)),
  [employees])

  // Filtragem por função
  const filteredEmployees = useMemo(() => {
    if (!filterFuncao) return availableEmployees
    const f = filterFuncao.toLowerCase()
    return availableEmployees.filter((e: any) => 
      (e.cargo || '').toLowerCase().includes(f) || (e.fullName || '').toLowerCase().includes(f)
    )
  }, [availableEmployees, filterFuncao])

  // Jornada efetiva atual (base + alocações temporárias desta sessão)
  const getEffectiveJornada = (empId: string): number => {
    const base = employees.find(e => e.id === empId)?.jornadaAlocada || 0
    const extra = tempJornada[empId] || 0
    return Math.min(100, base + extra)
  }

  // Projeta após adicionar N postos novos nesta sessão
  const getProjectedJornada = (empId: string): number => {
    const current = getEffectiveJornada(empId)
    const alreadyAssignedCount = Object.values(assignmentsByPosto).flat().filter(id => id === empId).length
    return Math.min(100, current + (alreadyAssignedCount > 0 ? 0 : 22))
  }

  const canAssign = (empId: string): boolean => {
    const emp = employees.find(e => e.id === empId)
    if (!emp) return false
    const proj = getProjectedJornada(empId)
    return proj <= (emp.maxJornadaPercentual || 100)
  }

  // === AÇÕES DE ALOCAÇÃO POR POSTO (individual e poderoso) ===
  const assignSelectedToPosto = (postoId: string) => {
    if (selectedEmployeeIds.length === 0) {
      showNotification('Selecione pelo menos um funcionário', 'info')
      return
    }

    const _posto = plannedPosts.find(p => p.id === postoId)
    let assignedCount = 0
    let blocked: string[] = []

    setAssignmentsByPosto(prev => {
      const current = prev[postoId] || []
      const next = { ...prev }

      selectedEmployeeIds.forEach(empId => {
        if (current.includes(empId)) return
        const emp = employees.find(e => e.id === empId)
        if (!emp) return

        const proj = getProjectedJornada(empId)
        if (proj > (emp.maxJornadaPercentual || 100)) {
          blocked.push(emp.fullName || empId)
          return
        }
        current.push(empId)
        assignedCount++

        // Atualiza preview de jornada temporária
        setTempJornada(t => ({ ...t, [empId]: (t[empId] || 0) + 22 }))
      })

      next[postoId] = [...current]
      return next
    })

    if (assignedCount > 0) {
      showNotification(`${assignedCount} funcionário(s) alocado(s) no posto ${_posto?.nome}`, 'success')
    }
    if (blocked.length > 0) {
      showNotification(`Bloqueado por jornada máxima: ${blocked.join(', ')}`, 'warning')
    }
    setSelectedEmployeeIds([])
  }

  const removeEmployeeFromPosto = (empId: string, postoId: string) => {
    setAssignmentsByPosto(prev => ({
      ...prev,
      [postoId]: (prev[postoId] || []).filter(id => id !== empId)
    }))
    // Ajusta jornada temporária
    setTempJornada(t => {
      const updated = { ...t }
      const stillAssigned = Object.values(assignmentsByPosto).flat().filter(id => id === empId).length > 1
      if (!stillAssigned) delete updated[empId]
      return updated
    })
  }

  // Sugerir matches automáticos por função/CBO (poderoso)
  const handleSuggestMatches = () => {
    const matches: Record<string, string[]> = {}
    let totalSuggested = 0

    plannedPosts.forEach(posto => {
      const funcaoPosto = (posto.funcao || '').toLowerCase()
      const candidates = filteredEmployees.filter((emp: any) => {
        const cargo = (emp.cargo || '').toLowerCase()
        return cargo.includes(funcaoPosto) || funcaoPosto.includes(cargo.split(' ')[0] || '')
      })

      candidates.slice(0, 1).forEach((emp: any) => {
        if (!canAssign(emp.id)) return
        if (!matches[posto.id]) matches[posto.id] = []
        if (!matches[posto.id].includes(emp.id)) {
          matches[posto.id].push(emp.id)
          totalSuggested++
          // preview jornada
          setTempJornada(t => ({ ...t, [emp.id]: (t[emp.id] || 0) + 22 }))
        }
      })
    })

    setAssignmentsByPosto(prev => {
      const next = { ...prev }
      Object.entries(matches).forEach(([pid, ids]) => {
        const existing = next[pid] || []
        next[pid] = [...new Set([...existing, ...ids])]
      })
      return next
    })

    if (totalSuggested > 0) {
      showNotification(`${totalSuggested} sugestões automáticas aplicadas por compatibilidade de função`, 'success')
    } else {
      showNotification('Nenhum match forte encontrado com os filtros atuais', 'info')
    }
  }

  const clearAllAssignments = () => {
    setAssignmentsByPosto({})
    setTempJornada({})
    setSelectedEmployeeIds([])
  }

  // Confirmação real: chama o hook de assign para cada vínculo (partida individual por posto)
  const handleConfirm = async () => {
    const flat = Object.entries(assignmentsByPosto).flatMap(([postoId, empIds]) =>
      empIds.map(empId => ({ postoId, empId }))
    )

    if (flat.length === 0) {
      showNotification('Nenhuma alocação para confirmar', 'warning')
      return
    }

    const selectedContract = contracts.find((c: any) => c.id === selectedBiddingId)
    if (!selectedContract) {
      showNotification('Selecione uma licitação/contrato válida', 'error')
      return
    }

    let successCount = 0
    let errors: string[] = []

    for (const { empId, postoId } of flat) {
      try {
        await assignEmployee.mutateAsync({
          employeeId: empId,
          contractId: selectedContract.id,
          postId: postoId, // ligação explícita com o posto da licitação
          role: allocationType,
          dataInicio: new Date().toISOString().slice(0, 10),
        } as any)
        successCount++
      } catch (e: any) {
        errors.push(e?.message || 'Erro desconhecido')
      }
    }

    if (successCount > 0) {
      showNotification(`${successCount} alocações individuais por posto salvas com sucesso!`, 'success')
    }
    if (errors.length > 0) {
      showNotification(`${errors.length} falhas (ver console).`, 'error')
      console.error('Erros de alocação:', errors)
    }

    onSuccess?.()
    onClose()
    setAssignmentsByPosto({})
    setTempJornada({})
    setSelectedEmployeeIds([])
  }

  const totalAllocations = Object.values(assignmentsByPosto).flat().length
  const selectedContract = contracts.find((c: any) => c.id === selectedBiddingId)

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="xl" 
      fullWidth
      fullScreen={window.innerWidth < 900} // bom para telas menores / tablets
      PaperProps={{ sx: { maxHeight: '94dvh', display: 'flex', flexDirection: 'column', borderRadius: { xs: 0, sm: 3 } } }}
    >
      <DialogTitle sx={{ flexShrink: 0 }}>
        Ligação em Massa: Funcionários ↔ Postos da Licitação (Alocação por Posto Individual)
      </DialogTitle>
      <DialogContent
        dividers
        sx={{ flex: 1, overflowY: 'auto', pb: 2.5, display: 'flex', flexDirection: 'column' }}
      >
        <Alert severity="info" sx={{ mb: 2 }}>
          Modo avançado para terceirizadoras: aloque funcionários <strong>individualmente em cada posto planejado</strong> (com local e município). 
          Controle rigoroso de jornada máxima por colaborador. Tudo é rastreado para folha, eSocial e contabilidade.
        </Alert>

        {/* Toolbar de seleção e filtros */}
        <Stack direction="row" spacing={2} sx={{ mb: 2 }} alignItems="flex-end" flexWrap="wrap">
          <TextField
            select
            label="Licitação / Contrato *"
            value={selectedBiddingId}
            onChange={(e) => { setSelectedBiddingId(e.target.value); setAssignmentsByPosto({}); setTempJornada({}) }}
            sx={{ minWidth: 280 }}
            
          >
            <MenuItem value="">Selecione...</MenuItem>
            {contracts.map((c: any) => (
              <MenuItem key={c.id} value={c.id}>{c.numero} — {c.orgao}</MenuItem>
            ))}
          </TextField>

          <TextField
            select
            label="Lote"
            value={selectedLotId}
            onChange={(e) => setSelectedLotId(e.target.value)}
            sx={{ minWidth: 160 }}
            
          >
            <MenuItem value="">Todos</MenuItem>
          </TextField>

          <TextField
            select
            label="Tipo Alocação"
            value={allocationType}
            onChange={(e) => setAllocationType(e.target.value as any)}
            sx={{ minWidth: 140 }}
            
          >
            <MenuItem value="TITULAR">TITULAR</MenuItem>
            <MenuItem value="VOLANTE">VOLANTE</MenuItem>
          </TextField>

          <TextField
            label="Filtrar Função/Nome"
            value={filterFuncao}
            onChange={(e) => setFilterFuncao(e.target.value)}
            sx={{ minWidth: 160 }}
          />

          <Button variant="outlined" startIcon={<UserPlus size={16} />} onClick={handleSuggestMatches}>
            Sugerir Matches Automáticos
          </Button>

          <Button variant="text" color="error" onClick={clearAllAssignments} disabled={totalAllocations === 0}>
            Limpar Tudo
          </Button>
        </Stack>

        {/* Grids de contexto */}
        <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
          <Paper sx={{ flex: 1, p: 1.5, borderRadius: 2 }}>
            <Typography variant="subtitle2" gutterBottom>Postos Planejados da Licitação</Typography>
            <EnterpriseDataGrid
              title=""
              rowData={plannedPosts}
              columnDefs={[
                { headerName: 'Posto', field: 'nome', flex: 1.4 },
                { headerName: 'Função', field: 'funcao' },
                { headerName: 'Local', field: 'localExecucao', flex: 1 },
                { headerName: 'Município', field: 'municipioExecucao' },
                { headerName: 'Valor', field: 'valorMensal', valueFormatter: (p: any) => p.value ? `R$ ${p.value.toLocaleString('pt-BR')}` : '-' },
              ]}
              height={210}
            />
          </Paper>

          <Paper sx={{ flex: 1, p: 1.5, borderRadius: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              Funcionários Disponíveis ({filteredEmployees.length}) — com controle de jornada
            </Typography>
            <EnterpriseDataGrid
              title=""
              rowData={filteredEmployees}
              columnDefs={[
                { headerName: 'Nome', field: 'fullName', flex: 1 },
                { headerName: 'Cargo', field: 'cargo' },
                { 
                  headerName: 'Jornada Atual', 
                  field: 'jornadaAlocada',
                  width: 110,
                  cellRenderer: (p: any) => {
                    const val = p.value || 0
                    const color = val > 85 ? '#d32f2f' : val > 60 ? '#ed6c02' : '#2e7d32'
                    return <span style={{ color, fontWeight: 700 }}>{val}%</span>
                  }
                },
              ]}
              height={210}
            />
          </Paper>
        </Stack>

        {/* === ZONA VISUAL PODEROSA: Drag & Drop Simulado por Posto Individual === */}
        <Paper sx={{ p: 2.5, borderRadius: 3, border: '1px solid #e0e0e0' }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1.5 }}>
            <Typography variant="subtitle1" fontWeight={600}>
              Alocação Visual por Posto — Clique no funcionário e depois no posto (ou use o botão)
            </Typography>
            <Chip label={`${totalAllocations} alocações nesta sessão`} color={totalAllocations > 0 ? 'primary' : 'default'} />
          </Stack>

          <Stack direction="row" spacing={2.5}>
            {/* COLUNA ESQUERDA: Funcionários com preview de impacto */}
            <Box sx={{ flex: 1, minWidth: 320 }}>
              <Typography variant="subtitle2" sx={{ mb: 1, color: 'text.secondary' }}>
                1. Selecione funcionários (jornada em tempo real)
              </Typography>

              {filteredEmployees.length === 0 && (
                <Alert severity="warning">Nenhum funcionário disponível com os filtros atuais.</Alert>
              )}

              <Box sx={{ maxHeight: 380, overflowY: 'auto', pr: 1 }}>
                {filteredEmployees.map((emp: any) => {
                  const currentJ = getEffectiveJornada(emp.id)
                  const after = getProjectedJornada(emp.id)
                  const maxJ = emp.maxJornadaPercentual || 100
                  const over = after > maxJ
                  const isSel = selectedEmployeeIds.includes(emp.id)

                  return (
                    <Paper
                      key={emp.id}
                      elevation={isSel ? 4 : 1}
                      sx={{
                        p: 1.25, mb: 1, borderRadius: 2, cursor: 'pointer',
                        border: isSel ? '2px solid #1565c0' : over ? '1px solid #ffcdd2' : '1px solid #e0e0e0',
                        bgcolor: over ? '#fff8e1' : isSel ? '#e3f2fd' : 'white',
                      }}
                      onClick={() => {
                        if (isSel) {
                          setSelectedEmployeeIds(prev => prev.filter(id => id !== emp.id))
                        } else if (canAssign(emp.id)) {
                          setSelectedEmployeeIds(prev => [...prev, emp.id])
                        } else {
                          showNotification(`${emp.fullName} já está no limite de jornada`, 'warning')
                        }
                      }}
                    >
                      <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Box>
                          <Typography fontWeight={700} fontSize="0.95rem">{emp.fullName}</Typography>
                          <Typography variant="caption" color="text.secondary">{emp.cargo}</Typography>
                        </Box>
                        <Chip 
                          size="small" 
                          label={`${currentJ}% → ${after}% (máx ${maxJ}%)`}
                          color={over ? 'error' : isSel ? 'primary' : 'default'}
                        />
                      </Stack>
                      <LinearProgress 
                        variant="determinate" 
                        value={Math.min(100, after)} 
                        sx={{ mt: 0.75, height: 6, borderRadius: 1, bgcolor: '#eee' }}
                        color={over ? 'error' : after > 75 ? 'warning' : 'success'}
                      />
                    </Paper>
                  )
                })}
              </Box>

              <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                <Button size="small" variant="outlined" onClick={() => setSelectedEmployeeIds(filteredEmployees.map((e: any) => e.id))}>
                  Selecionar Todos Visíveis
                </Button>
                <Button size="small" variant="text" onClick={() => setSelectedEmployeeIds([])}>Limpar seleção</Button>
              </Stack>
            </Box>

            {/* COLUNA DIREITA: Postos como ZONAS DE ALOCAÇÃO individuais */}
            <Box sx={{ flex: 1.15, minWidth: 360 }}>
              <Typography variant="subtitle2" sx={{ mb: 1, color: 'text.secondary' }}>
                2. Clique em um Posto para alocar os selecionados (ou use o botão rápido)
              </Typography>

              {plannedPosts.map(posto => {
                const assignedIds = assignmentsByPosto[posto.id] || []
                const assignedEmps = employees.filter(e => assignedIds.includes(e.id))
                const isTarget = selectedEmployeeIds.length > 0

                return (
                  <Paper
                    key={posto.id}
                    sx={{
                      p: 1.5, mb: 1.25, borderRadius: 2.5,
                      border: isTarget ? '2px dashed #1976d2' : '1px solid #c5cae9',
                      bgcolor: assignedIds.length > 0 ? '#f8fafc' : 'white',
                      transition: 'all 0.2s'
                    }}
                  >
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Box>
                        <Typography fontWeight={700}>{posto.nome}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {posto.localExecucao} • {posto.municipioExecucao} • {posto.escala}
                        </Typography>
                        <Typography variant="caption" display="block" sx={{ mt: 0.25, fontWeight: 600, color: '#1565c0' }}>
                          R$ {posto.valorMensal?.toLocaleString('pt-BR')} /mês
                        </Typography>
                      </Box>
                      <Chip size="small" label={`${assignedIds.length} alocado(s)`} color={assignedIds.length ? 'success' : 'default'} />
                    </Stack>

                    {/* Lista de alocados neste posto (individual) */}
                    {assignedEmps.length > 0 && (
                      <Box sx={{ mt: 1, pl: 0.5 }}>
                        {assignedEmps.map(emp => (
                          <Chip
                            key={emp.id}
                            label={emp.fullName}
                            size="small"
                            onDelete={() => removeEmployeeFromPosto(emp.id, posto.id)}
                            deleteIcon={<X size={14} />}
                            sx={{ mr: 0.5, mb: 0.5 }}
                          />
                        ))}
                      </Box>
                    )}

                    <Button
                      fullWidth
                      size="small"
                      variant={isTarget ? 'contained' : 'outlined'}
                      sx={{ mt: 1.25 }}
                      startIcon={<UserPlus size={15} />}
                      onClick={() => assignSelectedToPosto(posto.id)}
                      disabled={selectedEmployeeIds.length === 0}
                    >
                      Alocar {selectedEmployeeIds.length || ''} selecionado(s) aqui ({allocationType})
                    </Button>
                  </Paper>
                )
              })}
            </Box>
          </Stack>

          <Alert severity="info" sx={{ mt: 2 }} icon={<AlertTriangle size={18} />}>
            O sistema bloqueia automaticamente alocações que fariam o colaborador exceder sua <strong>jornada máxima configurada</strong>. 
            Cada posto adiciona aproximadamente 22% de carga. Local e Município do posto são propagados para eventos eSocial e folha.
          </Alert>
        </Paper>

        {/* Resumo da sessão */}
        {totalAllocations > 0 && (
          <Paper sx={{ p: 2, mt: 2, bgcolor: '#f0f7ff', borderRadius: 2 }}>
            <Typography variant="subtitle2">Resumo desta sessão de alocação</Typography>
            <Typography variant="body2">
              {totalAllocations} vínculos • {Object.keys(assignmentsByPosto).length} postos preenchidos • 
              Contrato: {selectedContract?.numero || '—'}
            </Typography>
          </Paper>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="outlined" onClick={clearAllAssignments} disabled={totalAllocations === 0}>Limpar Alocações</Button>
        <Button 
          variant="contained" 
          size="large"
          startIcon={<CheckCircle2 size={18} />}
          onClick={handleConfirm} 
          disabled={!selectedBiddingId || totalAllocations === 0}
        >
          Confirmar {totalAllocations} Alocações por Posto
        </Button>
      </DialogActions>
    </Dialog>
  )
}
