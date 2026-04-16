<script setup>
const props = defineProps({
  type: {
    type: String,
    default: 'button',
  },
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
  primary: 'theme-primary-surface border-transparent shadow-[0_6px_18px_rgba(151,240,125,0.22)] hover:brightness-105 hover:shadow-[0_8px_22px_rgba(151,240,125,0.26)] active:brightness-95',
  secondary: 'theme-outline-surface text-slate-900 hover:bg-[color:rgba(148,163,184,0.14)] hover:border-[color:var(--color-border-strong)] active:bg-[color:rgba(148,163,184,0.22)]',
  info: 'theme-info-surface border-transparent shadow-[0_6px_16px_rgba(37,99,235,0.26)] hover:brightness-105 hover:shadow-[0_8px_20px_rgba(37,99,235,0.32)] active:brightness-95',
  danger: 'theme-danger-surface border-transparent shadow-[0_6px_16px_rgba(239,68,68,0.26)] hover:brightness-105 hover:shadow-[0_8px_20px_rgba(239,68,68,0.32)] active:brightness-95 disabled:opacity-100 disabled:saturate-100 disabled:brightness-90 disabled:text-white',
  ghost: 'bg-transparent border-transparent text-slate-200 hover:bg-[color:rgba(148,163,184,0.16)] active:bg-[color:rgba(148,163,184,0.24)]',
}

const sizes = {
  sm: 'h-11 px-4 text-sm',
  md: 'h-12 px-4 text-sm',
  lg: 'h-12 px-5 text-base',
}
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : undefined"
    :class="[
      'inline-flex items-center justify-center rounded-xl font-medium min-w-11 border select-none touch-manipulation cursor-pointer',
      'transition-[transform,filter,box-shadow,background-color,border-color,color] duration-150 ease-out',
      'hover:-translate-y-px active:translate-y-0 active:scale-[0.985]',
      'motion-reduce:transition-none motion-reduce:hover:translate-y-0 motion-reduce:active:scale-100',
      'disabled:opacity-45 disabled:cursor-not-allowed disabled:saturate-50 disabled:shadow-none disabled:transform-none',
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--color-focus)] focus-visible:ring-offset-2 focus-visible:ring-offset-[color:var(--color-bg)]',
      variants[variant] || variants.primary,
      sizes[size] || sizes.md,
      block ? 'w-full' : ''
    ]"
  >
    <span v-if="loading" aria-live="polite">Cargando...</span>
    <span v-else><slot /></span>
  </button>
</template>
