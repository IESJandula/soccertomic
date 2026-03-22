<script setup>
import AppIcon from '../ui/AppIcon.vue'

defineProps({
  presets: {
    type: Object,
    required: true,
  },
  selected: {
    type: String,
    default: null,
  },
})

const emit = defineEmits(['apply-preset'])

const presetOrder = ['STRIKER', 'MIDFIELDER', 'DEFENDER', 'GOALKEEPER']
</script>

<template>
  <div class="space-y-2">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
      <button
        v-for="key in presetOrder"
        :key="key"
        type="button"
        @click="emit('apply-preset', key)"
        :class="[
          'relative px-3 py-2.5 rounded-lg border-2 transition-all text-left',
          selected === key
            ? 'border-blue-500 bg-blue-50'
            : 'border-slate-200 bg-white hover:border-slate-300',
        ]"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <p class="text-sm font-bold text-slate-900">
              {{ presets[key].name }}
            </p>
            <p class="text-xs text-slate-600 mt-0.5">{{ presets[key].description }}</p>
          </div>
          <div
            v-if="selected === key"
            class="ml-2 flex-shrink-0 w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center"
          >
            <AppIcon name="check" :size="12" class="text-white" />
          </div>
        </div>
      </button>
    </div>
  </div>
</template>
