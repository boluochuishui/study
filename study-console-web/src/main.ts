import { createApp } from 'vue'

import App from './App.vue'
import { i18n } from './locales'
import { pinia } from './stores'
import { router } from './router'
import './styles/index.scss'

createApp(App).use(pinia).use(router).use(i18n).mount('#app')
