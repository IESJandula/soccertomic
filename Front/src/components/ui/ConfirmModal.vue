<script setup>
import { computed } from 'vue'
import BaseButton from './BaseButton.vue'
import { useUiStore } from '../../stores/ui'

const uiStore = useUiStore()

const panelClass = computed(() => {
  return 'w-full max-w-md rounded-2xl bg-[color:var(--color-primary)] p-5 shadow-[0_16px_40px_rgba(12,0,5,0.42)] border border-[color:rgba(2,4,11,0.35)]'
})

const titleClass = computed(() => 'confirm-modal-title text-lg font-semibold')
const messageClass = computed(() => 'confirm-modal-message text-sm mt-2 leading-6')

const cancelButtonStyle = computed(() => {
  return {
    color: '#111827',
    backgroundColor: 'rgba(233, 251, 207, 0.92)',
    borderColor: 'rgba(2, 4, 11, 0.35)',
    boxShadow: '0 2px 8px rgba(2, 4, 11, 0.12)',
  }
})

const confirmButtonStyle = computed(() => {
  return {
    color: '#111827',
    backgroundColor: 'var(--color-primary)',
    borderColor: 'rgba(2, 4, 11, 0.45)',
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
        <BaseButton variant="primary" :style="cancelButtonStyle" block @click="uiStore.resolveConfirm(false)">
          {{ uiStore.confirmState.cancelLabel }}
        </BaseButton>
        <BaseButton variant="primary" :style="confirmButtonStyle" block @click="uiStore.resolveConfirm(true)">
          {{ uiStore.confirmState.confirmLabel }}
        </BaseButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.confirm-modal-title {
  color: #02040b !important;
}

.confirm-modal-message {
  color: #111827 !important;
}
</style>
