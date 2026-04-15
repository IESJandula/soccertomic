<script setup>
import { useUiStore } from '../../stores/ui'

const uiStore = useUiStore()

const classesByType = {
  'login-success': 'bg-[color:var(--color-primary)] border-[color:rgba(2,4,11,0.45)]',
  success: 'bg-[color:var(--color-primary)] border-[color:rgba(2,4,11,0.35)]',
  error: 'theme-danger-surface border-transparent',
  warning: 'theme-chip-muted border-transparent',
  info: 'theme-chip border-transparent',
}

const textClassesByType = {
  'login-success': 'text-slate-900',
  success: 'text-slate-900',
  error: 'text-slate-100',
  warning: 'text-slate-100',
  info: 'text-slate-100',
}

const closeClassesByType = {
  'login-success': 'text-slate-900 hover:text-black',
  success: 'text-slate-900/90 hover:text-slate-900',
  error: 'text-slate-100/90 hover:text-slate-100',
  warning: 'text-slate-100/90 hover:text-slate-100',
  info: 'text-slate-100/90 hover:text-slate-100',
}

const textStyleByType = {
  'login-success': { color: '#02040b' },
  success: { color: '#02040b' },
}

const closeStyleByType = {
  'login-success': { color: '#02040b' },
  success: { color: '#02040b' },
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
      <p
        class="text-sm font-medium leading-5"
        :class="textClassesByType[toast.type] || textClassesByType.info"
        :style="textStyleByType[toast.type] || undefined"
      >
        {{ toast.message }}
      </p>
      <button
        class="text-xs font-semibold rounded-lg px-2 py-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-200"
        :class="closeClassesByType[toast.type] || closeClassesByType.info"
        :style="closeStyleByType[toast.type] || undefined"
        @click="uiStore.removeToast(toast.id)"
      >
        Cerrar
      </button>
    </div>
  </div>
</template>
