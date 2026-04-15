<template>
  <div v-if="partida" class="space-y-4">
    <section v-if="feedbackMessage" class="bg-[color:var(--color-primary)] border border-[color:rgba(2,4,11,0.35)] text-slate-900 rounded-lg p-3 text-sm font-semibold shadow-[0_8px_18px_rgba(151,240,125,0.26)]">
      {{ feedbackMessage }}
    </section>

    <section :class="['card-surface p-3 border', estadoActualUsuario.containerClass]">
      <div class="flex items-center justify-between gap-2">
        <p :class="['text-xs uppercase tracking-wide font-semibold', estadoActualUsuario.titleClass]">Tu estado actual</p>
        <span :class="['inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', estadoActualUsuario.badgeClass]">
          <span class="inline-flex items-center"><AppIcon :name="estadoActualUsuario.emoji" :size="12" /></span>
          <span>{{ estadoActualUsuario.label }}</span>
        </span>
      </div>
      <p :class="['text-sm mt-1', estadoActualUsuario.descriptionClass]">{{ estadoActualUsuario.description }}</p>
    </section>

    <section v-if="jugadoresSinEquipo.length > 0" class="card-surface p-3 md:p-4 space-y-2.5">
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-bold text-slate-800">
          Pendientes de asignación ({{ jugadoresSinEquipo.length }})
        </h3>
        <button
          type="button"
          class="text-xs font-semibold text-blue-700 hover:text-blue-800"
          @click="showPendingPlayers = !showPendingPlayers"
        >
          {{ showPendingPlayers ? 'Ocultar personas' : 'Ver personas' }}
        </button>
      </div>

      <div v-if="showPendingPlayers" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
        <div
          v-for="jugador in jugadoresSinEquipo"
          :key="`sin-equipo-${jugador.id}`"
          :class="[
            'flex items-center gap-2.5 p-2.5 rounded-lg hover:shadow-md transition-shadow border-2',
            isCurrentUser(jugador)
              ? 'bg-white border-black'
              : 'bg-white border-amber-200'
          ]"
        >
          <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada">
            <AppIcon name="check" :size="12" />
          </span>
          <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm" :style="teamAColor.avatarStyle">
            <AppIcon name="user" :size="14" :style="teamAColor.iconStyle" />
          </div>
          <div>
            <p class="font-semibold text-slate-800 text-sm leading-tight">
              {{ jugador.nombre }}
              <span v-if="isCurrentUser(jugador)" class="ml-1.5 text-xs font-bold">(Tú)</span>
              <span
                v-if="getRolEmoji(jugador)"
                class="ml-1.5 inline-flex items-center gap-1 rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-semibold text-amber-800"
                :title="getRolTitle(jugador)"
              >
                <span class="inline-flex items-center"><AppIcon :name="getRolEmoji(jugador)" :size="12" /></span>
                <span>{{ getRolShortLabel(jugador) }}</span>
              </span>
            </p>
            <p class="text-[11px] text-slate-600" v-if="jugador.playTendency">
              Tendencia: {{ formatTendencia(jugador.playTendency) }}
            </p>
          </div>
        </div>
      </div>

    </section>

    <section v-if="hayEquiposFormados" class="grid grid-cols-1 md:grid-cols-2 gap-3">
      <!-- Equipo A -->
      <article class="p-3 border-2 space-y-2.5 rounded-lg shadow-sm card-surface" :style="teamAColor.cardStyle">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold flex items-center gap-2 text-slate-100">
            <span class="inline-flex h-7 w-7 items-center justify-center rounded-full" :style="teamAColor.chipStyle">
              <AppIcon name="soccer" :size="12" :style="teamAColor.iconStyle" />
            </span>
            <span>Equipo {{ partida.colorEquipoA || 'A' }}</span>
          </h2>
          <span class="px-2.5 py-0.5 text-xs font-bold rounded-full" :style="teamAColor.chipStyle">
            {{ partida.equipoA?.length || 0 }} / {{ partida.jugadoresPorEquipo }}
          </span>
        </div>

        <div v-if="!partida.equipoA || partida.equipoA.length === 0" class="text-sm italic p-4 rounded-lg text-center text-slate-300">
          Sin personas asignadas
        </div>

        <div v-else class="space-y-1.5">
          <div
            v-for="jugador in partida.equipoA"
            :key="`equipo-a-${jugador.id}`"
            :class="[
              'rounded-lg p-2.5 border-2 flex items-center gap-2.5',
              String(jugador.id) === String(currentUserId) ? 'border-black' : ''
            ]"
            :style="teamAColor.playerRowStyle"
          >
            <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada">
              <AppIcon name="check" :size="12" />
            </span>
            <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm" :style="teamAColor.avatarStyle">
              <AppIcon name="user" :size="14" :style="teamAColor.iconStyle" />
            </div>
            <div>
              <p class="font-semibold text-sm leading-tight text-slate-100">
                {{ jugador.nombre }}
                <span v-if="String(jugador.id) === String(currentUserId)" class="ml-1.5 text-xs font-bold">(Tú)</span>
                <span
                  v-if="getRolEmoji(jugador)"
                  class="ml-1.5 inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-semibold"
                  :style="teamAColor.auraStyle"
                  :title="getRolTitle(jugador)"
                >
                  <span class="inline-flex items-center"><AppIcon :name="getRolEmoji(jugador)" :size="12" :style="teamAColor.iconStyle" /></span>
                  <span>{{ getRolShortLabel(jugador) }}</span>
                </span>
              </p>
              <p class="text-[11px] text-slate-300" v-if="jugador.playTendency">
                Tendencia: {{ formatTendencia(jugador.playTendency) }}
              </p>
            </div>
          </div>
        </div>
      </article>

      <!-- Equipo B -->
      <article class="p-3 border-2 space-y-2.5 rounded-lg shadow-sm card-surface" :style="teamBColor.cardStyle">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold flex items-center gap-2 text-slate-100">
            <span class="inline-flex h-7 w-7 items-center justify-center rounded-full" :style="teamBColor.chipStyle">
              <AppIcon name="soccer" :size="12" :style="teamBColor.iconStyle" />
            </span>
            <span>Equipo {{ partida.colorEquipoB || 'B' }}</span>
          </h2>
          <span class="px-2.5 py-0.5 text-xs font-bold rounded-full" :style="teamBColor.chipStyle">
            {{ partida.equipoB?.length || 0 }} / {{ partida.jugadoresPorEquipo }}
          </span>
        </div>

        <div v-if="!partida.equipoB || partida.equipoB.length === 0" class="text-sm italic p-4 rounded-lg text-center text-slate-300">
          Sin personas asignadas
        </div>

        <div v-else class="space-y-1.5">
          <div
            v-for="jugador in partida.equipoB"
            :key="`equipo-b-${jugador.id}`"
            :class="[
              'rounded-lg p-2.5 border-2 flex items-center gap-2.5',
              String(jugador.id) === String(currentUserId) ? 'border-black' : ''
            ]"
            :style="teamBColor.playerRowStyle"
          >
            <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada">
              <AppIcon name="check" :size="12" />
            </span>
            <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm" :style="teamBColor.avatarStyle">
              <AppIcon name="user" :size="14" :style="teamBColor.iconStyle" />
            </div>
            <div>
              <p class="font-semibold text-sm leading-tight text-slate-100">
                {{ jugador.nombre }}
                <span v-if="String(jugador.id) === String(currentUserId)" class="ml-1.5 text-xs font-bold">(Tú)</span>
                <span
                  v-if="getRolEmoji(jugador)"
                  class="ml-1.5 inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-semibold"
                  :style="teamBColor.auraStyle"
                  :title="getRolTitle(jugador)"
                >
                  <span class="inline-flex items-center"><AppIcon :name="getRolEmoji(jugador)" :size="12" :style="teamBColor.iconStyle" /></span>
                  <span>{{ getRolShortLabel(jugador) }}</span>
                </span>
              </p>
              <p class="text-[11px] text-slate-300" v-if="jugador.playTendency">
                Tendencia: {{ formatTendencia(jugador.playTendency) }}
              </p>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppIcon from '../ui/AppIcon.vue'

