import { useMemo, useState, useEffect } from 'react'
import { Button, Stack } from '@mui/material'
import { EnterpriseDataGrid } from '../../components/common/EnterpriseDataGrid'
import { Contract } from '../../api/types'

/**
 * ContractsGrid — AG Grid Enterprise-ready (Scaffold)
 *
 * Implementa o padrão visual da SPEC v1.0:
 * - Row Grouping por órgão e status (exatamente como pedido em 3.3 e 27)
 * - Colunas relevantes para gestão de contratos de mão de obra exclusiva
 * - Formatação brasileira (R$, datas, status)
 * - Preparado para upgrade para Enterprise (Pivot, Server-side, etc.)
 */
interface ContractsGridProps {
  contracts?: Contract[]
  onViewPosts?: (contract: Contract) => void
  onEditContract?: (contract: Contract) => void
  onRefresh?: () => void
}

export default function ContractsGrid({ contracts = [], onViewPosts, onEditContract, onRefresh }: ContractsGridProps) {
  const [rowData, setRowData] = useState<any[]>([])

  // Sempre usa os dados vindos do backend (já mapeados)
  useEffect(() => {
    const mapped = (contracts ?? []).map((c) => ({
      id: c.id,
      numero: c.numero,
      orgao: c.orgao,
      objeto: c.objeto || '',
      vigencia_inicio: c.vigenciaInicio,
      vigencia_fim: c.vigenciaFim,
      valor_mensal: c.valorMensal || 0,
      qtd_postos_contratados: c.qtdPostosContratados || 0,
      status: c.status,
      empresa: 'Vigilância Alpha Ltda', // pode vir do backend futuramente
      raw: c, // guardamos o objeto original para ações
    }))
    setRowData(mapped)
  }, [contracts])

  const columnDefs: any[] = useMemo(() => [
    {
      headerName: 'Contrato',
      field: 'numero',
      minWidth: 110,
      cellRenderer: 'agGroupCellRenderer',
      cellStyle: { fontWeight: 500 },
    },
    {
      headerName: 'Órgão',
      field: 'orgao',
      minWidth: 260,
      rowGroup: true,                // ← GROUPING por órgão (SPEC 3.3)
      hide: true,                    // escondido na coluna porque é grupo
    },
    {
      headerName: 'Objeto',
      field: 'objeto',
      minWidth: 280,
      flex: 1,
      tooltipField: 'objeto',
    },
    {
      headerName: 'Vigência Início',
      field: 'vigencia_inicio',
      minWidth: 115,
      valueFormatter: (p: any) => p.value ? new Date(p.value).toLocaleDateString('pt-BR') : '',
    },
    {
      headerName: 'Vigência Fim',
      field: 'vigencia_fim',
      minWidth: 115,
      valueFormatter: (p: any) => p.value ? new Date(p.value).toLocaleDateString('pt-BR') : '',
    },
    {
      headerName: 'Valor Mensal',
      field: 'valor_mensal',
      minWidth: 130,
      type: 'numericColumn',
      valueFormatter: (p: any) =>
        new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(p.value),
      cellStyle: { fontWeight: 500 },
    },
    {
      headerName: 'Postos',
      field: 'qtd_postos_contratados',
      minWidth: 90,
      type: 'numericColumn',
      cellStyle: { textAlign: 'center' },
    },
    {
      headerName: 'Status',
      field: 'status',
      minWidth: 130,
      rowGroup: true,                // ← GROUPING por status também
      hide: true,
      cellRenderer: (params: any) => {
        const status = params.value as string
        const color =
          status === 'ATIVO' ? '#2E7D32' :
          status === 'SUSPENSO' ? '#C62828' :
          status === 'EM_IMPLANTACAO' ? '#ED6C02' : '#616161'
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
      headerName: 'Planilha Vencedora',
      minWidth: 160,
      valueGetter: (p: any) => p.data.raw?.winningSpreadsheet?.arquivoNome || '—',
    },
    {
      headerName: 'Empresa Executora',
      field: 'empresa',
      minWidth: 180,
    },
    {
      headerName: 'Ações',
      minWidth: 180,
      cellRenderer: (params: any) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" variant="outlined" onClick={() => onViewPosts?.(params.data.raw)}>
            Postos
          </Button>
          <Button size="small" variant="text" onClick={() => onEditContract?.(params.data.raw)}>
            Editar
          </Button>
        </Stack>
      ),
      sortable: false,
      filter: false,
    },
  ], [onViewPosts])

  // Mantemos as opções de grouping avançado
  // const gridOptions = {
  //   autoGroupColumnDef: {
  //     headerName: 'Grupo (Órgão / Status)',
  //     minWidth: 280,
  //   },
  //   groupDefaultExpanded: 1,
  //   suppressAggFuncInHeader: true,
  //   rowHeight: 42,
  //   headerHeight: 40,
  //   animateRows: true,
  // }

  const advancedGridOptions = {
    autoGroupColumnDef: {
      headerName: 'Grupo (Órgão / Status)',
      minWidth: 280,
    },
    groupDefaultExpanded: 1,
    suppressAggFuncInHeader: true,
    rowHeight: 42,
    headerHeight: 40,
    animateRows: true,
  }

  return (
    <EnterpriseDataGrid
      title="Contratos e Postos (Row Grouping por Órgão + Status) — Clique em uma linha para detalhes e ações"
      rowData={rowData}
      columnDefs={columnDefs}
      onRefresh={onRefresh}
      height="calc(100dvh - 380px)"
      gridOptions={advancedGridOptions}
    />
  )
}
