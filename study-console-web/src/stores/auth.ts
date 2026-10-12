import { defineStore } from 'pinia'

import { adminApi } from '@/api'
import { TOKEN_KEY } from '@/api/client'
import type { AdminIdentity, LoginRequest, PermissionCode } from '@/types/admin'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) ?? '',
    identity: null as AdminIdentity | null,
    restoring: false,
  }),
  getters: {
    authenticated: (state) => Boolean(state.token),
    initials: (state) => {
      const name = state.identity?.displayName || state.identity?.username || 'U'
      return name.slice(0, 2).toUpperCase()
    },
  },
  actions: {
    hasPermission(permission?: PermissionCode) {
      return !permission || Boolean(this.identity?.permissions.includes(permission))
    },
    async login(request: LoginRequest) {
      const session = await adminApi.login(request)
      this.token = session.accessToken
      localStorage.setItem(TOKEN_KEY, session.accessToken)
      this.identity = await adminApi.me()
    },
    async restore() {
      if (!this.token || this.identity || this.restoring) return
      this.restoring = true
      try {
        this.identity = await adminApi.me()
      } catch (error) {
        this.clear()
        throw error
      } finally {
        this.restoring = false
      }
    },
    async logout() {
      try {
        await adminApi.logout()
      } finally {
        this.clear()
      }
    },
    clear() {
      this.token = ''
      this.identity = null
      localStorage.removeItem(TOKEN_KEY)
    },
  },
})
