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
const mesesHistorialVisibles = ref(2)
const diaSeleccionadoKey = ref('')
const mostrandoDetalleDia = ref(false)
const resumenVotacion = ref({
  vecesDiferencial: 0,
  valoracionesPositivas: 0,
  valoracionesNegativas: 0,
})

const expandedSections = ref({
  basic: true,
  futbolista: false,
  stats: true,
})

const toggleSection = (key) => {
  expandedSections.value[key] = !expandedSections.value[key]
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

const nivelVisibleTexto = computed(() => {
  const raw = Number(usuario.value?.nivelVisible)
  if (!Number.isFinite(raw)) return '0.00'
  return raw.toFixed(2)
})

const fiabilidadLabelTexto = computed(() => {
  return usuario.value?.fiabilidadLabel || 'MEDIA'
})

const fiabilidadLabelClase = computed(() => {
  if (fiabilidadLabelTexto.value === 'ALTA') return 'bg-emerald-100 text-emerald-800 border-emerald-200'
  if (fiabilidadLabelTexto.value === 'BAJA') return 'bg-rose-100 text-rose-800 border-rose-200'
  return 'bg-amber-100 text-amber-800 border-amber-200'
})

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
  <div class="max-w-6xl mx-auto space-y-3">

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
            <p class="text-xs text-slate-500">Nombre, bio y métricas visibles</p>
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
              <p class="text-xs text-slate-600 font-semibold">Nivel visible</p>
              <p class="text-base font-bold text-slate-900">{{ nivelVisibleTexto }}</p>
            </div>
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3">
              <p class="text-xs text-slate-600 font-semibold">Bio</p>
              <p class="text-sm font-medium text-slate-900">{{ usuario?.bio || 'Sin bio' }}</p>
            </div>
            <div class="bg-slate-50 border border-slate-200 rounded-lg p-3 md:col-span-3">
              <p class="text-xs text-slate-600 font-semibold">Fiabilidad del perfil</p>
              <span :class="['inline-flex mt-1 items-center rounded-full border px-2.5 py-1 text-xs font-semibold', fiabilidadLabelClase]">
                {{ fiabilidadLabelTexto }}
              </span>
            </div>
          </div>

          <p class="rounded-lg border border-indigo-200 bg-indigo-50 px-3 py-2 text-xs text-indigo-900 mt-3">
            El nivel visible y la fiabilidad se calculan con datos reales de partidos y votaciones válidas.
          </p>
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

          <div v-else class="mt-3">
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
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
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