const props = defineProps({
  partida: {
    type: Object,
    required: true,
  },
  currentUserId: {
    type: [Number, String],
    default: null,
  },
  feedbackMessage: {
    type: String,
    default: '',
  },
  organizadores: {
    type: Array,
    default: () => [],
  },
  estadoPagoReserva: {
    type: Object,
    default: () => ({}),
  },
})

const teamPalette = {
  Blanco: { jersey: '#ffffff', onJersey: '#111827', border: 'rgba(255, 255, 255, 0.62)', aura: 'rgba(255, 255, 255, 0.24)' },
  Negro: { jersey: '#050505', onJersey: '#f8fafc', border: 'rgba(255, 255, 255, 0.2)', aura: 'rgba(255, 255, 255, 0.08)' },
  Rojo: { jersey: '#ef4444', onJersey: '#ffffff', border: 'rgba(239, 68, 68, 0.55)', aura: 'rgba(239, 68, 68, 0.18)' },
  Azul: { jersey: '#2563eb', onJersey: '#ffffff', border: 'rgba(37, 99, 235, 0.55)', aura: 'rgba(37, 99, 235, 0.18)' },
  Verde: { jersey: '#16a34a', onJersey: '#ffffff', border: 'rgba(22, 163, 74, 0.55)', aura: 'rgba(22, 163, 74, 0.18)' },
  Amarillo: { jersey: '#facc15', onJersey: '#111827', border: 'rgba(250, 204, 21, 0.7)', aura: 'rgba(250, 204, 21, 0.2)' },
  Naranja: { jersey: '#f97316', onJersey: '#ffffff', border: 'rgba(249, 115, 22, 0.55)', aura: 'rgba(249, 115, 22, 0.18)' },
  Morado: { jersey: '#a855f7', onJersey: '#ffffff', border: 'rgba(168, 85, 247, 0.55)', aura: 'rgba(168, 85, 247, 0.18)' },
}

