<script setup>
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import apiService from '../services/apiService'
import { formatDateTimeEs } from '../utils/dateFormat'
import playerProfileService from '../services/playerProfileService'
import partidoService from '../services/partidoService'

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
  bio: '',
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
    form.value.bio = resumenData?.bio || ''
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
    await apiService.upsertPerfil(form.value.nombre.trim(), authStore.user?.email || resumen.value?.email, form.value.bio || '')
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
  try {
    await playerProfileService.guardarMiPerfil({
      attributes: {
        goalkeeper: preferenciasFutbolista.value.posicionPreferida === 'PORTERO' || preferenciasFutbolista.value.playStyle === 'G',
        playStyle: preferenciasFutbolista.value.playStyle || 'A',
        posicionPreferida: preferenciasFutbolista.value.posicionPreferida || 'MEDIOCAMPISTA',
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
  if ((form.value.nombre || '').trim()) partes.push('nombre configurado')
  if ((form.value.bio || '').trim()) partes.push('bio configurada')
  return partes.length ? partes.join(' · ') : 'Completa tus datos básicos'
})

const resumenEstadisticas = computed(() => {
  return `${partidosJugados.value} partidos jugados · ${historialPartidos.value.length} en historial`
})

const resumenPerfilFutbolista = computed(() => {
  if (!playerProfile.value?.attributes && !editandoFutbolista.value) return 'Configura tu perfil futbolista'
  return `${tendenciaPreferidaLabel.value} · ${posicionPreferidaLabel.value}`
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

const nivelVisibleTexto = computed(() => {
  const raw = Number(resumen.value?.nivelVisible)
  if (!Number.isFinite(raw)) return '0.00'
  return raw.toFixed(2)
})

const nivelVisibleRaw = computed(() => {
  const raw = Number(resumen.value?.nivelVisible)
  if (!Number.isFinite(raw)) return 0
  return Math.max(0, raw)
})

const nivelBase = computed(() => Math.floor(nivelVisibleRaw.value))
const siguienteNivel = computed(() => nivelBase.value + 1)
const nivelProgresoPct = computed(() => {
  const fraccion = nivelVisibleRaw.value - nivelBase.value
  const pct = Math.round(fraccion * 100)
  return Math.max(0, Math.min(100, pct))
})

const puntosParaSiguienteNivel = computed(() => {
  const diff = siguienteNivel.value - nivelVisibleRaw.value
  return Math.max(0, diff).toFixed(2)
})

const fiabilidadLabelTexto = computed(() => {
  return resumen.value?.fiabilidadLabel || 'MEDIA'
})

const confianzaNivelTexto = computed(() => {
  if (fiabilidadLabelTexto.value === 'ALTA') return 'Muy estable'
  if (fiabilidadLabelTexto.value === 'BAJA') return 'En ajuste'
  return 'Estable'
})

const fiabilidadLabelClase = computed(() => {
  if (fiabilidadLabelTexto.value === 'ALTA') return 'bg-emerald-100 text-emerald-800 border-emerald-200'
  if (fiabilidadLabelTexto.value === 'BAJA') return 'bg-rose-100 text-rose-800 border-rose-200'
  return 'bg-amber-100 text-amber-800 border-amber-200'
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
        <button type="button" @click="toggleSection('basic')" class="w-full px-4 py-3 flex items-center justify-between text-left">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-gray-800">Información básica</h2>
            <p class="text-xs text-slate-500 truncate">{{ resumenInfoBasica }}</p>
          </div>
          <svg class="w-5 h-5 text-slate-500 transition-transform" :class="expandedSections.basic ? 'rotate-180' : ''" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
        </button>
        <transition name="accordion">
        <div v-if="expandedSections.basic" class="px-4 pb-4 space-y-3 border-t border-slate-100 overflow-hidden">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3 mt-3">
            <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 md:col-span-2">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-slate-600 font-semibold">Nivel actual</p>
                <span class="inline-flex items-center rounded-full border border-slate-300 bg-white px-2 py-0.5 text-xs font-semibold text-slate-700">
                  Nivel {{ nivelBase }}
                </span>
              </div>
              <p class="text-2xl font-bold text-slate-900 mt-1">{{ nivelVisibleTexto }}</p>
              <div class="mt-2">
                <div class="h-2.5 w-full rounded-full bg-slate-200 overflow-hidden">
                  <div class="h-full rounded-full bg-lime-500 transition-all duration-500" :style="{ width: `${nivelProgresoPct}%` }"></div>
                </div>
                <div class="mt-1 flex items-center justify-between text-[11px] text-slate-500">
                  <span>Nivel {{ nivelBase }}</span>
                  <span>{{ nivelProgresoPct }}%</span>
                  <span>Nivel {{ siguienteNivel }}</span>
                </div>
              </div>
              <p class="text-[11px] text-slate-500 mt-2">
                Te faltan {{ puntosParaSiguienteNivel }} puntos para subir al nivel {{ siguienteNivel }}.
              </p>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <p class="text-xs text-slate-600 font-semibold">Confianza del nivel</p>
              <span :class="['inline-flex mt-1 items-center rounded-full border px-2.5 py-1 text-xs font-semibold', fiabilidadLabelClase]">
                {{ confianzaNivelTexto }}
              </span>
              <p class="text-[11px] text-slate-500 mt-1">Tu nivel será más preciso cuando tengas más actividad.</p>
              <p class="text-[11px] text-slate-600 mt-2 font-semibold">Cómo mejorarla:</p>
              <ul class="text-[11px] text-slate-500 mt-1 space-y-0.5 list-disc pl-4">
                <li>Juega partidos completos con regularidad.</li>
                <li>Vota al final de cada partido.</li>
                <li>Evita ausencias y abandonos.</li>
              </ul>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 md:col-span-3">
              <p class="text-xs text-slate-600 font-semibold">Preferencia de juego</p>
              <p class="text-base font-bold text-slate-900">{{ tendenciaPreferidaLabel }} · {{ posicionPreferidaLabel }}</p>
              <p class="text-[11px] text-slate-500 mt-1">Nos ayuda a armar equipos más equilibrados</p>
            </div>
          </div>

          <p class="rounded-lg border border-indigo-200 bg-indigo-50 px-3 py-2 text-xs text-indigo-900">
            Este bloque te resume de forma rápida cómo va tu progresión en la app.
          </p>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">Nombre</label>
              <input v-model="form.nombre" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">Bio (opcional)</label>
              <textarea
                v-model="form.bio"
                maxlength="400"
                rows="3"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                placeholder="Cuéntanos brevemente cómo juegas"
              />
              <p class="text-[11px] text-slate-500 mt-1">{{ (form.bio || '').length }}/400</p>
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
                  <p class="text-lg md:text-xl font-extrabold leading-none">{{ marcadorEntero(partido.golesEquipoA) }} - {{ marcadorEntero(partido.golesEquipoB) }}</p>
                  <span class="px-2 py-0.5 rounded-full text-[10px] md:text-[11px] font-semibold bg-white/20 text-white border border-white/40">{{ formatearResultado(partido.resultadoParaUsuario) }}</span>
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
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div class="bg-white border-2 border-blue-300 rounded-xl p-4 text-center shadow-sm">
              <p class="text-xs md:text-sm text-slate-700 font-semibold mb-1">Tendencia</p>
              <p class="text-2xl md:text-3xl font-bold text-slate-900">
                {{ tendenciaPreferidaLabel }}
              </p>
            </div>
            <div class="bg-white border-2 border-violet-300 rounded-xl p-4 text-center shadow-sm">
              <p class="text-xs md:text-sm text-slate-700 font-semibold mb-1">Posición preferida</p>
              <p class="text-2xl md:text-3xl font-bold text-slate-900">
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

          <p class="text-xs text-slate-600">
            Estas preferencias se usan para balancear roles en los equipos, no para calcular tu nivel operativo.
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
