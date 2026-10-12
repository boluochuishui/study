<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDownRight, ArrowUpRight, Minus, RefreshCw } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type { DashboardSnapshot } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()
const loading = ref(false)
const snapshot = ref<DashboardSnapshot>({ metrics: [], services: [], recentReleases: [] })

function metricLabel(key: string) {
  return t(`dashboard.${key}`)
}

async function load() {
  loading.value = true
  try {
    snapshot.value = await adminApi.dashboard()
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page-stack">
    <PageHeader :title="t('dashboard.title')" :subtitle="t('dashboard.subtitle')">
      <template #actions>
        <el-button :icon="RefreshCw" @click="load">{{ t('common.refresh') }}</el-button>
      </template>
    </PageHeader>

    <section class="metric-grid" aria-label="Key metrics">
      <article v-for="metric in snapshot.metrics" :key="metric.key" class="metric-card">
        <span>{{ metricLabel(metric.key) }}</span>
        <strong>{{ metric.value }}</strong>
        <small :class="`trend--${metric.trend}`">
          <ArrowUpRight v-if="metric.trend === 'up'" :size="14" />
          <ArrowDownRight v-else-if="metric.trend === 'down'" :size="14" />
          <Minus v-else :size="14" />
          {{ metric.change }}
        </small>
      </article>
    </section>

    <section class="content-section">
      <div class="section-heading">
        <h2>{{ t('dashboard.serviceHealth') }}</h2>
      </div>
      <el-table :data="snapshot.services" row-key="name" table-layout="fixed">
        <el-table-column :label="t('dashboard.service')" min-width="210">
          <template #default="{ row }"
            ><strong class="table-primary">{{ row.name }}</strong></template
          >
        </el-table-column>
        <el-table-column prop="kind" :label="t('dashboard.protocol')" width="100" />
        <el-table-column :label="t('common.status')" width="120">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="replicas" :label="t('dashboard.replicas')" width="100" />
        <el-table-column :label="t('dashboard.latency')" width="130">
          <template #default="{ row }">{{ row.latency }} ms</template>
        </el-table-column>
        <el-table-column prop="updatedAt" :label="t('dashboard.lastHeartbeat')" width="130" />
      </el-table>
    </section>

    <section class="content-section">
      <div class="section-heading">
        <h2>{{ t('dashboard.recentReleases') }}</h2>
      </div>
      <el-table :data="snapshot.recentReleases" row-key="releaseId">
        <el-table-column prop="ruleSetCode" :label="t('dashboard.ruleSet')" min-width="200" />
        <el-table-column prop="scene" :label="t('dashboard.scene')" width="110" />
        <el-table-column :label="t('common.version')" width="100">
          <template #default="{ row }">v{{ row.releaseVersion }}</template>
        </el-table-column>
        <el-table-column prop="publishedBy" :label="t('dashboard.publisher')" width="140" />
        <el-table-column :label="t('dashboard.publishedAt')" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>
