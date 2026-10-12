<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckCheck, RefreshCw, Rocket } from '@lucide/vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import type { RuleDraft, RuleRelease, RuleSet } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()
const auth = useAuthStore()
const loading = ref(false)
const actionLoading = ref(false)
const ruleSets = ref<RuleSet[]>([])
const selectedCode = ref('')
const rules = ref<RuleDraft[]>([])
const releases = ref<RuleRelease[]>([])
const activeTab = ref('rules')
const selected = computed(() =>
  ruleSets.value.find((item) => item.ruleSetCode === selectedCode.value),
)

async function loadRuleSets() {
  loading.value = true
  try {
    ruleSets.value = await adminApi.ruleSets()
    if (!selectedCode.value && ruleSets.value.length)
      selectedCode.value = ruleSets.value[0]!.ruleSetCode
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadDetail(code: string) {
  if (!code) return
  loading.value = true
  try {
    ;[rules.value, releases.value] = await Promise.all([
      adminApi.rules(code),
      adminApi.releases(code),
    ])
  } catch {
    ElMessage.error(t('error.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function validateRules() {
  actionLoading.value = true
  try {
    const result = await adminApi.validateRuleSet(selectedCode.value)
    ElMessage.success(t('rules.validationPassed', { count: result.compiledRuleCount }))
  } finally {
    actionLoading.value = false
  }
}

async function publishRules() {
  if (!selected.value) return
  await ElMessageBox.confirm(
    t('rules.publishConfirm', { name: selected.value.ruleSetName }),
    t('common.publish'),
    {
      type: 'warning',
      confirmButtonText: t('common.publish'),
      cancelButtonText: t('common.cancel'),
    },
  )
  actionLoading.value = true
  try {
    await adminApi.publishRuleSet(selected.value.ruleSetCode, selected.value.version)
    ElMessage.success(t('rules.publishSuccess'))
    await Promise.all([loadRuleSets(), loadDetail(selected.value.ruleSetCode)])
  } finally {
    actionLoading.value = false
  }
}

watch(selectedCode, (code) => loadDetail(code))
onMounted(loadRuleSets)
</script>

<template>
  <div v-loading="loading" class="page-stack">
    <PageHeader :title="t('rules.title')">
      <template #actions>
        <el-button :icon="RefreshCw" @click="loadRuleSets">{{ t('common.refresh') }}</el-button>
        <el-button
          v-if="auth.hasPermission('RULE_READ')"
          :icon="CheckCheck"
          :loading="actionLoading"
          @click="validateRules"
          >{{ t('common.validate') }}</el-button
        >
        <el-button
          v-if="auth.hasPermission('RULE_PUBLISH')"
          type="primary"
          :icon="Rocket"
          :loading="actionLoading"
          @click="publishRules"
          >{{ t('common.publish') }}</el-button
        >
      </template>
    </PageHeader>

    <div class="rules-workspace">
      <aside class="rule-set-list">
        <div class="section-heading">
          <h2>{{ t('rules.ruleSets') }}</h2>
          <span>{{ ruleSets.length }}</span>
        </div>
        <button
          v-for="item in ruleSets"
          :key="item.ruleSetCode"
          type="button"
          :class="{ active: selectedCode === item.ruleSetCode }"
          @click="selectedCode = item.ruleSetCode"
        >
          <span
            ><strong>{{ item.ruleSetName }}</strong
            ><small>{{ item.ruleSetCode }}</small></span
          >
          <StatusBadge :status="item.enabled ? 'enabled' : 'offline'" />
        </button>
      </aside>

      <section v-if="selected" class="rule-detail">
        <div class="rule-detail__summary">
          <div>
            <small>{{ t('dashboard.scene') }}</small
            ><strong>{{ selected.scene }}</strong>
          </div>
          <div>
            <small>{{ t('rules.mode') }}</small
            ><strong>{{ selected.evaluationMode }}</strong>
          </div>
          <div>
            <small>{{ t('rules.errorPolicy') }}</small
            ><strong>{{ selected.errorPolicy }}</strong>
          </div>
          <div>
            <small>{{ t('rules.publishedVersion') }}</small
            ><strong>v{{ selected.publishedVersion }}</strong>
          </div>
        </div>
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="t('rules.draftRules')" name="rules">
            <el-table :data="rules" row-key="ruleId">
              <el-table-column :label="t('rules.ruleName')" min-width="190">
                <template #default="{ row }"
                  ><strong class="table-primary">{{ row.ruleName }}</strong
                  ><small class="table-secondary">{{ row.ruleId }}</small></template
                >
              </el-table-column>
              <el-table-column :label="t('rules.expression')" min-width="310">
                <template #default="{ row }"
                  ><code class="expression-code">{{ row.expression }}</code></template
                >
              </el-table-column>
              <el-table-column prop="priority" :label="t('rules.priority')" width="90" />
              <el-table-column :label="t('common.status')" width="110">
                <template #default="{ row }"
                  ><StatusBadge :status="row.enabled ? 'enabled' : 'offline'"
                /></template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="t('rules.releases')" name="releases">
            <el-table :data="releases" row-key="releaseId">
              <el-table-column :label="t('common.version')" width="100"
                ><template #default="{ row }">v{{ row.releaseVersion }}</template></el-table-column
              >
              <el-table-column prop="checksum" label="Checksum" min-width="150" />
              <el-table-column prop="publishedBy" :label="t('dashboard.publisher')" width="140" />
              <el-table-column :label="t('dashboard.publishedAt')" min-width="180"
                ><template #default="{ row }">{{
                  formatDateTime(row.publishedAt)
                }}</template></el-table-column
              >
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </div>
</template>
