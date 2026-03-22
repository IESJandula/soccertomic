<template>
  <div v-if="partida" class="space-y-4">
    <section v-if="feedbackMessage" class="bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-lg p-3 text-sm font-medium">
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
          <div class="w-8 h-8 rounded-full bg-amber-100 flex items-center justify-center text-sm">
            <AppIcon name="user" :size="14" />
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
            <p class="text-[11px] text-slate-600" v-if="jugador.skillTier || jugador.playTendency">
              {{ formatNivel(jugador.skillTier) }} • {{ formatTendencia(jugador.playTendency) }}
            </p>
          </div>
        </div>
      </div>

    </section>

    <section v-if="hayEquiposFormados" class="grid grid-cols-1 md:grid-cols-2 gap-3">
      <!-- Equipo A -->
      <article :class="['p-3 border-2 space-y-2.5 rounded-lg shadow-sm', teamAColor.cardBg, teamAColor.cardBorder]">
        <div class="flex items-center justify-between">
          <h2 :class="['text-lg font-bold flex items-center gap-2', teamAColor.titleText]">
            <span>{{ teamAColor.emoji }}</span>
            <span>Equipo {{ partida.colorEquipoA || 'A' }}</span>
          </h2>
          <span :class="['px-2.5 py-0.5 text-xs font-bold rounded-full', teamAColor.badgeBg, teamAColor.badgeText]">
            {{ partida.equipoA?.length || 0 }} / {{ partida.jugadoresPorEquipo }}
          </span>
        </div>

        <div v-if="!partida.equipoA || partida.equipoA.length === 0" :class="['text-sm italic p-4 rounded-lg text-center', teamAColor.emptyText]">
          Sin personas asignadas
        </div>

        <div v-else class="space-y-1.5">
          <div
            v-for="jugador in partida.equipoA"
            :key="`equipo-a-${jugador.id}`"
            :class="[
              'rounded-lg p-2.5 border-2 flex items-center gap-2.5',
              String(jugador.id) === String(currentUserId) ? ['border-black', teamAColor.playerCardBg] : [teamAColor.playerCardBg, teamAColor.playerBorder]
            ]"
          >
            <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada">
              <AppIcon name="check" :size="12" />
            </span>
            <div :class="['w-8 h-8 rounded-full flex items-center justify-center text-sm', teamAColor.avatarBg]">
              <AppIcon name="user" :size="14" />
            </div>
            <div>
              <p :class="['font-semibold text-sm leading-tight', teamAColor.playerText]">
                {{ jugador.nombre }}
                <span v-if="String(jugador.id) === String(currentUserId)" class="ml-1.5 text-xs font-bold">(Tú)</span>
                <span
                  v-if="getRolEmoji(jugador)"
                  class="ml-1.5 inline-flex items-center gap-1 rounded-full border border-white/40 bg-white/20 px-2 py-0.5 text-[11px] font-semibold"
                  :class="teamAColor.playerText"
                  :title="getRolTitle(jugador)"
                >
                  <span class="inline-flex items-center"><AppIcon :name="getRolEmoji(jugador)" :size="12" /></span>
                  <span>{{ getRolShortLabel(jugador) }}</span>
                </span>
              </p>
              <p :class="['text-[11px] opacity-80', teamAColor.playerText]" v-if="jugador.skillTier || jugador.playTendency">
                {{ formatNivel(jugador.skillTier) }} • {{ formatTendencia(jugador.playTendency) }}
              </p>
            </div>
          </div>
        </div>
      </article>

      <!-- Equipo B -->
      <article :class="['p-3 border-2 space-y-2.5 rounded-lg shadow-sm', teamBColor.cardBg, teamBColor.cardBorder]">
        <div class="flex items-center justify-between">
          <h2 :class="['text-lg font-bold flex items-center gap-2', teamBColor.titleText]">
            <span>{{ teamBColor.emoji }}</span>
            <span>Equipo {{ partida.colorEquipoB || 'B' }}</span>
          </h2>
          <span :class="['px-2.5 py-0.5 text-xs font-bold rounded-full', teamBColor.badgeBg, teamBColor.badgeText]">
            {{ partida.equipoB?.length || 0 }} / {{ partida.jugadoresPorEquipo }}
          </span>
        </div>

        <div v-if="!partida.equipoB || partida.equipoB.length === 0" :class="['text-sm italic p-4 rounded-lg text-center', teamBColor.emptyText]">
          Sin personas asignadas
        </div>

        <div v-else class="space-y-1.5">
          <div
            v-for="jugador in partida.equipoB"
            :key="`equipo-b-${jugador.id}`"
            :class="[
              'rounded-lg p-2.5 border-2 flex items-center gap-2.5',
              String(jugador.id) === String(currentUserId) ? ['border-black', teamBColor.playerCardBg] : [teamBColor.playerCardBg, teamBColor.playerBorder]
            ]"
          >
            <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada">
              <AppIcon name="check" :size="12" />
            </span>
            <div :class="['w-8 h-8 rounded-full flex items-center justify-center text-sm', teamBColor.avatarBg]">
              <AppIcon name="user" :size="14" />
            </div>
            <div>
              <p :class="['font-semibold text-sm leading-tight', teamBColor.playerText]">
                {{ jugador.nombre }}
                <span v-if="String(jugador.id) === String(currentUserId)" class="ml-1.5 text-xs font-bold">(Tú)</span>
                <span
                  v-if="getRolEmoji(jugador)"
                  class="ml-1.5 inline-flex items-center gap-1 rounded-full border border-white/40 bg-white/20 px-2 py-0.5 text-[11px] font-semibold"
                  :class="teamBColor.playerText"
                  :title="getRolTitle(jugador)"
                >
                  <span class="inline-flex items-center"><AppIcon :name="getRolEmoji(jugador)" :size="12" /></span>
                  <span>{{ getRolShortLabel(jugador) }}</span>
                </span>
              </p>
              <p :class="['text-[11px] opacity-80', teamBColor.playerText]" v-if="jugador.skillTier || jugador.playTendency">
                {{ formatNivel(jugador.skillTier) }} • {{ formatTendencia(jugador.playTendency) }}
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

