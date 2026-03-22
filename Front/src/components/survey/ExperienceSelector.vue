<script setup>
defineProps({
  modelValue: {
    type: String,
    required: true,
  },
  options: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <div class="space-y-1.5">
    <label class="text-sm font-semibold text-slate-900">Rango de edad</label>
    <div class="grid grid-cols-2 md:grid-cols-3 gap-2">
      <button
        v-for="opt in options"
        :key="opt.value"
        type="button"
        @click="emit('update:modelValue', opt.value)"
        :class="[
          'px-3 py-2 rounded-lg font-semibold text-sm transition-all border-2 hover:bg-slate-50',
          modelValue === opt.value
            ? 'border-blue-500 bg-blue-50 text-blue-900'
            : 'border-slate-200 bg-white text-slate-700',
        ]"
      >
        <p>{{ opt.label }}</p>
      </button>
    </div>
  </div>
</template>
