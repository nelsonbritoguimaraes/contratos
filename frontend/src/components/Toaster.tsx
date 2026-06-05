/**
 * Toaster global usando Sonner (Fase 0)
 * Substitui / complementa o NotificationProvider antigo.
 * Posição: bottom-right (padrão CFO/enterprise).
 */
import { Toaster as SonnerToaster } from 'sonner'

export function Toaster() {
  return (
    <SonnerToaster
      position="bottom-right"
      closeButton
      richColors
      theme="system"
      className="toaster"
    />
  )
}
