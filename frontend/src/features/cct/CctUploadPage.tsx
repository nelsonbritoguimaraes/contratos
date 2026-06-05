/**
 * CCT Upload + Impact Simulator (SPEC 4.15)
 * Permite upload de CCT/ACT, extração básica de cláusulas e simulação de impacto em custos/glosas.
 */
import { useState } from 'react'
import { Box, Typography, Paper, Alert, Button, Stack, LinearProgress } from '@mui/material'
import { Upload, TrendingUp } from 'lucide-react'
import { useNotification } from '../../components/NotificationProvider'
import { apiUpload } from '../../api/client'

export default function CctUploadPage() {
  const { showNotification } = useNotification()
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<any>(null)

  const handleUpload = async () => {
    if (!file) return
    setLoading(true)
    try {
      const formData = new FormData()
      formData.append('arquivo', file)
      const res = await apiUpload<any>('/cct/upload', formData)
      setResult(res)
      showNotification('CCT processada com sucesso. Impacto calculado.', 'success')
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Falha ao processar CCT'
      showNotification(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>CCT Upload + Simulador de Impacto</Typography>
      <Typography color="text.secondary" mb={3}>
        Faça upload da Convenção Coletiva de Trabalho. O sistema extrai cláusulas relevantes e estima impacto em custos e glosas.
      </Typography>

      <Paper sx={{ p: 4, borderRadius: 3, maxWidth: 720 }}>
        <Stack spacing={2}>
          <Button variant="outlined" component="label" startIcon={<Upload />}>
            Selecionar arquivo CCT (PDF / DOCX)
            <input type="file" hidden onChange={(e) => setFile(e.target.files?.[0] || null)} />
          </Button>

          {file && <Typography variant="body2">Arquivo: {file.name}</Typography>}

          <Button
            variant="contained"
            onClick={handleUpload}
            disabled={!file || loading}
            startIcon={<TrendingUp />}
          >
            {loading ? 'Processando...' : 'Processar CCT e Calcular Impacto'}
          </Button>

          {loading && <LinearProgress />}
        </Stack>
      </Paper>

      {result && (
        <Paper sx={{ p: { xs: 2, sm: 2.5 }, mt: 2.5, borderRadius: 2.5, maxWidth: 720 }}>
          <Typography variant="h6" gutterBottom>Resultado da Análise</Typography>
          <Stack spacing={1}>
            <div><strong>Contrato afetado:</strong> {result.contrato}</div>
            <div><strong>Cláusulas extraídas:</strong> {result.cláusulasExtraidas}</div>
            <div><strong>Impacto estimado no custo mensal:</strong> R$ {result.impactoCustoMensal?.toLocaleString('pt-BR')}</div>
            <div><strong>Impacto estimado em glosas:</strong> R$ {result.impactoGlosaEstimado?.toLocaleString('pt-BR')}</div>
            <Alert severity="warning" sx={{ mt: 1 }}>
              <strong>Recomendação da IA:</strong> {result.recomendacao}
            </Alert>
          </Stack>
        </Paper>
      )}
    </Box>
  )
}
