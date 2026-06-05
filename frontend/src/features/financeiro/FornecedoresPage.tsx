import { useState } from 'react'
import { Box, Typography, Paper, Stack, Button, TextField, Alert, MenuItem, CircularProgress } from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { useNotification } from '../../components/NotificationProvider'
import { useCriarFornecedor, useFornecedores } from '../../api/hooks/useFornecedores'

export default function FornecedoresPage() {
  const { showNotification } = useNotification()
  const { data: fornecedores = [], isLoading, isError, refetch } = useFornecedores()
  const criar = useCriarFornecedor()

  const [novo, setNovo] = useState({ razaoSocial: '', cnpj: '', contato: '', categoria: '' })

  const columns = [
    { headerName: 'Razão Social', field: 'razaoSocial', flex: 2 },
    { headerName: 'CNPJ', field: 'cnpj', flex: 1 },
    { headerName: 'Contato', field: 'contato', flex: 1 },
    { headerName: 'Categoria', field: 'categoria', flex: 1 },
  ]

  const handleAdicionar = async () => {
    if (!novo.razaoSocial?.trim()) {
      showNotification('Razão Social é obrigatória', 'error')
      return
    }
    try {
      await criar.mutateAsync({
        razaoSocial: novo.razaoSocial.trim(),
        cnpj: novo.cnpj || undefined,
        contato: novo.contato || undefined,
        categoria: novo.categoria || undefined,
      })
      setNovo({ razaoSocial: '', cnpj: '', contato: '', categoria: '' })
      showNotification('Fornecedor cadastrado (API)', 'success')
      refetch()
    } catch {
      showNotification('Erro ao salvar fornecedor', 'error')
    }
  }

  const categoriasComuns = ['Uniformes e EPIs', 'Produtos de Limpeza', 'Material de Escritório', 'Equipamentos de Segurança', 'Serviços de Manutenção', 'Outros']

  return (
    <Box>
      <Typography variant="h4" fontWeight={600} gutterBottom>Cadastro de Fornecedores</Typography>
      <Alert severity="info" sx={{ mb: 2 }}>
        Persistência no backend (<code>/api/financeiro/fornecedores</code>). Fornecedores ficam disponíveis em Lançamentos.
      </Alert>

      <Paper sx={{ p: { xs: 2, sm: 2.5 }, mb: 2.5, borderRadius: 2.5 }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>Novo Fornecedor</Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField label="Razão Social *" value={novo.razaoSocial} onChange={e => setNovo({ ...novo, razaoSocial: e.target.value })} sx={{ flex: 2 }} />
          <TextField label="CNPJ" value={novo.cnpj} onChange={e => setNovo({ ...novo, cnpj: e.target.value })} sx={{ flex: 1 }} />
          <TextField label="Contato" value={novo.contato} onChange={e => setNovo({ ...novo, contato: e.target.value })} sx={{ flex: 1 }} />
          <TextField
            select
            label="Categoria"
            value={novo.categoria}
            onChange={e => setNovo({ ...novo, categoria: e.target.value })}
            sx={{ flex: 1, minWidth: 180 }}
          >
            <MenuItem value="">Selecione...</MenuItem>
            {categoriasComuns.map(cat => (
              <MenuItem key={cat} value={cat}>{cat}</MenuItem>
            ))}
          </TextField>
          <Button variant="contained" onClick={handleAdicionar} disabled={criar.isPending} sx={{ height: 56, minWidth: 110 }}>
            {criar.isPending ? <CircularProgress size={22} color="inherit" /> : 'Adicionar'}
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2, borderRadius: 3 }}>
        <EnterpriseDataGrid
          title="Fornecedores Cadastrados"
          rowData={fornecedores}
          columnDefs={columns}
          loading={isLoading}
          error={isError ? 'Erro ao carregar fornecedores' : null}
          onRefresh={() => refetch()}
          height="calc(100dvh - 400px)"
        />
      </Paper>
    </Box>
  )
}
