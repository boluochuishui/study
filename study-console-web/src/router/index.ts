import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import type { PermissionCode } from '@/types/admin'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    permission?: PermissionCode
    titleKey?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, titleKey: 'auth.title' },
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { titleKey: 'dashboard.title' },
      },
      {
        path: 'rules',
        name: 'rules',
        component: () => import('@/views/RulesView.vue'),
        meta: { permission: 'RULE_READ', titleKey: 'rules.title' },
      },
      {
        path: 'configs',
        name: 'configs',
        component: () => import('@/views/ConfigsView.vue'),
        meta: { permission: 'CONFIG_READ', titleKey: 'configs.title' },
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('@/views/LogsView.vue'),
        meta: { permission: 'LOG_READ', titleKey: 'logs.title' },
      },
      {
        path: 'iam',
        name: 'iam',
        component: () => import('@/views/IamView.vue'),
        meta: { permission: 'IAM_READ', titleKey: 'iam.title' },
      },
      {
        path: 'forbidden',
        name: 'forbidden',
        component: () => import('@/views/ForbiddenView.vue'),
        meta: { titleKey: 'error.forbidden' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true, titleKey: 'error.notFound' },
  },
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore(pinia)
  if (to.meta.public) {
    if (to.name === 'login' && auth.authenticated) return { name: 'dashboard' }
    return true
  }
  if (!auth.authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  try {
    await auth.restore()
  } catch {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (!auth.hasPermission(to.meta.permission)) return { name: 'forbidden' }
  return true
})
