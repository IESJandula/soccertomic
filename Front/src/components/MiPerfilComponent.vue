<script setup>
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { usePlayerProfileStore } from '../stores/playerProfile'
import apiService from '../services/apiService'
import { formatDateTimeEs } from '../utils/dateFormat'
import playerProfileService from '../services/playerProfileService'
import partidoService from '../services/partidoService'
import TierIcon from './ui/TierIcon.vue'

const authStore = useAuthStore()
const profileStore = usePlayerProfileStore()

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const ok = ref('')

const resumen = ref(null)
const playerProfile = ref(null)
const historialPartidos = ref([])
const mostrarHistorialCompleto = ref(false)
const expandedSections = ref({
  basic: false,
  stats: false,
  futbolista: false,
})
const editandoFutbolista = ref(false)
const resumenVotacion = ref({
  vecesDiferencial: 0,
  valoracionesPositivas: 0,
  valoracionesNegativas: 0,
})

const form = ref({
  nombre: '',
  piernaBuena: '',
  disponibilidad: [],
  ageRange: '18_25',
})

const futbolistaForm = ref({
  goalkeeper: false,
  shooting: 3,
  speed: 3,
  dribbling: 3,
  defense: 3,
  strength: 3,
  stamina: 3,
  aerial: 3,
  playStyle: 'A',
  skillTier: 'BRONCE',
})

const preferenciasFutbolista = ref({
  playStyle: 'A',
  posicionPreferida: 'MEDIOCAMPISTA',
})

const normalizarPiernaDesdeBack = (valor) => {
  if (!valor) return ''
  const v = String(valor).trim().toUpperCase()
  if (v === 'RIGHT' || v === 'DERECHA') return 'Derecha'
  if (v === 'LEFT' || v === 'IZQUIERDA') return 'Izquierda'
  if (v === 'BOTH' || v === 'AMBIDIESTRA' || v === 'AMBAS') return 'Ambidiestra'
  return ''
}

const normalizarPiernaParaBack = (valor) => {
  if (!valor) return null
  const v = String(valor).trim().toUpperCase()
  if (v === 'RIGHT' || v === 'DERECHA') return 'Derecha'
  if (v === 'LEFT' || v === 'IZQUIERDA') return 'Izquierda'
  if (v === 'BOTH' || v === 'AMBIDIESTRA' || v === 'AMBAS') return 'Ambidiestra'
  return null
}

const cargarDatos = async () => {
  loading.value = true
  error.value = ''
  try {
    const [resumenData, profileData, historialData, votacionResumenData] = await Promise.all([
      apiService.getUsuarioResumen(),
      playerProfileService.obtenerMiPerfil().catch((err) => {
        if (err?.status === 404) return null
        throw err
      }),
      partidoService.obtenerMiHistorial().catch((err) => {
        console.warn('No se pudo cargar el historial de partidos', err)
        return []
      }),
      apiService.getMiResumenVotacion().catch((err) => {
        console.warn('No se pudo cargar el resumen de votación del usuario', err)
        return { vecesDiferencial: 0, valoracionesPositivas: 0, valoracionesNegativas: 0 }
      }),
    ])

    resumen.value = resumenData
    playerProfile.value = profileData
    historialPartidos.value = (historialData || []).slice().sort((a, b) => {
      const fechaA = new Date(a?.fecha || 0).getTime()
      const fechaB = new Date(b?.fecha || 0).getTime()
      return fechaB - fechaA
    })
    resumenVotacion.value = votacionResumenData || { vecesDiferencial: 0, valoracionesPositivas: 0, valoracionesNegativas: 0 }
    form.value.nombre = resumenData?.nombre || ''
    form.value.piernaBuena = normalizarPiernaDesdeBack(profileData?.attributes?.piernaBuena)
    form.value.disponibilidad = Array.isArray(profileData?.attributes?.disponibilidad) ? profileData.attributes.disponibilidad : []
    form.value.ageRange = profileData?.attributes?.ageRange || '18_25'
    futbolistaForm.value = {
      goalkeeper: profileData?.attributes?.goalkeeper ?? false,
      shooting: profileData?.attributes?.shooting ?? 3,
      speed: profileData?.attributes?.speed ?? 3,
      dribbling: profileData?.attributes?.dribbling ?? 3,
      defense: profileData?.attributes?.defense ?? 3,
      strength: profileData?.attributes?.strength ?? 3,
      stamina: profileData?.attributes?.stamina ?? 3,
      aerial: profileData?.attributes?.aerial ?? 3,
      playStyle: profileData?.attributes?.playStyle || 'A',
      skillTier: profileData?.attributes?.skillTier || 'BRONCE',
    }
    preferenciasFutbolista.value = {
      playStyle: profileData?.attributes?.playStyle || 'A',
      posicionPreferida: profileData?.attributes?.posicionPreferida || (profileData?.attributes?.goalkeeper ? 'PORTERO' : 'MEDIOCAMPISTA'),
    }
    editandoFutbolista.value = false
  } catch (err) {
    error.value = err?.message || 'No se pudo cargar tu perfil'
  } finally {
    loading.value = false
  }
}

