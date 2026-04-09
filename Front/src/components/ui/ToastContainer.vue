<script setup>
import { useUiStore } from '../../stores/ui'

const uiStore = useUiStore()

const classesByType = {
  success: 'theme-primary-surface border-transparent',
  error: 'theme-danger-surface border-transparent',
  warning: 'theme-chip-muted border-transparent',
  info: 'theme-chip border-transparent',
}
</script>

<template>
  <div class="fixed z-[100] top-3 left-1/2 -translate-x-1/2 w-[calc(100%-1rem)] max-w-md space-y-2 pointer-events-none">
    <div
      v-for="toast in uiStore.toasts"
      :key="toast.id"
      class="pointer-events-auto border rounded-xl px-4 py-3 shadow-[0_12px_24px_rgba(12,0,5,0.35)] flex items-start justify-between gap-3"
      :class="classesByType[toast.type] || classesByType.info"
    >
      <p class="text-sm font-medium leading-5 text-slate-100">{{ toast.message }}</p>
      <button class="text-xs font-semibold rounded-lg px-2 py-1 text-slate-100/90 hover:text-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-200" @click="uiStore.removeToast(toast.id)">Cerrar</button>
    </div>
  </div>
</template>
