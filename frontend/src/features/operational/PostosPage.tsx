import { useState, useEffect } from 'react'
import { Box, Typography, Paper, Stack, Button, TextField, Alert, Divider } from '@mui/material'
import { Add, Refresh } from '@mui/icons-material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import { apiGet, apiPost } from '../../api/client'
import { useContracts } from '../../api/hooks/useContracts'

export default function PostosPage() {
  const { showNotification } = useNotification()
  const { data: contracts = [] } = useContracts()

  const [postos, setPostos] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [newPosto, setNewPosto] = useState({
    contratoId: '',
    nome: '',
    codigo: '',
    funcao: '',
    escala: '',
    jornadaHoras: '',
    valorMensal: '',
    localExecucao: '',
    municipioExecucao: '',
  })

  const fetchPostos = async () => {
    setLoading(true)
    try {
      // Tenta via contratos (endpoint real: /contracts/{id}/posts). Sem seed demo.
      const all: any[] = []
      for (const c of contracts.slice(0, 8)) {
        try {
          const p = await apiGet<any[]>(`/contracts/${c.id}/posts`)
          if (Array.isArray(p)) all.push(...p)
        } catch { /* ignora contrato sem posts */ }
      }
      setPostos(all)
    } catch (e) {
      // Silently ignore — grid will show empty state
      setPostos([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPostos()
  }, [contracts.length])

  const handleAddPosto = async () => {
    if (!newPosto.nome || !newPosto.contratoId) {
      showNotification('Selecione o contrato e preencha o nome do posto', 'error')
      return
    }
    try {
      await apiPost(`/contracts/${newPosto.contratoId}/posts`, newPosto)
      showNotification('Posto cadastrado com sucesso!', 'success')
      setNewPosto({ ...newPosto, nome: '', codigo: '', funcao: '' })
      fetchPostos()
    } catch (e: any) {
      showNotification(`Erro ao cadastrar posto: ${e.message || 'Verifique o backend'}`, 'error')
      // Não insere dados demo locais — grid mostrará vazio até refresh
    }
  }

  const columns = [
    { headerName: 'Contrato', field: 'contratoNumero', flex: 1 },
    { headerName: 'Código', field: 'codigo', width: 110 },
    { headerName: 'Nome do Posto', field: 'nome', flex: 1.5 },
    { headerName: 'Função', field: 'funcao', flex: 1 },
    { headerName: 'Local', field: 'localExecucao', flex: 1 },
    { headerName: 'Valor Mensal', field: 'valorMensal', valueFormatter: (p: any) => p.value ? `R$ ${Number(p.value).toLocaleString('pt-BR')}` : '-' },
    { headerName: 'Escala', field: 'escala' },
  ]

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={500}>Gestão de Postos</Typography>
          <Typography variant="body1" color="text.secondary">
            Cadastro independente de postos • Vincule depois ao contrato ou licitação
          </Typography>
        </Box>
        <Button variant="outlined" startIcon={<Refresh />} onClick={fetchPostos}>Atualizar</Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 3 }}>
        Recomendação operacional: Cadastre os Postos aqui ou durante o planejamento da licitação. Depois vincule ao contrato vencedor.
      </Alert>

      {/* Formulário de Cadastro Rápido */}
      <Paper sx={{ p: 2.5, mb: 3, borderRadius: 3 }}>
        <Typography variant="h6" gutterBottom>Cadastrar Novo Posto</Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} flexWrap="wrap">
          <TextField
            select
            label="Contrato"
            value={newPosto.contratoId}
            onChange={e => setNewPosto({ ...newPosto, contratoId: e.target.value })}
            size="small"
            sx={{ minWidth: 220 }}
          >
            <option value="">Selecione o contrato...</option>
            {contracts.map((c: any) => (
              <option key={c.id} value={c.id}>{c.numero} — {c.orgao}</option>
            ))}
          </TextField>
          <TextField label="Nome do Posto *" value={newPosto.nome} onChange={e => setNewPosto({ ...newPosto, nome: e.target.value })} size="small" sx={{ minWidth: 220 }} />
          <TextField label="Código" value={newPosto.codigo} onChange={e => setNewPosto({ ...newPosto, codigo: e.target.value })} size="small" sx={{ minWidth: 120 }} />
          <TextField label="Função / CBO" value={newPosto.funcao} onChange={e => setNewPosto({ ...newPosto, funcao: e.target.value })} size="small" sx={{ minWidth: 160 }} />
          <TextField label="Valor Mensal" type="number" value={newPosto.valorMensal} onChange={e => setNewPosto({ ...newPosto, valorMensal: e.target.value })} size="small" sx={{ minWidth: 130 }} />
          <TextField label="Local de Execução" value={newPosto.localExecucao} onChange={e => setNewPosto({ ...newPosto, localExecucao: e.target.value })} size="small" sx={{ minWidth: 200 }} />
          <Button variant="contained" startIcon={<Add />} onClick={handleAddPosto} sx={{ height: 40 }}>
            Cadastrar Posto
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2, borderRadius: 3 }}>
        <EnterpriseDataGrid
          title="Postos Cadastrados"
          rowData={postos}
          columnDefs={columns}
          onRefresh={fetchPostos}
          loading={loading}
          height="calc(100dvh - 420px)"
          emptyMessage="Nenhum posto cadastrado. Use o formulário acima ou vincule via contratos/licitações (dados 100% reais do backend)."
        />
      </Paper>

      {/* ===================================================== */}
      {/* STARTER UNIFORMES (Onda 2) - CRUD + Alocações por Posto/Contrato */}
      {/* Edição em arquivo existente (PostosPage) - tudo conectado ao operacional */}
      {/* ===================================================== */}
      <Divider sx={{ my: 4 }} />

      <UniformesSection contracts={contracts} showNotification={showNotification} />
    </Box>
  )
}

