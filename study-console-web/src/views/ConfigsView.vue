<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Pencil, Plus, RefreshCw } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import type { ConfigItem, SaveConfigRequest } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()
const auth = useAuthStore()
const namespaces = ['detect.text', 'detect.image', 'auth.routing', 'thread-pool']
const selectedNamespace = ref('detect.text')
const configs = ref<ConfigItem[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editing = ref<ConfigItem | null>(null)
const form = reactive({ key: '', value: '', description: '', enabled: true })

async function load() {
  loading.value = true
  try {
    configs.value = await adminApi.configs(selectedNamespace.value)
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openEditor(item?: ConfigItem) {
  editing.value = item ?? null
  form.key = item?.configKey ?? ''
  form.value = item?.configValue ?? ''
  form.description = item?.description ?? ''
  form.enabled = item?.enabled ?? true
  dialogOpen.value = true
}

function openEditorRow(row: unknown) {
  openEditor(row as ConfigItem)
}

async function save() {
  if (!form.key.trim() || !form.value.trim()) return
  saving.value = true
  try {
    const request: SaveConfigRequest = {
      value: form.value,
      enabled: form.enabled,
      description: form.description,
      version: editing.value?.version,
    }
    await adminApi.saveConfig(selectedNamespace.value, form.key, request)
    dialogOpen.value = false
    ElMessage.success(t('common.success'))
    await load()
  } finally {
    saving.value = false
  }
}

watch(selectedNamespace, load, { immediate: true })
</script>

<template>
  <div class="page-stack">
    <PageHeader :title="t('configs.title')">
      <template #actions>
        <el-button :icon="RefreshCw" @click="load">{{ t('common.refresh') }}</el-button>
        <el-button
          v-if="auth.hasPermission('CONFIG_WRITE')"
          type="primary"
          :icon="Plus"
          @click="openEditor()"
          >{{ t('common.create') }}</el-button
        >
      </template>
    </PageHeader>

    <section class="filter-bar">
      <label>{{ t('configs.namespace') }}</label>
      <el-select v-model="selectedNamespace" style="width: 240px">
        <el-option v-for="item in namespaces" :key="item" :label="item" :value="item" />
      </el-select>
      <span class="filter-bar__count">{{ configs.length }}</span>
    </section>

    <section v-loading="loading" class="content-section">
      <el-table :data="configs" row-key="id">
        <el-table-column :label="t('configs.key')" min-width="230"
          ><template #default="{ row }"
            ><strong class="table-primary">{{ row.configKey }}</strong></template
          ></el-table-column
        >
        <el-table-column :label="t('configs.value')" min-width="220"
          ><template #default="{ row }"
            ><code class="config-value">{{ row.configValue }}</code></template
          ></el-table-column
        >
        <el-table-column prop="description" :label="t('configs.description')" min-width="220" />
        <el-table-column :label="t('common.version')" width="90"
          ><template #default="{ row }">v{{ row.version }}</template></el-table-column
        >
        <el-table-column :label="t('common.status')" width="110"
          ><template #default="{ row }"
            ><StatusBadge :status="row.enabled ? 'enabled' : 'offline'" /></template
        ></el-table-column>
        <el-table-column :label="t('common.updatedAt')" width="160"
          ><template #default="{ row }">{{
            formatDateTime(row.updatedAt)
          }}</template></el-table-column
        >
        <el-table-column
          v-if="auth.hasPermission('CONFIG_WRITE')"
          :label="t('common.actions')"
          width="86"
          fixed="right"
        >
          <template #default="{ row }"
            ><el-button link type="primary" :icon="Pencil" @click="openEditorRow(row)">{{
              t('common.edit')
            }}</el-button></template
          >
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogOpen"
      :title="editing ? t('configs.editTitle') : t('configs.newTitle')"
      width="min(520px, 92vw)"
    >
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item :label="t('configs.namespace')"
          ><el-input :model-value="selectedNamespace" disabled
        /></el-form-item>
        <el-form-item :label="t('configs.key')" required
          ><el-input v-model="form.key" :disabled="Boolean(editing)"
        /></el-form-item>
        <el-form-item :label="t('configs.value')" required
          ><el-input v-model="form.value" type="textarea" :rows="4"
        /></el-form-item>
        <el-form-item :label="t('configs.description')"
          ><el-input v-model="form.description"
        /></el-form-item>
        <el-form-item :label="t('common.status')"
          ><el-switch v-model="form.enabled"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
