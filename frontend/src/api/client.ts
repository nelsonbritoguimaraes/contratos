/**
 * API client for ContractOps backend — Enterprise version (Fase 1 complete).
 * All calls go through Vite proxy (/api → http://localhost:8080).
 * Supports:
 *  - JWT Bearer token (Authorization header) when present in localStorage
 *  - tenantId query param (still used as fallback / multi-tenant header in some endpoints)
 *  - 401 auto-clear + console signal (AuthContext listens for 'auth:unauthorized')
 */

export const API_BASE = '/api'
const TOKEN_KEY = 'contractops:jwt'

let cachedToken: string | null = null

export function getAuthToken(): string | null {
  if (cachedToken !== null) return cachedToken
  cachedToken = localStorage.getItem(TOKEN_KEY)
  return cachedToken
}

export function setAuthToken(token: string) {
  cachedToken = token
  localStorage.setItem(TOKEN_KEY, token)
  // Notify listeners (AuthContext, etc.)
  window.dispatchEvent(new CustomEvent('auth:token-changed', { detail: { hasToken: true } }))
}

export function clearAuthToken() {
  cachedToken = null
  localStorage.removeItem(TOKEN_KEY)
  window.dispatchEvent(new CustomEvent('auth:token-changed', { detail: { hasToken: false } }))
}

function withTenant(path: string): string {
  if (path.includes('tenantId=') || path.includes('/actuator') || path.includes('/auth')) return path
  // Não injeta tenant automático — o hook useTenant já inclui tenantId explicitamente em todas as chamadas.
  // Se chegar aqui sem tenantId, é um bug no hook chamador que precisa ser corrigido.
  console.warn(`[api] Chamada sem tenantId explícito: ${path}. Adicione o parâmetro no hook.`)
  return path
}

function authHeaders(): HeadersInit {
  const token = getAuthToken()
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

async function handleResponse<T>(res: Response, finalPath: string, method: string): Promise<T | undefined> {
  if (res.status === 401) {
    clearAuthToken()
    // Let AuthContext / pages react
    window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    const text = await res.text().catch(() => '')
    throw new Error(`Unauthorized (401) on ${method} ${finalPath}. Token cleared. ${text}`)
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    // Notify global listeners that backend might be offline
    if (res.status >= 500 || res.status === 0) {
      window.dispatchEvent(new CustomEvent('api:backend-offline', { detail: { path: finalPath, status: res.status } }))
    }
    throw new Error(`${method} ${finalPath} failed: ${res.status} ${text}`)
  }
  if (res.status === 204) return undefined
  return res.json()
}

export async function apiGet<T>(path: string): Promise<T> {
  const finalPath = withTenant(path)
  const res = await fetch(`${API_BASE}${finalPath}`, {
    headers: authHeaders(),
  })
  return handleResponse<T>(res, finalPath, 'GET') as Promise<T>
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const finalPath = withTenant(path)
  const res = await fetch(`${API_BASE}${finalPath}`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(body),
  })
  return handleResponse<T>(res, finalPath, 'POST') as Promise<T>
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  const finalPath = withTenant(path)
  const res = await fetch(`${API_BASE}${finalPath}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(body),
  })
  return handleResponse<T>(res, finalPath, 'PUT') as Promise<T>
}

export async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  const finalPath = withTenant(path)
  const res = await fetch(`${API_BASE}${finalPath}`, {
    method: 'PATCH',
    headers: authHeaders(),
    body: JSON.stringify(body),
  })
  return handleResponse<T>(res, finalPath, 'PATCH') as Promise<T>
}

export async function apiDelete(path: string): Promise<void> {
  const finalPath = withTenant(path)
  const res = await fetch(`${API_BASE}${finalPath}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  await handleResponse<void>(res, finalPath, 'DELETE')
}

// Convenience for multipart / file uploads (CCT, planilhas, AFD, etc.)
export async function apiUpload<T>(path: string, formData: FormData): Promise<T> {
  const finalPath = withTenant(path)
  const token = getAuthToken()
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${finalPath}`, {
    method: 'POST',
    headers,
    body: formData,
  })
  return handleResponse<T>(res, finalPath, 'UPLOAD') as Promise<T>
}
