<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Play, RefreshCw, Save } from '@lucide/vue'
import 'element-plus/es/components/message/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import type { DetectContentType, OnlineTestRecord, OnlineTestResult } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()
const auth = useAuthStore()
const tab = ref<'test' | 'records'>('test')
const contentType = ref<DetectContentType>('TEXT')
const mode = ref<'SYNC' | 'ASYNC'>('SYNC')
const source = ref<'text' | 'url' | 'file'>('text')
const text = ref('')
const batchText = ref(false)
const url = ref('')
const files = ref<File[]>([])
const language = ref('zh')
const region = ref('CN')
const appId = ref('')
const sceneCode = ref('')
const configVersion = ref(0)
const savedProfile = ref({ appId: '', sceneCode: '' })
const running = ref(false)
const saving = ref(false)
const loadingRecords = ref(false)
const result = ref<OnlineTestResult | null>(null)
const records = ref<OnlineTestRecord[]>([])
const detail = ref<OnlineTestRecord | null>(null)
const detailOpen = ref(false)
const types: DetectContentType[] = ['TEXT', 'IMAGE', 'AUDIO', 'VIDEO']
const maxBytes = computed(() =>
  contentType.value === 'IMAGE' ? 10 * 1024 * 1024 : 40 * 1024 * 1024,
)
const canEdit = computed(
  () => auth.hasPermission('CONFIG_READ') && auth.hasPermission('CONFIG_WRITE'),
)

function statusLabel(status: string) {
  const key = `onlineTest.state.${status}`
  return t(key) === key ? status : t(key)
}

async function loadProfile() {
  const type = contentType.value
  appId.value = ''
  sceneCode.value = ''
  savedProfile.value = { appId: '', sceneCode: '' }
  configVersion.value = 0
  try {
    const profile = await adminApi.onlineTestProfile(type)
    if (type !== contentType.value) return
    appId.value = profile.appId
    sceneCode.value = profile.sceneCode
    savedProfile.value = profile
  } catch {
    // 未配置时保留空表单，允许配置权限用户直接建立配置。
  }
  if (canEdit.value) {
    try {
      const configs = await adminApi.configs('console.online-test')
      if (type === contentType.value) {
        configVersion.value = configs.find((item) => item.configKey === type)?.version ?? 0
      }
    } catch {
      ElMessage.error(t('error.loadFailed'))
    }
  }
}

async function saveProfile() {
  if (!appId.value.trim() || !sceneCode.value.trim()) return
  saving.value = true
  try {
    const saved = await adminApi.saveConfig('console.online-test', contentType.value, {
      value: JSON.stringify({ appId: appId.value.trim(), sceneCode: sceneCode.value.trim() }),
      enabled: true,
      description: 'Online test profile',
      version: configVersion.value,
    })
    configVersion.value = saved.version
    savedProfile.value = { appId: appId.value.trim(), sceneCode: sceneCode.value.trim() }
    ElMessage.success(t('onlineTest.saved'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('error.loadFailed'))
  } finally {
    saving.value = false
  }
}

function chooseFiles(event: Event) {
  files.value = Array.from((event.target as HTMLInputElement).files ?? [])
}

function encode(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.onerror = () => reject(new Error('File read failed'))
    reader.readAsDataURL(file)
  })
}

