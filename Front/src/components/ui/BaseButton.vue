<script setup>
const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
  },
  size: {
    type: String,
    default: 'md',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  block: {
    type: Boolean,
    default: false,
  },
})

const variants = {
  primary: 'bg-blue-700 text-white active:bg-blue-800',
  secondary: 'bg-white border border-slate-300 text-slate-800 active:bg-slate-50',
  danger: 'bg-red-700 text-white active:bg-red-800',
  ghost: 'bg-transparent text-slate-700 active:bg-slate-100',
}

const sizes = {
  sm: 'h-11 px-4 text-sm',
  md: 'h-12 px-4 text-sm',
  lg: 'h-12 px-5 text-base',
}
</script>

<template>
  <button
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : undefined"
    :class="[
      'inline-flex items-center justify-center rounded-xl font-medium transition min-w-11',
      'disabled:opacity-55 disabled:cursor-not-allowed',
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2',
      variants[variant] || variants.primary,
      sizes[size] || sizes.md,
      block ? 'w-full' : ''
    ]"
  >
    <span v-if="loading" aria-live="polite">Cargando...</span>
    <span v-else><slot /></span>
  </button>
</template>
