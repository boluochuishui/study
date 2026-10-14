<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Pencil, Plus, RefreshCw } from '@lucide/vue'
import 'element-plus/es/components/message/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import type { ManagedRecord, ManagedResource, RuleRelease } from '@/types/admin'
import { formatDateTime } from '@/utils/format'
import { managedFields, primaryKey, resourceGroups, type ManagedField } from './managed-schema'

const props = defineProps<{ group: 'access' | 'media' }>()
const { t } = useI18n()
const auth = useAuthStore()
const resources = computed(() => resourceGroups[props.group])
const selected = ref<ManagedResource>(resources.value[0]!)
const records = reactive({
  apps: [],
  scenes: [],
  rules: [],
  libraries: [],
  assets: [],
  memberships: [],
} as Record<ManagedResource, ManagedRecord[]>)
const releases = ref<RuleRelease[]>([])
const busy = ref(false)
const saving = ref(false)
const editorOpen = ref(false)
const editing = ref<ManagedRecord | null>(null)
const form = reactive<Record<string, string | number | boolean | null>>({})
const filterText = ref('')
const sampleText = ref('')
const sampleFile = ref<File | null>(null)

const fields = computed(() => managedFields[selected.value])
const visibleFields = computed(() =>
  fields.value.filter((field) => {
    if (selected.value !== 'rules') return true
    const ruleType = form.rule_type
    if (field.key === 'media_library_id') return ruleType === 'MEDIA_HASH'
    if (field.key === 'spel_release_id' || field.key === 'spel_rule_id') return ruleType === 'SPEL'
    if (field.key === 'on_hit_action' || field.key === 'risk_labels_json')
      return ruleType !== 'SPEL'
    if (field.key === 'parameters_json')
      return ['KEYWORD', 'REGEX', 'LLM'].includes(String(ruleType))
    return true
  }),
)
function requiredField(field: ManagedField) {
  if (field.required) return true
  if (selected.value !== 'rules') return false
  if (field.key === 'on_hit_action') return form.rule_type !== 'SPEL'
  if (field.key === 'media_library_id') return form.rule_type === 'MEDIA_HASH'
  if (field.key === 'spel_release_id' || field.key === 'spel_rule_id')
    return form.rule_type === 'SPEL'
  if (field.key === 'parameters_json')
    return ['KEYWORD', 'REGEX', 'LLM'].includes(String(form.rule_type))
  return false
}
const shownRows = computed(() => {
  const needle = filterText.value.trim().toLowerCase()
  return records[selected.value].filter(
    (row) =>
      !needle ||
      fields.value.some((field) =>
        String(row[field.key] ?? '')
          .toLowerCase()
          .includes(needle),
      ),
  )
})

async function load() {
  busy.value = true
  try {
    const needed: ManagedResource[] =
      props.group === 'access'
        ? ['apps', 'scenes', 'rules', 'libraries']
        : ['libraries', 'assets', 'memberships']
    const results = await Promise.all(needed.map((resource) => adminApi.managedRecords(resource)))
    needed.forEach((resource, index) => {
      records[resource] = results[index] ?? []
    })
    if (props.group === 'access' && auth.hasPermission('RULE_READ')) {
      const sets = await adminApi.ruleSets()
      releases.value = (await Promise.all(sets.map((set) => adminApi.releases(set.ruleSetCode))))
        .flat()
        .filter((release) => !release.deleted)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('error.loadFailed'))
  } finally {
    busy.value = false
  }
}

function recordName(resource: ManagedResource, row: ManagedRecord): string {
  if (resource === 'memberships') {
    return `${referenceLabel('libraries', row.library_id)} / ${referenceLabel('assets', row.asset_id)}`
  }
  return String(row[primaryKey(resource)] ?? row.id ?? '')
}

function referenceLabel(resource: ManagedResource, id: unknown): string {
  const row = records[resource].find((item) => Number(item.id) === Number(id))
  return row ? recordName(resource, row) : String(id ?? '')
}

function fieldDisplay(row: ManagedRecord, field: ManagedField) {
  const value = row[field.key]
  if (value === null || value === undefined || value === '') return '—'
  if (field.reference) return referenceLabel(field.reference, value)
  const text = String(value)
  return text.length > 92 ? `${text.slice(0, 92)}…` : text
}

function openEditor(row?: ManagedRecord) {
  editing.value = row ?? null
  for (const key of Object.keys(form)) delete form[key]
  for (const field of fields.value) {
    const value = row?.[field.key]
    form[field.key] =
      field.kind === 'switch'
        ? value === undefined
          ? Boolean(field.default)
          : value === true || value === 1
        : value === undefined || value === null
          ? (field.default ?? null)
          : value
  }
  sampleText.value = ''
  sampleFile.value = null
  editorOpen.value = true
}