const createTeamTone = (name, fallback) => {
  const tone = teamPalette[name] || teamPalette[fallback]
  return {
    cardStyle: {
      backgroundColor: tone.jersey,
      borderColor: tone.border,
      boxShadow: '0 14px 30px rgba(12, 0, 5, 0.22)',
    },
    titleText: 'text-slate-100',
    playerText: 'text-slate-100',
    emptyText: 'text-slate-300',
    chipStyle: {
      backgroundColor: tone.jersey,
      color: tone.onJersey,
      boxShadow: `0 0 0 1px ${tone.border}, 0 0 16px ${tone.aura}`,
    },
    avatarStyle: {
      backgroundColor: tone.jersey,
      color: tone.onJersey,
      boxShadow: `0 0 0 4px ${tone.aura}, 0 0 0 1px ${tone.border}`,
    },
    auraStyle: {
      backgroundColor: 'rgba(151, 240, 125, 0.14)',
      color: 'var(--color-secondary)',
      boxShadow: '0 0 0 1px var(--color-border)',
    },
    playerRowStyle: {
      backgroundColor: 'rgba(0, 0, 0, 0.14)',
      borderColor: tone.border,
    },
    iconStyle: {
      color: 'var(--color-secondary)',
    },
  }
}

const teamAColor = computed(() => {
  return createTeamTone(props.partida?.colorEquipoA, 'Blanco')
})

const teamBColor = computed(() => {
  return createTeamTone(props.partida?.colorEquipoB, 'Negro')
})

const jugadoresSinEquipo = computed(() => {
  return props.partida?.jugadoresInscritos || []
})

const showPendingPlayers = ref(false)

const hayEquiposFormados = computed(() => {
  return (props.partida?.equipoA?.length || 0) > 0 || (props.partida?.equipoB?.length || 0) > 0
})

const rolPorUsuario = computed(() => {
  const map = new Map()
  for (const organizador of props.organizadores || []) {
    const usuarioId = organizador?.usuario?.id
    if (!usuarioId) continue
    map.set(String(usuarioId), organizador.rol)
  }
  return map
})

