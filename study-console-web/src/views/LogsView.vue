<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RotateCcw, Search } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type { AuditFilter, AuditLog } from '@/types/admin'
import { compactId, formatDateTime } from '@/utils/format'

const { t } = useI18n()
const loading = ref(false)
const logs = ref<AuditLog[]>([])
const detail = ref<AuditLog | null>(null)
const drawerOpen = ref(false)
const filter = reactive<AuditFilter>({ keyword: '', action: '', contentType: '' })

async function load() {
  loading.value = true
  try {
    logs.value = await adminApi.auditLogs(filter)
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    loading.value = false
  }
}

function reset() {
  filter.keyword = ''
  filter.action = ''
  filter.contentType = ''
  load()
}

function openDetail(row: AuditLog) {
  detail.value = row
  drawerOpen.value = true
}

function openDetailRow(row: unknown) {
  openDetail(row as AuditLog)
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <PageHeader :title="t('logs.title')" />
    <section class="filter-bar filter-bar--wrap">
      <el-input
        v-model="filter.keyword"
        :placeholder="t('logs.keyword')"
        clearable
        class="filter-keyword"
        @keyup.enter="load"
      />
      <el-select
        v-model="filter.contentType"
        :placeholder="t('logs.contentType')"
        clearable
        style="width: 150px"
      >
        <el-option
          v-for="type in ['TEXT', 'IMAGE', 'AUDIO', 'VIDEO']"
          :key="type"
          :label="type"
          :value="type"
        />
      </el-select>
      <el-select
        v-model="filter.action"
        :placeholder="t('logs.action')"
        clearable
        style="width: 140px"
      >
        <el-option
          v-for="action in ['PASS', 'REVIEW', 'REJECT', 'ERROR']"
          :key="action"
          :label="t(`status.${action}`)"
          :value="action"
        />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load">{{ t('common.search') }}</el-button>
      <el-button :icon="RotateCcw" @click="reset">{{ t('common.reset') }}</el-button>
    </section>
    <section v-loading="loading" class="content-section">
      <el-table :data="logs" row-key="id" @row-click="openDetail">
        <el-table-column :label="t('logs.time')" width="150"
          ><template #default="{ row }">{{
            formatDateTime(row.occurredAt)
          }}</template></el-table-column
        >
        <el-table-column :label="t('logs.taskId')" min-width="220"
          ><template #default="{ row }"
            ><button
              type="button"
              class="table-link"
              :title="row.taskId"
              @click.stop="openDetailRow(row)"
            >
              {{ compactId(row.taskId) }}
            </button></template
          ></el-table-column
        >
        <el-table-column prop="appId" :label="t('logs.appId')" width="140" />
        <el-table-column prop="contentType" :label="t('logs.contentType')" width="110" />
        <el-table-column prop="sceneCode" :label="t('logs.scene')" width="150" />
        <el-table-column :label="t('logs.action')" width="110"
          ><template #default="{ row }"><StatusBadge :status="row.action" /></template
        ></el-table-column>
        <el-table-column :label="t('logs.duration')" width="100"
          ><template #default="{ row }">{{ row.durationMs }} ms</template></el-table-column
        >
        <el-table-column :label="t('logs.labels')" min-width="160"
          ><template #default="{ row }"
            ><span v-if="!row.labels.length">-</span>
            <div v-else class="tag-list">
              <el-tag
                v-for="label in row.labels"
                :key="label"
                type="danger"
                effect="plain"
                size="small"
                >{{ label }}</el-tag
              >
            </div></template
          ></el-table-column
        >
      </el-table>
    </section>

    <el-drawer v-model="drawerOpen" :title="t('logs.detail')" size="min(560px, 92vw)">
      <template v-if="detail">
        <dl class="detail-grid">
          <div>
            <dt>{{ t('logs.taskId') }}</dt>
            <dd>{{ detail.taskId }}</dd>
          </div>
          <div>
            <dt>{{ t('logs.traceId') }}</dt>
            <dd>{{ detail.traceId }}</dd>
          </div>
          <div>
            <dt>{{ t('logs.appId') }}</dt>
            <dd>{{ detail.appId }}</dd>
          </div>
          <div>
            <dt>{{ t('logs.action') }}</dt>
            <dd><StatusBadge :status="detail.action" /></dd>
          </div>
        </dl>
        <ol class="trace-timeline">
          <li
            v-for="node in detail.nodes"
            :key="node.name"
            :class="`trace-timeline--${node.status}`"
          >
            <span class="trace-timeline__dot"></span>
            <div>
              <strong>{{ node.name }}</strong>
              <p>{{ node.message }}</p>
            </div>
            <time>{{ node.durationMs }} ms</time>
          </li>
        </ol>
      </template>
    </el-drawer>
  </div>
</template>
