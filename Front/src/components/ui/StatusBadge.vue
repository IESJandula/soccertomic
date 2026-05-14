<script setup>
const props = defineProps({
  status: {
    type: String,
    default: '',
  },
  paid: {
    type: Boolean,
    default: false,
  },
  showIcon: {
    type: Boolean,
    default: true,
  },
})

const map = {
  CREADO: 'theme-chip-muted',
  CONFIRMADO: 'bg-blue-50 text-blue-700 border-blue-200',
  EN_JUEGO: 'theme-secondary-surface',
  EN_CURSO: 'theme-secondary-surface',
  FINALIZADO: 'theme-primary-surface',
  ABIERTO: 'bg-blue-50 text-blue-700 border-blue-200',
  COMPLETO: 'bg-blue-50 text-blue-700 border-blue-200',
  BORRADOR: 'theme-chip-muted',
  CANCELADO: 'theme-danger-surface',
  PUBLICO: 'theme-chip',
  PRIVADO: 'theme-chip-muted',
  PENDIENTE: 'theme-chip-muted',
  ACEPTADA: 'theme-primary-surface',
  RECHAZADA: 'theme-danger-surface',
  CANCELADA: 'theme-danger-surface',
  RESERVADA: 'bg-blue-50 text-blue-700 border-blue-200',
}

const labels = {
  BORRADOR: 'ORGANIZANDO',
  CREADO: 'ORGANIZANDO',
  ABIERTO: 'RESERVADO',
  COMPLETO: 'RESERVADO',
  CONFIRMADO: 'RESERVADO',
  EN_JUEGO: 'EN_CURSO',
  EN_CURSO: 'EN_CURSO',
  FINALIZADO: 'FINALIZADO',
  CANCELADO: 'CANCELADO',
}

const showStatusIcon = () => props.showIcon && (
  props.paid ||
  props.status === 'CONFIRMADO' ||
  props.status === 'RESERVADA' ||
  props.status === 'RESERVADO' ||
  props.status === 'EN_JUEGO' ||
  props.status === 'EN_CURSO' ||
  props.status === 'FINALIZADO'
)
const iconPath = () => {
  if (props.status === 'RESERVADA' || props.status === 'CONFIRMADO') {
    return 'M7 4.5h10a1.5 1.5 0 0 1 1.5 1.5V20l-6.5-3.8L5.5 20V6A1.5 1.5 0 0 1 7 4.5z'
  }
  if (props.status === 'EN_JUEGO' || props.status === 'EN_CURSO') {
    return 'M12 4a8 8 0 1 0 0 16a8 8 0 0 0 0-16M12 8v4l2.5 2'
  }
  if (props.status === 'FINALIZADO') {
    return 'M6 4v16M8 6h9l-2 3 2 3H8z'
  }
  return 'M20 6L9 17l-5-5'
}
const iconColor = () => {
  if (props.status === 'RESERVADA' || props.status === 'CONFIRMADO') return '#1d4ed8'
  if (props.status === 'EN_JUEGO' || props.status === 'EN_CURSO') return '#f97316'
  if (props.status === 'FINALIZADO') return '#475569'
  return '#14532d'
}
</script>

<template>
  <span :class="['inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold border', map[status] || 'theme-chip-muted']">
    <svg
      v-if="showStatusIcon()"
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
      class="shrink-0"
      width="13"
      height="13"
      fill="none"
      :style="`color: ${iconColor()};`"
    >
      <path
        :d="iconPath()"
        stroke="currentColor"
        stroke-width="3.2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    {{ labels[status] || status }}
  </span>
</template>
