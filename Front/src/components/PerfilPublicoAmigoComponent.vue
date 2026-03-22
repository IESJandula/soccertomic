<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import apiService from '../services/apiService'
import playerProfileService from '../services/playerProfileService'
import partidoService from '../services/partidoService'
import { formatDateTimeEs } from '../utils/dateFormat'

const route = useRoute()

const loading = ref(true)
const error = ref('')
const perfil = ref(null)
const usuario = ref(null)
const historialPartidos = ref([])
const resumenVotacion = ref({
  vecesDiferencial: 0,
  valoracionesPositivas: 0,
  valoracionesNegativas: 0,
})

const expandedSections = ref({
  basic: true,
  futbolista: false,
  competencias: false,
  stats: true,
})

const toggleSection = (key) => {
  expandedSections.value[key] = !expandedSections.value[key]
}

const categoriaAtributo = (valor) => {
  const v = Number(valor)
  if (!Number.isFinite(v)) return 'Sin dato'
  if (v <= 1) return 'Inicial'
  if (v <= 2) return 'Intermedio'
  if (v <= 3) return 'Competente'
  if (v <= 4) return 'Fuerte'
  return 'Destacado'
}

const posicionPreferidaLabel = computed(() => {
  const posicion = perfil.value?.attributes?.posicionPreferida || 'MEDIOCAMPISTA'
  if (posicion === 'PORTERO') return 'Portero'
  if (posicion === 'DEFENSA') return 'Defensa'
  if (posicion === 'DELANTERO') return 'Delantero'
  return 'Mediocampista'
})

const tendenciaLabel = computed(() => {
  const playStyle = perfil.value?.attributes?.playStyle || 'A'
  if (playStyle === 'O') return 'Ofensiva'
  if (playStyle === 'D') return 'Defensiva'
  return 'Adaptable'
})

const ageRangeLabel = computed(() => {
  const value = perfil.value?.attributes?.ageRange
  const map = {
    '18_25': '18-25',
    '26_35': '26-35',
    '36_45': '36-45',
    '46_PLUS': '46+',
  }
  return map[value] || 'No especificado'
})

const disponibilidadLabel = computed(() => {
  const disp = perfil.value?.attributes?.disponibilidad
  if (!Array.isArray(disp) || disp.length === 0) return 'No especificada'
  return disp.join(', ')
})

const partidosJugados = computed(() => {
  return historialPartidos.value.filter((p) => ['VICTORIA', 'DERROTA', 'EMPATE'].includes(p.resultadoParaUsuario)).length
})

const victoriasCount = computed(() => historialPartidos.value.filter((p) => p.resultadoParaUsuario === 'VICTORIA').length)
const empatesCount = computed(() => historialPartidos.value.filter((p) => p.resultadoParaUsuario === 'EMPATE').length)
const derrotasCount = computed(() => historialPartidos.value.filter((p) => p.resultadoParaUsuario === 'DERROTA').length)

const porcentajeResultados = (cantidad) => {
  const total = partidosJugados.value
  if (!total) return 0
  return Math.round((cantidad * 100) / total)
}

const categoriaReputacionSocial = computed(() => {
  const positivas = Number(resumenVotacion.value?.valoracionesPositivas || 0)
  const negativas = Number(resumenVotacion.value?.valoracionesNegativas || 0)
  const puntaje = positivas - (negativas * 3)

  if (puntaje >= 25) return 'Muy positiva'
  if (puntaje >= 10) return 'Positiva'
  if (puntaje >= -5) return 'Neutra'
  if (puntaje >= -20) return 'En riesgo'
  return 'Crítica'
})

const nivelSocialExperiencia = computed(() => {
  const jugados = Number(partidosJugados.value || 0)
  const diferenciales = Number(resumenVotacion.value?.vecesDiferencial || 0)
  const puntajeExperiencia = jugados + (diferenciales * 4)

  if (puntajeExperiencia >= 120) return 'Referente'
  if (puntajeExperiencia >= 60) return 'Experimentado'
  if (puntajeExperiencia >= 25) return 'Intermedio'
  return 'Inicial'
})

