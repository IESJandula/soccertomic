<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: '',
  },
  type: {
    type: String,
    default: 'text',
  },
  placeholder: {
    type: String,
    default: '',
  },
  error: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  label: {
    type: String,
    default: '',
  },
  id: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const generatedId = `input-${Math.random().toString(36).slice(2, 10)}`
const inputId = computed(() => props.id || generatedId)
const errorId = computed(() => `${inputId.value}-error`)
</script>

<template>
  <label class="block" :for="inputId">
    <span v-if="label" class="block text-sm font-semibold text-slate-200 mb-1.5">{{ label }}</span>
    <input
      :id="inputId"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : 'false'"
      :aria-describedby="error ? errorId : undefined"
      :class="[
        'w-full h-12 px-4 rounded-xl border theme-input focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)] focus:ring-offset-1 focus:ring-offset-[color:var(--color-bg)]',
        error ? 'border-[color:var(--color-error)]' : 'border-[color:var(--color-border-strong)]',
        disabled ? 'opacity-60 cursor-not-allowed' : ''
      ]"
      @input="emit('update:modelValue', $event.target.value)"
    />
    <span v-if="error" :id="errorId" class="text-xs text-[color:var(--color-error)] mt-1.5 block">{{ error }}</span>
  </label>
</template>

<style scoped>
input[type='date']::-webkit-calendar-picker-indicator,
input[type='time']::-webkit-calendar-picker-indicator {
  filter: invert(1) brightness(1.15);
  opacity: 1;
}

input[type='date']:disabled::-webkit-calendar-picker-indicator,
input[type='time']:disabled::-webkit-calendar-picker-indicator {
  opacity: 0.7;
}
</style>
