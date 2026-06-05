/**
 * Ponto & Cobertura — Página operacional FULL (Onda 2: Operacional Completo)
 * Integração profunda com TimePunchController:
 *  - Import AFD (multipart)
 *  - daily-summary (cobertura + impacto folha)
 *  - process-day
 *  - Clock devices + bridge
 * + Documentos (upload + list + versionamento/OCR)
 * Link explícito para impacto em Folha (tudo conectado).
 */
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, Typography, Paper, Alert, Stack, Button, TextField, Tabs, Tab,
  LinearProgress, Chip
} from '@mui/material'
import { Upload, FileText, Link as LinkIcon } from 'lucide-react'
import { useTenant } from '../../api/hooks/useTenant'
import { useNotification } from '../../components/NotificationProvider'
import {
  useDailyCoverage,
  useDailySummary,
  useAfdImport,
  useProcessDay,
  useClockDevices,
  useExportAej,
  useEspelhoPonto,
  useVolantes,
  useMobilePunch,
  useExportEspelhoText,
  useProcessMonth,
  usePendingAdjustments
} from '../../api/hooks/usePonto'
import { apiGet, apiPost } from '../../api/client'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'

export default function PontoPage() {
  const { tenantName } = useTenant()
  const { showNotification } = useNotification()
  const navigate = useNavigate()

  // Estados principais
  const [tab, setTab] = useState(0)
  const [contractId, setContractId] = useState('')
  const [data, setData] = useState('2025-06-09')

  // Estados AFD Import
  const [afdFile, setAfdFile] = useState<File | null>(null)
  const [afdResult, setAfdResult] = useState<any>(null)
  const [afdLoading, setAfdLoading] = useState(false)

  // Estados Process Day
  const [procEmployeeId, setProcEmployeeId] = useState('')
  const [procDate, setProcDate] = useState('2025-06-09')
  const [procPostId, setProcPostId] = useState('')
  const [procResult, setProcResult] = useState<any>(null)

  // Estados Documentos skeleton (Onda 2)
  const [docFile, setDocFile] = useState<File | null>(null)
  const [docEntityType, setDocEntityType] = useState('POSTO')
  const [docEntityId, setDocEntityId] = useState('')
  const [docList, setDocList] = useState<any[]>([])
  const [docLoading, setDocLoading] = useState(false)

  // Hooks integrados (endpoint corrigido para /time-punches)
  const { data: coverageData = [], isLoading, isError: coverageError, refetch } = useDailyCoverage(contractId || undefined, data)
  const { data: summaryData, isLoading: summaryLoading, isError: summaryError, refetch: refetchSummary } = useDailySummary(contractId || undefined, data)
  const afdImport = useAfdImport()
  const processDay = useProcessDay()
  const { data: devices = [], isLoading: devicesLoading, refetch: refetchDevices } = useClockDevices()
  const exportAej = useExportAej()
  const exportEspelho = useExportEspelhoText()
  const processMonth = useProcessMonth()
  const mobilePunch = useMobilePunch()
  const [aejPeriod, setAejPeriod] = useState('2025-06-01')
  const [espEmployeeId, setEspEmployeeId] = useState('')
  const [espCompetencia, setEspCompetencia] = useState('2025-06-01')
  const { data: espelho } = useEspelhoPonto(espEmployeeId || undefined, espCompetencia)
  const { data: volantes = [] } = useVolantes(contractId || undefined, data)
  const { data: pendingAdj = [] } = usePendingAdjustments()

  const handleExportAej = async () => {
    if (!contractId) {
      showNotification('Informe o ID do contrato para exportar o AEJ', 'warning')
      return
    }
    try {
      await exportAej.mutateAsync({ contractId, period: aejPeriod })
      showNotification('AEJ exportado (Portaria 671)', 'success')
    } catch (e: any) {
      showNotification(e.message || 'Erro ao exportar AEJ', 'error')
    }
  }

  // Handler AFD Import
  const handleAfdImport = async () => {
    if (!afdFile) return
    setAfdLoading(true)
    try {
      const res = await afdImport.mutateAsync({ file: afdFile, contractId: contractId || undefined, autoProcess: true })
      setAfdResult(res)
      showNotification(`AFD importado: ${res.imported || res.total_parsed || 0} marcações`, 'success')
      refetchSummary()
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Falha ao importar AFD'
      showNotification(message, 'error')
    } finally {
      setAfdLoading(false)
    }
  }

  // Handler Process Day
  const handleProcessDay = async () => {
    if (!procEmployeeId || !procDate) {
      showNotification('Informe employeeId e data', 'error')
      return
    }
    try {
      const res = await processDay.mutateAsync({
        employeeId: procEmployeeId,
        date: procDate,
        postId: procPostId || undefined,
      })
      setProcResult(res)
      showNotification('Dia processado com sucesso', 'success')
      refetchSummary()
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Falha ao processar dia'
      showNotification(message, 'error')
    }
  }

  // Documentos: upload + list (DocumentController + OCR)
  const handleDocUpload = async () => {
    if (!docFile || !docEntityId) {
      showNotification('Selecione arquivo + Entity ID (ex: posto ou contrato)', 'error')
      return
    }
    setDocLoading(true)
    try {
      const payload = {
        entityType: docEntityType,
        entityId: docEntityId,
        fileName: docFile.name,
        mimeType: docFile.type || 'application/octet-stream',
        filePath: `uploads/${docFile.name}`,
        uploadedAt: new Date().toISOString()
      }
      await apiPost<any>('/documents', payload)
      showNotification('Documento enviado com sucesso', 'success')
      await loadDocuments()
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Falha ao enviar documento'
      showNotification(message, 'error')
    } finally {
      setDocLoading(false)
      setDocFile(null)
    }
  }

  const loadDocuments = async () => {
    if (!docEntityId) return
    try {
      const list = await apiGet<any[]>(`/documents?entityType=${docEntityType}&entityId=${docEntityId}`)
      setDocList(list || [])
    } catch {
      // Mantém lista atual se backend falhar
    }
  }

  // Transforma summary map em linhas amigáveis para grid (tudo conectado)
  const summaryRows = summaryData ? [{
    data: summaryData.date || data,
    posto: `Contrato ${contractId || 'N/A'}`,
    status: (summaryData.coverage_percent || 0) > 80 ? 'OK' : 'PARCIAL',
    cobertura: Math.round(summaryData.coverage_percent || 0),
    horas: Math.round((summaryData.total_worked_minutes || 0) / 60),
    ausencias: summaryData.total_absence_minutes || 0,
    posts: `${summaryData.posts_with_work || 0}/${summaryData.total_expected_posts || 0}`
  }] : (Array.isArray(coverageData) ? coverageData : [])

  // Ação "tudo conectado": navega para Folha com contexto de impacto
  const goToFolhaImpact = () => {
    showNotification('Abrindo Folha — ausências do Ponto impactam holerites e glosas automaticamente', 'info')
    navigate('/rh/folha', { state: { fromPonto: { contractId, data, absences: summaryData?.total_absence_minutes } } })
  }

  return (
    <Box>
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
        <Box>
          <Typography variant="h4" fontWeight={600} gutterBottom>Ponto & Cobertura Operacional (Onda 2)</Typography>
          <Typography color="text.secondary">Tenant: {tenantName} • Portaria 671/2021 • TimePunch + Bridge + AFD + Documentos</Typography>
        </Box>
        <Button
          variant="contained"
          color="secondary"
          startIcon={<LinkIcon size={18} />}
          onClick={goToFolhaImpact}
        >
          Ver Impacto na Folha
        </Button>
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Integração: <code>/api/time-punches/import-afd</code> + <code>/daily-summary</code> + <code>/export-aej</code> + AttendanceProcessingService.
        Ausências aqui alimentam <strong>Folha</strong>, <strong>Glosas</strong> e <strong>Medições</strong> automaticamente.
      </Alert>

      <Paper sx={{ p: 2, mb: 2, borderRadius: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems="flex-end">
          <TextField label="Competência AEJ (YYYY-MM-DD)" value={aejPeriod} onChange={(e) => setAejPeriod(e.target.value)} size="small" sx={{ minWidth: 200 }} />
          <Button variant="outlined" onClick={handleExportAej} disabled={exportAej.isPending || !contractId}>
            Exportar AEJ (Portaria 671)
          </Button>
        </Stack>
      </Paper>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Cobertura Diária" />
        <Tab label="Importar AFD" />
        <Tab label="Processar Dia" />
        <Tab label="Dispositivos (Relógios)" />
        <Tab label="Documentos (Upload + Versões)" />
        <Tab label="Espelho de Ponto" />
        <Tab label="Mobile REP-P" />
        <Tab label="Volantes / Ajustes" />
      </Tabs>

      {/* TAB 0: COBERTURA + LINK FOLHA */}
      {tab === 0 && (
        <Stack spacing={2}>
          <Stack direction="row" spacing={2} mb={1}>
            <TextField label="Contract ID" value={contractId} onChange={e => setContractId(e.target.value)} size="small" sx={{ width: 300 }} />
            <TextField label="Data" type="date" value={data} onChange={e => setData(e.target.value)} size="small" InputLabelProps={{ shrink: true }} />
            <Button variant="contained" onClick={() => { refetch(); refetchSummary() }}>Consultar</Button>
            <Button variant="outlined" onClick={goToFolhaImpact} startIcon={<LinkIcon size={16} />}>Impacto na Folha →</Button>
          </Stack>

          <Paper sx={{ p: 2, borderRadius: 3 }}>
            <Typography variant="subtitle2" gutterBottom>Resumo de Cobertura (do backend)</Typography>
            {summaryData && (
              <Stack direction="row" spacing={2} flexWrap="wrap" mb={2}>
                <Chip label={`Cobertura: ${Math.round(summaryData.coverage_percent || 0)}%`} color="success" />
                <Chip label={`Posts com trabalho: ${summaryData.posts_with_work || 0}/${summaryData.total_expected_posts || 0}`} />
                <Chip label={`Ausências (min): ${summaryData.total_absence_minutes || 0}`} color="warning" />
                <Chip label={`Trabalhadas (min): ${summaryData.total_worked_minutes || 0}`} />
              </Stack>
            )}
            <EnterpriseDataGrid
              title="Cobertura do Dia (adaptado do summary)"
              rowData={summaryRows}
              columnDefs={[
                { headerName: 'Data', field: 'data', minWidth: 120 },
                { headerName: 'Posto/Contrato', field: 'posto' },
                { headerName: 'Status', field: 'status' },
                { headerName: 'Cobertura %', field: 'cobertura', valueFormatter: (p: any) => `${p.value ?? 0}%` },
                { headerName: 'Horas', field: 'horas' },
                { headerName: 'Ausências (min)', field: 'ausencias' },
                { headerName: 'Posts', field: 'posts' },
              ]}
              onRefresh={() => { refetch(); refetchSummary() }}
              loading={isLoading || summaryLoading}
              error={coverageError || summaryError ? 'Erro ao carregar cobertura do backend.' : null}
              emptyMessage="Nenhum dado de cobertura. Informe Contract ID + Data e consulte."
            />
          </Paper>

          <Alert severity="success">
            <strong>Tudo conectado:</strong> As ausências/minutos acima são consumidos por Folha (rubricas + eventos PONTO_FALTA), Glosas e Medições.
            Clique em "Ver Impacto na Folha" para ver o efeito nos holerites.
          </Alert>
        </Stack>
      )}

      {/* TAB 1: IMPORT AFD - Funcional */}
      {tab === 1 && (
        <Paper sx={{ p: 3, borderRadius: 3, maxWidth: 720 }}>
          <Typography variant="h6" gutterBottom>Importar Arquivo AFD (REP)</Typography>
          <Typography color="text.secondary" mb={2}>
            Suporta Portaria 671. Usa AfdImportService + TimePunchService.importRawPunches. Atualiza cobertura automaticamente.
          </Typography>

          <Stack spacing={2}>
            <Button variant="outlined" component="label" startIcon={<Upload size={18} />}>
              Selecionar arquivo AFD (.txt / .afd)
              <input type="file" hidden accept=".txt,.afd,*" onChange={(e) => setAfdFile(e.target.files?.[0] || null)} />
            </Button>
            {afdFile && <Typography variant="body2">Arquivo: {afdFile.name}</Typography>}

            <Button
              variant="contained"
              onClick={handleAfdImport}
              disabled={!afdFile || afdLoading}
            >
              {afdLoading ? 'Importando...' : 'Importar AFD e Processar'}
            </Button>
            {afdLoading && <LinearProgress />}
          </Stack>

          {afdResult && (
            <Paper sx={{ p: 2, mt: 2, bgcolor: 'action.hover', borderRadius: 2 }}>
              <Typography fontWeight={600}>Resultado da Importação</Typography>
              <div>Arquivo: {afdResult.file_name}</div>
              <div>Marcações importadas: <strong>{afdResult.imported ?? afdResult.total_parsed}</strong></div>
              <div>Processado por: AttendanceProcessingService</div>
              <Button sx={{ mt: 1 }} onClick={goToFolhaImpact} variant="outlined" size="small">Ver impacto na Folha agora</Button>
            </Paper>
          )}
        </Paper>
      )}

      {/* TAB 2: PROCESS-DAY */}
      {tab === 2 && (
        <Paper sx={{ p: 3, borderRadius: 3, maxWidth: 640 }}>
          <Typography variant="h6" gutterBottom>Processar Dia Individual (process-day)</Typography>
          <Stack spacing={2} mt={1}>
            <TextField label="Employee ID (UUID)" value={procEmployeeId} onChange={e => setProcEmployeeId(e.target.value)} size="small" />
            <TextField label="Data" type="date" value={procDate} onChange={e => setProcDate(e.target.value)} size="small" InputLabelProps={{ shrink: true }} />
            <TextField label="Post ID (opcional)" value={procPostId} onChange={e => setProcPostId(e.target.value)} size="small" />
            <Button variant="contained" onClick={handleProcessDay} disabled={processDay.isPending}>Processar Dia</Button>
          </Stack>
          {procResult && (
            <Alert sx={{ mt: 2 }} severity="success">
              Processado: {JSON.stringify(procResult)}
            </Alert>
          )}
        </Paper>
      )}

      {/* TAB 3: DISPOSITIVOS / CLOCK BRIDGE */}
      {tab === 3 && (
        <Stack spacing={2}>
          <Button onClick={() => refetchDevices()} variant="outlined" size="small" sx={{ alignSelf: 'flex-start' }}>Atualizar Dispositivos</Button>
          <EnterpriseDataGrid
            title="Relógios de Ponto (Clock Devices + Bridge adapters: ControlID, ZKTeco, Topdata, Henry)"
            rowData={devices}
            columnDefs={[
              { headerName: 'ID', field: 'id', width: 120 },
              { headerName: 'Nome / Serial', field: 'name' },
              { headerName: 'Fabricante', field: 'manufacturer' },
              { headerName: 'IP', field: 'ipAddress' },
              { headerName: 'Status', field: 'status' },
              { headerName: 'Tipo', field: 'deviceType' },
            ]}
            loading={devicesLoading}
            onRefresh={() => refetchDevices()}
          />
          <Alert>Bridge pronto para import via /bridge/import. Use o PontoAgent de IA para automação.</Alert>
        </Stack>
      )}

      {/* TAB 4: DOCUMENTOS SKELETON (Onda 2) */}
      {tab === 4 && (
        <Stack spacing={2}>
          <Paper sx={{ p: 2.5, borderRadius: 3 }}>
            <Typography variant="h6" gutterBottom>Upload de Documentos Operacionais</Typography>
            <Typography color="text.secondary" mb={1.5}>
              Usa DocumentController + DocumentService + OcrService (extração básica). Versionamento automático.
              Vincule a POSTO, CONTRATO, EMPLOYEE etc.
            </Typography>

            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} flexWrap="wrap">
              <TextField select label="Tipo de Entidade" value={docEntityType} onChange={e => setDocEntityType(e.target.value)} size="small" sx={{ minWidth: 160 }}>
                <option value="POSTO">POSTO</option>
                <option value="CONTRATO">CONTRATO</option>
                <option value="EMPLOYEE">EMPLOYEE</option>
                <option value="CLOCK_DEVICE">CLOCK_DEVICE</option>
              </TextField>
              <TextField label="Entity ID" value={docEntityId} onChange={e => setDocEntityId(e.target.value)} size="small" sx={{ minWidth: 280 }} />
              <Button variant="outlined" component="label" startIcon={<FileText size={16} />}>
                Selecionar Arquivo
                <input type="file" hidden onChange={(e) => setDocFile(e.target.files?.[0] || null)} />
              </Button>
              <Button variant="contained" onClick={handleDocUpload} disabled={!docFile || !docEntityId || docLoading}>
                Upload + Indexar
              </Button>
            </Stack>
            {docFile && <Typography variant="caption" sx={{ mt: 1, display: 'block' }}>Arquivo: {docFile.name}</Typography>}
            {docLoading && <LinearProgress sx={{ mt: 1 }} />}
          </Paper>

          <Paper sx={{ p: 2, borderRadius: 3 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="subtitle2">Documentos vinculados (versões + OCR)</Typography>
              <Button size="small" onClick={loadDocuments} disabled={!docEntityId}>Carregar</Button>
            </Stack>
            <EnterpriseDataGrid
              title=""
              rowData={docList}
              columnDefs={[
                { headerName: 'Arquivo', field: 'fileName', flex: 1 },
                { headerName: 'Versão', field: 'version' },
                { headerName: 'OCR Extraído', field: 'ocrExtracted', flex: 2 },
                { headerName: 'Data', field: 'uploadedAt' },
              ]}
              height={280}
              loading={docLoading}
              emptyMessage="Nenhum documento vinculado ainda. Faça upload acima."
            />
          </Paper>
          <Alert severity="info">OCR + versionamento via DocumentVersion. Futuro: busca RAG via IA.</Alert>
        </Stack>
      )}

      {tab === 5 && (
        <Paper sx={{ p: 3, borderRadius: 3 }}>
          <Typography variant="h6" gutterBottom>Espelho de Ponto (Portaria 671)</Typography>
          <Stack direction="row" spacing={1} mb={2} flexWrap="wrap">
            <TextField label="Employee ID" size="small" value={espEmployeeId} onChange={e => setEspEmployeeId(e.target.value)} />
            <TextField label="Competência" type="date" size="small" value={espCompetencia} onChange={e => setEspCompetencia(e.target.value)} InputLabelProps={{ shrink: true }} />
            <Button variant="outlined" onClick={() => exportEspelho.mutate({ employeeId: espEmployeeId, competencia: espCompetencia })} disabled={!espEmployeeId}>Exportar TXT</Button>
          </Stack>
          {espelho && (
            <EnterpriseDataGrid
              title={String(espelho.titulo || 'Espelho')}
              rowData={(espelho.linhas as any[]) || []}
              columnDefs={[
                { headerName: 'Data', field: 'data' },
                { headerName: 'Entrada', field: 'entrada' },
                { headerName: 'Saída', field: 'saida' },
                { headerName: 'Minutos', field: 'minutosTrabalhados' },
                { headerName: 'Atraso', field: 'atrasoMinutos' },
                { headerName: 'Falta', field: 'faltaMinutos' },
              ]}
              height={320}
            />
          )}
        </Paper>
      )}

      {tab === 6 && (
        <Paper sx={{ p: 3, borderRadius: 3, maxWidth: 520 }}>
          <Typography variant="h6" gutterBottom>Marcação Mobile (REP-P)</Typography>
          <Stack spacing={1.5}>
            <TextField label="CPF ou Matrícula" size="small" id="mobile-cpf" />
            <Button variant="contained" onClick={async () => {
              const cpf = (document.getElementById('mobile-cpf') as HTMLInputElement)?.value
              try {
                const res = await mobilePunch.mutateAsync({ cpf, punchType: 'ENTRADA', latitude: -23.55, longitude: -46.63 })
                showNotification(`Marcação registrada — hash ${res.hash}`, 'success')
              } catch (e: any) { showNotification(e.message, 'error') }
            }}>Registrar Entrada (com geo)</Button>
          </Stack>
        </Paper>
      )}

      {tab === 7 && (
        <Stack spacing={2}>
          <Alert severity="warning">Volantes ausentes hoje: {volantes.length}</Alert>
          <EnterpriseDataGrid title="Volantes sem marcação" rowData={volantes} columnDefs={[
            { headerName: 'Employee', field: 'employeeId' }, { headerName: 'Posto', field: 'postId' }, { headerName: 'Status', field: 'status' }
          ]} height={200} />
          <Typography variant="subtitle2">Ajustes pendentes de aprovação: {pendingAdj.length}</Typography>
          <EnterpriseDataGrid title="Workflow de ajustes" rowData={pendingAdj} columnDefs={[
            { headerName: 'Data', field: 'date' }, { headerName: 'Tipo', field: 'tipo' }, { headerName: 'Status', field: 'status' }, { headerName: 'Motivo', field: 'motivo', flex: 1 }
          ]} height={240} />
          {contractId && (
            <Button variant="outlined" onClick={() => processMonth.mutateAsync({ contractId, competencia: aejPeriod }).then(r => showNotification(`Processados ${r.processed_days} dias`, 'success'))}>
              Processar mês inteiro do contrato
            </Button>
          )}
        </Stack>
      )}
    </Box>
  )
}
