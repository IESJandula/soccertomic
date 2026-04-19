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
  min: {
    type: Number,
    default: 0,
  },
  max: {
    type: Number,
    default: 5,
  },
  step: {
    type: Number,
    default: 1,
  },
  lowLabel: {
    type: String,
    default: '',
  },
  highLabel: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const handleInput = (event) => {
  const parsed = Number.parseInt(event.target.value, 10)
  emit('update:modelValue', Number.isFinite(parsed) ? parsed : props.min)
}

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
      <label class="text-sm font-semibold text-slate-100">{{ label }}</label>
      <span class="text-xs font-bold text-lime-800 bg-lime-200 px-2 py-0.5 rounded-full">
        {{ modelValue }}/{{ max }}
      </span>
    </div>
    <input
      type="range"
      :min="min"
      :max="max"
      :step="step"
      :value="modelValue"
      @input="handleInput"
      class="attribute-slider w-full cursor-pointer"
      :aria-label="`${label}: ${modelValue} de ${max}. ${tooltip}`"
    />
    <div v-if="lowLabel || highLabel" class="flex items-center justify-between text-xs text-slate-500">
      <span>{{ lowLabel }}</span>
      <span>{{ highLabel }}</span>
    </div>
    <p class="text-xs text-slate-600">{{ ratingLabel }}</p>
  </div>
</template>

<style scoped>
.attribute-slider {
  -webkit-appearance: none;
  appearance: none;
  height: 10px;
  border-radius: 9999px;
  background: transparent;
}

.attribute-slider::-webkit-slider-runnable-track {
  height: 10px;
  border-radius: 9999px;
  background: linear-gradient(90deg, rgba(132, 204, 22, 0.95), rgba(163, 230, 53, 0.95));
}

.attribute-slider::-moz-range-track {
  height: 10px;
  border-radius: 9999px;
  border: none;
  background: linear-gradient(90deg, rgba(132, 204, 22, 0.95), rgba(163, 230, 53, 0.95));
}

.attribute-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  margin-top: -5px;
  border-radius: 9999px;
  border: 2px solid #0f172a;
  background: #d9f99d;
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.1);
}

.attribute-slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 9999px;
  border: 2px solid #0f172a;
  background: #d9f99d;
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.1);
}

.attribute-slider:focus-visible {
  outline: 2px solid rgba(163, 230, 53, 0.9);
  outline-offset: 3px;
}
</style>