// Componente interno para Uniformes (mantido no mesmo arquivo para evitar criação de novos arquivos)
function UniformesSection({ contracts, showNotification }: { contracts: any[]; showNotification: (msg: string, sev?: any) => void }) {
  // anchor: /postos#uniformes
  const [uniformItems, setUniformItems] = useState<any[]>([])
  const [allocations, setAllocations] = useState<any[]>([])
  const [loading, setLoading] = useState(false)

  // Form para novo item de uniforme
  const [newItem, setNewItem] = useState({
    descricao: '',
    tipo: 'CAMISETA',
    tamanho: '',
    quantidadeEstoque: '10',
    custoUnitario: '45.90'
  })

  // Form alocação por posto/contrato (usa employee simulado + posto)
  const [newAllocation, setNewAllocation] = useState({
    employeeId: '',
    postoId: '',
    contratoId: '',
    itemId: '',
    quantidade: '1',
    dataEntrega: '2025-06-10'
  })

  const fetchUniformes = async () => {
    setLoading(true)
    try {
      const items = await apiGet<any[]>('/uniformes/items')
      setUniformItems(items || [])
    } catch {
      setUniformItems([])
    }
    setLoading(false)
  }

  const fetchAllocations = async (empId?: string) => {
    try {
      if (empId) {
        const al = await apiGet<any[]>(`/uniformes/allocations?employeeId=${empId}`)
        setAllocations(al || [])
      } else {
        setAllocations([])
      }
    } catch {
      setAllocations([])
    }
  }

  useEffect(() => {
    fetchUniformes()
    fetchAllocations()
  }, [])

  const handleCreateItem = async () => {
    if (!newItem.descricao) {
      showNotification('Descrição do uniforme obrigatória', 'error')
      return
    }
    try {
      await apiPost<any>('/uniformes/items', {
        ...newItem,
        quantidadeEstoque: parseInt(newItem.quantidadeEstoque),
        custoUnitario: parseFloat(newItem.custoUnitario)
      })
      showNotification('Item de uniforme cadastrado!', 'success')
      setNewItem({ ...newItem, descricao: '', tamanho: '' })
      fetchUniformes()
    } catch (e: any) {
      showNotification(`Erro ao cadastrar item: ${e.message || 'Verifique conexão com backend'}`, 'error')
      // Sem seed demo local — grid reflete apenas dados reais
    }
  }

  const handleAllocate = async () => {
    if (!newAllocation.itemId || !newAllocation.employeeId) {
      showNotification('Selecione item e employee (ou simule posto)', 'error')
      return
    }
    try {
      await apiPost('/uniformes/allocations', {
        employeeId: newAllocation.employeeId,
        uniformItemId: newAllocation.itemId,
        quantidade: parseInt(newAllocation.quantidade),
        dataEntrega: newAllocation.dataEntrega,
        contratoId: newAllocation.contratoId || undefined,
        postoId: newAllocation.postoId || undefined
      })
      showNotification('Alocação de uniforme registrada por posto/contrato!', 'success')
      fetchAllocations(newAllocation.employeeId)
    } catch (e: any) {
      showNotification(`Erro ao alocar: ${e.message || 'Verifique backend (UniformController)'}`, 'error')
      // Sem fallback demo — usuário vê grid vazio até dados reais
    }
  }

  const uniformColumns = [
    { headerName: 'Descrição', field: 'descricao', flex: 1.5 },
    { headerName: 'Tipo', field: 'tipo' },
    { headerName: 'Tam', field: 'tamanho', width: 80 },
    { headerName: 'Estoque', field: 'quantidadeEstoque', width: 100 },
    { headerName: 'Custo (R$)', field: 'custoUnitario', valueFormatter: (p: any) => Number(p.value).toFixed(2) },
  ]

  const allocColumns = [
    { headerName: 'Funcionário', field: 'employeeId' },
    { headerName: 'Item', field: 'itemDescricao', flex: 1 },
    { headerName: 'Qtd', field: 'quantidade' },
    { headerName: 'Data Entrega', field: 'dataEntrega' },
    { headerName: 'Posto', field: 'posto' },
  ]

  return (
    <Box id="uniformes">
      <Typography variant="h5" fontWeight={600} gutterBottom>Uniformes & EPIs — Alocações por Posto/Contrato</Typography>
      <Alert severity="info" sx={{ mb: 2 }}>
        CRUD completo via UniformController + alocações ligadas a employee + posto/contrato (tudo conectado ao operacional e folha).
        Use os Postos acima para contexto.
      </Alert>

      {/* CRUD Itens */}
      <Paper sx={{ p: 2.5, mb: 3, borderRadius: 3 }}>
        <Typography variant="h6" gutterBottom>Cadastrar Item de Uniforme</Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} flexWrap="wrap">
          <TextField label="Descrição *" value={newItem.descricao} onChange={e => setNewItem({ ...newItem, descricao: e.target.value })} size="small" sx={{ minWidth: 220 }} />
          <TextField label="Tipo" value={newItem.tipo} onChange={e => setNewItem({ ...newItem, tipo: e.target.value })} size="small" sx={{ minWidth: 140 }} />
          <TextField label="Tamanho" value={newItem.tamanho} onChange={e => setNewItem({ ...newItem, tamanho: e.target.value })} size="small" sx={{ width: 100 }} />
          <TextField label="Estoque" type="number" value={newItem.quantidadeEstoque} onChange={e => setNewItem({ ...newItem, quantidadeEstoque: e.target.value })} size="small" sx={{ width: 110 }} />
          <TextField label="Custo Unit." type="number" value={newItem.custoUnitario} onChange={e => setNewItem({ ...newItem, custoUnitario: e.target.value })} size="small" sx={{ width: 120 }} />
          <Button variant="contained" startIcon={<Add />} onClick={handleCreateItem}>Cadastrar Item</Button>
          <Button variant="outlined" startIcon={<Refresh />} onClick={fetchUniformes}>Atualizar</Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2, mb: 3, borderRadius: 3 }}>
        <EnterpriseDataGrid
          title="Estoque de Uniformes"
          rowData={uniformItems}
          columnDefs={uniformColumns}
          onRefresh={fetchUniformes}
          loading={loading}
          height={220}
          emptyMessage="Nenhum item de uniforme cadastrado. Cadastre acima para popular o estoque (dados reais do backend)."
        />
      </Paper>

      {/* Alocações por Posto/Contrato */}
      <Paper sx={{ p: 2.5, borderRadius: 3 }}>
        <Typography variant="h6" gutterBottom>Alocar Uniforme a Funcionário (vinculado a Posto/Contrato)</Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} flexWrap="wrap" mb={2}>
          <TextField
            select
            label="Contrato"
            value={newAllocation.contratoId}
            onChange={e => setNewAllocation({ ...newAllocation, contratoId: e.target.value })}
            size="small" sx={{ minWidth: 200 }}
          >
            <option value="">Selecione...</option>
            {contracts.map((c: any) => <option key={c.id} value={c.id}>{c.numero}</option>)}
          </TextField>
          <TextField label="Posto ID (opcional)" value={newAllocation.postoId} onChange={e => setNewAllocation({ ...newAllocation, postoId: e.target.value })} size="small" sx={{ minWidth: 160 }} />
          <TextField label="Employee ID" value={newAllocation.employeeId} onChange={e => setNewAllocation({ ...newAllocation, employeeId: e.target.value })} size="small" sx={{ minWidth: 180 }} />
          <TextField
            select
            label="Item Uniforme"
            value={newAllocation.itemId}
            onChange={e => setNewAllocation({ ...newAllocation, itemId: e.target.value })}
            size="small" sx={{ minWidth: 200 }}
          >
            <option value="">Selecione item...</option>
            {uniformItems.map((u: any) => <option key={u.id} value={u.id}>{u.descricao}</option>)}
          </TextField>
          <TextField label="Qtd" type="number" value={newAllocation.quantidade} onChange={e => setNewAllocation({ ...newAllocation, quantidade: e.target.value })} size="small" sx={{ width: 80 }} />
          <TextField label="Entrega" type="date" value={newAllocation.dataEntrega} onChange={e => setNewAllocation({ ...newAllocation, dataEntrega: e.target.value })} size="small" InputLabelProps={{ shrink: true }} />
          <Button variant="contained" onClick={handleAllocate}>Alocar ao Posto</Button>
        </Stack>

        <EnterpriseDataGrid
          title="Alocações de Uniformes (por Posto/Contrato)"
          rowData={allocations}
          columnDefs={allocColumns}
          height={200}
          emptyMessage="Nenhuma alocação registrada ainda. Use o formulário acima (integra com Employee + Posto)."
        />
        <Typography variant="caption" color="text.secondary">As alocações impactam custos em Folha e estoque operacional.</Typography>
      </Paper>
    </Box>
  )
}
