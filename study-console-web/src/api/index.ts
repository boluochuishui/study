import type { AdminApi } from './contracts'
import { httpAdminApi } from './http-admin-api'
import { mockAdminApi } from './mock-admin-api'

export const adminApi: AdminApi =
  import.meta.env.VITE_ENABLE_MOCK === 'true' ? mockAdminApi : httpAdminApi

export const isMockMode = import.meta.env.VITE_ENABLE_MOCK === 'true'
