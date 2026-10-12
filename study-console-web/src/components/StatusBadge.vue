<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { AlertTriangle, CheckCircle2, CircleX, Clock3 } from '@lucide/vue'

const props = defineProps<{ status: string }>()
const { t } = useI18n()

const tone = computed(() => {
  if (['healthy', 'PASS', 'success', 'enabled'].includes(props.status)) return 'success'
  if (['degraded', 'REVIEW', 'warning'].includes(props.status)) return 'warning'
  if (['offline', 'REJECT', 'ERROR', 'error'].includes(props.status)) return 'danger'
  return 'neutral'
})

const icon = computed(() => {
  if (tone.value === 'success') return CheckCircle2
  if (tone.value === 'warning') return AlertTriangle
  if (tone.value === 'danger') return CircleX
  return Clock3
})

const label = computed(() => {
  const key = `status.${props.status}`
  const translated = t(key)
  return translated === key ? props.status : translated
})
</script>

<template>
  <span class="status-badge" :class="`status-badge--${tone}`">
    <component :is="icon" :size="13" />
    {{ label }}
  </span>
</template>
