<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Number,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const ratingLabel = computed(() => {
  const labels = {
    0: 'Nulo',
    1: 'Muy bajo',
    2: 'Bajo',
    3: 'Medio',
    4: 'Alto',
    5: 'Muy alto',
  }
  return labels[props.modelValue] || ''
})

const tooltip = computed(() => {
  const tips = {
    shooting: 'Precisión y potencia en los disparos',
    speed: 'Velocidad de desplazamiento',
    dribbling: 'Control del balón en carrera',
    defense: 'Habilidad defensiva y marca',
    strength: 'Potencia física y força',
    stamina: 'Resistencia durante el partido',
    aerial: 'Dominio en el juego aéreo',
  }
  return tips[props.label.toLowerCase()] || ''
})
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <div class="flex items-center justify-between">
      <label class="text-sm font-semibold text-slate-900">{{ label }}</label>
      <span class="text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full">
        {{ modelValue }}/5
      </span>
    </div>
    <input
      type="range"
      min="0"
      max="5"
      :value="modelValue"
      @input="emit('update:modelValue', parseInt($event.target.value, 10))"
      class="h-2 w-full bg-slate-200 rounded-full accent-blue-500 cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500"
      :aria-label="`${label}: ${modelValue} de 5. ${tooltip}`"
    />
    <p class="text-xs text-slate-600">{{ ratingLabel }}</p>
  </div>
</template>
