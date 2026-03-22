<script setup>
const props = defineProps({
  tier: {
    type: String,
    default: 'BRONCE',
  },
  size: {
    type: [Number, String],
    default: 18,
  },
})
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
    focusable="false"
  >
    <circle
      cx="12"
      cy="12"
      r="9"
      :class="{
        'fill-amber-500': tier === 'BRONCE',
        'fill-slate-400': tier === 'PLATA',
        'fill-yellow-400': tier === 'ORO',
        'fill-cyan-400': tier === 'DIAMANTE',
      }"
    />
    <path
      d="M12 7.2l1.6 3.2 3.5.5-2.5 2.4.6 3.4L12 15.1l-3.2 1.6.6-3.4-2.5-2.4 3.5-.5L12 7.2z"
      class="fill-white"
    />
  </svg>
</template>
