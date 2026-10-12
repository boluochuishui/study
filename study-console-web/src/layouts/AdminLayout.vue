<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  BookOpenCheck,
  ChevronLeft,
  ChevronRight,
  FileSearch,
  Gauge,
  Languages,
  LogOut,
  Menu,
  Settings2,
  UsersRound,
} from '@lucide/vue'

import AppBrand from '@/components/AppBrand.vue'
import { isMockMode } from '@/api'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import type { PermissionCode } from '@/types/admin'

interface NavigationItem {
  route: string
  label: string
  icon: typeof Gauge
  permission?: PermissionCode
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const app = useAppStore()
const auth = useAuthStore()
const appEnvironment = import.meta.env.VITE_APP_ENV

const groups: Array<{ label: string; items: NavigationItem[] }> = [
  {
    label: 'nav.overview',
    items: [{ route: '/dashboard', label: 'nav.dashboard', icon: Gauge }],
  },
  {
    label: 'nav.policy',
    items: [
      { route: '/rules', label: 'nav.rules', icon: BookOpenCheck, permission: 'RULE_READ' },
      { route: '/configs', label: 'nav.configs', icon: Settings2, permission: 'CONFIG_READ' },
    ],
  },
  {
    label: 'nav.operations',
    items: [{ route: '/logs', label: 'nav.logs', icon: FileSearch, permission: 'LOG_READ' }],
  },
  {
    label: 'nav.access',
    items: [{ route: '/iam', label: 'nav.iam', icon: UsersRound, permission: 'IAM_READ' }],
  },
]

const visibleGroups = computed(() =>
  groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => auth.hasPermission(item.permission)),
    }))
    .filter((group) => group.items.length > 0),
)

async function navigate(path: string) {
  app.setMobileNavigation(false)
  await router.push(path)
}

async function logout() {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="admin-shell" :class="{ 'admin-shell--collapsed': app.sidebarCollapsed }">
    <aside class="sidebar">
      <div class="sidebar__brand">
        <AppBrand :compact="app.sidebarCollapsed" inverse />
      </div>
      <nav class="sidebar__nav" aria-label="Primary navigation">
        <section v-for="group in visibleGroups" :key="group.label" class="nav-group">
          <p v-if="!app.sidebarCollapsed" class="nav-group__title">{{ t(group.label) }}</p>
          <button
            v-for="item in group.items"
            :key="item.route"
            type="button"
            class="nav-item"
            :class="{ active: route.path === item.route }"
            :title="app.sidebarCollapsed ? t(item.label) : undefined"
            @click="navigate(item.route)"
          >
            <component :is="item.icon" :size="18" />
            <span v-if="!app.sidebarCollapsed">{{ t(item.label) }}</span>
          </button>
        </section>
      </nav>
      <button
        type="button"
        class="sidebar__collapse"
        :title="app.sidebarCollapsed ? 'Expand navigation' : 'Collapse navigation'"
        @click="app.toggleSidebar"
      >
        <ChevronRight v-if="app.sidebarCollapsed" :size="18" />
        <ChevronLeft v-else :size="18" />
      </button>
    </aside>

    <el-drawer
      v-model="app.mobileNavigationOpen"
      direction="ltr"
      :show-close="false"
      size="270px"
      class="mobile-nav-drawer"
    >
      <template #header><AppBrand inverse /></template>
      <nav class="mobile-nav">
        <template v-for="group in visibleGroups" :key="group.label">
          <p>{{ t(group.label) }}</p>
          <button
            v-for="item in group.items"
            :key="item.route"
            type="button"
            :class="{ active: route.path === item.route }"
            @click="navigate(item.route)"
          >
            <component :is="item.icon" :size="18" />
            {{ t(item.label) }}
          </button>
        </template>
      </nav>
    </el-drawer>

    <div class="admin-main">
      <header class="topbar">
        <button
          type="button"
          class="icon-button topbar__menu"
          title="Open navigation"
          @click="app.setMobileNavigation(true)"
        >
          <Menu :size="20" />
        </button>
        <div class="topbar__context">
          <span class="context-item"
            ><small>{{ t('common.tenant') }}</small
            >{{ auth.identity?.tenantCode }}</span
          >
          <span class="context-item context-item--desktop"
            ><small>{{ t('common.environment') }}</small
            >{{ appEnvironment }}</span
          >
          <span v-if="isMockMode" class="environment-badge">{{ t('common.mock') }}</span>
        </div>
        <div class="topbar__actions">
          <el-dropdown trigger="click">
            <button type="button" class="icon-button" title="Change language">
              <Languages :size="18" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="app.setLocale('zh-CN')">简体中文</el-dropdown-item>
                <el-dropdown-item @click="app.setLocale('en-US')">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click">
            <button type="button" class="user-button">
              <span class="user-avatar">{{ auth.initials }}</span>
              <span class="user-copy">
                <strong>{{ auth.identity?.displayName }}</strong>
                <small>{{ auth.identity?.username }}</small>
              </span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout"
                  ><LogOut :size="15" />{{ t('auth.logout') }}</el-dropdown-item
                >
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="page-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>