// Color mapping for teams with complete Tailwind classes
const colorMap = {
  'Blanco': { 
    cardBg: 'bg-slate-50',
    cardBorder: 'border-slate-300',
    titleText: 'text-slate-800',
    badgeBg: 'bg-slate-200',
    badgeText: 'text-slate-800',
    playerBorder: 'border-slate-200',
    playerCardBg: 'bg-white',
    playerText: 'text-slate-800',
    emptyText: 'text-slate-600',
    avatarBg: 'bg-slate-200',
    emoji: '' 
  },
  'Negro': { 
    cardBg: 'bg-gray-900',
    cardBorder: 'border-gray-700',
    titleText: 'text-white',
    badgeBg: 'bg-gray-700',
    badgeText: 'text-white',
    playerBorder: 'border-gray-700',
    playerCardBg: 'bg-gray-800',
    playerText: 'text-white',
    emptyText: 'text-gray-400',
    avatarBg: 'bg-gray-700',
    emoji: '' 
  },
  'Oscuro': { 
    cardBg: 'bg-gray-900',
    cardBorder: 'border-gray-700',
    titleText: 'text-white',
    badgeBg: 'bg-gray-700',
    badgeText: 'text-white',
    playerBorder: 'border-gray-700',
    playerCardBg: 'bg-gray-800',
    playerText: 'text-white',
    emptyText: 'text-gray-400',
    avatarBg: 'bg-gray-700',
    emoji: '' 
  },
  'Rojo': { 
    cardBg: 'bg-red-600',
    cardBorder: 'border-red-700',
    titleText: 'text-white',
    badgeBg: 'bg-red-800',
    badgeText: 'text-white',
    playerBorder: 'border-red-700',
    playerCardBg: 'bg-red-700',
    playerText: 'text-white',
    emptyText: 'text-red-100',
    avatarBg: 'bg-red-800',
    emoji: '' 
  },
  'Azul': { 
    cardBg: 'bg-blue-600',
    cardBorder: 'border-blue-700',
    titleText: 'text-white',
    badgeBg: 'bg-blue-800',
    badgeText: 'text-white',
    playerBorder: 'border-blue-700',
    playerCardBg: 'bg-blue-700',
    playerText: 'text-white',
    emptyText: 'text-blue-100',
    avatarBg: 'bg-blue-800',
    emoji: '' 
  },
  'Verde': { 
    cardBg: 'bg-emerald-600',
    cardBorder: 'border-emerald-700',
    titleText: 'text-white',
    badgeBg: 'bg-emerald-800',
    badgeText: 'text-white',
    playerBorder: 'border-emerald-700',
    playerCardBg: 'bg-emerald-700',
    playerText: 'text-white',
    emptyText: 'text-emerald-100',
    avatarBg: 'bg-emerald-800',
    emoji: '' 
  },
  'Amarillo': { 
    cardBg: 'bg-yellow-400',
    cardBorder: 'border-yellow-500',
    titleText: 'text-yellow-900',
    badgeBg: 'bg-yellow-600',
    badgeText: 'text-white',
    playerBorder: 'border-yellow-500',
    playerCardBg: 'bg-yellow-500',
    playerText: 'text-yellow-900',
    emptyText: 'text-yellow-800',
    avatarBg: 'bg-yellow-600',
    emoji: '' 
  },
  'Naranja': { 
    cardBg: 'bg-orange-600',
    cardBorder: 'border-orange-700',
    titleText: 'text-white',
    badgeBg: 'bg-orange-800',
    badgeText: 'text-white',
    playerBorder: 'border-orange-700',
    playerCardBg: 'bg-orange-700',
    playerText: 'text-white',
    emptyText: 'text-orange-100',
    avatarBg: 'bg-orange-800',
    emoji: '' 
  },
  'Morado': { 
    cardBg: 'bg-purple-600',
    cardBorder: 'border-purple-700',
    titleText: 'text-white',
    badgeBg: 'bg-purple-800',
    badgeText: 'text-white',
    playerBorder: 'border-purple-700',
    playerCardBg: 'bg-purple-700',
    playerText: 'text-white',
    emptyText: 'text-purple-100',
    avatarBg: 'bg-purple-800',
    emoji: '' 
  },
}

const teamAColor = computed(() => {
  if (!props.partida?.colorEquipoA) return colorMap['Blanco']
  return colorMap[props.partida.colorEquipoA] || colorMap['Blanco']
})

const teamBColor = computed(() => {
  if (!props.partida?.colorEquipoB) return colorMap['Negro']
  return colorMap[props.partida.colorEquipoB] || colorMap['Negro']
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

const formatNivel = (skillTier) => {
  if (!skillTier) return ''
  const niveles = {
    'BRONCE': 'Bronce',
    'PLATA': 'Plata',
    'ORO': 'Oro',
    'DIAMANTE': 'Diamante'
  }
  return niveles[skillTier] || skillTier
}

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
