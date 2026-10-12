<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight, RotateCcw, Search } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import type { LogEvent, LogFilter } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()
const loading = ref(false)
const detailLoading = ref(false)
const rows = ref<LogEvent[]>([])
const selected = ref<LogEvent | null>(null)
const taskEvents = ref<LogEvent[]>([])
const taskTruncated = ref(false)
const drawerOpen = ref(false)
const view = ref<'event' | 'task'>('event')
const selectedItem = ref('')
const nextCursor = ref('')
const hasMore = ref(false)
const timeRange = ref<[Date, Date]>([new Date(Date.now() - 60 * 60_000), new Date()])
const filter = reactive({
  taskId: '',
  traceId: '',
  appId: '',
  serviceName: '',
  logSpace: '',
  level: '',
  success: '',
})

const itemIds = computed(
  () => [...new Set(taskEvents.value.map((event) => event.itemId).filter(Boolean))] as string[],
)
const visibleEvents = computed(() =>
  taskEvents.value.filter(
    (event) => !selectedItem.value || !event.itemId || event.itemId === selectedItem.value,
  ),
)

function query(cursor = ''): LogFilter {
  return {
    from: timeRange.value[0].toISOString(),
    to: timeRange.value[1].toISOString(),
    size: 50,
    cursor: cursor || undefined,
    taskId: filter.taskId || undefined,
    traceId: filter.traceId || undefined,
    appId: filter.appId || undefined,
    serviceName: filter.serviceName || undefined,
    logSpace: filter.logSpace || undefined,
    level: filter.level || undefined,
    success: filter.success === '' ? undefined : filter.success === 'true',
  }
}

