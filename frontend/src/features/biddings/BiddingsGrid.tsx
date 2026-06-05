import { useMemo } from 'react'
import { AgGridReact } from 'ag-grid-react'
import { ColDef, GridOptions } from 'ag-grid-community'
import { Button, Stack } from '@mui/material'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-material.css'
import { Bidding } from '../../api/types'

interface Props {
  biddings: Bidding[]
  onEdit: (bidding: Bidding) => void
  onOpenDetail?: (bidding: Bidding) => void
  onRefresh: () => void
}

export default function BiddingsGrid({ biddings, onEdit, onOpenDetail, onRefresh: _onRefresh }: Props) {
  const columnDefs: ColDef<Bidding>[] = useMemo(() => [
    {
      headerName: 'Edital',
      field: 'editalNumero',
      minWidth: 130,
      cellStyle: { fontWeight: 500 },
    },
    {
      headerName: 'Órgão',
      field: 'orgao',
      minWidth: 260,
      flex: 1,
    },
    {
      headerName: 'Modalidade',
      field: 'modalidade',
      minWidth: 140,
    },
    {
      headerName: 'Valor Vencedor',
      field: 'valorVencedor',
      minWidth: 150,
      type: 'numericColumn',
      valueFormatter: (p) =>
        p.value ? new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(p.value) : '—',
    },
    {
      headerName: 'Status',
      field: 'status',
      minWidth: 130,
      cellRenderer: (params: any) => {
        const status = params.value as string
        const color = status === 'HOMOLOGADA' ? '#2E7D32' : '#ED6C02'
        return (
          <span style={{
            backgroundColor: color + '22',
            color,
            padding: '2px 10px',
            borderRadius: 999,
            fontSize: 12,
            fontWeight: 600,
          }}>
            {status}
          </span>
        )
      },
    },
    {
      headerName: 'Data Homologação',
      field: 'dataHomologacao',
      minWidth: 130,
      valueFormatter: (p) => p.value ? new Date(p.value).toLocaleDateString('pt-BR') : '—',
    },
    {
      headerName: 'Ações',
      minWidth: 180,
      cellRenderer: (params: any) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" variant="outlined" onClick={() => onEdit(params.data)}>
            Editar
          </Button>
          {onOpenDetail && (
            <Button size="small" variant="contained" onClick={() => onOpenDetail(params.data)}>
              Detalhes
            </Button>
          )}
        </Stack>
      ),
      sortable: false,
      filter: false,
    },
  ], [onEdit])

  const gridOptions: GridOptions<Bidding> = {
    rowHeight: 42,
    headerHeight: 40,
    animateRows: true,
  }

  return (
    <div className="ag-theme-material" style={{ height: 480, width: '100%', borderRadius: 8, border: '1px solid #e0e0e0' }}>
      <AgGridReact<Bidding>
        rowData={biddings}
        columnDefs={columnDefs}
        gridOptions={gridOptions}
        defaultColDef={{ resizable: true, sortable: true, filter: true, floatingFilter: true }}
      />
    </div>
  )
}
