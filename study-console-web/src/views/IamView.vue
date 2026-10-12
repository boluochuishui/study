<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type { AdminRole, AdminUser } from '@/types/admin'

const { t } = useI18n()
const loading = ref(false)
const activeTab = ref('users')
const users = ref<AdminUser[]>([])
const roles = ref<AdminRole[]>([])

async function load() {
  loading.value = true
  try {
    ;[users.value, roles.value] = await Promise.all([adminApi.users(), adminApi.roles()])
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
    <PageHeader :title="t('iam.title')">
      <template #actions
        ><el-button :icon="RefreshCw" @click="load">{{ t('common.refresh') }}</el-button></template
      >
    </PageHeader>
    <section class="content-section tabs-section">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('iam.users')" name="users">
          <el-table :data="users" row-key="id">
            <el-table-column :label="t('iam.username')" min-width="180"
              ><template #default="{ row }"
                ><strong class="table-primary">{{ row.username }}</strong
                ><small class="table-secondary">ID {{ row.id }}</small></template
              ></el-table-column
            >
            <el-table-column prop="displayName" :label="t('iam.displayName')" min-width="150" />
            <el-table-column :label="t('iam.role')" min-width="250"
              ><template #default="{ row }"
                ><div class="tag-list">
                  <el-tag v-for="role in row.roleCodes" :key="role" effect="plain">{{
                    role
                  }}</el-tag>
                </div></template
              ></el-table-column
            >
            <el-table-column :label="t('common.status')" width="120"
              ><template #default="{ row }"
                ><StatusBadge :status="row.enabled ? 'enabled' : 'offline'" /></template
            ></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="t('iam.roles')" name="roles">
          <el-table :data="roles" row-key="id">
            <el-table-column :label="t('iam.role')" min-width="220"
              ><template #default="{ row }"
                ><strong class="table-primary">{{ row.roleName }}</strong
                ><small class="table-secondary">{{ row.roleCode }}</small></template
              ></el-table-column
            >
            <el-table-column :label="t('iam.permissionCount')" width="140"
              ><template #default="{ row }">{{ row.permissions.length }}</template></el-table-column
            >
            <el-table-column label="Permissions" min-width="380"
              ><template #default="{ row }"
                ><div class="tag-list">
                  <el-tag
                    v-for="permission in row.permissions"
                    :key="permission"
                    size="small"
                    type="info"
                    effect="plain"
                    >{{ permission }}</el-tag
                  >
                </div></template
              ></el-table-column
            >
            <el-table-column :label="t('common.status')" width="120"
              ><template #default="{ row }"
                ><StatusBadge :status="row.enabled ? 'enabled' : 'offline'" /></template
            ></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>
