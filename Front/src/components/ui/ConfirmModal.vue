<script setup>
import BaseButton from './BaseButton.vue'
import { useUiStore } from '../../stores/ui'

const uiStore = useUiStore()
</script>

<template>
  <div v-if="uiStore.confirmState.open" class="fixed inset-0 z-[110] flex items-end sm:items-center justify-center bg-slate-900/35 p-4">
    <div class="w-full max-w-md rounded-2xl bg-white p-5 shadow-[0_8px_24px_rgba(15,23,42,0.15)] border border-slate-200">
      <h3 class="text-lg font-semibold text-slate-800">{{ uiStore.confirmState.title }}</h3>
      <p class="text-sm text-slate-600 mt-2 leading-6">{{ uiStore.confirmState.message }}</p>

      <div class="mt-5 grid grid-cols-2 gap-3">
        <BaseButton variant="secondary" block @click="uiStore.resolveConfirm(false)">
          {{ uiStore.confirmState.cancelLabel }}
        </BaseButton>
        <BaseButton :variant="uiStore.confirmState.variant === 'danger' ? 'danger' : 'primary'" block @click="uiStore.resolveConfirm(true)">
          {{ uiStore.confirmState.confirmLabel }}
        </BaseButton>
      </div>
    </div>
  </div>
</template>
