<script setup>
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import apiService from '../services/apiService'
import { formatDateTimeEs } from '../utils/dateFormat'
import playerProfileService from '../services/playerProfileService'
import partidoService from '../services/partidoService'
import TierIcon from './ui/TierIcon.vue'

const authStore = useAuthStore()

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const ok = ref('')

const resumen = ref(null)
const playerProfile = ref(null)
const historialPartidos = ref([])
const mesesHistorialVisibles = ref(2)
const diaSeleccionadoKey = ref('')
const mostrandoDetalleDia = ref(false)
const expandedSections = ref({
  ficha: false,
  stats: false,
})
const editandoFutbolista = ref(false)
const resumenVotacion = ref({
  vecesDiferencial: 0,
  valoracionesPositivas: 0,
  valoracionesNegativas: 0,
})

const form = ref({
  nombre: '',
})

const preferenciasFutbolista = ref({
  playStyle: 'A',
  posicionPreferida: 'MEDIOCAMPISTA',
})

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
    await authStore.refreshUsuario()
    await cargarDatos()
    ok.value = 'Ficha de jugador actualizada correctamente'
  } catch (err) {
    error.value = err?.message || 'No se pudo guardar la información'
  } finally {
    saving.value = false
  }
}

const toggleSection = (key) => {
  const estabaAbierta = expandedSections.value[key]
  expandedSections.value = {
    ficha: false,
    stats: false,
  }
  if (!estabaAbierta) {
    expandedSections.value[key] = true
  }
}

const guardarPerfilFutbolista = async () => {
  saving.value = true
  error.value = ''
  ok.value = ''
  try {
    await playerProfileService.guardarMiPerfil({
      attributes: {
        goalkeeper: preferenciasFutbolista.value.posicionPreferida === 'PORTERO' || preferenciasFutbolista.value.playStyle === 'G',
        playStyle: preferenciasFutbolista.value.playStyle || 'A',
        posicionPreferida: preferenciasFutbolista.value.posicionPreferida || 'MEDIOCAMPISTA',
      },
    })
    await cargarDatos()
    ok.value = 'Ficha de jugador actualizada correctamente'
  } catch (err) {
    error.value = err?.message || 'No se pudo guardar la ficha de jugador'
  } finally {
    saving.value = false
  }
}

const resumenEstadisticas = computed(() => {
  return `${partidosJugados.value} partidos jugados · ${historialPartidos.value.length} en historial`
})

const nivelVisibleBase = computed(() => {
  const valor = Number(resumen.value?.nivelVisible)
  return Number.isFinite(valor) ? valor : 0
})

const sigmaRating = computed(() => {
  const valor = Number(resumen.value?.ratingSigma)
  return Number.isFinite(valor) ? valor : 8.33
})

const nivelVisibleMaximo = 25

const nivelVisibleNormalizado = computed(() => {
  const porcentaje = nivelVisibleBase.value / nivelVisibleMaximo
  return Math.max(0, Math.min(1, porcentaje))
})

const nivelEscalado = computed(() => {
  const nivel = Math.round(nivelVisibleNormalizado.value * 9) + 1
  return Math.max(1, Math.min(10, nivel))
})

const tierActual = computed(() => {
  const nivel = nivelEscalado.value
  if (nivel <= 3) return 'BRONCE'
  if (nivel <= 6) return 'PLATA'
  if (nivel <= 8) return 'ORO'
  return 'DIAMANTE'
})

const rangosNivel = [
  { tier: 'BRONCE', label: 'Bronce', min: 1, max: 3, gradient: 'from-amber-500 to-amber-600' },
  { tier: 'PLATA', label: 'Plata', min: 4, max: 6, gradient: 'from-slate-300 to-slate-500' },
  { tier: 'ORO', label: 'Oro', min: 7, max: 8, gradient: 'from-yellow-400 to-amber-500' },
  { tier: 'DIAMANTE', label: 'Diamante', min: 9, max: 10, gradient: 'from-cyan-300 to-cyan-500' },
]

const fiabilidadScore = computed(() => {
  const valor = Number(resumen.value?.fiabilidadScore)
  return Number.isFinite(valor) ? valor : 0
})

const mostrarNivelVisible = computed(() => {
  return sigmaRating.value <= 7
})