async function load(append = false) {
  if (loading.value || !timeRange.value?.[0] || !timeRange.value?.[1]) return
  loading.value = true
  try {
    const page = await adminApi.searchLogs(query(append ? nextCursor.value : ''))
    rows.value = append ? [...rows.value, ...page.items] : page.items
    nextCursor.value = page.nextCursor
    hasMore.value = page.hasMore
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(filter, {
    taskId: '',
    traceId: '',
    appId: '',
    serviceName: '',
    logSpace: '',
    level: '',
    success: '',
  })
  timeRange.value = [new Date(Date.now() - 60 * 60_000), new Date()]
  void load()
}

function openEvent(row: LogEvent) {
  selected.value = row
  view.value = 'event'
  taskEvents.value = []
  drawerOpen.value = true
}

async function openTask(row: LogEvent) {
  selected.value = row
  view.value = 'task'
  selectedItem.value = ''
  taskEvents.value = []
  taskTruncated.value = false
  drawerOpen.value = true
  detailLoading.value = true
  try {
    const task = await adminApi.taskLogs(row.taskId, query().from, query().to)
    taskEvents.value = task.events
    taskTruncated.value = task.truncated
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <div class="page-stack log-workspace">
    <PageHeader :title="t('logs.title')" />
    <section class="filter-bar filter-bar--wrap log-filters">
      <el-date-picker
        v-model="timeRange"
        type="datetimerange"
        format="MM/DD HH:mm"
        :start-placeholder="t('logs.from')"
        :end-placeholder="t('logs.to')"
        :clearable="false"
        class="log-filters__time"
      />
      <el-input
        v-model="filter.taskId"
        :placeholder="t('logs.taskId')"
        clearable
        @keyup.enter="load()"
      />
      <el-input
        v-model="filter.traceId"
        :placeholder="t('logs.traceId')"
        clearable
        @keyup.enter="load()"
      />
      <el-input
        v-model="filter.appId"
        :placeholder="t('logs.appId')"
        clearable
        @keyup.enter="load()"
      />
      <el-input
        v-model="filter.serviceName"
        :placeholder="t('logs.service')"
        clearable
        @keyup.enter="load()"
      />
      <el-select v-model="filter.logSpace" :placeholder="t('logs.space')" clearable>
        <el-option label="content-risk" value="content-risk" />
        <el-option label="api" value="api" />
        <el-option label="thirdparty" value="thirdparty" />
      </el-select>
      <el-select v-model="filter.level" :placeholder="t('logs.level')" clearable>
        <el-option
          v-for="level in ['INFO', 'WARN', 'ERROR', 'DEBUG']"
          :key="level"
          :label="level"
          :value="level"
        />
      </el-select>
      <el-select v-model="filter.success" :placeholder="t('logs.outcome')" clearable>
        <el-option :label="t('status.success')" value="true" />
        <el-option :label="t('status.error')" value="false" />
      </el-select>
      <el-button type="primary" :icon="Search" :loading="loading" @click="load()">{{
        t('common.search')
      }}</el-button>
      <el-button :icon="RotateCcw" @click="reset">{{ t('common.reset') }}</el-button>
    </section>

    <section v-loading="loading" class="content-section">
      <el-table :data="rows" row-key="eventId" @row-click="openEvent">
        <el-table-column :label="t('logs.time')" width="166">
          <template #default="{ row }">{{ formatDateTime(row['@timestamp']) }}</template>
        </el-table-column>
        <el-table-column
          prop="serviceName"
          :label="t('logs.service')"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="logSpace"
          :label="t('logs.space')"
          width="125"
          show-overflow-tooltip
        />
        <el-table-column
          prop="eventType"
          :label="t('logs.event')"
          width="150"
          show-overflow-tooltip
        />
        <el-table-column :label="t('logs.outcome')" width="85">
          <template #default="{ row }"
            ><el-tag :type="row.success ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.success ? t('status.success') : t('status.error') }}</el-tag
            ></template
          >
        </el-table-column>
        <el-table-column prop="costMillis" :label="t('logs.duration')" width="95">
          <template #default="{ row }">{{ row.costMillis }} ms</template>
        </el-table-column>
        <el-table-column :label="t('logs.taskId')" min-width="175">
          <template #default="{ row }"
            ><button
              v-if="row.taskId"
              type="button"
              class="table-link"
              :title="row.taskId"
              @click.stop="openTask(row as LogEvent)"
            >
              {{ row.taskId }}</button
            ><span v-else>-</span></template
          >
        </el-table-column>
        <el-table-column
          prop="message"
          :label="t('logs.message')"
          min-width="200"
          show-overflow-tooltip
        />
      </el-table>
      <div v-if="hasMore" class="log-next">
        <el-button :icon="ArrowRight" :loading="loading" @click="load(true)">
          {{ t('logs.next') }}</el-button
        >
      </div>
    </section>

    <el-drawer
      v-model="drawerOpen"
      :title="view === 'task' ? t('logs.taskDetail') : t('logs.detail')"
      size="min(720px, 96vw)"
    >
      <template v-if="selected">
        <div class="log-drawer-actions">
          <el-segmented
            v-model="view"
            :options="[
              { label: t('logs.detail'), value: 'event' },
              { label: t('logs.taskDetail'), value: 'task' },
            ]"
            @change="view === 'task' && selected && openTask(selected)"
          />
        </div>
        <template v-if="view === 'event'">
          <dl class="detail-grid">
            <div
              v-for="field in [
                '@timestamp',
                'serviceName',
                'logSpace',
                'level',
                'eventType',
                'taskId',
                'itemId',
                'traceId',
                'appId',
                'sceneCode',
                'source',
                'handlerName',
                'success',
                'costMillis',
                'message',
              ]"
              :key="field"
            >
              <dt>{{ field }}</dt>
              <dd>{{ selected[field as keyof LogEvent] ?? '-' }}</dd>
            </div>
          </dl>
        </template>
        <template v-else>
          <div class="log-task-head">
            <strong>{{ selected.taskId }}</strong> <span>{{ t('logs.incompleteHint') }}</span>
          </div>
          <div v-if="itemIds.length" class="log-item-filter">
            <el-select v-model="selectedItem" :placeholder="t('logs.allItems')" clearable>
              <el-option v-for="id in itemIds" :key="id" :label="id" :value="id" />
            </el-select>
          </div>
          <p v-if="taskTruncated" class="log-truncated">{{ t('logs.truncated') }}</p>
          <div v-loading="detailLoading" class="log-timeline">
            <el-empty
              v-if="!detailLoading && !visibleEvents.length"
              :description="t('logs.noEvents')"
            />
            <article
              v-for="event in visibleEvents"
              :key="event.eventId"
              class="log-timeline__event"
            >
              <span
                class="log-timeline__mark"
                :class="{ 'log-timeline__mark--error': !event.success }"
              ></span>
              <div class="log-timeline__content">
                <header>
                  <strong>{{ event.handlerName || event.eventType }}</strong>
                  <time>{{ formatDateTime(event['@timestamp']) }}</time>
                </header>
                <p>{{ event.message }}</p>
                <small
                  >{{ event.serviceName }} · {{ event.itemId || t('logs.taskLevel') }} ·
                  {{ event.costMillis }} ms</small
                >
              </div>
            </article>
          </div>
        </template>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.log-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.log-filters .el-input {
  width: 160px;
}
.log-filters .el-select {
  width: 135px;
}
.log-filters :deep(.log-filters__time) {
  width: 350px !important;
  min-width: 0 !important;
  flex: 0 1 350px;
}
.log-next {
  display: flex;
  justify-content: center;
  padding: 18px;
}
.log-drawer-actions {
  margin-bottom: 20px;
}
.log-task-head {
  display: grid;
  gap: 5px;
  overflow-wrap: anywhere;
}
.log-task-head span,
.log-timeline__content small {
  color: var(--color-text-muted);
  font-size: 12px;
}
.log-item-filter {
  margin: 18px 0;
}
.log-item-filter .el-select {
  width: 220px;
  max-width: 100%;
}
.log-truncated {
  color: var(--color-warning);
}
.log-timeline {
  min-height: 80px;
}
.log-timeline__event {
  display: flex;
  gap: 14px;
  padding: 0 0 20px;
}
.log-timeline__mark {
  position: relative;
  flex: 0 0 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: var(--color-primary);
}
.log-timeline__mark::after {
  position: absolute;
  top: 12px;
  left: 4px;
  height: calc(100% + 20px);
  border-left: 1px solid var(--color-border);
  content: '';
}
.log-timeline__event:last-child .log-timeline__mark::after {
  display: none;
}
.log-timeline__mark--error {
  background: var(--color-danger);
}
.log-timeline__content {
  flex: 1;
  min-width: 0;
}
.log-timeline__content header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.log-timeline__content p {
  margin: 6px 0;
  overflow-wrap: anywhere;
}
@media (max-width: 700px) {
  .log-filters .el-input,
  .log-filters .el-select,
  .log-filters :deep(.log-filters__time) {
    width: 100% !important;
    flex: none;
  }
}
</style>