onMounted(cargarDatos)

const guardarInformacionBasica = async () => {
  if (!form.value.nombre?.trim()) {
    error.value = 'El nombre no puede estar vacío'
    return
  }

  saving.value = true
  error.value = ''
  ok.value = ''

  try {
    await apiService.upsertPerfil(form.value.nombre.trim(), authStore.user?.email || resumen.value?.email)
    const attrs = playerProfile.value?.attributes || {}
    await playerProfileService.guardarMiPerfil({
      attributes: {
        goalkeeper: attrs.goalkeeper ?? futbolistaForm.value.goalkeeper ?? false,
        shooting: attrs.shooting ?? futbolistaForm.value.shooting ?? 3,
        speed: attrs.speed ?? futbolistaForm.value.speed ?? 3,
        dribbling: attrs.dribbling ?? futbolistaForm.value.dribbling ?? 3,
        defense: attrs.defense ?? futbolistaForm.value.defense ?? 3,
        strength: attrs.strength ?? futbolistaForm.value.strength ?? 3,
        stamina: attrs.stamina ?? futbolistaForm.value.stamina ?? 3,
        aerial: attrs.aerial ?? futbolistaForm.value.aerial ?? 3,
        playStyle: attrs.playStyle || futbolistaForm.value.playStyle || 'A',
        skillTier: attrs.skillTier || futbolistaForm.value.skillTier || 'BRONCE',
        ageRange: form.value.ageRange || '18_25',
        posicionPreferida: attrs.posicionPreferida || (attrs.goalkeeper ? 'PORTERO' : 'MEDIOCAMPISTA'),
        piernaBuena: normalizarPiernaParaBack(form.value.piernaBuena),
        disponibilidad: Array.isArray(form.value.disponibilidad) ? form.value.disponibilidad : [],
      },
    })
    await authStore.refreshUsuario()
    await cargarDatos()
    ok.value = 'Información básica actualizada correctamente'
  } catch (err) {
    error.value = err?.message || 'No se pudo guardar la información'
  } finally {
    saving.value = false
  }
}

const toggleSection = (key) => {
  const estabaAbierta = expandedSections.value[key]
  expandedSections.value = {
    basic: false,
    stats: false,
    futbolista: false,
  }
  if (!estabaAbierta) {
    expandedSections.value[key] = true
  }
}

