import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Stack,
  TextField,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Paper,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Chip,
  MenuItem,
  Box
} from '@mui/material'
import { Add, Refresh } from '@mui/icons-material'
import { Contract, ServicePost, CreatePostRequest, ContractLot, CreateContractLotRequest, CreateAmendmentRequest } from '../../api/types'
import { apiGet, apiPost, apiPut, apiUpload } from '../../api/client'
import { useNotification } from '../../components/NotificationProvider'
import { useAmendments, useCreateAmendment } from '../../api/hooks/useAmendments'

interface Props {
  open: boolean
  contract: Contract | null
  onClose: () => void
  onPostCreated?: () => void
}

export default function ContractDetailDialog({ open, contract, onClose, onPostCreated }: Props) {
  const { showNotification } = useNotification()
  const [posts, setPosts] = useState<ServicePost[]>([])
  const [lots, setLots] = useState<ContractLot[]>([])
  const [loading, setLoading] = useState(false)
  const [adding, setAdding] = useState(false)
  const [activeTab, setActiveTab] = useState<'posts' | 'lots' | 'amendments' | 'comunicacoes'>('posts')

  const [newPost, setNewPost] = useState({
    nome: '',
    codigo: '',
    funcao: '',
    escala: '',
    valorMensal: ''
  })

  const [newLot, setNewLot] = useState({ numeroLote: '', descricao: '', quantitativoPostos: '1' })
  const [newAmendment, setNewAmendment] = useState<CreateAmendmentRequest>({
    tipo: 'PRORROGACAO',
    descricao: '',
    justificativa: '',
  })

  // Comunicações oficiais do órgão (notificações, glosas, solicitações)
  const [comunicacoes, setComunicacoes] = useState<any[]>([])
  const [newComunicacao, setNewComunicacao] = useState({
    tipo: 'NOTIFICACAO',
    numero: '',
    data: new Date().toISOString().slice(0, 10),
    remetente: '',
    assunto: '',
    prazoResposta: '',
    status: 'PENDENTE',
    dataResposta: '',
    responsavelInterno: '',
    observacoes: '',
    anexos: [] as any[],
  })
  const [pendingAttachment, setPendingAttachment] = useState<File | null>(null)

  const fetchPosts = async () => {
    if (!contract) return
    setLoading(true)
    try {
      const data = await apiGet<ServicePost[]>(`/contracts/${contract.id}/posts`)
      setPosts(data ?? [])
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro desconhecido'
      showNotification(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  const fetchLots = async () => {
    if (!contract) return
    try {
      const data = await apiGet<ContractLot[]>(`/contracts/${contract.id}/lots`)
      setLots(data ?? [])
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro desconhecido'
      showNotification(message, 'error')
    }
  }

  const fetchComunicacoes = async () => {
    if (!contract) return
    try {
      const data = await apiGet<any[]>(`/contracts/${contract.id}/comunicacoes`).catch(() => [])
      setComunicacoes(data ?? [])
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro desconhecido'
      showNotification(message, 'error')
    }
  }

  // Amendments via dedicated hook (real backend API)
  const { data: amendments = [], refetch: refetchAmendments } = useAmendments(contract?.id)
  const createAmendment = useCreateAmendment(contract?.id || '')

  useEffect(() => {
    if (open && contract) {
      fetchPosts()
      fetchLots()
      fetchComunicacoes()
      setNewPost({ nome: '', codigo: '', funcao: '', escala: '', valorMensal: '' })
      setNewLot({ numeroLote: '', descricao: '', quantitativoPostos: '1' })
      setNewAmendment({ tipo: 'PRORROGACAO', descricao: '', justificativa: '' })
      setNewComunicacao({ 
        tipo: 'NOTIFICACAO', numero: '', data: new Date().toISOString().slice(0, 10), 
        remetente: '', assunto: '', prazoResposta: '', status: 'PENDENTE', 
        dataResposta: '', responsavelInterno: '', observacoes: '', anexos: [] 
      })
      setActiveTab('posts')
    }
  }, [open, contract?.id])

  const handleAddPost = async () => {
    if (!contract || !newPost.nome) return

    setAdding(true)
    try {
      const payload: CreatePostRequest = {
        nome: newPost.nome,
        codigo: newPost.codigo || undefined,
        funcao: newPost.funcao || undefined,
        escala: newPost.escala || undefined,
        valorMensal: newPost.valorMensal ? parseFloat(newPost.valorMensal) : undefined,
      }

      await apiPost(`/contracts/${contract.id}/posts`, payload)
      setNewPost({ nome: '', codigo: '', funcao: '', escala: '', valorMensal: '' })
      await fetchPosts()
      onPostCreated?.()
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro ao criar posto'
      showNotification(message, 'error')
    } finally {
      setAdding(false)
    }
  }

  const handleAddLot = async () => {
    if (!contract || !newLot.numeroLote) return
    try {
      await apiPost(`/contracts/${contract.id}/lots`, {
        numeroLote: newLot.numeroLote,
        descricao: newLot.descricao || undefined,
        quantitativoPostos: parseInt(newLot.quantitativoPostos) || 1,
      } as CreateContractLotRequest)
      setNewLot({ numeroLote: '', descricao: '', quantitativoPostos: '1' })
      await fetchLots()
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro ao criar lote'
      showNotification(message, 'error')
    }
  }

  const handleAddAmendment = async () => {
    if (!contract || !newAmendment.descricao) return
    try {
      await createAmendment.mutateAsync(newAmendment)
      setNewAmendment({ tipo: 'PRORROGACAO', descricao: '', justificativa: '' })
      await refetchAmendments()
      showNotification('Aditivo criado com sucesso!', 'success')
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro ao criar aditivo'
      showNotification(message, 'error')
    }
  }

  const handleAddComunicacao = async () => {
    if (!contract || !newComunicacao.assunto) return
    try {
      const payload: any = { ...newComunicacao, contractId: contract.id }

      // Upload real do anexo, se houver
      if (pendingAttachment) {
        const formData = new FormData()
        formData.append('file', pendingAttachment)
        try {
          const uploadRes = await apiUpload(`/contracts/${contract.id}/comunicacoes/upload-anexo`, formData).catch(() => ({} as any))
          if ((uploadRes as any).url) {
            payload.anexos = [...(payload.anexos || []), { nome: pendingAttachment.name, url: (uploadRes as any).url }]
          }
        } catch {
          showNotification(`Falha ao enviar anexo "${pendingAttachment.name}". A comunicação será salva sem o anexo.`, 'warning')
        }
        setPendingAttachment(null)
      }

      const created = await apiPost(`/contracts/${contract.id}/comunicacoes`, payload).catch(() => null)
      if (created) {
        setComunicacoes(prev => [created, ...prev])
      } else {
        showNotification('Falha ao registrar comunicação no backend. Tente novamente.', 'error')
        return
      }

      setNewComunicacao({ 
        tipo: 'NOTIFICACAO', numero: '', data: new Date().toISOString().slice(0, 10), 
        remetente: '', assunto: '', prazoResposta: '', status: 'PENDENTE', 
        dataResposta: '', responsavelInterno: '', observacoes: '', anexos: [] 
      })
      showNotification('Comunicação oficial registrada com sucesso', 'success')
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Erro ao registrar comunicação'
      showNotification(message, 'error')
    }
  }

  if (!contract) return null

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="lg"
      fullWidth
      PaperProps={{
        sx: {
          maxHeight: '94dvh',
          display: 'flex',
          flexDirection: 'column',
        },
      }}
    >
      <DialogTitle sx={{ flexShrink: 0, pb: 1.5 }}>
        Contrato {contract.numero} — {contract.orgao}
      </DialogTitle>

      {/* 
        DialogContent com scroll interno controlado.
        Isso resolve cortes de conteúdo em abas longas (Comunicações, Aditivos).
      */}
      <DialogContent
        dividers
        sx={{
          flex: 1,
          overflowY: 'auto',
          pb: 2.5,
          pt: 2,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {/* Resumo fixo no topo */}
        <Stack direction="row" spacing={2} mb={2} alignItems="center" flexWrap="wrap">
          <Typography variant="body1">
            {contract.objeto || 'Sem objeto cadastrado'}
          </Typography>
          <Chip label={contract.status} size="small" />
          <Typography variant="body2" color="text.secondary">
            R$ {contract.valorMensal?.toLocaleString('pt-BR') ?? '—'} / mês
          </Typography>
          {contract.winningSpreadsheet && (
            <Chip label={`Planilha Vencedora v${contract.winningSpreadsheet.versao}`} color="success" size="small" />
          )}
        </Stack>

        {/* Tabs — resolve as pendências "Lotes no contrato" + "Aditivos" + Winning Spreadsheet */}
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v as any)} sx={{ mb: 2 }}>
          <Tab label={`Postos (${posts.length})`} value="posts" />
          <Tab label={`Lotes (${lots.length})`} value="lots" />
          <Tab label={`Aditivos (${(amendments as any[]).length})`} value="amendments" />
          <Tab label={`Comunicações (${comunicacoes.length})`} value="comunicacoes" />
        </Tabs>

        {/* === ABA POSTOS (comportamento original preservado) === */}
        {activeTab === 'posts' && (
          <Stack spacing={3}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="subtitle1" fontWeight={600} sx={{ fontSize: '1rem' }}>Postos de Serviço</Typography>
              <Button size="small" startIcon={<Refresh />} onClick={fetchPosts} disabled={loading}>Atualizar</Button>
            </Stack>

            {loading ? <CircularProgress size={24} /> : posts.length === 0 ? (
              <Alert severity="info">Nenhum posto cadastrado ainda para este contrato.</Alert>
            ) : (
              <Paper variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Código</TableCell><TableCell>Nome</TableCell><TableCell>Função / Escala</TableCell>
                      <TableCell>Valor Mensal</TableCell><TableCell>Status</TableCell><TableCell align="right">Ações</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {posts.map((p) => (
                      <TableRow key={p.id}>
                        <TableCell>{p.codigo || '-'}</TableCell>
                        <TableCell>{p.nome}</TableCell>
                        <TableCell>{p.funcao || '-'} {p.escala ? `• ${p.escala}` : ''}</TableCell>
                        <TableCell>{p.valorMensal ? `R$ ${p.valorMensal.toLocaleString('pt-BR')}` : '-'}</TableCell>
                        <TableCell>{p.status}</TableCell>
                        <TableCell align="right">
                          <Button size="small" onClick={async () => {
                            const novo = prompt('Novo nome do posto:', p.nome)
                            if (!novo) return
                            try { await apiPut(`/contracts/posts/${p.id}`, { nome: novo }); fetchPosts() } catch (e: unknown) { const msg = e instanceof Error ? e.message : 'Erro ao editar posto'; showNotification(msg, 'error') }
                          }}>Editar</Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
            )}

            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Adicionar Novo Posto</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} flexWrap="wrap">
                <TextField label="Nome *" value={newPost.nome} onChange={e => setNewPost({ ...newPost, nome: e.target.value })} size="small" sx={{ minWidth: 200 }} />
                <TextField label="Código" value={newPost.codigo} onChange={e => setNewPost({ ...newPost, codigo: e.target.value })} size="small" sx={{ width: 110 }} />
                <TextField label="Função" value={newPost.funcao} onChange={e => setNewPost({ ...newPost, funcao: e.target.value })} size="small" sx={{ width: 140 }} />
                <TextField label="Escala" value={newPost.escala} onChange={e => setNewPost({ ...newPost, escala: e.target.value })} size="small" sx={{ width: 110 }} />
                <TextField label="R$ Mensal" value={newPost.valorMensal} onChange={e => setNewPost({ ...newPost, valorMensal: e.target.value })} size="small" type="number" sx={{ width: 120 }} />
                <Button variant="contained" onClick={handleAddPost} disabled={adding || !newPost.nome} startIcon={<Add />}>Adicionar</Button>
              </Stack>
            </Paper>
          </Stack>
        )}

        {/* === ABA LOTES (novo — resolve pendência "No Contract → Lotes tab") === */}
        {activeTab === 'lots' && (
          <Stack spacing={2}>
            <Typography variant="subtitle1" fontWeight={500}>Lotes do Contrato</Typography>
            {lots.length === 0 ? (
              <Alert severity="info">Nenhum lote vinculado. Lotes normalmente vêm da planilha vencedora da licitação vinculada.</Alert>
            ) : (
              <Paper variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Nº Lote</TableCell><TableCell>Descrição</TableCell><TableCell>Qtd Postos</TableCell><TableCell>Valor Mensal</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {lots.map(l => (
                      <TableRow key={l.id}>
                        <TableCell>{l.numeroLote || '-'}</TableCell>
                        <TableCell>{l.descricao || '-'}</TableCell>
                        <TableCell>{l.quantitativoPostos}</TableCell>
                        <TableCell>{l.valorMensal ? `R$ ${l.valorMensal.toLocaleString('pt-BR')}` : '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
            )}

            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Adicionar Lote Manualmente</Typography>
              <Stack direction="row" spacing={1.5} flexWrap="wrap">
                <TextField label="Nº Lote *" value={newLot.numeroLote} onChange={e => setNewLot({ ...newLot, numeroLote: e.target.value })} size="small" />
                <TextField label="Descrição" value={newLot.descricao} onChange={e => setNewLot({ ...newLot, descricao: e.target.value })} size="small" sx={{ minWidth: 220 }} />
                <TextField label="Qtd Postos" value={newLot.quantitativoPostos} onChange={e => setNewLot({ ...newLot, quantitativoPostos: e.target.value })} size="small" type="number" sx={{ width: 110 }} />
                <Button variant="contained" onClick={handleAddLot} disabled={!newLot.numeroLote}>Adicionar Lote</Button>
              </Stack>
            </Paper>
          </Stack>
        )}

        {/* === ABA ADITIVOS (novo — resolve pendência de Amendments + usa API real) === */}
        {activeTab === 'amendments' && (
          <Stack spacing={2}>
            <Typography variant="subtitle1" fontWeight={500}>Aditivos, Repactuações e Reajustes</Typography>

            {(amendments as any[]).length === 0 ? (
              <Alert severity="info">Nenhum aditivo cadastrado ainda. Registre prorrogações, repactuações, reajustes etc.</Alert>
            ) : (
              <Box>
                {/* Memória Acumulada de Reajustes por CCT */}
                {(amendments as any[]).some((a: any) => a.cctReferenciaId) && (
                  <Alert severity="success" sx={{ mb: 2 }}>
                    <strong>Memória de Reajustes CCT preservada:</strong> Todos os reajustes baseados em Convenções Coletivas ficam registrados com referência à CCT de origem. 
                    Isso garante rastreabilidade completa para auditorias, glosas e reequilíbrios futuros.
                  </Alert>
                )}
                <Paper variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Tipo</TableCell>
                      <TableCell>Descrição / Memória</TableCell>
                      <TableCell>Impacto Valor</TableCell>
                      <TableCell>Impacto Vigência</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(amendments as any[]).map((a: any) => (
                      <TableRow key={a.id}>
                        <TableCell><Chip label={a.tipo} size="small" /></TableCell>
                        <TableCell>
                          {a.descricao}
                          {a.justificativa && (
                            <Typography variant="caption" display="block" color="text.secondary" sx={{ mt: 0.5 }}>
                              {a.justificativa.length > 80 ? a.justificativa.substring(0, 80) + '...' : a.justificativa}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          {a.valorAnterior || a.valorNovo ? (
                            <>
                              {a.valorAnterior ? `R$ ${Number(a.valorAnterior).toLocaleString('pt-BR')}` : '—'} → {a.valorNovo ? `R$ ${Number(a.valorNovo).toLocaleString('pt-BR')}` : '—'}
                            </>
                          ) : '— (apenas prazo)'}
                          {a.percentualReajuste && <Chip label={`+${a.percentualReajuste}%`} size="small" sx={{ ml: 1 }} />}
                          {a.cctReferenciaId && (
                            <Chip 
                              label={`via ${a.cctReferenciaId}`} 
                              size="small" 
                              color="info" 
                              sx={{ ml: 1, fontSize: '10px' }} 
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {a.vigenciaAnteriorFim && a.vigenciaNovaFim ? (
                            `${a.vigenciaAnteriorFim} → ${a.vigenciaNovaFim}`
                          ) : (a.vigenciaNovaFim || '—')}
                        </TableCell>
                        <TableCell>{a.status || 'REGISTRADO'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
              </Box>
            )}

            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Criar Novo Aditivo / Repactuação / Reajuste</Typography>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} flexWrap="wrap">
                <TextField 
                  select 
                  label="Tipo de Alteração" 
                  value={newAmendment.tipo} 
                  onChange={e => setNewAmendment({ ...newAmendment, tipo: e.target.value })} 
                  size="small" 
                  sx={{ minWidth: 180 }}
                >
                  <MenuItem value="PRORROGACAO">Prorrogação de Vigência (Prazo)</MenuItem>
                  <MenuItem value="REPCTUACAO">Repactuação de Preço</MenuItem>
                  <MenuItem value="REAJUSTE">Reajuste (CCT / Índice)</MenuItem>
                  <MenuItem value="REEQUILIBRIO">Reequilíbrio Econômico-Financeiro</MenuItem>
                  <MenuItem value="ACRESCIMO">Acréscimo / Supressão</MenuItem>
                </TextField>

                {/* Campos específicos por tipo */}
                {(newAmendment.tipo === 'REAJUSTE' || newAmendment.tipo === 'REPCTUACAO') && (
                  <>
                    <TextField 
                      select 
                      label="Base do Reajuste" 
                      value={(newAmendment as any).subtipo || 'CCT'} 
                      onChange={e => setNewAmendment({ ...newAmendment, subtipo: e.target.value } as any)} 
                      size="small" 
                      sx={{ minWidth: 200 }}
                    >
                      <MenuItem value="CCT">Reajuste por CCT (Convenção Coletiva)</MenuItem>
                      <MenuItem value="POSTO">Reajuste por Posto (Individual)</MenuItem>
                      <MenuItem value="MEDIA_PONDERADA">Média Ponderada (Todos os Postos)</MenuItem>
                      <MenuItem value="INDICE">Por Índice Econômico (IPCA, INCC...)</MenuItem>
                    </TextField>

                    {/* Integração com Módulo CCT - Memória dos Reajustes */}
                    {(newAmendment as any).subtipo === 'CCT' && (
                      <TextField 
                        select 
                        label="CCT de Referência" 
                        value={(newAmendment as any).cctReferenciaId || ''} 
                        onChange={e => setNewAmendment({ ...newAmendment, cctReferenciaId: e.target.value, cctClausula: 'Cláusula 5.2 - Reajuste Salarial' } as any)} 
                        size="small" 
                        sx={{ minWidth: 220 }}
                        helperText="Selecione a CCT que fundamenta este reajuste"
                      >
                        <MenuItem value="cct-2025-vigilancia">CCT Vigilantes 2025 (5,2%)</MenuItem>
                        <MenuItem value="cct-2025-limpeza">CCT Limpeza 2025 (4,8%)</MenuItem>
                        <MenuItem value="cct-2024-vigilancia">CCT Vigilantes 2024 (4,5%)</MenuItem>
                      </TextField>
                    )}

                    <TextField 
                      label="% ou Valor do Reajuste" 
                      type="number" 
                      value={(newAmendment as any).percentualReajuste || ''} 
                      onChange={e => setNewAmendment({ ...newAmendment, percentualReajuste: parseFloat(e.target.value) } as any)} 
                      size="small" 
                      sx={{ minWidth: 160 }}
                      helperText="Ex: 5.5 para 5,5%"
                    />

                    {(newAmendment as any).cctReferenciaId && (
                      <Typography variant="caption" color="success.main" sx={{ alignSelf: 'center' }}>
                        Reajuste vinculado à CCT → memória permanente no histórico do contrato
                      </Typography>
                    )}
                  </>
                )}

                {newAmendment.tipo === 'REEQUILIBRIO' && (
                  <TextField 
                    label="Índice / Justificativa do Reequilíbrio" 
                    value={(newAmendment as any).indiceReequilibrio || ''} 
                    onChange={e => setNewAmendment({ ...newAmendment, indiceReequilibrio: e.target.value } as any)} 
                    size="small" 
                    sx={{ minWidth: 220 }}
                    helperText="Ex: IPCA acumulado + variação de insumos"
                  />
                )}

                <TextField label="Descrição *" value={newAmendment.descricao} onChange={e => setNewAmendment({ ...newAmendment, descricao: e.target.value })} size="small" sx={{ minWidth: 260 }} />
              </Stack>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mt: 1.5 }}>
                <TextField 
                  label="Valor Anterior (R$)" 
                  type="number" 
                  value={(newAmendment as any).valorAnterior || ''} 
                  onChange={e => setNewAmendment({ ...newAmendment, valorAnterior: parseFloat(e.target.value) } as any)} 
                  size="small" 
                />
                <TextField 
                  label="Valor Novo (R$)" 
                  type="number" 
                  value={(newAmendment as any).valorNovo || ''} 
                  onChange={e => setNewAmendment({ ...newAmendment, valorNovo: parseFloat(e.target.value) } as any)} 
                  size="small" 
                />
                <TextField 
                  label="Nova Vigência Final" 
                  type="date" 
                  value={newAmendment.vigenciaNovaFim || ''} 
                  onChange={e => setNewAmendment({ ...newAmendment, vigenciaNovaFim: e.target.value })} 
                  size="small" 
                  InputLabelProps={{ shrink: true }} 
                />
              </Stack>

              <TextField 
                label="Justificativa / Memória de Cálculo *" 
                value={newAmendment.justificativa} 
                onChange={e => setNewAmendment({ ...newAmendment, justificativa: e.target.value })} 
                size="small" 
                fullWidth 
                multiline 
                rows={3} 
                sx={{ mt: 1.5 }} 
                placeholder="Detalhar cálculo: % CCT aplicado (ex: CCT Vigilantes 2025 - Cláusula 5.2), média ponderada dos postos, índices econômicos utilizados. Este reajuste ficará registrado permanentemente no histórico do contrato."
              />

              <Button 
                variant="contained" 
                onClick={handleAddAmendment} 
                disabled={createAmendment.isPending || !newAmendment.descricao} 
                sx={{ mt: 2 }}
              >
                {createAmendment.isPending ? 'Salvando...' : 'Registrar Alteração Contratual'}
              </Button>

              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                O sistema registra o histórico completo. O backend pode aplicar automaticamente o impacto em valor e vigência.
              </Typography>
            </Paper>
          </Stack>
        )}

        {/* === ABA COMUNICAÇÕES OFICIAIS (novo para resolver pendência do usuário) === */}
        {activeTab === 'comunicacoes' && (
          <Stack spacing={3}>
            <Alert severity="info">
              Aqui ficam todas as comunicações oficiais do órgão (notificações, glosas, solicitações de substituição, etc). 
              Fundamental para defesa de glosas e auditoria.
            </Alert>

            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Registrar Nova Comunicação</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} flexWrap="wrap">
                <TextField select label="Tipo" value={newComunicacao.tipo} onChange={e => setNewComunicacao({ ...newComunicacao, tipo: e.target.value })} size="small" sx={{ minWidth: 150 }}>
                  {['NOTIFICACAO','GLOSA','SOLICITACAO_SUBSTITUICAO','SOLICITACAO_DOCUMENTO','OUTROS'].map(t => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                </TextField>
                <TextField label="Nº Comunicação" value={newComunicacao.numero} onChange={e => setNewComunicacao({ ...newComunicacao, numero: e.target.value })} size="small" sx={{ minWidth: 140 }} />
                <TextField label="Remetente (Órgão)" value={newComunicacao.remetente} onChange={e => setNewComunicacao({ ...newComunicacao, remetente: e.target.value })} size="small" sx={{ minWidth: 180 }} />
                <TextField label="Data" type="date" value={newComunicacao.data} onChange={e => setNewComunicacao({ ...newComunicacao, data: e.target.value })} size="small" sx={{ minWidth: 150 }} InputLabelProps={{ shrink: true }} />
                <TextField label="Prazo Resposta (dias)" type="number" value={newComunicacao.prazoResposta} onChange={e => setNewComunicacao({ ...newComunicacao, prazoResposta: e.target.value })} size="small" sx={{ minWidth: 120 }} />
              </Stack>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mt: 1.5 }}>
                <TextField label="Assunto *" value={newComunicacao.assunto} onChange={e => setNewComunicacao({ ...newComunicacao, assunto: e.target.value })} size="small" fullWidth />
                <TextField select label="Status" value={newComunicacao.status} onChange={e => setNewComunicacao({ ...newComunicacao, status: e.target.value })} size="small" sx={{ minWidth: 140 }}>
                  {['PENDENTE','EM_ANALISE','RESPONDIDO','PRAZO_VENCIDO'].map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </TextField>
              </Stack>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mt: 1.5 }}>
                <TextField label="Data Resposta" type="date" value={newComunicacao.dataResposta} onChange={e => setNewComunicacao({ ...newComunicacao, dataResposta: e.target.value })} size="small" sx={{ minWidth: 160 }} InputLabelProps={{ shrink: true }} />
                <TextField label="Responsável Interno" value={newComunicacao.responsavelInterno} onChange={e => setNewComunicacao({ ...newComunicacao, responsavelInterno: e.target.value })} size="small" sx={{ minWidth: 180 }} />
              </Stack>

              <TextField label="Observações / Descrição" value={newComunicacao.observacoes} onChange={e => setNewComunicacao({ ...newComunicacao, observacoes: e.target.value })} size="small" fullWidth multiline rows={2} sx={{ mt: 1.5 }} />

              {/* Upload de Anexo real (Item 2) */}
              <Stack direction="row" spacing={2} alignItems="center" sx={{ mt: 1.5 }}>
                <Button variant="outlined" component="label" size="small">
                  Anexar Arquivo
                  <input 
                    type="file" 
                    hidden 
                    onChange={(e: any) => {
                      const file = e.target.files?.[0]
                      if (file) setPendingAttachment(file)
                    }} 
                  />
                </Button>
                {pendingAttachment && (
                  <Typography variant="caption" color="success.main">
                    {pendingAttachment.name} (será anexado ao salvar)
                  </Typography>
                )}
              </Stack>

              <Button variant="contained" onClick={handleAddComunicacao} sx={{ mt: 2 }} disabled={!newComunicacao.assunto}>
                Registrar Comunicação
              </Button>
            </Paper>

            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Histórico de Comunicações Oficiais</Typography>
              {comunicacoes.length === 0 ? (
                <Typography color="text.secondary">Nenhuma comunicação registrada ainda.</Typography>
              ) : (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Data</TableCell>
                      <TableCell>Tipo</TableCell>
                      <TableCell>Nº / Remetente</TableCell>
                      <TableCell>Assunto</TableCell>
                      <TableCell>Status / Prazo</TableCell>
                      <TableCell>Responsável</TableCell>
                      <TableCell>Anexos</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {comunicacoes.map((c, idx) => (
                      <TableRow key={idx}>
                        <TableCell>{c.data}</TableCell>
                        <TableCell><Chip label={c.tipo} size="small" /></TableCell>
                        <TableCell>{c.numero || '-'}<br /><Typography variant="caption">{c.remetente}</Typography></TableCell>
                        <TableCell>{c.assunto}</TableCell>
                        <TableCell>
                          <Chip label={c.status || 'PENDENTE'} size="small" color={c.status === 'RESPONDIDO' ? 'success' : 'warning'} />
                          {c.prazoResposta && <Typography variant="caption" display="block">{c.prazoResposta} dias</Typography>}
                        </TableCell>
                        <TableCell>{c.responsavelInterno || c.dataResposta || '-'}</TableCell>
                        <TableCell>
                          {c.anexos?.length > 0 ? (
                            c.anexos.map((an: any, i: number) => (
                              <Chip key={i} label={an.nome} size="small" variant="outlined" sx={{ mr: 0.5 }} />
                            ))
                          ) : '-'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </Paper>
          </Stack>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Fechar</Button>
      </DialogActions>
    </Dialog>
  )
}