function readFile(event: Event) {
  sampleFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

function setNumber(key: string, value: number | undefined) {
  form[key] = value ?? null
}

async function calculateHash() {
  let bytes: ArrayBuffer
  if (form.content_type === 'TEXT') {
    if (!sampleText.value.trim()) return
    const normalized = sampleText.value
      .normalize('NFKC')
      .replaceAll('\u200B', '')
      .replaceAll('\u200C', '')
      .replaceAll('\u200D', '')
      .replaceAll('\uFEFF', '')
      .toLowerCase()
      .replace(/\s+/g, ' ')
      .trim()
    bytes = new TextEncoder().encode(normalized).buffer
    form.hash_version = 'text-normalize-v1'
  } else {
    if (!sampleFile.value) return
    if (sampleFile.value.size > 20 * 1024 * 1024) {
      ElMessage.error(t('managed.fileTooLarge'))
      return
    }
    bytes = await sampleFile.value.arrayBuffer()
    form.hash_version = 'raw-bytes-v1'
  }
  const hash = await crypto.subtle.digest('SHA-256', bytes)
  form.content_sha256 = Array.from(new Uint8Array(hash), (byte) =>
    byte.toString(16).padStart(2, '0'),
  ).join('')
  sampleText.value = ''
  sampleFile.value = null
}

function referenceOptions(resource: ManagedResource) {
  if (resource === 'assets' && selected.value === 'memberships') {
    const library = records.libraries.find((row) => row.id === form.library_id)
    return records.assets
      .filter((row) => row.enabled === 1 || row.enabled === true)
      .filter((row) => !library || row.content_type === library.content_type)
  }
  if (resource === 'libraries' && selected.value === 'rules') {
    return records.libraries.filter(
      (row) =>
        row.library_type !== 'TESTSET' &&
        row.content_type === form.content_type &&
        (row.enabled === 1 || row.enabled === true),
    )
  }
  return records[resource].filter((row) => row.enabled === 1 || row.enabled === true)
}

async function save() {
  const values: Record<string, string | number | boolean | null> = {}
  for (const field of fields.value) {
    const visible = visibleFields.value.includes(field)
    let value = visible ? (form[field.key] ?? null) : null
    if (requiredField(field) && visible && (value === null || value === '')) {
      ElMessage.error(t('managed.required'))
      return
    }
    if (field.kind === 'json' && value) {
      try {
        JSON.parse(String(value))
      } catch {
        ElMessage.error(t('managed.invalidJson'))
        return
      }
    }
    if (field.kind === 'number' && value !== null) value = Number(value)
    values[field.key] = value
  }
  saving.value = true
  try {
    const request = { version: Number(editing.value?.version ?? 0), values }
    if (editing.value) {
      await adminApi.updateManagedRecord(selected.value, Number(editing.value.id), request)
    } else {
      await adminApi.createManagedRecord(selected.value, request)
    }
    editorOpen.value = false
    ElMessage.success(t('common.success'))
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('error.loadFailed'))
  } finally {
    saving.value = false
  }
}

watch(
  () => props.group,
  () => {
    selected.value = resources.value[0]!
    filterText.value = ''
    void load()
  },
  { immediate: true },
)
watch(selected, () => {
  filterText.value = ''
})
watch(
  () => form.rule_type,
  (type) => {
    if (selected.value === 'rules' && type === 'LLM') form.execution_mode = 'ASYNC'
  },
)
</script>