const guardarPerfilFutbolista = async () => {
  saving.value = true
  error.value = ''
  ok.value = ''
  const attrs = playerProfile.value?.attributes || {}
  try {
    await playerProfileService.guardarMiPerfil({
      attributes: {
        goalkeeper: preferenciasFutbolista.value.posicionPreferida === 'PORTERO',
        shooting: Number(attrs.shooting ?? futbolistaForm.value.shooting ?? 3),
        speed: Number(attrs.speed ?? futbolistaForm.value.speed ?? 3),
        dribbling: Number(attrs.dribbling ?? futbolistaForm.value.dribbling ?? 3),
        defense: Number(attrs.defense ?? futbolistaForm.value.defense ?? 3),
        strength: Number(attrs.strength ?? futbolistaForm.value.strength ?? 3),
        stamina: Number(attrs.stamina ?? futbolistaForm.value.stamina ?? 3),
        aerial: Number(attrs.aerial ?? futbolistaForm.value.aerial ?? 3),
        playStyle: preferenciasFutbolista.value.playStyle || 'A',
        skillTier: attrs.skillTier || futbolistaForm.value.skillTier || 'BRONCE',
        ageRange: form.value.ageRange || '18_25',
        posicionPreferida: preferenciasFutbolista.value.posicionPreferida || 'MEDIOCAMPISTA',
        piernaBuena: normalizarPiernaParaBack(form.value.piernaBuena),
        disponibilidad: Array.isArray(form.value.disponibilidad) ? form.value.disponibilidad : [],
      },
    })
    await cargarDatos()
    ok.value = 'Perfil futbolista actualizado'
  } catch (err) {
    error.value = err?.message || 'No se pudo guardar el perfil futbolista'
  } finally {
    saving.value = false
  }
}

const resumenInfoBasica = computed(() => {
  const partes = []
  if (form.value.piernaBuena) partes.push('pierna configurada')
  if (Array.isArray(form.value.disponibilidad) && form.value.disponibilidad.length > 0) partes.push('disponibilidad configurada')
  if (form.value.ageRange) partes.push('rango de edad configurado')
  return partes.length ? partes.join(' · ') : 'Completa tus datos básicos'
})

const resumenEstadisticas = computed(() => {
  return `${partidosJugados.value} partidos jugados · ${historialPartidos.value.length} en historial`
})

const resumenPerfilFutbolista = computed(() => {
  if (!playerProfile.value?.attributes && !editandoFutbolista.value) return 'Configura tu perfil futbolista'
  const tier = skillTierLabel.value?.label || 'Sin nivel'
  const tendencia = playTendencyLabel.value?.label || 'Sin tendencia'
  return `${tier} · ${tendencia}`
})

const tendenciaPreferidaLabel = computed(() => {
  const estilo = preferenciasFutbolista.value.playStyle || playerProfile.value?.attributes?.playStyle || 'A'
  if (estilo === 'O') return 'Ofensiva'
  if (estilo === 'D') return 'Defensiva'
  return 'Adaptable'
})

const posicionPreferidaLabel = computed(() => {
  const posicion = preferenciasFutbolista.value.posicionPreferida || playerProfile.value?.attributes?.posicionPreferida || 'MEDIOCAMPISTA'
  if (posicion === 'PORTERO') return 'Portero'
  if (posicion === 'DEFENSA') return 'Defensa'
  if (posicion === 'DELANTERO') return 'Delantero'
  return 'Mediocampista'
})

const categoriaAtributo = (valor) => {
  const v = Number(valor)
  if (!Number.isFinite(v)) return 'Sin dato'
  if (v <= 1) return 'Inicial'
  if (v <= 2) return 'Intermedio'
  if (v <= 3) return 'Competente'
  if (v <= 4) return 'Fuerte'
  return 'Destacado'
}

const formatearFecha = (fecha) => {
  return formatDateTimeEs(fecha)
}

const claseResultado = (resultado) => {
  if (resultado === 'VICTORIA') return 'bg-emerald-100 text-emerald-800'
  if (resultado === 'DERROTA') return 'bg-red-100 text-red-800'
  if (resultado === 'EMPATE') return 'bg-amber-100 text-amber-800'
  return 'bg-gray-100 text-gray-700'
}