const tendenciaPreferidaLabel = computed(() => {
  const estilo = preferenciasFutbolista.value.playStyle || playerProfile.value?.attributes?.playStyle || 'A'
  if (estilo === 'G') return 'Portero'
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

const mostrarAmbosResultados = (partido) => {
  return Boolean(partido?.mostrarAmbosResultados)
}

const resultadoBaseLabel = (partido) => {
  const golesA = partido?.golesBaseEquipoA
  const golesB = partido?.golesBaseEquipoB
  if (!Number.isFinite(Number(golesA)) || !Number.isFinite(Number(golesB))) return 'Resultado: -'
  return `Resultado: ${marcadorEntero(golesA)} - ${marcadorEntero(golesB)}`
}

const resultadoConsensuadoLabel = (partido) => {
  const golesA = partido?.golesConsensuadoEquipoA
  const golesB = partido?.golesConsensuadoEquipoB
  if (!Number.isFinite(Number(golesA)) || !Number.isFinite(Number(golesB))) return 'Consensuado: -'
  return `Consensuado: ${marcadorEntero(golesA)} - ${marcadorEntero(golesB)}`
}

const formatearResultado = (resultado) => {
  const map = {
    VICTORIA: 'Victoria',
    DERROTA: 'Derrota',
    EMPATE: 'Empate',
    SIN_PARTICIPACION: 'Sin participacion',
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

const diasSemana = ['L', 'M', 'X', 'J', 'V', 'S', 'D']
const hoyKey = new Date().toISOString().slice(0, 10)

const etiquetaMes = (year, monthIndex) => {
  return new Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' })
    .format(new Date(year, monthIndex, 1))
}

const prioridadResultado = (resultado) => {
  if (resultado === 'DERROTA') return 3
  if (resultado === 'EMPATE') return 2
  if (resultado === 'VICTORIA') return 1
  return 0
}

const resultadoDia = (partidos = []) => {
  if (!partidos.length) return 'SIN_DATOS'
  return partidos.reduce((acc, p) => (prioridadResultado(p.resultadoParaUsuario) > prioridadResultado(acc) ? p.resultadoParaUsuario : acc), 'SIN_DATOS')
}

const claseDiaCalendario = (partidos = [], dayKey = '') => {
  const isToday = dayKey === hoyKey
  const hasMatch = partidos.length > 0
  if (isToday && hasMatch) return 'calendar-day-today-with-match'
  if (isToday) return 'calendar-day-today'
  if (hasMatch) return 'calendar-day-with-match'
  return 'calendar-day-empty'
}

const historialPorMes = computed(() => {
  const partidosPorDia = new Map()

  for (const partido of historialPartidos.value) {
    const fecha = new Date(partido?.fecha)
    if (!Number.isFinite(fecha.getTime())) continue
    const dayKey = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`
    if (!partidosPorDia.has(dayKey)) partidosPorDia.set(dayKey, [])
    partidosPorDia.get(dayKey).push(partido)
  }

  const monthKeys = [...new Set([...partidosPorDia.keys()].map((k) => k.slice(0, 7)))]
    .sort((a, b) => (a < b ? 1 : -1))

  return monthKeys.map((monthKey) => {
    const [yearStr, monthStr] = monthKey.split('-')
    const year = Number(yearStr)
    const month = Number(monthStr) - 1
    const firstDay = new Date(year, month, 1)
    const totalDays = new Date(year, month + 1, 0).getDate()
    const startOffset = (firstDay.getDay() + 6) % 7
    const cells = []

    for (let i = 0; i < startOffset; i += 1) {
      cells.push({ key: `${monthKey}-empty-start-${i}`, empty: true })
    }

    for (let day = 1; day <= totalDays; day += 1) {
      const dayKey = `${monthKey}-${String(day).padStart(2, '0')}`
      const partidos = partidosPorDia.get(dayKey) || []
      cells.push({
        key: dayKey,
        empty: false,
        day,
        partidos,
      })
    }

    while (cells.length % 7 !== 0) {
      cells.push({ key: `${monthKey}-empty-end-${cells.length}`, empty: true })
    }

    return {
      key: monthKey,
      label: etiquetaMes(year, month),
      cells,
    }
  })
})

const historialMesesVisibles = computed(() => historialPorMes.value.slice(0, mesesHistorialVisibles.value))
const hayMasMeses = computed(() => mesesHistorialVisibles.value < historialPorMes.value.length)

const verMasMeses = () => {
  mesesHistorialVisibles.value = Math.min(mesesHistorialVisibles.value + 2, historialPorMes.value.length)
}

const verMenosMeses = () => {
  mesesHistorialVisibles.value = 2
}

const seleccionarDiaHistorial = (cell) => {
  if (!cell || cell.empty) return
  diaSeleccionadoKey.value = cell.key
  mostrandoDetalleDia.value = true
}

const volverAlCalendario = () => {
  mostrandoDetalleDia.value = false
}

const diaSeleccionadoKeyEfectivo = computed(() => {
  if (diaSeleccionadoKey.value) return diaSeleccionadoKey.value

  for (const mes of historialMesesVisibles.value) {
    const conPartidos = mes.cells.find((c) => !c.empty && Array.isArray(c.partidos) && c.partidos.length > 0)
    if (conPartidos) return conPartidos.key
  }

  for (const mes of historialMesesVisibles.value) {
    const primerDia = mes.cells.find((c) => !c.empty)
    if (primerDia) return primerDia.key
  }

  return ''
})

const diaSeleccionadoLabel = computed(() => {
  const key = diaSeleccionadoKeyEfectivo.value
  if (!key) return 'Sin día seleccionado'
  const [year, month, day] = key.split('-').map(Number)
  const date = new Date(year, (month || 1) - 1, day || 1)
  return new Intl.DateTimeFormat('es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(date)
})

const diaSeleccionadoPartidos = computed(() => {
  const key = diaSeleccionadoKeyEfectivo.value
  if (!key) return []

  for (const mes of historialPorMes.value) {
    const match = mes.cells.find((c) => c.key === key)
    if (match && !match.empty) return match.partidos || []
  }

  return []
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

</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <div v-if="loading" class="bg-white rounded-xl shadow p-6 text-center" role="status" aria-live="polite">
      <div class="inline-block animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
      <p class="text-gray-600 mt-3">Cargando perfil...</p>
    </div>

    <div v-else class="space-y-6">
      <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg" role="alert">{{ error }}</div>
      <div v-if="ok" class="bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg" role="status" aria-live="polite">{{ ok }}</div>

      <section class="bg-white rounded-xl shadow">
        <button type="button" @click="toggleSection('ficha')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-gray-800">Ficha de jugador</h2>
            <p class="text-xs text-slate-500 truncate">{{ tendenciaPreferidaLabel }} · {{ posicionPreferidaLabel }}</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.ficha ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <transition name="accordion">
        <div v-if="expandedSections.ficha" class="px-4 pb-4 space-y-3 border-t border-slate-100 overflow-hidden">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3">
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">Nombre</label>
              <input v-model="form.nombre" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
            </div>
          </div>

          <div>
            <button @click="guardarInformacionBasica" :disabled="saving" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition disabled:opacity-60">
              {{ saving ? 'Guardando...' : 'Guardar ficha' }}
            </button>
          </div>

          <div v-if="!playerProfile && !editandoFutbolista" class="text-center py-8">
            <p class="text-gray-600 mb-4">Aún no has completado tu perfil futbolístico.</p>
            <button
              @click="editandoFutbolista = true"
              class="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg font-medium transition"
            >
              Configurar ficha de jugador
            </button>
          </div>

          <div v-else class="space-y-3">
            <div v-if="mostrarNivelVisible" class="rounded-2xl border border-slate-200 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-700 p-4 text-white shadow-sm">
              <div class="flex flex-wrap items-start justify-between gap-3 mb-3">
                <div>
                  <p class="text-[11px] uppercase tracking-[0.2em] text-slate-300 font-semibold">Nivel deportivo</p>
                  <div class="flex items-center gap-2">
                    <TierIcon :tier="tierActual" :size="22" />
                    <p class="text-2xl md:text-3xl font-bold leading-tight">{{ tierActual }}</p>
                  </div>
                </div>
                <div class="text-right">
                  <p class="text-[11px] uppercase tracking-[0.16em] text-slate-300 font-semibold">Nivel 1-10</p>
                  <p class="text-sm md:text-base font-semibold text-slate-100">{{ nivelEscalado }} / 10</p>
                </div>
              </div>

              <div class="space-y-3">
                <div class="grid grid-cols-2 gap-2 md:grid-cols-4">
                  <div
                    v-for="rango in rangosNivel"
                    :key="rango.tier"
                    class="rounded-xl border p-3 transition-all duration-300"
                    :class="tierActual === rango.tier
                      ? `border-white/20 bg-gradient-to-br ${rango.gradient} text-slate-950 shadow-lg shadow-black/20`
                      : 'border-white/10 bg-white/10 text-slate-200 opacity-70'"
                  >
                    <div class="flex items-center justify-between gap-2 mb-2">
                      <TierIcon :tier="rango.tier" :size="18" />
                      <span class="text-[10px] font-semibold uppercase tracking-[0.16em]">{{ rango.min }}-{{ rango.max }}</span>
                    </div>
                    <p class="text-sm font-bold leading-none">{{ rango.label }}</p>
                    <p class="mt-1 text-[11px] leading-tight opacity-90">Nivel deportivo de {{ rango.min }} a {{ rango.max }}</p>
                  </div>
                </div>

                <div class="rounded-xl border border-white/10 bg-white/5 p-3">
                  <div class="flex items-center justify-between text-xs text-slate-300 mb-2">
                    <span>Nivel visible actual</span>
                    <span>{{ nivelVisibleBase.toFixed(2) }} / {{ nivelVisibleMaximo }}</span>
                  </div>
                  <div class="grid grid-cols-10 gap-1.5" role="progressbar" aria-valuemin="1" aria-valuemax="10" :aria-valuenow="nivelEscalado" :aria-label="`Nivel deportivo ${tierActual}, nivel ${nivelEscalado} de 10`">
                    <div
                      v-for="paso in 10"
                      :key="paso"
                      class="h-3 rounded-full transition-all duration-300"
                      :class="paso <= nivelEscalado ? 'bg-gradient-to-r from-emerald-400 to-cyan-400' : 'bg-white/15'"
                    ></div>
                  </div>
                </div>
              </div>

              <p class="mt-3 text-sm text-slate-200">
                {{ tierActual }} · datos suficientes para mostrar tu evolución visible.
              </p>
            </div>

            <div v-else class="rounded-2xl border border-amber-200 bg-amber-50 p-4 shadow-sm">
              <p class="text-sm font-semibold text-amber-900">Aún no hay suficiente certeza para mostrar tu nivel.</p>
              <p class="mt-1 text-sm text-amber-800">Debe bajar un poco más tu sigma para mostrar la barra deportiva.</p>
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
                  {{ saving ? 'Guardando...' : 'Guardar preferencias' }}
                </button>
              </div>
            </div>
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

        <div v-else>
          <transition name="history-switch" mode="out-in">
          <div v-if="!mostrandoDetalleDia" key="calendar" class="space-y-3">
            <div class="flex items-center justify-between gap-2 text-xs text-slate-500">
              <span>Vista mensual</span>
              <span>{{ historialMesesVisibles.length }} / {{ historialPorMes.length }} meses</span>
            </div>

            <div class="grid grid-cols-1 xl:grid-cols-2 gap-3">
              <article
                v-for="mes in historialMesesVisibles"
                :key="mes.key"
                class="rounded-xl border border-slate-200 bg-slate-50 p-3"
              >
                <h4 class="text-sm font-semibold text-slate-800 mb-2 capitalize">{{ mes.label }}</h4>
                <div class="grid grid-cols-7 gap-1 mb-1">
                  <span v-for="dia in diasSemana" :key="`${mes.key}-${dia}`" class="text-[10px] font-semibold text-slate-500 text-center">{{ dia }}</span>
                </div>
                <div class="grid grid-cols-7 gap-1">
                  <div
                    v-for="cell in mes.cells"
                    :key="cell.key"
                    class="calendar-cell"
                    :class="[
                      cell.empty ? 'calendar-cell-empty' : claseDiaCalendario(cell.partidos, cell.key),
                      !cell.empty ? 'calendar-cell-clickable' : '',
                      !cell.empty && cell.key === diaSeleccionadoKeyEfectivo ? 'calendar-cell-selected' : ''
                    ]"
                    :title="cell.empty ? '' : `${cell.partidos.length} partidos`"
                    @click="seleccionarDiaHistorial(cell)"
                    @keydown.enter.prevent="seleccionarDiaHistorial(cell)"
                    @keydown.space.prevent="seleccionarDiaHistorial(cell)"
                    :tabindex="cell.empty ? -1 : 0"
                  >
                    <template v-if="!cell.empty">
                      <span :class="['text-[11px] font-semibold leading-none', cell.partidos.length ? 'calendar-day-number-hit' : 'calendar-day-number-idle']">{{ cell.day }}</span>
                      <span v-if="cell.partidos.length" class="text-[9px] mt-0.5 font-semibold calendar-day-count-hit">({{ cell.partidos.length }})</span>
                    </template>
                  </div>
                </div>
              </article>
            </div>

            <div class="flex gap-2">
              <button
                v-if="hayMasMeses"
                type="button"
                @click="verMasMeses"
                class="flex-1 rounded-lg border border-slate-300 bg-white text-slate-700 text-sm font-semibold py-2 hover:bg-slate-100 transition"
              >
                Ver más meses
              </button>
              <button
                v-if="mesesHistorialVisibles > 2"
                type="button"
                @click="verMenosMeses"
                class="flex-1 rounded-lg border border-slate-300 bg-white text-slate-700 text-sm font-semibold py-2 hover:bg-slate-100 transition"
              >
                Ver menos meses
              </button>
            </div>
          </div>

          <section v-else-if="diaSeleccionadoKeyEfectivo" key="detail" class="rounded-xl border border-slate-200 bg-white p-3 space-y-2">
            <div class="flex items-center justify-between gap-2 mb-2">
              <h5 class="text-sm font-semibold text-slate-800 capitalize">{{ diaSeleccionadoLabel }}</h5>
              <span class="text-xs text-slate-500">{{ diaSeleccionadoPartidos.length }} partidos</span>
            </div>

            <div v-if="diaSeleccionadoPartidos.length === 0" class="text-xs text-slate-500">
              No hay partidos registrados en este día.
            </div>

            <div v-else class="space-y-2">
              <article
                v-for="partido in diaSeleccionadoPartidos"
                :key="`dia-${partido.id}`"
                :class="['rounded-lg border px-3 py-2.5 flex items-center justify-between gap-3', claseBannerResultado(partido.resultadoParaUsuario)]"
              >
                <div class="min-w-0 flex-1">
                  <p class="font-semibold truncate">{{ partido.lugar }}</p>
                  <p class="text-xs md:text-sm opacity-90 truncate">{{ formatearFecha(partido.fecha) }}</p>
                </div>
                <div class="flex items-center gap-2 shrink-0">
                  <div v-if="mostrarAmbosResultados(partido)" class="text-right leading-tight">
                    <p class="text-[20px] md:text-xs font-semibold opacity-95">{{ resultadoBaseLabel(partido) }}</p>
                    
                  </div>
                  <p v-else class="text-lg md:text-xl font-extrabold leading-none">{{ marcadorEntero(partido.golesEquipoA) }} - {{ marcadorEntero(partido.golesEquipoB) }}</p>
                  <span class="px-2 py-0.5 rounded-full text-[10px] md:text-[11px] font-semibold bg-slate-300 text-slate-900 border border-slate-400">{{ formatearResultado(partido.resultadoParaUsuario) }}</span>
                </div>
              </article>
            </div>

            <button
              type="button"
              @click="volverAlCalendario"
              class="w-full rounded-lg border border-slate-300 bg-white text-slate-700 text-sm font-semibold py-2 hover:bg-slate-100 transition"
            >
              Volver al calendario
            </button>
          </section>
          </transition>
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

.calendar-cell {
  min-height: 2.15rem;
  border-radius: 0.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
}

.calendar-cell-empty {
  background: transparent;
}

.calendar-cell-clickable {
  cursor: pointer;
  transition: transform 120ms ease, box-shadow 120ms ease, filter 120ms ease;
}

.calendar-cell-clickable:hover {
  filter: brightness(1.04);
  transform: translateY(-1px);
}

.calendar-cell-clickable:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.45);
}

.calendar-cell-selected {
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.45);
}

.calendar-day-empty {
  background: rgba(148, 163, 184, 0.1);
  border-color: rgba(148, 163, 184, 0.2);
  color: #64748b;
}

.calendar-day-with-match {
  background: rgba(147, 197, 253, 0.42);
  border-color: rgba(96, 165, 250, 0.88);
  color: #ffffff;
}

.calendar-day-today {
  background: rgba(151, 240, 125, 0.46);
  border-color: rgba(151, 240, 125, 0.95);
  color: #ffffff;
}

.calendar-day-today-with-match {
  background: linear-gradient(90deg, rgba(151, 240, 125, 0.6) 0 50%, rgba(147, 197, 253, 0.56) 50% 100%);
  border-color: rgba(151, 240, 125, 0.95);
  color: #ffffff;
}

.calendar-day-number-hit {
  color: #ffffff;
}

.calendar-day-number-idle {
  color: #334155;
}

.calendar-day-count-hit {
  color: #ffffff;
}

.history-switch-enter-active,
.history-switch-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.history-switch-enter-from,
.history-switch-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
