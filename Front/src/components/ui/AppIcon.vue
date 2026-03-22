<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: {
    type: String,
    default: 'circle',
  },
  size: {
    type: [Number, String],
    default: 18,
  },
  strokeWidth: {
    type: [Number, String],
    default: 1.8,
  },
})

const iconSize = computed(() => Number(props.size) || 18)

const paths = {
  soccer: 'M12 3l5.7 3.3v6.4L12 18l-5.7-5.3V6.3L12 3zm-2.9 4.2h5.8l1.8 2.7L12 13l-4.7-3.1 1.8-2.7z',
  plus: 'M12 5v14M5 12h14',
  users: 'M16 21v-1.2a4.3 4.3 0 0 0-4.3-4.3H8.3A4.3 4.3 0 0 0 4 19.8V21M15.5 7.8a3 3 0 1 1 0 6M8.5 13.8a3.8 3.8 0 1 1 0-7.6 3.8 3.8 0 0 1 0 7.6',
  bell: 'M15 17h5l-1.5-1.8a2 2 0 0 1-.5-1.3V11a6 6 0 1 0-12 0v2.9c0 .5-.2 1-.5 1.3L4 17h5m3 0v1a2 2 0 1 1-4 0v-1h4z',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm7 9v-1.3A5.7 5.7 0 0 0 13.3 14h-2.6A5.7 5.7 0 0 0 5 19.7V21',
  calendar: 'M8 3v3M16 3v3M4 9h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z',
  pin: 'M12 21s6-5.3 6-10a6 6 0 1 0-12 0c0 4.7 6 10 6 10zm0-8a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4z',
  crown: 'M4 18h16l-1.5-9-4 3-2.5-4-2.5 4-4-3L4 18z',
  settings: 'M12 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zm8 2.5l-1.9-.7a6.5 6.5 0 0 0-.6-1.5l.9-1.8-2-2-.9 1A6.5 6.5 0 0 0 14 5.9L13.3 4h-2.6L10 5.9a6.5 6.5 0 0 0-1.5.6l-1.8-1-2 2 .9 1.8a6.5 6.5 0 0 0-.6 1.5L3 12l.7 2a6.5 6.5 0 0 0 .6 1.5l-1 1.8 2 2 1.8-.9a6.5 6.5 0 0 0 1.5.6L10.7 20h2.6l.7-1.9a6.5 6.5 0 0 0 1.5-.6l1.8.9 2-2-.9-1.8c.3-.5.5-1 .6-1.5L20 12z',
  check: 'M20 6L9 17l-5-5',
  clock: 'M12 7v5l3 2m6-2a9 9 0 1 1-18 0 9 9 0 0 1 18 0z',
  warning: 'M12 3L2.5 19h19L12 3zm0 6v5m0 3h.01',
  send: 'M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z',
  search: 'M11 5a6 6 0 1 0 0 12a6 6 0 0 0 0-12zm0 0l8 8',
  back: 'M15 18l-6-6 6-6',
  circle: 'M12 12m-5.5 0a5.5 5.5 0 1 0 11 0a5.5 5.5 0 1 0 -11 0',
}

const iconPath = computed(() => paths[props.name] || paths.circle)
</script>

<template>
  <svg
    :width="iconSize"
    :height="iconSize"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
    focusable="false"
  >
    <path
      :d="iconPath"
      fill="none"
      stroke="currentColor"
      :stroke-width="strokeWidth"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
  </svg>
</template>