const claseBannerResultado = (resultado) => {
  if (resultado === 'VICTORIA') return 'bg-emerald-600 text-white border-emerald-500'
  if (resultado === 'DERROTA') return 'bg-rose-600 text-white border-rose-500'
  if (resultado === 'EMPATE') return 'bg-orange-500 text-white border-orange-400'
  return 'bg-slate-600 text-white border-slate-500'
}

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

const partidosJugados = computed(() => {
  return historialPartidos.value.filter((p) => ['VICTORIA', 'DERROTA', 'EMPATE'].includes(p.resultadoParaUsuario)).length
})

const historialPartidosVisibles = computed(() => {
  if (mostrarHistorialCompleto.value) return historialPartidos.value
  return historialPartidos.value.slice(0, 5)
})

const hayMasDeCincoPartidos = computed(() => historialPartidos.value.length > 5)

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

const skillTierLabel = computed(() => {
  const tierValue = futbolistaForm.value.skillTier || playerProfile.value?.attributes?.skillTier || 'BRONCE'
  const tier = profileStore.skillTierOptions.find(t => t.value === tierValue)
  return tier ? { label: tier.label, value: tier.value } : { label: 'Bronce', value: 'BRONCE' }
})

const playTendencyLabel = computed(() => {
  const attrs = {
    shooting: Number(futbolistaForm.value.shooting ?? playerProfile.value?.attributes?.shooting ?? 3),
    speed: Number(futbolistaForm.value.speed ?? playerProfile.value?.attributes?.speed ?? 3),
    dribbling: Number(futbolistaForm.value.dribbling ?? playerProfile.value?.attributes?.dribbling ?? 3),
    defense: Number(futbolistaForm.value.defense ?? playerProfile.value?.attributes?.defense ?? 3),
    strength: Number(futbolistaForm.value.strength ?? playerProfile.value?.attributes?.strength ?? 3),
    aerial: Number(futbolistaForm.value.aerial ?? playerProfile.value?.attributes?.aerial ?? 3),
  }

  const offensive = (attrs.shooting || 0) + (attrs.speed || 0) + (attrs.dribbling || 0)
  const defensive = (attrs.defense || 0) + (attrs.strength || 0) + (attrs.aerial || 0)
  const diff = Math.abs(offensive - defensive)
  
  if (diff < 3) return { label: 'Adaptable' }
  return offensive > defensive ? { label: 'Ofensiva' } : { label: 'Defensiva' }
})

const ageRangeLabel = computed(() => {
  const value = form.value.ageRange || playerProfile.value?.attributes?.ageRange
  if (!value) return 'No especificado'
  const found = profileStore.ageRangeOptions.find(option => option.value === value)
  return found ? found.label : 'No especificado'
})

const globalRating = computed(() => {
  if (!playerProfile.value?.attributes?.globalRating) return '0'
  return playerProfile.value.attributes.globalRating
})
</script>

