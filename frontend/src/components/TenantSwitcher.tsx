/**
 * TenantSwitcher — Componente visual para troca de tenant (Fase 0)
 * Usado no Topbar. Totalmente funcional com persistência.
 */
import { MenuItem, Select, SelectChangeEvent, FormControl, InputLabel } from '@mui/material'
import { useTenant } from '../api/hooks/useTenant'

export default function TenantSwitcher() {
  const { tenantId, setTenant, availableTenants } = useTenant()

  const handleChange = (event: SelectChangeEvent) => {
    const selectedId = event.target.value as string
    const selected = availableTenants.find((t) => t.id === selectedId)
    if (selected) {
      setTenant(selected.id, selected.name)
    }
  }

  return (
    <FormControl size="small" sx={{ minWidth: 220, mr: 1 }}>
      <InputLabel>Tenant</InputLabel>
      <Select
        value={tenantId}
        onChange={handleChange}
        sx={{ bgcolor: 'background.paper' }}
      >
        {availableTenants.map((t) => (
          <MenuItem key={t.id} value={t.id}>
            {t.name}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  )
}
