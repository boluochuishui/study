<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, LockKeyhole, UserRound } from '@lucide/vue'
import type { FormInstance, FormRules } from 'element-plus'

import AppBrand from '@/components/AppBrand.vue'
import { isMockMode } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest } from '@/types/admin'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive<LoginRequest>({
  tenantCode: 'default',
  username: 'admin',
  password: isMockMode ? 'admin123' : '',
})
const rules: FormRules<LoginRequest> = {
  tenantCode: [{ required: true, message: t('auth.tenantRequired'), trigger: 'blur' }],
  username: [{ required: true, message: t('auth.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('auth.passwordRequired'), trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await auth.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch {
    errorMessage.value = t('auth.failed')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-page__topline"></div>
    <section class="login-panel">
      <AppBrand />
      <div class="login-panel__heading">
        <h1>{{ t('auth.title') }}</h1>
        <span v-if="isMockMode" class="environment-badge">{{ t('common.mock') }}</span>
      </div>
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submit"
      >
        <el-form-item :label="t('auth.tenantCode')" prop="tenantCode">
          <el-input v-model="form.tenantCode" size="large" autocomplete="organization" />
        </el-form-item>
        <el-form-item :label="t('auth.username')" prop="username">
          <el-input v-model="form.username" size="large" autocomplete="username">
            <template #prefix><UserRound :size="17" /></template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('auth.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            size="large"
            show-password
            autocomplete="current-password"
          >
            <template #prefix><LockKeyhole :size="17" /></template>
          </el-input>
        </el-form-item>
        <el-button
          native-type="submit"
          type="primary"
          size="large"
          :loading="submitting"
          class="login-submit"
        >
          {{ t('auth.submit') }}<ArrowRight :size="17" />
        </el-button>
      </el-form>
      <footer>ContentGuard Console · {{ new Date().getFullYear() }}</footer>
    </section>
  </main>
</template>
