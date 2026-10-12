import axios, { AxiosError } from 'axios'

import { i18n } from '@/locales'
import type { ApiEnvelope } from '@/types/admin'

const TOKEN_KEY = 'study.console.token'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly code: string,
    readonly status?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['Accept-Language'] = i18n.global.locale.value
  return config
})

httpClient.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiEnvelope<unknown>
    if (envelope?.code && envelope.code !== 'SUCCESS') {
      return Promise.reject(new ApiError(envelope.message, envelope.code, response.status))
    }
    return response
  },
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
    }
    const message = error.response?.data?.message || error.message || 'Request failed'
    const code = error.response?.data?.code || 'NETWORK_ERROR'
    return Promise.reject(new ApiError(message, code, error.response?.status))
  },
)

export function unwrap<T>(envelope: ApiEnvelope<T>): T {
  return envelope.data
}

export { TOKEN_KEY }
