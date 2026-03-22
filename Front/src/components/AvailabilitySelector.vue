<template>
  <div class="space-y-2">
    <label class="block text-sm font-semibold text-gray-700">
      Disponibilidad de horarios
    </label>
    <p class="text-xs text-gray-600 mb-1.5">Selecciona todas las que apliquen</p>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
      <button
        v-for="option in options"
        :key="option"
        type="button"
        @click="toggleOption(option)"
        :class="[
          'py-2.5 px-3 rounded-lg font-medium transition-all text-xs text-left flex items-center justify-between gap-2 border',
          isSelected(option)
            ? 'bg-blue-600 text-white shadow-lg border-blue-600'
            : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50 hover:border-gray-300'
        ]"
        :aria-pressed="isSelected(option)"
        :aria-label="`Disponibilidad: ${option}`"
      >
        <span>{{ option }}</span>
        <svg
          v-if="isSelected(option)"
          class="w-4 h-4 flex-shrink-0"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.4" d="M5 13l4 4L19 7" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  options: {
    type: Array,
    default: () => [
      'Entre semana mañana',
      'Entre semana tarde',
      'Entre semana ambos',
      'Fines de semana mañana',
      'Fines de semana tarde',
      'Toda la semana mañana',
      'Toda la semana tarde',
      'Siempre'
    ]
  }
})

const emit = defineEmits(['update:modelValue'])

const isSelected = (option) => {
  return props.modelValue?.includes(option) || false
}

const toggleOption = (option) => {
  const newValue = isSelected(option)
    ? props.modelValue.filter(item => item !== option)
    : [...(props.modelValue || []), option]
  
  emit('update:modelValue', newValue)
}
</script>
