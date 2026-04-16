<script setup>
import { computed } from 'vue'
import BaseButton from './BaseButton.vue'
import { useUiStore } from '../../stores/ui'

const uiStore = useUiStore()

const confirmVariant = computed(() => {
  if (uiStore.confirmState.variant === 'danger') return 'danger'
  if (uiStore.confirmState.variant === 'info') return 'info'
  return 'primary'
})

const isPrimaryConfirm = computed(() => confirmVariant.value === 'primary')

const panelClass = computed(() => {
  if (isPrimaryConfirm.value) {
    return 'w-full max-w-md rounded-2xl bg-[color:var(--color-primary)] p-5 shadow-[0_16px_40px_rgba(12,0,5,0.42)] border border-[color:rgba(2,4,11,0.35)]'
  }
  return 'w-full max-w-md rounded-2xl bg-[color:var(--color-surface)] p-5 shadow-[0_16px_40px_rgba(12,0,5,0.42)] border border-[color:var(--color-border)]'
})

const titleClass = computed(() => (isPrimaryConfirm.value ? 'text-lg font-semibold text-slate-900' : 'text-lg font-semibold text-slate-100'))
const messageClass = computed(() => (isPrimaryConfirm.value ? 'text-sm text-slate-800 mt-2 leading-6' : 'text-sm text-slate-200 mt-2 leading-6'))

const confirmButtonStyle = computed(() => {
  if (confirmVariant.value !== 'primary') return null
  return {
    color: '#111827',
  }
})
</script>

<template>
  <div
    v-if="uiStore.confirmState.open"
    class="fixed inset-0 z-[110] flex items-end sm:items-center justify-center backdrop-blur-[2px] p-4"
    :class="uiStore.confirmState.variant === 'info' ? 'bg-[color:rgba(30,64,175,0.62)]' : 'bg-[color:rgba(12,0,5,0.72)]'"
  >
    <div :class="panelClass">
      <h3 :class="titleClass">{{ uiStore.confirmState.title }}</h3>
      <p :class="messageClass">{{ uiStore.confirmState.message }}</p>

      <div class="mt-5 grid grid-cols-2 gap-3">
        <BaseButton variant="secondary" block @click="uiStore.resolveConfirm(false)">
          {{ uiStore.confirmState.cancelLabel }}
        </BaseButton>
        <BaseButton :variant="confirmVariant" :style="confirmButtonStyle" block @click="uiStore.resolveConfirm(true)">
          {{ uiStore.confirmState.confirmLabel }}
        </BaseButton>
      </div>
    </div>
  </div>
</template>
