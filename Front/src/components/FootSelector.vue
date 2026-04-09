<template>
  <div class="space-y-2">
    <label class="block text-sm font-semibold text-slate-200">
      Pierna buena
    </label>
    <div class="grid grid-cols-3 gap-2">
      <button
        v-for="option in footOptions"
        :key="option"
        type="button"
        @click="emitValue(option)"
        :class="[
          'py-2 px-3 rounded-lg font-medium transition-all text-sm',
          modelValue === option
            ? 'theme-primary-surface shadow-lg'
            : 'theme-muted-surface text-slate-200 hover:brightness-110'
        ]"
      >
        {{ option }}
      </button>
    </div>
  </div>
</template>

<script setup>

defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const footOptions = ['Izquierda', 'Derecha', 'Ambidiestra']

const emitValue = (value) => {
  emit('update:modelValue', value)
}
</script>
