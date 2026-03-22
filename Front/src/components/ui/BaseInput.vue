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
    <span v-if="label" class="block text-sm font-semibold text-slate-700 mb-1.5">{{ label }}</span>
    <input
      :id="inputId"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : 'false'"
      :aria-describedby="error ? errorId : undefined"
      :class="[
        'w-full h-12 px-4 rounded-xl border bg-white text-slate-900 placeholder:text-slate-400',
        'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 focus:border-blue-500',
        error ? 'border-red-600' : 'border-slate-300',
        disabled ? 'bg-slate-100 text-slate-500 border-slate-200' : ''
      ]"
      @input="emit('update:modelValue', $event.target.value)"
    />
    <span v-if="error" :id="errorId" class="text-xs text-red-700 mt-1.5 block">{{ error }}</span>
  </label>
</template>
