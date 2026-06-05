/**
 * BiddingDetailDialog — Master-detail para Licitação (Wave 1)
 * Tabs: Visão Geral | Lotes | Planilhas Vencedoras (versionamento + marcar vencedora)
 * Permite criar Contrato a partir da planilha vencedora.
 */
import { useState, useEffect } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography, Tabs, Tab, Alert, CircularProgress, Stack, Chip, Paper, TextField, MenuItem
} from '@mui/material'
import { apiGet, apiPost, apiPut, apiDelete } from '../../api/client'
import { Bidding, BiddingLot, WinningSpreadsheet, CreateBiddingLotRequest } from '../../api/types'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import MassAllocationDialog from '../../features/rh/MassAllocationDialog'
import ContractFormDialog from '../../features/contracts/ContractFormDialog'
import BiddingExtendedTabs from './BiddingExtendedTabs'
import { useImportSpreadsheet } from '../../api/hooks/useBiddings'
import { ColDef } from 'ag-grid-community'

type ExtendedTab = 'propostas' | 'prazos' | 'pncp' | 'impugnacoes' | 'certidoes' | 'dre' | null

interface Props {
  open: boolean
  bidding: Bidding | null
  onClose: () => void
  onRefresh?: () => void
}

export default function BiddingDetailDialog({ open, bidding, onClose, onRefresh }: Props) {
  const [tab, setTab] = useState(0)
  const [lots, setLots] = useState<BiddingLot[]>([])
  const [sheets, setSheets] = useState<WinningSpreadsheet[]>([])
  const [postos, setPostos] = useState<any[]>([]) // BiddingPosto
  const [loading, setLoading] = useState(false)
  const [massAllocationOpen, setMassAllocationOpen] = useState(false)
  const [openContractForm, setOpenContractForm] = useState(false)
  const [extendedTab, setExtendedTab] = useState<ExtendedTab>(null)
  const importSheet = useImportSpreadsheet(bidding?.id || '')
  const { showNotification } = useNotification()

  // Planilha vencedora (usada para pré-preencher o ContractForm + importar Postos)
  const vencedora = sheets.find(s => s.isVencedora) || sheets[0] || null

  // Form for new planned Posto
  const [newPosto, setNewPosto] = useState({
    biddingLotId: '',
    nome: '',
    funcao: '',
    cbo: '',
    escala: '',
    jornadaHoras: '',
    valorMensal: '',
    localExecucao: '',
    municipioExecucao: ''
  })

  // Campos para Edital e Vencedor (resolvendo pendência do usuário)
  const [editalUrl, setEditalUrl] = useState(bidding?.editalUrl || '')
  const [vencedorInfo, setVencedorInfo] = useState({
    empresa: bidding?.vencedorEmpresa || '',
    valor: bidding?.valorVencedor || '',
    dataHomologacao: bidding?.dataHomologacao || ''
  })

  const loadData = async () => {
    if (!bidding) return
    setLoading(true)
    try {
      const [lotsData, sheetsData, postosData] = await Promise.all([
        apiGet<BiddingLot[]>(`/biddings/${bidding.id}/lots`),
        apiGet<WinningSpreadsheet[]>(`/biddings/${bidding.id}/winning-spreadsheets`),
        apiGet<any[]>(`/biddings/${bidding.id}/postos`).catch(() => []), // may not exist yet
      ])
      setLots(lotsData || [])
      setSheets(sheetsData || [])
      setPostos(postosData || [])
    } catch (e: any) {
      showNotification(`Erro ao carregar detalhes da licitação: ${e.message}`, 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open && bidding) {
      setTab(0)
      setExtendedTab(null)
      loadData()
    }
  }, [open, bidding?.id])

  const handleTabChange = (_: unknown, v: number) => {
    setTab(v)
    const extended: ExtendedTab[] = [null, null, null, null, 'propostas', 'prazos', 'pncp', 'impugnacoes', 'certidoes', 'dre']
    setExtendedTab(extended[v] ?? null)
  }

  const lotColumns: ColDef<BiddingLot>[] = [
    { headerName: 'Nº Lote', field: 'numeroLote' },
    { headerName: 'Descrição', field: 'descricao', flex: 1 },
    { headerName: 'Postos', field: 'quantitativoPostos' },
    { headerName: 'Valor Mensal', field: 'valorMensal', valueFormatter: p => p.value ? `R$ ${Number(p.value).toLocaleString('pt-BR')}` : '-' },
  ]

  const sheetColumns: ColDef<WinningSpreadsheet>[] = [
    { headerName: 'Versão', field: 'versao' },
    { headerName: 'Arquivo', field: 'arquivoNome' },
    { headerName: 'Vencedora?', field: 'isVencedora', cellRenderer: (p: any) => p.value ? <Chip label="VENCEDORA" color="success" size="small" /> : '—' },
    { headerName: 'Criada em', field: 'createdAt', valueFormatter: p => p.value ? new Date(p.value).toLocaleDateString('pt-BR') : '' },
  ]

  const handleAddLot = async () => {
    if (!bidding) return
    const numero = prompt('Número do lote?')
    if (!numero) return
    try {
      await apiPost(`/biddings/${bidding.id}/lots`, { numeroLote: numero, quantitativoPostos: 1 } as CreateBiddingLotRequest)
      await loadData()
      onRefresh?.()
    } catch (e: any) {
      showNotification(e.message, 'error')
    }
  }

  const handleMarkVencedora = async (sheet: WinningSpreadsheet) => {
    if (!bidding || !confirm('Marcar esta versão como vencedora?')) return
    try {
      await apiPost(`/winning-spreadsheets/${sheet.id}/set-vencedora`, {})
      await loadData()
      showNotification('Planilha marcada como vencedora!', 'success')
    } catch (e: any) {
      showNotification(e.message, 'error')
    }
  }

  const handleAddPosto = async () => {
    if (!bidding || !newPosto.nome || !newPosto.biddingLotId) {
      showNotification('Selecione o Lote e preencha o nome do posto', 'error')
      return
    }
    if (!newPosto.localExecucao || !newPosto.municipioExecucao) {
      showNotification('Local de Trabalho e Município são obrigatórios (eSocial + fiscalização contratual)', 'warning')
      // still allow submission, but warn strongly
    }
    try {
      const postoToSave = {
        ...newPosto,
        jornadaHoras: newPosto.jornadaHoras ? parseFloat(newPosto.jornadaHoras) : undefined,
        valorMensal: newPosto.valorMensal ? parseFloat(newPosto.valorMensal) : undefined,
      }
      await apiPost(`/biddings/${bidding.id}/postos`, postoToSave)
      showNotification('Posto planejado adicionado com sucesso!', 'success')
      setNewPosto({ biddingLotId: '', nome: '', funcao: '', cbo: '', escala: '', jornadaHoras: '', valorMensal: '', localExecucao: '', municipioExecucao: '' })
      await loadData()
    } catch (e: any) {
      showNotification(e.message || 'Erro ao adicionar posto', 'error')
    }
  }

  const handleEditPosto = (posto: any) => {
    setNewPosto({
      biddingLotId: posto.biddingLotId || '',
      nome: posto.nome || '',
      funcao: posto.funcao || '',
      cbo: posto.cbo || '',
      escala: posto.escala || '',
      jornadaHoras: posto.jornadaHoras?.toString() || '',
      valorMensal: posto.valorMensal?.toString() || '',
      localExecucao: posto.localExecucao || '',
      municipioExecucao: posto.municipioExecucao || '',
    })
    // Simple approach: remove old and re-add (in real app use proper edit endpoint)
    setPostos(prev => prev.filter(p => p.id !== posto.id))
  }

  const handleDeletePosto = async (postoId: any) => {
    if (!confirm('Remover este posto planejado?')) return
    try {
      await apiDelete(`/biddings/postos/${postoId}`)
      await loadData()
      showNotification('Posto removido', 'success')
    } catch (e: any) {
      showNotification(e.message || 'Erro ao remover posto', 'error')
    }
  }

  const handleSaveOverview = async () => {
    if (!bidding) return
    try {
      await apiPut(`/biddings/${bidding.id}`, {
        ...bidding,
        editalUrl,
        vencedorEmpresa: vencedorInfo.empresa,
        valorVencedor: vencedorInfo.valor ? parseFloat(String(vencedorInfo.valor)) : bidding.valorVencedor,
        dataHomologacao: vencedorInfo.dataHomologacao || bidding.dataHomologacao,
        orgao: bidding.orgao,
      })
      showNotification('Dados da licitação salvos', 'success')
      onRefresh?.()
    } catch (e: any) {
      showNotification(e.message || 'Erro ao salvar', 'error')
    }
  }

  if (!bidding) return null

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="lg"
      fullWidth
      PaperProps={{ sx: { maxHeight: '94dvh', display: 'flex', flexDirection: 'column' } }}
    >
      <DialogTitle sx={{ flexShrink: 0, pb: 1.25 }}>
        Licitação {bidding.editalNumero || bidding.processoNumero} — {bidding.orgao}
      </DialogTitle>

      <DialogContent
        dividers
        sx={{ flex: 1, overflowY: 'auto', pb: 2.25, display: 'flex', flexDirection: 'column' }}
      >
        <Tabs value={tab} onChange={handleTabChange} sx={{ mb: 2 }} variant="scrollable" scrollButtons="auto">
          <Tab label="Visão Geral" />
          <Tab label={`Lotes (${lots.length})`} />
          <Tab label={`Postos (${postos.length})`} />
          <Tab label={`Planilhas (${sheets.length})`} />
          <Tab label="Propostas" />
          <Tab label="Prazos" />
          <Tab label="PNCP" />
          <Tab label="Impugnações" />
          <Tab label="Certidões" />
          <Tab label="DRE / Margem" />
        </Tabs>

        {loading && <CircularProgress />}

        {extendedTab && bidding && (
          <BiddingExtendedTabs bidding={bidding} active={extendedTab} />
        )}

        {!extendedTab && tab === 0 && !loading && (
          <Stack spacing={2}>
            <Typography variant="body1">{bidding.objeto || 'Sem objeto'}</Typography>
            <Typography color="text.secondary">Status: <strong>{bidding.status}</strong> | Valor Vencedor: R$ {bidding.valorVencedor?.toLocaleString('pt-BR') || '—'}</Typography>

            {/* Edital + Empresa Vencedora (resolvendo a pendência do usuário) */}
            <Paper variant="outlined" sx={{ p: 2, mt: 1 }}>
              <Typography variant="subtitle2" gutterBottom>Edital e Resultado da Licitação</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField 
                  label="Link do Edital (PDF)" 
                  value={editalUrl} 
                  onChange={e => setEditalUrl(e.target.value)} 
                  size="small" 
                  fullWidth 
                  placeholder="https://pncp.gov.br/... ou arquivo" 
                />
                <Button 
                  variant="outlined" 
                  size="small" 
                  component="label"
                  onChange={async (e: any) => {
                    const file = e.target.files?.[0]
                    if (!file || !bidding) return
                    const formData = new FormData()
                    formData.append('file', file)
                    try {
                      const res = await apiPost<any>(`/biddings/${bidding.id}/edital-upload`, formData)
                      setEditalUrl(res?.url || res?.path || URL.createObjectURL(file))
                      showNotification('Edital enviado com sucesso!', 'success')
                    } catch {
                      setEditalUrl(URL.createObjectURL(file))
                      showNotification('Edital anexado localmente (backend indisponível)', 'warning')
                    }
                  }}
                >
                  Upload Edital
                  <input type="file" hidden accept=".pdf,.doc,.docx" />
                </Button>
              </Stack>

              <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>Empresa Vencedora</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField label="Empresa Vencedora" value={vencedorInfo.empresa} onChange={e => setVencedorInfo({...vencedorInfo, empresa: e.target.value})} size="small" sx={{ minWidth: 200 }} />
                <TextField label="Valor Vencedor" value={vencedorInfo.valor} onChange={e => setVencedorInfo({...vencedorInfo, valor: e.target.value})} size="small" sx={{ minWidth: 140 }} />
                <TextField label="Data Homologação" type="date" value={vencedorInfo.dataHomologacao} onChange={e => setVencedorInfo({...vencedorInfo, dataHomologacao: e.target.value})} size="small" InputLabelProps={{ shrink: true }} />
              </Stack>
              <Button variant="contained" size="small" sx={{ mt: 2 }} onClick={handleSaveOverview}>
                Salvar edital / vencedor
              </Button>
            </Paper>

            <Alert severity="info">
              Fluxo correto: Planeje os Postos aqui na licitação → Após homologação, crie o Contrato → Os Postos são materializados no contrato com os dados da planilha vencedora.
            </Alert>
          </Stack>
        )}

        {!extendedTab && tab === 1 && !loading && (
          <>
            <Button variant="outlined" size="small" onClick={handleAddLot} sx={{ mb: 1 }}>Adicionar Lote</Button>
            <EnterpriseDataGrid title="Lotes da Licitação" rowData={lots} columnDefs={lotColumns} height={320} />
          </>
        )}

        {/* === ABA POSTOS PLANEJADOS (novo - completo para terceirização) === */}
        {!extendedTab && tab === 2 && !loading && (
          <Stack spacing={2}>
            <Alert severity="info">
              Cadastre aqui os Postos que serão executados por lote. Inclua <strong>Local de Trabalho</strong> e <strong>Município</strong> (obrigatórios para eSocial e fiscalização contratual). Depois use "Alocação em Massa" para ligar funcionários.
            </Alert>

            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Adicionar Posto Planejado</Typography>
              <Stack direction="row" spacing={1.5} flexWrap="wrap">
                <TextField 
                  select 
                  label="Lote *" 
                  value={newPosto.biddingLotId} 
                  onChange={e => setNewPosto({...newPosto, biddingLotId: e.target.value})}
                  sx={{ minWidth: 160 }}
                >
                  {lots.map(l => <MenuItem key={l.id} value={l.id}>{l.numeroLote} - {l.descricao}</MenuItem>)}
                </TextField>
                <TextField label="Nome do Posto *" value={newPosto.nome} onChange={e => setNewPosto({...newPosto, nome: e.target.value})} />
                <TextField label="Função" value={newPosto.funcao} onChange={e => setNewPosto({...newPosto, funcao: e.target.value})} />
                <TextField label="CBO" value={newPosto.cbo} onChange={e => setNewPosto({...newPosto, cbo: e.target.value})} sx={{ width: 100 }} />
                <TextField label="Escala" value={newPosto.escala} onChange={e => setNewPosto({...newPosto, escala: e.target.value})} />
                <TextField label="Jornada (h)" type="number" value={newPosto.jornadaHoras} onChange={e => setNewPosto({...newPosto, jornadaHoras: e.target.value})} sx={{ width: 100 }} />
                <TextField label="Valor Mensal" type="number" value={newPosto.valorMensal} onChange={e => setNewPosto({...newPosto, valorMensal: e.target.value})} />
                <TextField label="Local de Trabalho" value={newPosto.localExecucao} onChange={e => setNewPosto({...newPosto, localExecucao: e.target.value})} sx={{ minWidth: 180 }} />
                <TextField label="Município" value={newPosto.municipioExecucao} onChange={e => setNewPosto({...newPosto, municipioExecucao: e.target.value})} sx={{ width: 140 }} />
                <Button variant="contained" onClick={handleAddPosto}>Adicionar Posto</Button>
              </Stack>
            </Paper>

            <EnterpriseDataGrid 
              title="Postos Planejados na Licitação" 
              rowData={postos} 
              columnDefs={[
                { headerName: 'Nome', field: 'nome', flex: 1 },
                { headerName: 'Função', field: 'funcao' },
                { headerName: 'Local', field: 'localExecucao' },
                { headerName: 'Município', field: 'municipioExecucao' },
                { headerName: 'Valor Mensal', field: 'valorMensal', valueFormatter: p => p.value ? `R$ ${Number(p.value).toLocaleString('pt-BR')}` : '-' },
                {
                  headerName: 'Ações',
                  minWidth: 140,
                  cellRenderer: (params: any) => (
                    <Stack direction="row" spacing={0.5}>
                      <Button size="small" onClick={() => handleEditPosto(params.data)}>Editar</Button>
                      <Button size="small" color="error" onClick={() => handleDeletePosto(params.data.id)}>Excluir</Button>
                    </Stack>
                  )
                }
              ]} 
              height={280} 
            />

            <Button 
              variant="outlined" 
              sx={{ mt: 1.5 }} 
              onClick={() => setMassAllocationOpen(true)}
            >
              Abrir Alocação em Massa de Funcionários para os Postos desta Licitação
            </Button>
          </Stack>
        )}

        {!extendedTab && tab === 3 && !loading && (
          <>
            <Alert severity="info" sx={{ mb: 1.5 }}>Importe CSV ou Excel (.xlsx). Colunas: Nome; Função; CBO; Escala; Valor; Local; Município</Alert>
            <Button variant="outlined" size="small" component="label" sx={{ mb: 1 }}>
              Importar planilha (.xlsx / .csv)
              <input type="file" hidden accept=".xlsx,.xls,.csv" onChange={async (e) => {
                const file = e.target.files?.[0]
                if (!file || !bidding) return
                try {
                  const res = await importSheet.mutateAsync({ file, markVencedora: false })
                  showNotification(`Importados ${res.postosImportados} postos (${res.formato})`, 'success')
                  loadData()
                } catch (err: any) {
                  showNotification(err.message || 'Erro na importação', 'error')
                }
              }} />
            </Button>
            <EnterpriseDataGrid
              title="Planilhas"
              rowData={sheets}
              columnDefs={sheetColumns}
              height={320}
              // Simple action column via custom renderer would be better; for now user can use external button
            />
            <Stack direction="row" spacing={1} mt={1}>
              {sheets.map(s => (
                <Button key={s.id} size="small" variant="outlined" onClick={() => handleMarkVencedora(s)}>
                  Marcar v{s.versao} como vencedora
                </Button>
              ))}
            </Stack>
          </>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Fechar</Button>

        <Button 
          variant="outlined" 
          onClick={() => setMassAllocationOpen(true)}
        >
          Alocação em Massa de Funcionários nos Postos
        </Button>

        <Button 
          variant="contained" 
          color="primary"
          onClick={() => {
            const message = vencedora 
              ? `Criando contrato a partir da licitação ${bidding.editalNumero} + Planilha v${vencedora.versao}`
              : `Criando contrato a partir da licitação ${bidding.editalNumero}`
            
            showNotification(message, 'info')
            
            // Melhorado: Abre o formulário de contrato já preenchido com dados da licitação
            // e permite importar os Postos Planejados
            setOpenContractForm(true)
          }}
        >
          Criar Contrato a partir desta licitação
        </Button>
      </DialogActions>

      <MassAllocationDialog
        open={massAllocationOpen}
        onClose={() => setMassAllocationOpen(false)}
        biddingId={bidding.id}
        onSuccess={() => {
          setMassAllocationOpen(false)
          showNotification('Alocações em massa realizadas com sucesso!', 'success')
          loadData()
        }}
      />

      {/* Novo: Abre o formulário de contrato já com dados da licitação + opção de importar Postos */}
      <ContractFormDialog
        open={openContractForm}
        onClose={() => setOpenContractForm(false)}
        onSaved={() => {
          setOpenContractForm(false)
          onRefresh?.()
          showNotification('Contrato criado com sucesso! Postos da licitação podem ser importados no detalhe do contrato.', 'success')
        }}
        initialData={{
          biddingId: bidding?.id,
          orgao: bidding?.orgao,
          objeto: bidding?.objeto,
          winningSpreadsheetId: vencedora?.id,
        }}
        plannedPostsFromBidding={postos}   // Passa os postos planejados para importar
      />
    </Dialog>
  )
}