const formatearFecha = (fecha) => formatDateTimeEs(fecha)

const marcadorEntero = (valor) => {
  const numero = Number(valor)
  return Number.isFinite(numero) ? Math.round(numero) : 0
}

const formatearResultado = (resultado) => {
  const map = {
    VICTORIA: 'Victoria',
    DERROTA: 'Derrota',
    EMPATE: 'Empate',
  }
  return map[resultado] || resultado || '-'
}

const formatearTipoPartido = (tipo) => {
  const map = {
    PUBLICO: 'Público',
    PRIVADO: 'Privado',
  }
  return map[tipo] || tipo || '-'
}

const formatearIntensidad = (intensidad) => {
  if (!intensidad) return '—'
  return String(intensidad)
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

const claseBannerResultado = (resultado) => {
  if (resultado === 'VICTORIA') return 'bg-emerald-600 text-white border-emerald-500'
  if (resultado === 'DERROTA') return 'bg-rose-600 text-white border-rose-500'
  if (resultado === 'EMPATE') return 'bg-orange-500 text-white border-orange-400'
  return 'bg-slate-600 text-white border-slate-500'
}

const cargarPerfil = async () => {
  loading.value = true
  error.value = ''

  const usuarioId = Number(route.params.usuarioId)
  if (!Number.isFinite(usuarioId) || usuarioId <= 0) {
    error.value = 'Perfil no válido.'
    loading.value = false
    return
  }

  try {
    const [usuarioData, perfilData, historialData, votacionData] = await Promise.all([
      apiService.getUsuarioResumen(usuarioId),
      playerProfileService.obtenerPerfilPublico(usuarioId),
      partidoService.obtenerHistorialPublico(usuarioId),
      apiService.getResumenVotacionPublico(usuarioId),
    ])

    usuario.value = usuarioData
    perfil.value = perfilData
    historialPartidos.value = (historialData || []).slice().sort((a, b) => {
      const fechaA = new Date(a?.fecha || 0).getTime()
      const fechaB = new Date(b?.fecha || 0).getTime()
      return fechaB - fechaA
    })
    resumenVotacion.value = votacionData || { vecesDiferencial: 0, valoracionesPositivas: 0, valoracionesNegativas: 0 }
  } catch (err) {
    if (err?.status === 403) {
      error.value = 'Solo puedes ver el perfil de amistades confirmadas.'
    } else if (err?.status === 404) {
      error.value = 'No se encontró el perfil de esta persona.'
    } else {
      error.value = err?.message || 'No se pudo cargar el perfil de usuario.'
    }
  } finally {
    loading.value = false
  }
}

onMounted(cargarPerfil)
</script>

<template>
  <div class="max-w-3xl mx-auto space-y-3">

    <section v-if="loading" class="card-surface p-6 text-sm text-slate-600">
      Cargando perfil...
    </section>

    <section v-else-if="error" class="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
      {{ error }}
    </section>

    <template v-else>
      <section class="card-surface">
        <button type="button" @click="toggleSection('basic')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div>
            <h3 class="text-base font-semibold text-slate-800">Información básica</h3>
            <p class="text-xs text-slate-500">Nombre, rango y disponibilidad</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.basic ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <div v-if="expandedSections.basic" class="px-4 pb-4 border-t border-slate-100">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm mt-3">
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3">
              <p class="text-xs text-slate-600 font-semibold">Nombre</p>
              <p class="text-base font-bold text-slate-900">{{ usuario?.nombre || '-' }}</p>
            </div>
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3">
              <p class="text-xs text-slate-600 font-semibold">Nivel</p>
              <p class="text-base font-bold text-slate-900">{{ perfil?.attributes?.skillTier || '-' }}</p>
            </div>
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3">
              <p class="text-xs text-slate-600 font-semibold">Rating global</p>
              <p class="text-base font-bold text-slate-900">{{ perfil?.attributes?.globalRating ?? '-' }}</p>
            </div>
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3">
              <p class="text-xs text-slate-600 font-semibold">Pierna buena</p>
              <p class="text-base font-bold text-slate-900">{{ perfil?.attributes?.piernaBuena || 'No especificada' }}</p>
            </div>
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3">
              <p class="text-xs text-slate-600 font-semibold">Rango de edad</p>
              <p class="text-base font-bold text-slate-900">{{ ageRangeLabel }}</p>
            </div>
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3">
              <p class="text-xs text-slate-600 font-semibold">Disponibilidad</p>
              <p class="text-sm font-bold text-slate-900">{{ disponibilidadLabel }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="card-surface">
        <button type="button" @click="toggleSection('futbolista')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div>
            <h3 class="text-base font-semibold text-slate-800">Perfil futbolista</h3>
            <p class="text-xs text-slate-500">Tendencia y posición preferida</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.futbolista ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <div v-if="expandedSections.futbolista" class="px-4 pb-4 border-t border-slate-100">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm mt-3">
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <p class="text-xs text-blue-700 font-semibold">Tendencia</p>
              <p class="text-base font-bold text-blue-900">{{ tendenciaLabel }}</p>
            </div>
            <div class="bg-violet-50 border border-violet-200 rounded-lg p-3">
              <p class="text-xs text-violet-700 font-semibold">Posición preferida</p>
              <p class="text-base font-bold text-violet-900">{{ posicionPreferidaLabel }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="card-surface">
        <button type="button" @click="toggleSection('competencias')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div>
            <h3 class="text-base font-semibold text-slate-800">Competencias</h3>
            <p class="text-xs text-slate-500">Resumen por categoría</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.competencias ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <div v-if="expandedSections.competencias" class="px-4 pb-4 border-t border-slate-100">
          <div class="grid grid-cols-2 gap-2.5 text-xs mt-3">
            <div class="bg-white rounded-lg p-3 border border-slate-200"><p class="text-slate-900 font-semibold">Disparo</p><p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(perfil?.attributes?.shooting) }}</p></div>
            <div class="bg-white rounded-lg p-3 border border-slate-200"><p class="text-slate-900 font-semibold">Velocidad</p><p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(perfil?.attributes?.speed) }}</p></div>
            <div class="bg-white rounded-lg p-3 border border-slate-200"><p class="text-slate-900 font-semibold">Regate</p><p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(perfil?.attributes?.dribbling) }}</p></div>
            <div class="bg-white rounded-lg p-3 border border-slate-200"><p class="text-slate-900 font-semibold">Defensa</p><p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(perfil?.attributes?.defense) }}</p></div>
            <div class="bg-white rounded-lg p-3 border border-slate-200"><p class="text-slate-900 font-semibold">Físico</p><p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(perfil?.attributes?.strength) }}</p></div>
            <div class="bg-white rounded-lg p-3 border border-slate-200"><p class="text-slate-900 font-semibold">Resistencia</p><p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(perfil?.attributes?.stamina) }}</p></div>
            <div class="bg-white rounded-lg p-3 border border-slate-200 col-span-2"><p class="text-slate-900 font-semibold">Aéreo</p><p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(perfil?.attributes?.aerial) }}</p></div>
          </div>
          <p class="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mt-3">
            Las competencias se interpretan ajustadas al rango de edad y al nivel actual del usuario. El mismo descriptor puede representar distinto rendimiento entre rangos.
          </p>
        </div>
      </section>

      <section class="card-surface">
        <button type="button" @click="toggleSection('stats')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div>
            <h3 class="text-base font-semibold text-slate-800">Estadísticas e historial</h3>
            <p class="text-xs text-slate-500">Resumen social y partidos finalizados</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.stats ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <div v-if="expandedSections.stats" class="px-4 pb-4 border-t border-slate-100">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3">
            <div class="bg-slate-50 rounded-lg p-3 text-center border border-slate-200"><p class="text-xs text-gray-600">Partidos jugados</p><p class="text-2xl font-bold text-slate-800">{{ partidosJugados }}</p></div>
            <div class="bg-emerald-50 rounded-lg p-3 text-center border border-emerald-200"><p class="text-xs text-gray-600">Victorias</p><p class="text-2xl font-bold text-emerald-700">{{ porcentajeResultados(victoriasCount) }}%</p></div>
            <div class="bg-amber-50 rounded-lg p-3 text-center border border-amber-200"><p class="text-xs text-gray-600">Empates</p><p class="text-2xl font-bold text-amber-700">{{ porcentajeResultados(empatesCount) }}%</p></div>
            <div class="bg-rose-50 rounded-lg p-3 text-center border border-rose-200"><p class="text-xs text-gray-600">Derrotas</p><p class="text-2xl font-bold text-rose-700">{{ porcentajeResultados(derrotasCount) }}%</p></div>
          </div>

          <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 mt-3">
            <div class="flex flex-wrap items-center gap-2 text-xs md:text-sm font-semibold">
              <span class="inline-flex items-center gap-1 rounded-full bg-amber-100 text-amber-800 px-2.5 py-1">Veces diferencial: {{ resumenVotacion.vecesDiferencial ?? 0 }}</span>
              <span class="inline-flex items-center gap-1 rounded-full bg-emerald-100 text-emerald-800 px-2.5 py-1">Actitud positiva: {{ resumenVotacion.valoracionesPositivas ?? 0 }}</span>
              <span class="inline-flex items-center gap-1 rounded-full bg-rose-100 text-rose-800 px-2.5 py-1">Actitud negativa: {{ resumenVotacion.valoracionesNegativas ?? 0 }}</span>
            </div>
          </div>

          <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 mt-3">
            <div class="flex flex-wrap items-center gap-2 text-xs md:text-sm font-semibold">
              <span class="inline-flex items-center gap-1 rounded-full bg-blue-100 text-blue-800 px-2.5 py-1">Reputación: {{ categoriaReputacionSocial }}</span>
              <span class="inline-flex items-center gap-1 rounded-full bg-purple-100 text-purple-800 px-2.5 py-1">Experiencia: {{ nivelSocialExperiencia }}</span>
            </div>
          </div>

          <div v-if="historialPartidos.length === 0" class="text-sm text-slate-600 mt-3">
            Este usuario aún no tiene partidos finalizados visibles.
          </div>

          <div v-else class="space-y-2.5 mt-3">
            <div v-for="partido in historialPartidos" :key="partido.id" :class="['relative rounded-xl border px-3 py-2.5 pr-14 flex items-center justify-between gap-3', claseBannerResultado(partido.resultadoParaUsuario)]">
              <div v-if="Number(partido.vecesDiferencial || 0) > 0" class="absolute top-2 right-2 inline-flex items-center gap-1 rounded-full bg-amber-500/90 text-white border border-amber-300 px-2 py-0.5 text-[10px] font-bold" title="Veces votado como diferencial">
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg>
                <span>x{{ Number(partido.vecesDiferencial || 0) }}</span>
              </div>
              <div class="min-w-0 flex-1">
                <p class="font-semibold truncate">{{ partido.lugar }}</p>
                <p class="text-xs md:text-sm opacity-90 truncate">{{ formatearFecha(partido.fecha) }}</p>
                <div class="mt-1 flex flex-wrap gap-1">
                  <span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-white/20 text-white border border-white/40">Intensidad: {{ formatearIntensidad(partido.intensidadPartido) }}</span>
                  <span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-white/20 text-white border border-white/40">Balanceo: {{ Number(partido.porcentajeBalanceo || 0).toFixed(0) }}%</span>
                </div>
              </div>
              <div class="flex items-center gap-2 md:gap-3 shrink-0">
                <p class="text-2xl md:text-3xl font-extrabold leading-none">{{ marcadorEntero(partido.golesEquipoA) }} - {{ marcadorEntero(partido.golesEquipoB) }}</p>
                <span class="px-2.5 py-1 rounded-full text-[11px] md:text-xs font-semibold bg-white/20 text-white border border-white/40">{{ formatearResultado(partido.resultadoParaUsuario) }}</span>
                <span class="px-2.5 py-1 rounded-full text-[11px] md:text-xs font-semibold bg-white/20 text-white border border-white/40">{{ formatearTipoPartido(partido.tipo) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
