<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ShieldX } from '@lucide/vue'

const { t } = useI18n()
</script>

<template>
  <section class="result-page">
    <ShieldX :size="46" />
    <h1>403</h1>
    <p>{{ t('error.forbidden') }}</p>
    <el-button type="primary" @click="$router.push('/dashboard')">{{
      t('error.backHome')
    }}</el-button>
  </section>
</template>
