<script setup>
import TierIcon from '../ui/TierIcon.vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: 'BRONCE',
  },
  options: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const selectTier = (value) => {
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="space-y-2">
    <div class="text-sm font-medium text-slate-700">
      Selecciona tu nivel de habilidad
    </div>
    <div class="grid grid-cols-2 gap-2">
      <button
        v-for="tier in options"
        :key="tier.value"
        type="button"
        @click="selectTier(tier.value)"
        :class="[
          'p-3 rounded-lg border-2 transition-all text-left',
          modelValue === tier.value
            ? 'border-blue-500 bg-blue-50 shadow-md'
            : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50',
        ]"
      >
        <div class="flex items-start gap-2">
          <TierIcon :tier="tier.value" :size="26" class="mt-0.5" />
          <div class="flex-1">
            <p :class="['font-semibold text-sm', modelValue === tier.value ? 'text-blue-900' : 'text-slate-900']">
              {{ tier.label }}
            </p>
            <p :class="['text-xs mt-0.5', modelValue === tier.value ? 'text-blue-700' : 'text-slate-600']">
              {{ tier.description }}
            </p>
          </div>
          <div
            v-if="modelValue === tier.value"
            class="w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0"
          >
            <svg class="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
            </svg>
          </div>
        </div>
      </button>
    </div>
  </div>
</template>
