<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { FileQuestion } from '@lucide/vue'

const { t } = useI18n()
</script>

<template>
  <main class="result-page result-page--full">
    <FileQuestion :size="46" />
    <h1>404</h1>
    <p>{{ t('error.notFound') }}</p>
    <el-button type="primary" @click="$router.push('/dashboard')">{{
      t('error.backHome')
    }}</el-button>
  </main>
</template>
