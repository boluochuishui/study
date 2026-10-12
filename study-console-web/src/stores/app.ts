import { defineStore } from 'pinia'

import { i18n, type SupportedLocale } from '@/locales'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    mobileNavigationOpen: false,
    locale: i18n.global.locale.value as SupportedLocale,
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    setMobileNavigation(open: boolean) {
      this.mobileNavigationOpen = open
    },
    setLocale(locale: SupportedLocale) {
      this.locale = locale
      i18n.global.locale.value = locale
      localStorage.setItem('study.console.locale', locale)
    },
  },
})
