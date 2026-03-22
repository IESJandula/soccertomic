<script setup>
const props = defineProps({
  status: {
    type: String,
    default: '',
  },
})

const map = {
  CREADO: 'bg-blue-50 text-blue-800 border border-blue-200',
  CONFIRMADO: 'bg-indigo-50 text-indigo-800 border border-indigo-200',
  EN_JUEGO: 'bg-amber-50 text-amber-800 border border-amber-200',
  EN_CURSO: 'bg-amber-50 text-amber-800 border border-amber-200',
  FINALIZADO: 'bg-emerald-50 text-emerald-800 border border-emerald-200',
  ABIERTO: 'bg-blue-50 text-blue-800 border border-blue-200',
  COMPLETO: 'bg-rose-50 text-rose-800 border border-rose-200',
  BORRADOR: 'bg-sky-50 text-sky-800 border border-sky-200',
  CANCELADO: 'bg-slate-100 text-slate-700 border border-slate-300',
  PUBLICO: 'bg-cyan-50 text-cyan-800 border border-cyan-200',
  PRIVADO: 'bg-violet-50 text-violet-800 border border-violet-200',
  PENDIENTE: 'bg-amber-50 text-amber-800 border border-amber-200',
  ACEPTADA: 'bg-emerald-50 text-emerald-800 border border-emerald-200',
  RECHAZADA: 'bg-rose-50 text-rose-800 border border-rose-200',
  CANCELADA: 'bg-slate-100 text-slate-700 border border-slate-300',
  RESERVADA: 'bg-blue-50 text-blue-800 border border-blue-200',
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
</script>

<template>
  <span :class="['inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold', map[status] || 'bg-slate-100 text-slate-700 border border-slate-200']">
    {{ labels[status] || status }}
  </span>
</template>
