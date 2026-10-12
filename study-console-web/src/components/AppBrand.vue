<script setup lang="ts">
import { ShieldCheck } from '@lucide/vue'

defineProps<{ compact?: boolean; inverse?: boolean }>()
</script>

<template>
  <div class="app-brand" :class="{ compact, inverse }">
    <span class="app-brand__mark" aria-hidden="true"><ShieldCheck :size="22" /></span>
    <span v-if="!compact" class="app-brand__text">
      <strong>ContentGuard</strong>
      <small>Risk Console</small>
    </span>
  </div>
</template>

<style scoped lang="scss">
.app-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #17211f;

  &__mark {
    display: inline-flex;
    width: 36px;
    height: 36px;
    align-items: center;
    justify-content: center;
    flex: 0 0 36px;
    border-radius: 7px;
    color: #ffffff;
    background: #0f766e;
    box-shadow: inset 0 0 0 1px rgb(255 255 255 / 16%);
  }

  &__text {
    display: grid;
    line-height: 1.1;
  }

  strong {
    font-size: 15px;
    font-weight: 700;
  }

  small {
    margin-top: 4px;
    color: #66736f;
    font-size: 10px;
    text-transform: uppercase;
  }

  &.inverse {
    color: #f8faf9;

    small {
      color: #97aaa5;
    }
  }

  &.compact {
    justify-content: center;
  }
}
</style>
