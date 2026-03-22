<script setup>
import { useUiStore } from '../../stores/ui'

const uiStore = useUiStore()

const classesByType = {
  success: 'bg-emerald-50 border-emerald-200 text-emerald-700',
  error: 'bg-red-50 border-red-200 text-red-700',
  warning: 'bg-amber-50 border-amber-200 text-amber-700',
  info: 'bg-blue-50 border-blue-200 text-blue-700',
}
</script>

<template>
  <div class="fixed z-[100] top-3 left-1/2 -translate-x-1/2 w-[calc(100%-1rem)] max-w-md space-y-2 pointer-events-none">
    <div
      v-for="toast in uiStore.toasts"
      :key="toast.id"
      class="pointer-events-auto border rounded-xl px-4 py-3 shadow-[0_1px_2px_rgba(15,23,42,0.08)] flex items-start justify-between gap-3"
      :class="classesByType[toast.type] || classesByType.info"
    >
      <p class="text-sm font-medium leading-5">{{ toast.message }}</p>
      <button class="text-xs font-semibold rounded-lg px-2 py-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500" @click="uiStore.removeToast(toast.id)">Cerrar</button>
    </div>
  </div>
</template>
