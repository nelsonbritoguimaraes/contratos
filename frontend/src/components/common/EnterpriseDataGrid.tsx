/**
 * EnterpriseDataGrid — Wrapper poderoso em torno do AG Grid (Fase 0/1)
 * Inclui toolbar padrão, exportação, densidade, e prepara para server-side row model.
 * Este componente será usado em praticamente todas as telas de dados pesados.
 */
import { AgGridReact } from 'ag-grid-react'
import { ColDef } from 'ag-grid-community'
import { agGridEnterpriseEnabled } from '../../lib/agGridSetup'
import { Box, Stack, Button, Typography, CircularProgress } from '@mui/material'
import { Download, RefreshCw } from 'lucide-react'
import { useRef } from 'react'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-material.css'

interface EnterpriseDataGridProps {
  rowData: any[]
  columnDefs: ColDef[]
  title?: string
  onRefresh?: () => void
  loading?: boolean
  emptyMessage?: string
  error?: string | null
  height?: number | string
  compact?: boolean
  gridOptions?: any
  onRowClicked?: (params: any) => void
  /** Habilita Pivot + painel lateral (requer AG Grid Enterprise + licença) */
  pivotMode?: boolean
}

export function EnterpriseDataGrid({
  rowData,
  columnDefs,
  title,
  onRefresh,
  loading = false,
  emptyMessage,
  error = null,
  height,
  compact = false,
  gridOptions = {},
  onRowClicked,
  pivotMode = false,
}: EnterpriseDataGridProps) {
  const resolvedHeight = height ?? (compact ? '400px' : 'calc(100dvh - 340px)')
  const gridRef = useRef<AgGridReact>(null)

  const exportCsv = () => {
    gridRef.current?.api?.exportDataAsCsv({
      fileName: (title || 'export').toLowerCase().replace(/\s+/g, '_') + '.csv',
    })
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.25}>
        {title && (
          <Typography variant="subtitle1" fontWeight={600} sx={{ fontSize: '0.95rem', lineHeight: 1.2 }}>
            {title}
          </Typography>
        )}
        <Stack direction="row" spacing={1}>
          {onRefresh && (
            <Button
              variant="outlined"
              size="small"
              startIcon={<RefreshCw size={16} />}
              onClick={onRefresh}
              disabled={loading}
            >
              Atualizar
            </Button>
          )}
          <Button
            variant="outlined"
            size="small"
            startIcon={<Download size={16} />}
            onClick={exportCsv}
          >
            Exportar CSV
          </Button>
        </Stack>
      </Stack>

      <div
        className="ag-theme-material"
        style={{
          height: typeof resolvedHeight === 'number' ? `${resolvedHeight}px` : resolvedHeight,
          width: '100%',
          borderRadius: 10,
          overflow: 'hidden',
          border: '1px solid #e0e0e0',
          minHeight: compact ? 280 : 320,
          maxHeight: compact ? 480 : 'calc(100dvh - 200px)',
          background: '#fff',
          position: 'relative',
        }}
      >
        {/* Consistent loading / empty / error states for Onda 3 polish */}
        {loading && (
          <Box
            sx={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: 'rgba(255,255,255,0.85)',
              zIndex: 2,
              borderRadius: 2,
            }}
          >
            <Stack direction="row" spacing={1.5} alignItems="center">
              <CircularProgress size={22} />
              <Typography variant="body2" color="text.secondary">Carregando dados...</Typography>
            </Stack>
          </Box>
        )}

        {!loading && error && (
          <Box
            sx={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              p: 3,
              zIndex: 2,
            }}
          >
            <Typography variant="body2" color="error.main" textAlign="center">
              Erro ao carregar: {error}
            </Typography>
          </Box>
        )}

        {!loading && !error && (!rowData || rowData.length === 0) && (
          <Box
            sx={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              p: 3,
              zIndex: 2,
            }}
          >
            <Typography variant="body2" color="text.secondary" textAlign="center">
              {emptyMessage || 'Nenhum dado encontrado. Os registros aparecerão aqui quando disponíveis.'}
            </Typography>
          </Box>
        )}

        <AgGridReact
          ref={gridRef}
          rowData={rowData}
          columnDefs={columnDefs}
          gridOptions={{
            ...gridOptions,
            ...(pivotMode && agGridEnterpriseEnabled
              ? {
                  pivotMode: true,
                  sideBar: { toolPanels: ['columns', 'filters'] },
                  rowGroupPanelShow: 'always',
                }
              : {}),
          }}
          onRowClicked={onRowClicked}
          defaultColDef={{
            resizable: true,
            sortable: true,
            filter: true,
            flex: 1,
            minWidth: 85,
            wrapText: true,   // wrap real nas células (melhor para conteúdo longo em colunas estreitas)
            autoHeight: false,
          }}
          pagination={true}
          paginationPageSize={18}
          animateRows={true}
          suppressMovableColumns={true}
          rowHeight={34}
          headerHeight={36}
          suppressColumnVirtualisation={false}
          // Hide grid internals when overlay states active for clean UX
          overlayLoadingTemplate={loading ? '<span></span>' : undefined}
        />
      </div>
    </Box>
  )
}