const estadoActualUsuario = computed(() => {
  const uid = props.currentUserId
  if (!uid) {
    return {
      label: 'Sin contexto',
      description: 'No se pudo determinar tu identidad en esta vista.',
      emoji: 'circle',
      containerClass: 'border-slate-200 bg-slate-50',
      badgeClass: 'bg-slate-100 text-slate-700',
      titleClass: 'text-slate-500',
      descriptionClass: 'text-slate-700',
    }
  }

  const enEquipoA = (props.partida?.equipoA || []).some((j) => String(j?.id) === String(uid))
  const enEquipoB = (props.partida?.equipoB || []).some((j) => String(j?.id) === String(uid))
  const pendiente = (props.partida?.jugadoresInscritos || []).some((j) => String(j?.id) === String(uid))

  if (enEquipoA) {
    return {
      label: `Equipo ${props.partida?.colorEquipoA || 'A'}`,
      description: `Ya estás asignado al Equipo ${props.partida?.colorEquipoA || 'A'}.`,
      emoji: 'check',
      containerClass: 'border-emerald-200 bg-emerald-50',
      badgeClass: 'bg-emerald-100 text-emerald-800',
      titleClass: 'text-emerald-700',
      descriptionClass: 'text-emerald-900',
    }
  }

  if (enEquipoB) {
    return {
      label: `Equipo ${props.partida?.colorEquipoB || 'B'}`,
      description: `Ya estás asignado al Equipo ${props.partida?.colorEquipoB || 'B'}.`,
      emoji: 'check',
      containerClass: 'border-emerald-200 bg-emerald-50',
      badgeClass: 'bg-emerald-100 text-emerald-800',
      titleClass: 'text-emerald-700',
      descriptionClass: 'text-emerald-900',
    }
  }

  if (pendiente) {
    return {
      label: 'Pendiente de asignación',
      description: 'Estás inscrita/o y esperando a que la organización te asigne equipo.',
      emoji: 'clock',
      containerClass: 'border-amber-200 bg-amber-50',
      badgeClass: 'bg-amber-100 text-amber-800',
      titleClass: 'text-amber-700',
      descriptionClass: 'text-amber-900',
    }
  }

  return {
    label: 'No inscrita/o',
    description: 'Todavía no estás inscrita/o en este partido.',
    emoji: 'plus',
    containerClass: 'border-slate-200 bg-slate-50',
    badgeClass: 'bg-slate-100 text-slate-700',
    titleClass: 'text-slate-500',
    descriptionClass: 'text-slate-700',
  }
})

const formatTendencia = (playTendency) => {
  if (!playTendency) return ''
  const tendencias = {
    'OFENSIVA': 'Ofensiva',
    'DEFENSIVA': 'Defensiva',
    'ADAPTABLE': 'Adaptable'
  }
  return tendencias[playTendency] || playTendency
}

const isCurrentUser = (jugador) => {
  if (!props.currentUserId || !jugador?.id) return false
  return String(props.currentUserId) === String(jugador.id)
}

const getRolEmoji = (jugador) => {
  if (!jugador?.id) return ''
  const rol = rolPorUsuario.value.get(String(jugador.id))
  if (rol === 'OWNER') return 'crown'
  if (rol === 'CO_ORGANIZER') return 'settings'
  return ''
}

const getRolTitle = (jugador) => {
  if (!jugador?.id) return ''
  const rol = rolPorUsuario.value.get(String(jugador.id))
  if (rol === 'OWNER') return 'Responsable principal'
  if (rol === 'CO_ORGANIZER') return 'Coorganización'
  return ''
}

const getRolShortLabel = (jugador) => {
  if (!jugador?.id) return ''
  const rol = rolPorUsuario.value.get(String(jugador.id))
  if (rol === 'OWNER') return 'Responsable'
  if (rol === 'CO_ORGANIZER') return 'Coorganización'
  return ''
}

const usuarioPagoConfirmado = (jugadorId) => Boolean(props.estadoPagoReserva?.[jugadorId])
</script>