<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <div v-if="loading" class="bg-white rounded-xl shadow p-6 text-center" role="status" aria-live="polite">
      <div class="inline-block animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
      <p class="text-gray-600 mt-3">Cargando perfil...</p>
    </div>

    <div v-else class="space-y-6">
      <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg" role="alert">{{ error }}</div>
      <div v-if="ok" class="bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg" role="status" aria-live="polite">{{ ok }}</div>

      <section class="bg-white rounded-xl shadow">
        <button type="button" @click="toggleSection('basic')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-gray-800">Información básica</h2>
            <p class="text-xs text-slate-500 truncate">{{ resumenInfoBasica }}</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.basic ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <transition name="accordion">
        <div v-if="expandedSections.basic" class="px-4 pb-4 space-y-3 border-t border-slate-100 overflow-hidden">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">Nombre</label>
              <input v-model="form.nombre" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">Pierna buena</label>
              <select v-model="form.piernaBuena" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white">
                <option value="">Seleccionar</option>
                <option value="Derecha">Derecha</option>
                <option value="Izquierda">Izquierda</option>
                <option value="Ambidiestra">Ambidiestra</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">Rango de edad</label>
              <select v-model="form.ageRange" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white">
                <option v-for="option in profileStore.ageRangeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </div>
          </div>

          <div>
            <label class="block text-xs font-semibold text-gray-700 mb-1">Disponibilidad</label>
            <div class="flex flex-wrap gap-1.5">
              <button
                v-for="option in profileStore.disponibilidadOpciones"
                :key="option"
                type="button"
                @click="form.disponibilidad = form.disponibilidad.includes(option) ? form.disponibilidad.filter(v => v !== option) : [...form.disponibilidad, option]"
                :class="[
                  'px-2 py-1 rounded-full border text-[11px] font-medium transition',
                  form.disponibilidad.includes(option)
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-slate-700 border-slate-300'
                ]"
              >
                {{ option }}
              </button>
            </div>
          </div>

          <div>
            <button @click="guardarInformacionBasica" :disabled="saving" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition disabled:opacity-60">
              {{ saving ? 'Guardando...' : 'Guardar información' }}
            </button>
          </div>
        </div>
        </transition>
      </section>

      <section class="bg-white rounded-xl shadow">
        <button type="button" @click="toggleSection('stats')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-gray-800">Estadísticas</h2>
            <p class="text-xs text-slate-500 truncate">{{ resumenEstadisticas }}</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.stats ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <transition name="accordion">
        <div v-if="expandedSections.stats" class="px-4 pb-4 border-t border-slate-100 space-y-3 overflow-hidden">
        <h3 class="text-sm font-semibold text-gray-700">Historial de partidos</h3>

        <div v-if="historialPartidos.length === 0" class="text-gray-600">
          Aún no tienes partidos finalizados.
        </div>

        <div v-else class="space-y-2.5">
          <div
            v-for="partido in historialPartidosVisibles"
            :key="partido.id"
            :class="['rounded-xl border px-3 py-2.5 flex items-center justify-between gap-3', claseBannerResultado(partido.resultadoParaUsuario)]"
          >
            <div class="min-w-0 flex-1">
              <p class="font-semibold truncate">{{ partido.lugar }}</p>
              <p class="text-xs md:text-sm opacity-90 truncate">{{ formatearFecha(partido.fecha) }}</p>
              <div class="mt-1 flex flex-wrap gap-1">
                <span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-white/20 text-white border border-white/40">
                  Intensidad: {{ formatearIntensidad(partido.intensidadPartido) }}
                </span>
                <span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-white/20 text-white border border-white/40">
                  Balanceo: {{ Number(partido.porcentajeBalanceo || 0).toFixed(0) }}%
                </span>
              </div>
            </div>

            <div class="flex items-center gap-2 md:gap-3 shrink-0">
              <p class="text-2xl md:text-3xl font-extrabold leading-none">
                {{ marcadorEntero(partido.golesEquipoA) }} - {{ marcadorEntero(partido.golesEquipoB) }}
              </p>
              <span class="px-2.5 py-1 rounded-full text-[11px] md:text-xs font-semibold bg-white/20 text-white border border-white/40">
                {{ formatearResultado(partido.resultadoParaUsuario) }}
              </span>
              <span class="px-2.5 py-1 rounded-full text-[11px] md:text-xs font-semibold bg-white/20 text-white border border-white/40">
                {{ formatearTipoPartido(partido.tipo) }}
              </span>
            </div>
          </div>

          <button
            v-if="hayMasDeCincoPartidos"
            type="button"
            @click="mostrarHistorialCompleto = !mostrarHistorialCompleto"
            class="w-full rounded-lg border border-gray-200 bg-gray-50 text-gray-700 text-sm font-semibold py-2 hover:bg-gray-100 transition"
          >
            {{ mostrarHistorialCompleto ? 'Mostrar solo los últimos 5' : `Ver todos (${historialPartidos.length})` }}
          </button>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
          <div class="bg-slate-50 rounded-lg p-3 text-center border border-slate-200">
            <p class="text-xs text-gray-600">Partidos jugados</p>
            <p class="text-2xl font-bold text-slate-800">{{ partidosJugados }}</p>
          </div>
          <div class="bg-emerald-50 rounded-lg p-3 text-center border border-emerald-200">
            <p class="text-xs text-gray-600">Victorias</p>
            <p class="text-2xl font-bold text-emerald-700">{{ porcentajeResultados(victoriasCount) }}%</p>
          </div>
          <div class="bg-amber-50 rounded-lg p-3 text-center border border-amber-200">
            <p class="text-xs text-gray-600">Empates</p>
            <p class="text-2xl font-bold text-amber-700">{{ porcentajeResultados(empatesCount) }}%</p>
          </div>
          <div class="bg-rose-50 rounded-lg p-3 text-center border border-rose-200">
            <p class="text-xs text-gray-600">Derrotas</p>
            <p class="text-2xl font-bold text-rose-700">{{ porcentajeResultados(derrotasCount) }}%</p>
          </div>
        </div>

        <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 mb-4">
          <div class="flex flex-wrap items-center gap-2 text-xs md:text-sm font-semibold">
            <span class="inline-flex items-center gap-1 rounded-full bg-amber-100 text-amber-800 px-2.5 py-1">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg>
              Veces diferencial: {{ resumenVotacion.vecesDiferencial ?? 0 }}
            </span>
            <span class="inline-flex items-center gap-1 rounded-full bg-emerald-100 text-emerald-800 px-2.5 py-1">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.3l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 6.59 7.41C6.22 7.78 6 8.3 6 8.83V19c0 1.1.9 2 2 2h9c.82 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
              Actitud positiva: {{ resumenVotacion.valoracionesPositivas ?? 0 }}
            </span>
            <span class="inline-flex items-center gap-1 rounded-full bg-rose-100 text-rose-800 px-2.5 py-1">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M15 3H6c-.82 0-1.54.5-1.84 1.22L1.14 11.27c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.3l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10 23l6.41-6.41c.37-.37.59-.89.59-1.42V5c0-1.1-.9-2-2-2zm3 0v12h4V3h-4z"/></svg>
              Actitud negativa: {{ resumenVotacion.valoracionesNegativas ?? 0 }}
            </span>
          </div>
        </div>

        <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 mb-4">
          <div class="flex flex-wrap items-center gap-2 text-xs md:text-sm font-semibold">
            <span class="inline-flex items-center gap-1 rounded-full bg-blue-100 text-blue-800 px-2.5 py-1">
              Reputación: {{ categoriaReputacionSocial }}
            </span>
            <span class="inline-flex items-center gap-1 rounded-full bg-purple-100 text-purple-800 px-2.5 py-1">
              Experiencia: {{ nivelSocialExperiencia }}
            </span>
          </div>
        </div>
        </div>
        </transition>
      </section>

      <section class="bg-white rounded-xl shadow">
        <button type="button" @click="toggleSection('futbolista')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-gray-800">Perfil futbolista</h2>
            <p class="text-xs text-slate-500 truncate">{{ resumenPerfilFutbolista }}</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.futbolista ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <transition name="accordion">
        <div v-if="expandedSections.futbolista" class="px-4 pb-4 border-t border-slate-100 overflow-hidden">
        <div class="flex items-center justify-between mb-4 mt-3">
          <h3 class="text-sm font-semibold text-gray-700">Resumen técnico</h3>
        </div>

        <div v-if="!playerProfile && !editandoFutbolista" class="text-center py-8">
          <p class="text-gray-600 mb-4">Aún no has completado tu perfil futbolístico.</p>
          <button
            @click="editandoFutbolista = true"
            class="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg font-medium transition"
          >
            Configurar perfil futbolista
          </button>
        </div>

        <div v-else class="space-y-3">
          <!-- Nivel y Tendencia -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div class="bg-gradient-to-br from-amber-50 to-amber-100 border border-amber-200 rounded-xl p-4 text-center">
              <p class="text-xs md:text-sm text-amber-700 font-semibold mb-1">Nivel</p>
              <div class="flex items-center justify-center gap-1.5 text-2xl md:text-3xl font-bold text-amber-900">
                <TierIcon :tier="skillTierLabel.value" :size="24" />
                <span>{{ skillTierLabel.label }}</span>
              </div>
            </div>
            <div class="bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200 rounded-xl p-4 text-center">
              <p class="text-xs md:text-sm text-blue-700 font-semibold mb-1">Tendencia</p>
              <p class="text-2xl md:text-3xl font-bold text-blue-900">
                {{ tendenciaPreferidaLabel }}
              </p>
            </div>
            <div class="bg-gradient-to-br from-violet-50 to-violet-100 border border-violet-200 rounded-xl p-4 text-center">
              <p class="text-xs md:text-sm text-violet-700 font-semibold mb-1">Posición preferida</p>
              <p class="text-2xl md:text-3xl font-bold text-violet-900">
                {{ posicionPreferidaLabel }}
              </p>
            </div>
          </div>

          <div class="bg-slate-50 rounded-xl p-3 space-y-3 border border-slate-200">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1">Actualizar tendencia</label>
                <select v-model="preferenciasFutbolista.playStyle" class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white">
                  <option value="O">Ofensiva</option>
                  <option value="D">Defensiva</option>
                  <option value="A">Adaptable</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1">Actualizar posición preferida</label>
                <select v-model="preferenciasFutbolista.posicionPreferida" class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white">
                  <option value="DELANTERO">Delantero</option>
                  <option value="MEDIOCAMPISTA">Mediocampista</option>
                  <option value="DEFENSA">Defensa</option>
                  <option value="PORTERO">Portero</option>
                </select>
              </div>
            </div>

            <div class="flex justify-end">
              <button @click="guardarPerfilFutbolista" :disabled="saving" class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition disabled:opacity-60">
                {{ saving ? 'Guardando...' : 'Guardar cambios de perfil futbolista' }}
              </button>
            </div>
          </div>

          <!-- Grid de atributos -->
          <div class="bg-slate-50 rounded-xl p-4">
            <div class="grid grid-cols-2 gap-2.5 text-xs">
              <div class="bg-white rounded-lg p-3 border border-slate-200">
                <div class="flex items-center gap-2 text-slate-900 font-semibold">
                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="2" fill="currentColor" stroke="none"/></svg>
                  <span>Disparo</span>
                </div>
                <p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(playerProfile?.attributes?.shooting ?? futbolistaForm.shooting) }}</p>
              </div>

              <div class="bg-white rounded-lg p-3 border border-slate-200">
                <div class="flex items-center gap-2 text-slate-900 font-semibold">
                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M13 2L5 14h6l-1 8 9-13h-6l0-7z"/></svg>
                  <span>Velocidad</span>
                </div>
                <p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(playerProfile?.attributes?.speed ?? futbolistaForm.speed) }}</p>
              </div>

              <div class="bg-white rounded-lg p-3 border border-slate-200">
                <div class="flex items-center gap-2 text-slate-900 font-semibold">
                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 5h12"/><path d="M8 9h8"/><path d="M6 13h12"/><path d="M9 17h6"/><path d="M11 21h2"/></svg>
                  <span>Regate</span>
                </div>
                <p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(playerProfile?.attributes?.dribbling ?? futbolistaForm.dribbling) }}</p>
              </div>

              <div class="bg-white rounded-lg p-3 border border-slate-200">
                <div class="flex items-center gap-2 text-slate-900 font-semibold">
                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 2l8 3v6c0 5-3.4 9.7-8 11-4.6-1.3-8-6-8-11V5l8-3zm0 4l-5 2v3c0 3.7 2.2 7.1 5 8.5 2.8-1.4 5-4.8 5-8.5V8l-5-2z"/></svg>
                  <span>Defensa</span>
                </div>
                <p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(playerProfile?.attributes?.defense ?? futbolistaForm.defense) }}</p>
              </div>

              <div class="bg-white rounded-lg p-3 border border-slate-200">
                <div class="flex items-center gap-2 text-slate-900 font-semibold">
                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M7 11.5c1.7 0 3-1.3 3-3V6h2v2.5c0 1.2.9 2.2 2.1 2.4l2.1.4c2.2.4 3.8 2.4 3.8 4.7V18c0 2.2-1.8 4-4 4H9.5C6.5 22 4 19.5 4 16.5V13c0-.8.7-1.5 1.5-1.5H7zm5.5 2.7c-.9-.2-1.8-.6-2.5-1.2-.8.8-1.9 1.3-3.2 1.5V16.5c0 1.7 1.4 3.1 3.1 3.1H16c1 0 1.8-.8 1.8-1.8V16c0-1-.7-1.8-1.7-2l-2.1-.4z"/></svg>
                  <span>Físico</span>
                </div>
                <p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(playerProfile?.attributes?.strength ?? futbolistaForm.strength) }}</p>
              </div>

              <div class="bg-white rounded-lg p-3 border border-slate-200">
                <div class="flex items-center gap-2 text-slate-900 font-semibold">
                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 2c2.2 2.4 4 4.8 4 7.2 0 1.8-.9 3.3-2.3 4.2 3.1.6 5.3 2.8 5.3 5.6 0 3.1-2.9 5-7 5s-7-1.9-7-5c0-2.8 2.2-5 5.3-5.6C8.9 12.5 8 11 8 9.2 8 6.8 9.8 4.4 12 2z"/></svg>
                  <span>Resistencia</span>
                </div>
                <p class="mt-1.5 text-sm font-bold text-slate-800">{{ categoriaAtributo(playerProfile?.attributes?.stamina ?? futbolistaForm.stamina) }}</p>
              </div>

              <div class="bg-white rounded-lg p-3 border border-slate-200 col-span-2">
                <div class="flex items-center gap-2 text-slate-900 font-semibold justify-center sm:justify-start">
                  <svg class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M3 14c2.2-.1 3.4-1.3 4.6-2.7 1.2-1.3 2.4-2.8 4.7-3.1 2.1-.3 3.8.4 5.3 1.7L21 8l-1.2 4.6c-.8 3.2-3.2 5.5-6.7 6.2-2.6.5-4.8-.1-6.4-1.3-1.8-1.4-2.8-3.5-3.7-3.5z"/></svg>
                  <span>Aéreo</span>
                </div>
                <p class="mt-1.5 text-sm font-bold text-slate-800 text-center sm:text-left">{{ categoriaAtributo(playerProfile?.attributes?.aerial ?? futbolistaForm.aerial) }}</p>
              </div>
            </div>
          </div>

          <p class="text-xs text-slate-600">
            Estos datos se usan para balancear automáticamente los equipos.
          </p>
          <p class="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            Las categorías de habilidades se interpretan dentro de tu nivel actual ({{ skillTierLabel.label }}). El mismo descriptor no representa exactamente el mismo rendimiento entre distintos niveles.
          </p>
        </div>
        </div>
        </transition>
      </section>
    </div>
  </div>
</template>

<style scoped>
.accordion-enter-active,
.accordion-leave-active {
  transition: all 0.22s ease;
}

.accordion-enter-from,
.accordion-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