<template>
  <div class="page-stack">
    <PageHeader :title="t(`managed.${group}.title`)">
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

    <section class="filter-bar managed-toolbar">
      <el-segmented
        v-model="selected"
        :options="
          resources.map((resource) => ({
            label: t(`managed.resources.${resource}`),
            value: resource,
          }))
        "
      />
      <el-input
        v-model="filterText"
        clearable
        :placeholder="t('common.search')"
        class="managed-search"
      />
      <span class="filter-bar__count">{{ shownRows.length }}</span>
    </section>

    <section v-loading="busy" class="content-section">
      <el-table :data="shownRows" row-key="id" max-height="calc(100vh - 270px)">
        <el-table-column :label="t('managed.name')" min-width="190" fixed="left">
          <template #default="{ row }"
            ><strong class="table-primary">{{ recordName(selected, row) }}</strong></template
          >
        </el-table-column>
        <el-table-column
          v-for="field in fields.filter(
            (item) =>
              !['description', 'enabled'].includes(item.key) &&
              item.key !== primaryKey(selected) &&
              item.key !== 'credential_ref',
          )"
          :key="field.key"
          :label="t(`managed.fields.${field.key}`)"
          min-width="140"
        >
          <template #default="{ row }">{{ fieldDisplay(row, field) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110">
          <template #default="{ row }"
            ><StatusBadge
              :status="row.enabled === 1 || row.enabled === true ? 'enabled' : 'offline'"
          /></template>
        </el-table-column>
        <el-table-column :label="t('common.version')" width="85">
          <template #default="{ row }">v{{ row.version }}</template>
        </el-table-column>
        <el-table-column :label="t('common.updatedAt')" width="170">
          <template #default="{ row }">{{ formatDateTime(String(row.updated_at)) }}</template>
        </el-table-column>
        <el-table-column
          v-if="auth.hasPermission('CONFIG_WRITE')"
          :label="t('common.actions')"
          width="100"
          fixed="right"
        >
          <template #default="{ row }"
            ><el-button link type="primary" :icon="Pencil" @click="openEditor(row)">{{
              t('common.edit')
            }}</el-button></template
          >
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="editorOpen"
      :title="t(`managed.resources.${selected}`)"
      width="min(690px, 94vw)"
      destroy-on-close
    >
      <el-form label-position="top" class="managed-form" @submit.prevent="save">
        <el-form-item
          v-for="field in visibleFields"
          :key="field.key"
          :label="t(`managed.fields.${field.key}`)"
          :required="requiredField(field)"
        >
          <el-switch
            v-if="field.kind === 'switch'"
            :model-value="Boolean(form[field.key])"
            @update:model-value="(value) => (form[field.key] = value)"
          />
          <el-select
            v-else-if="field.kind === 'select'"
            :model-value="form[field.key] ?? undefined"
            clearable
            :disabled="Boolean(editing && field.immutable)"
            style="width: 100%"
            @update:model-value="(value) => (form[field.key] = value)"
          >
            <el-option
              v-for="option in field.options"
              :key="option"
              :value="option"
              :label="t(`managed.options.${option}`)"
            />
          </el-select>
          <el-select
            v-else-if="field.kind === 'reference'"
            :model-value="form[field.key] ?? undefined"
            clearable
            filterable
            :disabled="Boolean(editing && field.immutable)"
            style="width: 100%"
            @update:model-value="(value) => (form[field.key] = value)"
          >
            <el-option
              v-for="option in referenceOptions(field.reference!)"
              :key="Number(option.id)"
              :value="Number(option.id)"
              :label="recordName(field.reference!, option)"
            />
          </el-select>
          <el-select
            v-else-if="field.key === 'spel_release_id' && releases.length"
            :model-value="form[field.key] ?? undefined"
            clearable
            filterable
            style="width: 100%"
            @update:model-value="(value) => (form[field.key] = value)"
          >
            <el-option
              v-for="release in releases"
              :key="release.releaseId"
              :value="release.releaseId"
              :label="`${release.ruleSetCode} · v${release.releaseVersion}`"
            />
          </el-select>
          <el-input-number
            v-else-if="field.kind === 'number'"
            :model-value="typeof form[field.key] === 'number' ? Number(form[field.key]) : undefined"
            :min="field.key === 'priority' ? 0 : 1"
            :step="1"
            controls-position="right"
            @update:model-value="setNumber(field.key, $event)"
          />
          <el-input
            v-else-if="field.kind === 'textarea' || field.kind === 'json'"
            :model-value="String(form[field.key] ?? '')"
            type="textarea"
            :rows="field.kind === 'json' ? 3 : 2"
            @update:model-value="(value) => (form[field.key] = value)"
          />
          <el-input
            v-else
            :model-value="String(form[field.key] ?? '')"
            :disabled="Boolean(editing && field.immutable)"
            :placeholder="field.key === 'credential_ref' ? 'env:DETECT_APP_SECRET' : undefined"
            @update:model-value="(value) => (form[field.key] = value)"
          />
        </el-form-item>
        <div v-if="selected === 'assets' && !editing" class="managed-sample">
          <label>{{ t('managed.sample') }}</label>
          <el-input
            v-if="form.content_type === 'TEXT'"
            v-model="sampleText"
            type="textarea"
            :rows="2"
          />
          <input v-else type="file" @change="readFile" />
          <el-button :disabled="!sampleText && !sampleFile" @click="calculateHash">
            {{ t('managed.calculateHash') }}
          </el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editorOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.managed-toolbar {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px 16px;
}
.managed-search {
  width: min(240px, 100%);
  margin-left: auto;
}
.managed-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
  max-height: 62vh;
  overflow: auto;
}
.managed-form :deep(.el-form-item) {
  min-width: 0;
}
.managed-sample {
  grid-column: 1 / -1;
  display: grid;
  gap: 10px;
  padding: 12px 0;
}
.managed-sample label {
  font-weight: 600;
}
.managed-sample .el-button {
  justify-self: start;
}
@media (max-width: 700px) {
  .managed-form {
    grid-template-columns: 1fr;
  }
  .managed-search {
    margin-left: 0;
  }
}
</style>
