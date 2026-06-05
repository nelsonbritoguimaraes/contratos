/**
 * AG Grid Enterprise — registro de módulos e licença.
 * Defina VITE_AG_GRID_LICENSE_KEY no .env (licença Trial ou Enterprise da AG Grid).
 */
import { ModuleRegistry } from 'ag-grid-community'
import { LicenseManager } from 'ag-grid-enterprise'
import { RowGroupingModule } from 'ag-grid-enterprise'
import { SideBarModule } from 'ag-grid-enterprise'
import { MenuModule } from 'ag-grid-enterprise'
import { ColumnsToolPanelModule } from 'ag-grid-enterprise'
import { FiltersToolPanelModule } from 'ag-grid-enterprise'
import { SetFilterModule } from 'ag-grid-enterprise'

const licenseKey = import.meta.env.VITE_AG_GRID_LICENSE_KEY as string | undefined

if (licenseKey) {
  LicenseManager.setLicenseKey(licenseKey)
}

ModuleRegistry.registerModules([
  RowGroupingModule,
  SideBarModule,
  MenuModule,
  ColumnsToolPanelModule,
  FiltersToolPanelModule,
  SetFilterModule,
])

export const agGridEnterpriseEnabled = Boolean(licenseKey)