async function run() {
  if (!appId.value || !sceneCode.value || appId.value.trim() !== savedProfile.value.appId
      || sceneCode.value.trim() !== savedProfile.value.sceneCode) {
    ElMessage.warning(t('onlineTest.missingProfile'))
    return
  }
  const values =
    source.value === 'text'
      ? mode.value === 'SYNC' || !batchText.value
        ? [text.value.trim()].filter(Boolean)
        : text.value
            .split('\n')
            .map((value) => value.trim())
            .filter(Boolean)
      : url.value
          .split('\n')
          .map((value) => value.trim())
          .filter(Boolean)
  const chosen = source.value === 'file' ? files.value : []
  if (
    (source.value !== 'file' && values.length === 0) ||
    (source.value === 'file' && chosen.length === 0)
  ) {
    ElMessage.warning(t('onlineTest.empty'))
    return
  }
  if (chosen.some((file) => file.size > maxBytes.value)
      || chosen.reduce((total, file) => total + file.size, 0) > 40 * 1024 * 1024) {
    ElMessage.warning(t('onlineTest.tooLarge'))
    return
  }
  if (
    (source.value === 'text' && values.some((value) => value.length > 5000)) ||
    ((mode.value === 'SYNC' || contentType.value === 'AUDIO' || contentType.value === 'VIDEO') &&
      (source.value === 'file' ? chosen.length : values.length) !== 1) ||
    (source.value === 'file' ? chosen.length : values.length) > 100
  ) {
    ElMessage.warning(t('onlineTest.empty'))
    return
  }
  running.value = true
  result.value = null
  try {
    const items =
      source.value === 'file'
        ? await Promise.all(
            chosen.map(async (file, index) => ({
              itemId: String(index + 1),
              data: await encode(file),
            })),
          )
        : values.map((value, index) => ({
            itemId: String(index + 1),
            ...(source.value === 'text' ? { text: value } : { url: value }),
          }))
    result.value = await adminApi.runOnlineTest({
      contentType: contentType.value,
      executionMode: mode.value,
      language: language.value,
      region: region.value,
      items,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('error.loadFailed'))
  } finally {
    running.value = false
  }
}

async function loadRecords() {
  loadingRecords.value = true
  try {
    records.value = await adminApi.onlineTestRecords()
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    loadingRecords.value = false
  }
}

function showDetail(record: OnlineTestRecord) {
  detail.value = record
  detailOpen.value = true
}

watch(contentType, () => {
  if (contentType.value === 'AUDIO' || contentType.value === 'VIDEO') mode.value = 'ASYNC'
  source.value = contentType.value === 'TEXT' ? 'text' : 'url'
  text.value = ''
  batchText.value = false
  url.value = ''
  files.value = []
  result.value = null
  void loadProfile()
})
watch(tab, (value) => {
  if (value === 'records') void loadRecords()
})
onMounted(loadProfile)
</script>

<template>
  <div class="page-stack online-test">
    <PageHeader :title="t('onlineTest.title')" />
    <el-tabs v-model="tab">
      <el-tab-pane :label="t('onlineTest.title')" name="test">
        <section class="content-section online-test__section">
          <div class="online-test__toolbar">
            <el-segmented
              v-model="contentType"
              :options="
                types.map((type) => ({
                  label: t(`onlineTest.${type.toLowerCase()}`),
                  value: type,
                }))
              "
            />
            <el-segmented
              v-model="mode"
              :options="[
                {
                  label: t('onlineTest.sync'),
                  value: 'SYNC',
                  disabled: contentType === 'AUDIO' || contentType === 'VIDEO',
                },
                { label: t('onlineTest.async'), value: 'ASYNC' },
              ]"
            />
          </div>
          <div class="online-test__grid">
            <div>
              <h3>{{ t('onlineTest.profile') }}</h3>
              <div class="online-test__fields">
                <label
                  >{{ t('onlineTest.appId') }}<el-input v-model="appId" :disabled="!canEdit"
                /></label>
                <label
                  >{{ t('onlineTest.sceneCode')
                  }}<el-input v-model="sceneCode" :disabled="!canEdit"
                /></label>
                <el-button v-if="canEdit" :icon="Save" :loading="saving" @click="saveProfile">{{
                  t('common.save')
                }}</el-button>
              </div>
              <h3>{{ t('onlineTest.content') }}</h3>
              <el-checkbox v-if="contentType === 'TEXT' && mode === 'ASYNC'" v-model="batchText">
                {{ t('onlineTest.batchText') }}
              </el-checkbox>
              <el-radio-group v-if="contentType !== 'TEXT'" v-model="source">
                <el-radio-button value="url">URL</el-radio-button>
                <el-radio-button value="file">{{ t('onlineTest.file') }}</el-radio-button>
              </el-radio-group>
              <el-input
                v-if="source === 'text'"
                v-model="text"
                type="textarea"
                :rows="8"
                :maxlength="mode === 'SYNC' ? 5000 : undefined"
              />
              <el-input
                v-else-if="source === 'url'"
                v-model="url"
                type="textarea"
                :rows="5"
                :placeholder="t('onlineTest.url')"
              />
              <input
                v-else
                type="file"
                :accept="
                  contentType === 'IMAGE'
                    ? 'image/*'
                    : contentType === 'AUDIO'
                      ? 'audio/*'
                      : 'video/*'
                "
                :multiple="mode === 'ASYNC' && contentType === 'IMAGE'"
                @change="chooseFiles"
              />
              <div class="online-test__fields online-test__fields--meta">
                <label>{{ t('onlineTest.language') }}<el-input v-model="language" /></label>
                <label>{{ t('onlineTest.region') }}<el-input v-model="region" /></label>
              </div>
              <el-button type="primary" :icon="Play" :loading="running" @click="run">{{
                t('onlineTest.run')
              }}</el-button>
            </div>
            <div class="online-test__response">
              <h3>{{ t('onlineTest.result') }}</h3>
              <template v-if="result">
                <el-alert
                  v-if="result.executionMode === 'ASYNC' && result.status === 'ACCEPTED'"
                  type="info"
                  :title="t('onlineTest.pending')"
                  show-icon
                  :closable="false"
                />
                <p>
                  <strong>{{ t('onlineTest.taskId') }}:</strong> {{ result.taskId || '—' }}
                </p>
                <p>
                  <strong>{{ t('common.status') }}:</strong> {{ statusLabel(result.status) }}
                </p>
                <pre>{{ JSON.stringify(result.result, null, 2) }}</pre>
              </template>
              <el-empty v-else :description="t('common.noData')" />
            </div>
          </div>
        </section>
      </el-tab-pane>
      <el-tab-pane
        v-if="auth.hasPermission('TEST_READ')"
        :label="t('onlineTest.records')"
        name="records"
      >
        <section class="content-section">
          <el-button :icon="RefreshCw" @click="loadRecords">{{ t('common.refresh') }}</el-button>
          <el-table
            v-loading="loadingRecords"
            :data="records"
            row-key="id"
            class="online-test__table"
            @row-click="showDetail"
          >
            <el-table-column prop="createdAt" :label="t('onlineTest.time')" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="contentType" :label="t('logs.contentType')" width="95" />
            <el-table-column prop="executionMode" :label="t('onlineTest.mode')" width="95" />
            <el-table-column prop="sceneCode" :label="t('onlineTest.sceneCode')" min-width="150" />
            <el-table-column prop="appId" :label="t('onlineTest.appId')" min-width="130" />
            <el-table-column prop="itemCount" :label="t('onlineTest.itemCount')" width="90" />
            <el-table-column prop="taskId" :label="t('onlineTest.taskId')" min-width="220" />
            <el-table-column prop="status" :label="t('common.status')" width="120">
              <template #default="{ row }">{{ statusLabel(row.status) }}</template>
            </el-table-column>
            <el-table-column prop="durationMs" :label="t('onlineTest.duration')" width="100" />
          </el-table>
        </section>
      </el-tab-pane>
    </el-tabs>
    <el-drawer v-model="detailOpen" :title="t('onlineTest.result')" size="min(560px, 100%)">
      <pre v-if="detail">{{ JSON.stringify(JSON.parse(detail.resultJson || '{}'), null, 2) }}</pre>
    </el-drawer>
  </div>
</template>

<style scoped>
.online-test__section {
  padding: 22px;
}
.online-test__toolbar {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}
.online-test__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 28px;
}
.online-test h3 {
  font-size: 15px;
  margin: 0 0 14px;
}
.online-test__fields {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: end;
  margin-bottom: 22px;
}
.online-test__fields label {
  display: grid;
  gap: 6px;
  min-width: 180px;
  flex: 1;
  font-size: 12px;
}
.online-test__fields--meta {
  margin-top: 18px;
}
.online-test__response {
  min-width: 0;
  border-left: 1px solid var(--el-border-color-light);
  padding-left: 24px;
}
.online-test pre {
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
}
.online-test__table {
  margin-top: 12px;
  cursor: pointer;
}
@media (max-width: 900px) {
  .online-test__grid {
    grid-template-columns: 1fr;
  }
  .online-test__response {
    border-left: 0;
    border-top: 1px solid var(--el-border-color-light);
    padding: 20px 0 0;
  }
}
</style>
