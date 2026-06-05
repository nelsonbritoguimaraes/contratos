/**
 * Abas estendidas do detalhe de licitação: Propostas, Prazos, PNCP, Impugnações, Certidões, DRE
 */
import { useState } from 'react'
import {
  Typography, Paper, Stack, Button, TextField, Alert, Chip, MenuItem, Table, TableBody, TableCell,
  TableHead, TableRow
} from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import {
  useBiddingProposals, useCreateBiddingProposal, useBiddingDeadlines, useCreateBiddingDeadline,
  useBiddingImpugnacoes, useCreateBiddingImpugnacao, useBiddingCertidoes, useBiddingDre
} from '../../api/hooks/useBiddings'
import { apiGet } from '../../api/client'
import { Bidding } from '../../api/types'

type TabKey = 'propostas' | 'prazos' | 'pncp' | 'impugnacoes' | 'certidoes' | 'dre'

interface Props {
  bidding: Bidding
  active: TabKey
}

export default function BiddingExtendedTabs({ bidding, active }: Props) {
  const { showNotification } = useNotification()
  const biddingId = bidding.id!

  const { data: proposals = [], refetch: refetchProp } = useBiddingProposals(biddingId)
  const createProposal = useCreateBiddingProposal(biddingId)
  const { data: deadlines = [], refetch: refetchDead } = useBiddingDeadlines(biddingId)
  const createDeadline = useCreateBiddingDeadline(biddingId)
  const { data: impugnacoes = [], refetch: refetchImp } = useBiddingImpugnacoes(biddingId)
  const createImpugnacao = useCreateBiddingImpugnacao(biddingId)
  const { data: dre } = useBiddingDre(biddingId)

  const [cnpjCert, setCnpjCert] = useState('')
  const { data: certidoes = [], refetch: refetchCert } = useBiddingCertidoes(biddingId, cnpjCert)

  const [pncpTermo, setPncpTermo] = useState('')
  const [pncpResult, setPncpResult] = useState<any>(null)

  const [novaProposta, setNovaProposta] = useState({ cenario: 'BASE', valorProposta: '', custoTotal: '' })
  const [novoPrazo, setNovoPrazo] = useState({ tipo: 'SESSAO_PUBLICA', descricao: '', dataLimite: '' })
  const [novaImp, setNovaImp] = useState({ tipo: 'IMPUGNACAO', protocolo: '', argumentos: '' })

  const searchPncp = async () => {
    try {
      const res = await apiGet<any>(`/biddings/pncp/search?termo=${encodeURIComponent(pncpTermo)}`)
      setPncpResult(res)
    } catch (e: any) {
      showNotification(e.message || 'Erro PNCP', 'error')
    }
  }

  if (active === 'propostas') {
    return (
      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>Nova proposta comercial</Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap">
            <TextField select label="Cenário" size="small" value={novaProposta.cenario}
              onChange={(e) => setNovaProposta({ ...novaProposta, cenario: e.target.value })} sx={{ minWidth: 120 }}>
              {['PESSIMISTA', 'BASE', 'OTIMISTA'].map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
            </TextField>
            <TextField label="Valor proposta" size="small" type="number" value={novaProposta.valorProposta}
              onChange={(e) => setNovaProposta({ ...novaProposta, valorProposta: e.target.value })} />
            <TextField label="Custo total" size="small" type="number" value={novaProposta.custoTotal}
              onChange={(e) => setNovaProposta({ ...novaProposta, custoTotal: e.target.value })} />
            <Button variant="contained" size="small" onClick={async () => {
              await createProposal.mutateAsync({
                cenario: novaProposta.cenario,
                valorProposta: novaProposta.valorProposta ? parseFloat(novaProposta.valorProposta) : undefined,
                custoTotal: novaProposta.custoTotal ? parseFloat(novaProposta.custoTotal) : undefined,
              })
              refetchProp()
              showNotification('Proposta registrada', 'success')
            }}>Salvar</Button>
          </Stack>
        </Paper>
        <EnterpriseDataGrid
          title="Propostas"
          rowData={proposals}
          columnDefs={[
            { headerName: 'Versão', field: 'versao', width: 90 },
            { headerName: 'Cenário', field: 'cenario' },
            { headerName: 'Valor', field: 'valorProposta' },
            { headerName: 'Custo', field: 'custoTotal' },
            { headerName: 'Margem %', field: 'margemEstimadaPct' },
            { headerName: 'Status', field: 'status' },
          ]}
          height={260}
        />
      </Stack>
    )
  }

  if (active === 'prazos') {
    return (
      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Stack direction="row" spacing={1} flexWrap="wrap">
            <TextField select label="Tipo" size="small" value={novoPrazo.tipo}
              onChange={(e) => setNovoPrazo({ ...novoPrazo, tipo: e.target.value })} sx={{ minWidth: 180 }}>
              {['ESCLARECIMENTO', 'IMPUGNACAO', 'SESSAO_PUBLICA', 'PROPOSTA', 'HABILITACAO', 'HOMOLOGACAO'].map((t) => (
                <MenuItem key={t} value={t}>{t}</MenuItem>
              ))}
            </TextField>
            <TextField label="Descrição" size="small" value={novoPrazo.descricao}
              onChange={(e) => setNovoPrazo({ ...novoPrazo, descricao: e.target.value })} />
            <TextField label="Data limite" type="datetime-local" size="small" value={novoPrazo.dataLimite}
              onChange={(e) => setNovoPrazo({ ...novoPrazo, dataLimite: e.target.value })} InputLabelProps={{ shrink: true }} />
            <Button variant="contained" size="small" onClick={async () => {
              if (!novoPrazo.dataLimite) return
              await createDeadline.mutateAsync({
                tipo: novoPrazo.tipo,
                descricao: novoPrazo.descricao,
                dataLimite: new Date(novoPrazo.dataLimite).toISOString(),
              })
              refetchDead()
              showNotification('Prazo cadastrado', 'success')
            }}>Adicionar</Button>
          </Stack>
        </Paper>
        <EnterpriseDataGrid
          title="Calendário de prazos"
          rowData={deadlines}
          columnDefs={[
            { headerName: 'Tipo', field: 'tipo' },
            { headerName: 'Descrição', field: 'descricao', flex: 1 },
            { headerName: 'Limite', field: 'dataLimite' },
            { headerName: 'Concluído', field: 'concluido', cellRenderer: (p: any) => (
              <Chip size="small" label={p.value ? 'Sim' : 'Não'} color={p.value ? 'success' : 'warning'} />
            )},
          ]}
          height={280}
        />
      </Stack>
    )
  }

  if (active === 'pncp') {
    return (
      <Stack spacing={2}>
        <Alert severity="info">Consulta pública PNCP (Lei 14.133) — importe oportunidades para cadastro manual.</Alert>
        <Stack direction="row" spacing={1}>
          <TextField label="Palavra-chave / objeto" size="small" fullWidth value={pncpTermo}
            onChange={(e) => setPncpTermo(e.target.value)} placeholder="terceirização, vigilância..." />
          <Button variant="contained" onClick={searchPncp}>Buscar PNCP</Button>
        </Stack>
        {pncpResult?.erro && <Alert severity="warning">{String(pncpResult.erro)}</Alert>}
        <Paper variant="outlined" sx={{ p: 2, maxHeight: 320, overflow: 'auto' }}>
          <Typography variant="caption" color="text.secondary">Resultado bruto PNCP</Typography>
          <pre style={{ fontSize: 11, whiteSpace: 'pre-wrap' }}>{JSON.stringify(pncpResult?.data ?? pncpResult, null, 2)}</pre>
        </Paper>
      </Stack>
    )
  }

  if (active === 'impugnacoes') {
    return (
      <Stack spacing={2}>
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Stack direction="row" spacing={1} flexWrap="wrap">
            <TextField select label="Tipo" size="small" value={novaImp.tipo}
              onChange={(e) => setNovaImp({ ...novaImp, tipo: e.target.value })} sx={{ minWidth: 160 }}>
              {['IMPUGNACAO', 'ESCLARECIMENTO', 'RECURSO', 'CONTRARRAZOES'].map((t) => (
                <MenuItem key={t} value={t}>{t}</MenuItem>
              ))}
            </TextField>
            <TextField label="Protocolo" size="small" value={novaImp.protocolo}
              onChange={(e) => setNovaImp({ ...novaImp, protocolo: e.target.value })} />
            <TextField label="Argumentos" size="small" fullWidth value={novaImp.argumentos}
              onChange={(e) => setNovaImp({ ...novaImp, argumentos: e.target.value })} />
            <Button variant="contained" size="small" onClick={async () => {
              await createImpugnacao.mutateAsync(novaImp)
              refetchImp()
              showNotification('Registro salvo', 'success')
            }}>Registrar</Button>
          </Stack>
        </Paper>
        <EnterpriseDataGrid
          title="Impugnações / recursos"
          rowData={impugnacoes}
          columnDefs={[
            { headerName: 'Tipo', field: 'tipo' },
            { headerName: 'Protocolo', field: 'protocolo' },
            { headerName: 'Status', field: 'status' },
            { headerName: 'Argumentos', field: 'argumentos', flex: 1 },
          ]}
          height={280}
        />
      </Stack>
    )
  }

  if (active === 'certidoes') {
    return (
      <Stack spacing={2}>
        <Stack direction="row" spacing={1}>
          <TextField label="CNPJ empresa" size="small" value={cnpjCert}
            onChange={(e) => setCnpjCert(e.target.value)} placeholder="00000000000000" />
          <Button variant="contained" onClick={() => refetchCert()} disabled={cnpjCert.length < 14}>
            Consultar certidões
          </Button>
        </Stack>
        {certidoes.length === 0 ? (
          <Alert severity="info">Informe CNPJ e consulte RF, FGTS, CNDT e SICAF.</Alert>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Tipo</TableCell>
                <TableCell>Órgão</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Validade</TableCell>
                <TableCell>Mensagem</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {certidoes.map((c: any, i: number) => (
                <TableRow key={i}>
                  <TableCell>{c.tipo}</TableCell>
                  <TableCell>{c.orgao}</TableCell>
                  <TableCell><Chip size="small" label={c.status} color={c.status === 'REGULAR' || c.status === 'HABILITADO' ? 'success' : 'warning'} /></TableCell>
                  <TableCell>{c.validade || '—'}</TableCell>
                  <TableCell>{c.mensagem}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Stack>
    )
  }

  if (active === 'dre') {
    const d = dre as any
    return (
      <Stack spacing={2}>
        {!d ? <Alert severity="info">Carregando DRE...</Alert> : (
          <>
            <Stack direction="row" spacing={1} flexWrap="wrap">
              <Chip label={`Receita ref: R$ ${Number(d.receitaReferencia ?? 0).toLocaleString('pt-BR')}`} />
              <Chip label={`Custo folha: R$ ${Number(d.custoFolhaReal ?? 0).toLocaleString('pt-BR')}`} />
              <Chip label={`Margem: ${Number(d.margemPercentual ?? 0).toFixed(1)}%`}
                color={Number(d.margemPercentual) >= 10 ? 'success' : 'warning'} />
              <Chip label={`Contratos: ${d.contratosVinculados ?? 0}`} />
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow><TableCell>Conta</TableCell><TableCell align="right">Valor (R$)</TableCell></TableRow>
              </TableHead>
              <TableBody>
                {(d.linhas as any[] || []).map((l, i) => (
                  <TableRow key={i}>
                    <TableCell>{l.conta}</TableCell>
                    <TableCell align="right">{Number(l.valor).toLocaleString('pt-BR')}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Alert severity="info">
              DRE integrado: medições/contratos × folha real × postos planejados da licitação {bidding.editalNumero}.
            </Alert>
          </>
        )}
      </Stack>
    )
  }

  return null
}
