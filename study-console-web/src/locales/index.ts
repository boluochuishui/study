import { createI18n } from 'vue-i18n'

import enUS from './en-US'
import zhCN from './zh-CN'

export type SupportedLocale = 'zh-CN' | 'en-US'

const storedLocale = localStorage.getItem('study.console.locale')
const locale: SupportedLocale = storedLocale === 'en-US' ? 'en-US' : 'zh-CN'

export const i18n = createI18n({
  legacy: false,
  locale,
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})
