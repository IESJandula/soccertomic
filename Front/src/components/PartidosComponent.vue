<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePartidoStore } from '../stores/partido'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import partidoService from '../services/partidoService'
import BaseButton from './ui/BaseButton.vue'
import StatusBadge from './ui/StatusBadge.vue'
import AppIcon from './ui/AppIcon.vue'
import { formatDateTimeEs } from '../utils/dateFormat'

const router = useRouter()
const route = useRoute()
const partidoStore = usePartidoStore()
const uiStore = useUiStore()
const authStore = useAuthStore()

const tabActiva = ref('mis-partidos') // 'mis-partidos' | 'publicos'
const misPartidos = ref([])
const partidosPublicos = ref([])
const loading = ref(false)
const procesando = ref(null)
const filtroPublicos = ref('todos')
const vistaMisPartidos = ref('semanal')
const vistaPartidosPublicos = ref('semanal')
const diaCalendarioMis = ref('')
const diaCalendarioPublicos = ref('')
const mostrandoDetalleDiaMis = ref(false)
const mostrandoDetalleDiaPublicos = ref(false)
const loadError = ref('')
const teamSideByPartido = ref({})
const enrolledByPartido = ref({})
const paidByPartido = ref({})
const diasSemana = ['L', 'M', 'X', 'J', 'V', 'S', 'D']

const startOfDay = (dateLike) => {
  const date = new Date(dateLike)
  date.setHours(0, 0, 0, 0)
  return date
}

const addDays = (dateLike, days) => {
  const date = new Date(dateLike)
  date.setDate(date.getDate() + days)
  return date
}

const startOfWeekMonday = (dateLike) => {
  const date = startOfDay(dateLike)
  const day = date.getDay()
  const diff = day === 0 ? -6 : 1 - day
  return addDays(date, diff)
}

const toDayKey = (dateLike) => {
  const date = new Date(dateLike)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const hoyKey = toDayKey(new Date())

const sortByFechaAsc = (partidos = []) => {
  return [...(partidos || [])].sort((a, b) => new Date(a.fecha) - new Date(b.fecha))
}

const rangoSemanal = computed(() => {
  const inicioSemanaActual = startOfWeekMonday(new Date())
  const inicioVentana = addDays(inicioSemanaActual, -7)
  const finVentanaExclusivo = addDays(inicioSemanaActual, 14)
  return {
    inicio: inicioVentana,
    finExclusivo: finVentanaExclusivo,
  }
})

const estaEnVentanaSemanal = (fechaLike) => {
  if (!fechaLike) return false
  const fecha = new Date(fechaLike)
  return fecha >= rangoSemanal.value.inicio && fecha < rangoSemanal.value.finExclusivo
}

const misPartidosOrdenados = computed(() => sortByFechaAsc(misPartidos.value))
const partidosPublicosOrdenados = computed(() => sortByFechaAsc(partidosPublicosFiltrados.value))

const misPartidosVistaSemanal = computed(() => misPartidosOrdenados.value.filter((p) => estaEnVentanaSemanal(p.fecha)))
const partidosPublicosVistaSemanal = computed(() => partidosPublicosOrdenados.value.filter((p) => estaEnVentanaSemanal(p.fecha)))

const monthLabelFormatter = new Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' })

const crearMesesCalendario = (partidos = []) => {
  const mapa = new Map()

  for (const partido of partidos || []) {
    if (!partido?.fecha) continue
    const fecha = new Date(partido.fecha)
    const monthKey = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`
    const dayKey = toDayKey(fecha)

    if (!mapa.has(monthKey)) {
      mapa.set(monthKey, {
        key: monthKey,
        year: fecha.getFullYear(),
        month: fecha.getMonth(),
        days: new Map(),
      })
    }

    const mes = mapa.get(monthKey)
    if (!mes.days.has(dayKey)) mes.days.set(dayKey, [])
    mes.days.get(dayKey).push(partido)
  }

  const meses = Array.from(mapa.values())
    .sort((a, b) => (a.year - b.year) || (a.month - b.month))
    .map((mes) => {
      const firstDay = new Date(mes.year, mes.month, 1)
      const lastDay = new Date(mes.year, mes.month + 1, 0)
      const leading = (firstDay.getDay() + 6) % 7
      const cells = []

      for (let i = 0; i < leading; i += 1) {
        cells.push({ key: `${mes.key}-empty-${i}`, empty: true, day: null, partidos: [] })
      }

      for (let day = 1; day <= lastDay.getDate(); day += 1) {
        const current = new Date(mes.year, mes.month, day)
        const key = toDayKey(current)
        const partidosDia = mes.days.get(key) || []
        cells.push({ key, empty: false, day, partidos: partidosDia })
      }

      return {
        key: mes.key,
        label: monthLabelFormatter.format(new Date(mes.year, mes.month, 1)),
        cells,
      }
    })

  return meses
}

const calendarioMisMeses = computed(() => crearMesesCalendario(misPartidosOrdenados.value))
const calendarioPublicosMeses = computed(() => crearMesesCalendario(partidosPublicosOrdenados.value))

const partidosPorDiaMis = computed(() => {
  const mapa = {}
  for (const partido of misPartidosOrdenados.value) {
    if (!partido?.fecha) continue
    const key = toDayKey(partido.fecha)
    if (!mapa[key]) mapa[key] = []
    mapa[key].push(partido)
  }
  return mapa
})

const partidosPublicosFiltrados = computed(() => {
  const now = new Date()
  const sorted = [...partidosPublicos.value].sort((a, b) => new Date(a.fecha) - new Date(b.fecha))

  if (filtroPublicos.value === 'proximos') {
    return sorted.filter((p) => new Date(p.fecha) > now)
  }

  return sorted
})

const partidosPorDiaPublicos = computed(() => {
  const mapa = {}
  for (const partido of partidosPublicosOrdenados.value) {
    if (!partido?.fecha) continue
    const key = toDayKey(partido.fecha)
    if (!mapa[key]) mapa[key] = []
    mapa[key].push(partido)
  }
  return mapa
})

const diaMisSeleccionadoEfectivo = computed(() => {
  if (diaCalendarioMis.value && partidosPorDiaMis.value[diaCalendarioMis.value]) return diaCalendarioMis.value
  return Object.keys(partidosPorDiaMis.value)[0] || ''
})

const diaPublicosSeleccionadoEfectivo = computed(() => {
  if (diaCalendarioPublicos.value && partidosPorDiaPublicos.value[diaCalendarioPublicos.value]) return diaCalendarioPublicos.value
  return Object.keys(partidosPorDiaPublicos.value)[0] || ''
})

const partidosDiaMis = computed(() => partidosPorDiaMis.value[diaMisSeleccionadoEfectivo.value] || [])
const partidosDiaPublicos = computed(() => partidosPorDiaPublicos.value[diaPublicosSeleccionadoEfectivo.value] || [])

const normalizeId = (value) => String(value ?? '')
const currentUserId = computed(() => normalizeId(authStore.user?.id))

const extractUserId = (entity) => {
  if (!entity) return ''
  return normalizeId(
    entity.id ?? entity.usuario?.id ?? entity.user?.id ?? entity.organizadorId ?? entity.usuarioId
  )
}

const sameAsCurrentUser = (idLike) => {
  if (!currentUserId.value) return false
  return normalizeId(idLike) === currentUserId.value
}

const totalJugadores = (partido) => partido.totalJugadores || 0
const cupoTotal = (partido) => (partido.jugadoresPorEquipo || 0) * 2
const disponible = (partido) => totalJugadores(partido) < cupoTotal(partido)

const esOwner = (partido) => {
  const ownerId = partido?.owner?.id ?? partido?.creador?.id
  return sameAsCurrentUser(ownerId)
}

const esOrganizador = (partido) => {
  if (!currentUserId.value) return false
  return (partido.organizadores || []).some((o) => {
    const organizerId = extractUserId(o)
    return organizerId === currentUserId.value
  })
}

const rolIcono = (partido) => {
  if (esOwner(partido)) return { icon: 'crown', label: 'Responsable principal' }
  if (esOrganizador(partido)) return { icon: 'crown', label: 'Organización' }
  return null
}

const obtenerNombreOrganizador = (partido) => {
  // Prioridad: owner > creador > organizador OWNER > primer organizador > fallback
  if (partido?.owner?.nombre) return partido.owner.nombre
  if (partido?.creador?.nombre) return partido.creador.nombre

  const organizadores = partido?.organizadores || []
  const ownerOrg = organizadores.find((o) => o.rol === 'OWNER')
  if (ownerOrg?.usuario?.nombre) return ownerOrg.usuario.nombre

  if (organizadores.length > 0 && organizadores[0]?.usuario?.nombre) {
    return organizadores[0].usuario.nombre
  }

  return '—'
}

const isReservationState = (estado) => estado === 'CONFIRMADO' || estado === 'RESERVADA'
const isInGameStatus = (estado) => estado === 'EN_JUEGO' || estado === 'EN_CURSO'
const isOrganizingStatus = (estado) => estado === 'CREADO' || estado === 'BORRADOR'

const isPaidReservation = (partido) => {
  if (!partido) return false
  const key = String(partido.id || '')
  if (key && paidByPartido.value[key] !== undefined) return paidByPartido.value[key]
  return Boolean(
    partido.pagado === true ||
    partido.pagada === true ||
    partido.pistaPagada === true ||
    partido.reservaPagada === true
  )
}

const isReservedStatus = (partido) => isReservationState(partido?.estado) && isPaidReservation(partido)

const seleccionarDiaCalendarioMis = (cell) => {
  if (!cell || cell.empty || !cell.partidos?.length) return
  diaCalendarioMis.value = cell.key
  mostrandoDetalleDiaMis.value = true
}

const seleccionarDiaCalendarioPublicos = (cell) => {
  if (!cell || cell.empty || !cell.partidos?.length) return
  diaCalendarioPublicos.value = cell.key
  mostrandoDetalleDiaPublicos.value = true
}

const volverCalendarioMis = () => {
  mostrandoDetalleDiaMis.value = false
}

const volverCalendarioPublicos = () => {
  mostrandoDetalleDiaPublicos.value = false
}

const claseDiaCalendario = (cell) => {
  if (!cell || cell.empty) return 'calendar-cell-empty'
  const isToday = cell.key === hoyKey
  const hasMatch = Boolean(cell.partidos?.length)

  if (isToday && hasMatch) return 'calendar-cell-today-with-match'
  if (isToday) return 'calendar-cell-today'
  return hasMatch ? 'calendar-cell-with-match' : 'calendar-cell-idle'
}

const etiquetaRangoSemanal = computed(() => {
  const f = new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: 'short' })
  const inicio = f.format(rangoSemanal.value.inicio)
  const fin = f.format(addDays(rangoSemanal.value.finExclusivo, -1))
  return `${inicio} - ${fin}`
})

const formatearDiaCalendario = (dayKey) => {
  if (!dayKey) return ''
  const date = new Date(`${dayKey}T00:00:00`)
  return new Intl.DateTimeFormat('es-ES', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
  }).format(date)
}

const cargarMisPartidos = async () => {
  loading.value = true
  loadError.value = ''
  try {
    await partidoStore.cargarMisPartidos()
    misPartidos.value = partidoStore.misPartidos || []
    void hidratarEquipoUsuario(misPartidos.value)

    if (partidoStore.error) {
      loadError.value = partidoStore.error
      misPartidos.value = []
      uiStore.showToast({ message: partidoStore.error, type: 'error' })
    }
  } catch (error) {
    loadError.value = error?.message || 'No se pudo cargar tus partidos.'
    misPartidos.value = []
    uiStore.showToast({ message: loadError.value, type: 'error' })
  } finally {
    loading.value = false
  }
}

const cargarPartidosPublicos = async () => {
  loading.value = true
  loadError.value = ''
  try {
    await partidoStore.cargarPartidosPublicos()
    partidosPublicos.value = partidoStore.partidosPublicos || []
    void hidratarEquipoUsuario(partidosPublicos.value)

    if (partidoStore.error) {
      loadError.value = partidoStore.error
      partidosPublicos.value = []
      uiStore.showToast({ message: partidoStore.error, type: 'error' })
    }
  } catch (error) {
    loadError.value = error?.message || 'No se pudo cargar partidos públicos.'
    partidosPublicos.value = []
    uiStore.showToast({ message: loadError.value, type: 'error' })
  } finally {
    loading.value = false
  }
}

const cambiarTab = async (tab) => {
  tabActiva.value = tab
  if (tab === 'publicos' && partidosPublicos.value.length === 0) {
    await cargarPartidosPublicos()
  }
}

const irAPartido = (id) => {
  router.push(`/dashboard/partidos/${id}`)
}

const puedeVerDetalle = (partido) => partido?.estado !== 'CANCELADO'

const irACrearPartido = () => {
  router.push('/dashboard/crear-partido')
}

const formatearFecha = (fecha) => formatDateTimeEs(fecha)

const cambiarVistaMis = (vista) => {
  vistaMisPartidos.value = vista
  mostrandoDetalleDiaMis.value = false
}

const cambiarVistaPublicos = (vista) => {
  vistaPartidosPublicos.value = vista
  mostrandoDetalleDiaPublicos.value = false
}

onMounted(async () => {
  if (route.query?.cancelado === '1') {
    uiStore.showToast({ message: 'El partido se canceló correctamente.', type: 'info' })
    const nextQuery = { ...route.query }
    delete nextQuery.cancelado
    router.replace({ path: route.path, query: nextQuery })
  }

  await cargarMisPartidos()
})


const statusLabel = (partido) => {
  const estado = partido?.estado
  if (estado === 'CANCELADO') return 'CANCELADO'
  if (estado === 'FINALIZADO') return 'FINALIZADO'
  if (isInGameStatus(estado)) return estado
  if (isReservedStatus(partido)) return estado
  if (isOrganizingStatus(estado)) return estado
  if (!disponible(partido)) return 'COMPLETO'
  return 'ABIERTO'
}

const cardAuraClass = (partido) => {
  const estado = partido?.estado
  if (estado === 'CANCELADO') return 'cancelled-aura'
  if (isReservedStatus(partido)) return 'reserved-aura'
  if (isInGameStatus(estado)) return 'in-game-aura'
  if ((esOrganizador(partido) || estaInscritoEnPartido(partido)) && estado !== 'FINALIZADO') return 'organizer-pulse'
  return ''
}

const showStatusIconInListCard = (partido) => {
  const estado = statusLabel(partido)
  return estado === 'FINALIZADO' || isInGameStatus(estado) || isReservedStatus(partido)
}

const teamPalette = {
  Blanco: { jersey: '#ffffff', onJersey: '#111827', border: 'rgba(17, 24, 39, 0.22)' },
  Negro: { jersey: '#050505', onJersey: '#f8fafc', border: 'rgba(248, 250, 252, 0.22)' },
  Rojo: { jersey: '#ef4444', onJersey: '#ffffff', border: 'rgba(255, 255, 255, 0.24)' },
  Azul: { jersey: '#2563eb', onJersey: '#ffffff', border: 'rgba(255, 255, 255, 0.24)' },
  Verde: { jersey: '#16a34a', onJersey: '#ffffff', border: 'rgba(255, 255, 255, 0.24)' },
  Amarillo: { jersey: '#facc15', onJersey: '#111827', border: 'rgba(17, 24, 39, 0.22)' },
  Naranja: { jersey: '#f97316', onJersey: '#ffffff', border: 'rgba(255, 255, 255, 0.24)' },
  Morado: { jersey: '#a855f7', onJersey: '#ffffff', border: 'rgba(255, 255, 255, 0.24)' },
}

const getTeamTone = (colorName) => teamPalette[colorName] || teamPalette.Blanco

const getTeamBlockStyle = (colorName) => {
  const tone = getTeamTone(colorName)
  return {
    backgroundColor: tone.jersey,
    color: tone.onJersey,
    borderColor: tone.border,
  }
}

const isInTeam = (team = []) => {
  if (!currentUserId.value || !Array.isArray(team)) return false
  return team.some((j) => extractUserId(j) === currentUserId.value)
}

const inferTeamAndEnrollment = (partido) => {
  const enA = isInTeam(partido?.equipoA)
  const enB = isInTeam(partido?.equipoB)
  const inscrito =
    partido?.estaInscrito === true ||
    enA ||
    enB ||
    (Array.isArray(partido?.jugadoresInscritos) && partido.jugadoresInscritos.some((j) => extractUserId(j) === currentUserId.value))

  return {
    team: enA ? 'A' : (enB ? 'B' : null),
    enrolled: inscrito,
  }
}

const hidratarEquipoUsuario = async (partidos = []) => {
  if (!currentUserId.value || !Array.isArray(partidos) || partidos.length === 0) return

  const pendientes = partidos
    .map((p) => p?.id)
    .filter((id) => id !== null && id !== undefined)
    .filter((id) => teamSideByPartido.value[String(id)] === undefined || enrolledByPartido.value[String(id)] === undefined)

  if (pendientes.length === 0) return

  await Promise.allSettled(
    pendientes.map(async (id) => {
      const detalle = await partidoService.obtenerDetallePartido(id)
      const inferred = inferTeamAndEnrollment(detalle)
      const key = String(id)
      teamSideByPartido.value[key] = inferred.team
      enrolledByPartido.value[key] = inferred.enrolled

      if (isReservationState(detalle?.estado) && paidByPartido.value[key] === undefined) {
        try {
          const estadoPago = await partidoService.obtenerEstadoPagoReserva(id)
          paidByPartido.value[key] = Boolean(estadoPago?.[currentUserId.value])
        } catch {
          paidByPartido.value[key] = false
        }
      }
    })
  )
}

const normalizeTeamCode = (value) => {
  const raw = String(value ?? '').trim().toUpperCase()
  if (!raw) return null
  if (['A', 'EQUIPO_A', 'TEAM_A', 'LOCAL'].includes(raw)) return 'A'
  if (['B', 'EQUIPO_B', 'TEAM_B', 'VISITANTE'].includes(raw)) return 'B'
  return null
}

const teamFromColor = (partido, colorLike) => {
  const color = String(colorLike ?? '').trim().toLowerCase()
  if (!color) return null
  if (String(partido?.colorEquipoA ?? '').trim().toLowerCase() === color) return 'A'
  if (String(partido?.colorEquipoB ?? '').trim().toLowerCase() === color) return 'B'
  return null
}

const estaInscritoEnPartido = (partido) => {
  if (!partido || !currentUserId.value) return false
  const key = String(partido.id || '')
  if (key && enrolledByPartido.value[key] !== undefined) return enrolledByPartido.value[key]
  if (partido.estaInscrito === true) return true
  if (isInTeam(partido.equipoA) || isInTeam(partido.equipoB)) return true
  if (Array.isArray(partido.jugadoresInscritos)) {
    return partido.jugadoresInscritos.some((j) => extractUserId(j) === currentUserId.value)
  }
  return false
}

const equipoDelUsuario = (partido) => {
  const key = String(partido?.id || '')
  if (key && teamSideByPartido.value[key] !== undefined) return teamSideByPartido.value[key]
  if (isInTeam(partido?.equipoA)) return 'A'
  if (isInTeam(partido?.equipoB)) return 'B'

  if (partido?.usuarioEnEquipoA === true || partido?.enEquipoA === true) return 'A'
  if (partido?.usuarioEnEquipoB === true || partido?.enEquipoB === true) return 'B'

  const directTeam =
    normalizeTeamCode(partido?.miEquipo) ||
    normalizeTeamCode(partido?.equipoUsuario) ||
    normalizeTeamCode(partido?.equipoDelUsuario) ||
    normalizeTeamCode(partido?.equipoAsignado)
  if (directTeam) return directTeam

  const directFromColor =
    teamFromColor(partido, partido?.miEquipo) ||
    teamFromColor(partido, partido?.equipoUsuario) ||
    teamFromColor(partido, partido?.equipoDelUsuario) ||
    teamFromColor(partido, partido?.equipoAsignado)
  if (directFromColor) return directFromColor

  if (Array.isArray(partido?.jugadoresInscritos) && currentUserId.value) {
    const current = partido.jugadoresInscritos.find((j) => extractUserId(j) === currentUserId.value)
    if (current) {
      const nestedTeam =
        normalizeTeamCode(current?.equipo) ||
        normalizeTeamCode(current?.team) ||
        normalizeTeamCode(current?.equipoAsignado)
      if (nestedTeam) return nestedTeam

      const nestedFromColor =
        teamFromColor(partido, current?.equipo) ||
        teamFromColor(partido, current?.team) ||
        teamFromColor(partido, current?.equipoAsignado)
      if (nestedFromColor) return nestedFromColor
    }
  }

  return null
}

const equipoBadgePosClass = (partido) => {
  const team = equipoDelUsuario(partido)
  if (team === 'A') return 'my-team-badge-a'
  if (team === 'B') return 'my-team-badge-b'
  return 'my-team-badge-center'
}

const equipoBadgeStateClass = (partido) => {
  const team = equipoDelUsuario(partido)
  return team === 'A' || team === 'B' ? 'my-team-badge-assigned' : 'my-team-badge-unassigned'
}

const equipoBadgeText = (partido) => {
  const team = equipoDelUsuario(partido)
  return team === 'A' || team === 'B' ? 'Tu equipo' : 'Sin equipo'
}

const abrirPublico = async (partido) => {
  procesando.value = `abrir-${partido.id}`
  const result = await partidoStore.abrirPartidoAlPublico(partido.id)
  if (result.success) {
    uiStore.showToast({ message: 'Partido abierto al público.', type: 'success' })
    await cargarMisPartidos()
    if (result.partido?.tipo === 'PUBLICO') {
      const index = partidosPublicos.value.findIndex(p => p.id === result.partido.id)
      if (index === -1) {
        partidosPublicos.value.push(result.partido)
      } else {
        partidosPublicos.value[index] = result.partido
      }
    }
  } else {
    uiStore.showToast({ message: result.message || 'No se pudo actualizar el partido.', type: 'error' })
  }
  procesando.value = null
}

const volverPrivado = async (partido) => {
  procesando.value = `privado-${partido.id}`
  const result = await partidoStore.volverPartidoAPrivado(partido.id)
  if (result.success) {
    uiStore.showToast({ message: 'Partido vuelto a privado.', type: 'success' })
    await cargarMisPartidos()
  } else {
    uiStore.showToast({ message: result.message || 'No se pudo actualizar el partido.', type: 'error' })
  }
  procesando.value = null
}

const eliminarPartido = async (partido) => {
  const accepted = await uiStore.askConfirm({
    title: 'Eliminar partido',
    message: 'Esta acción no se puede deshacer. ¿Deseas continuar?',
    confirmLabel: 'Eliminar',
    cancelLabel: 'Cancelar',
    variant: 'danger',
  })

  if (!accepted) return

  procesando.value = `eliminar-${partido.id}`
  try {
    await partidoService.eliminarPartido(partido.id)
    uiStore.showToast({ message: 'Partido eliminado correctamente.', type: 'success' })
    await cargarMisPartidos()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al eliminar partido.', type: 'error' })
  } finally {
    procesando.value = null
  }
}
</script>

<template>
  <div class="space-y-4 md:space-y-6">
    <!-- Header con tabs -->
    <section class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
      <!-- Tabs -->
      <div class="card-surface p-1 inline-flex self-start lg:self-auto gap-1 overflow-x-auto">
        <button
          @click="cambiarTab('mis-partidos')"
          :class="[
            'px-4 py-2 text-sm font-medium transition-all rounded-lg whitespace-nowrap',
            tabActiva === 'mis-partidos'
              ? 'text-[color:var(--color-secondary)] bg-[color:rgba(151,240,125,0.14)] shadow-sm'
              : 'text-slate-600 hover:text-slate-800 hover:bg-slate-50'
          ]"
          :aria-current="tabActiva === 'mis-partidos' ? 'page' : undefined"
        >
            <span class="flex items-center gap-1.5">
            <AppIcon name="soccer" :size="15" />
            <span>Mis partidos</span>
            <span 
              v-if="misPartidos.length > 0" 
              :class="[
                'ml-1.5 px-1.5 py-0.5 text-xs rounded-full',
                tabActiva === 'mis-partidos' ? 'bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)]' : 'bg-slate-200 text-slate-600'
              ]"
            >
              {{ misPartidos.length }}
            </span>
          </span>
        </button>
        <button
          @click="cambiarTab('publicos')"
          :class="[
            'px-4 py-2 text-sm font-medium transition-all rounded-lg whitespace-nowrap',
            tabActiva === 'publicos'
              ? 'text-[color:var(--color-secondary)] bg-[color:rgba(151,240,125,0.14)] shadow-sm'
              : 'text-slate-600 hover:text-slate-800 hover:bg-slate-50'
          ]"
          :aria-current="tabActiva === 'publicos' ? 'page' : undefined"
        >
          <span class="flex items-center gap-1.5">
            <AppIcon name="users" :size="15" />
            <span>Partidos públicos</span>
            <span 
              v-if="partidosPublicosFiltrados.length > 0 && tabActiva === 'publicos'" 
              :class="[
                'ml-1.5 px-1.5 py-0.5 text-xs rounded-full',
                tabActiva === 'publicos' ? 'bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)]' : 'bg-slate-200 text-slate-600'
              ]"
            >
              {{ partidosPublicosFiltrados.length }}
            </span>
          </span>
        </button>
      </div>

      <!-- Filtros para partidos públicos -->
      <div v-if="tabActiva === 'publicos'" class="flex gap-2 self-start lg:self-auto">
        <BaseButton :variant="filtroPublicos === 'todos' ? 'primary' : 'secondary'" size="sm" @click="filtroPublicos = 'todos'">Todos</BaseButton>
        <BaseButton :variant="filtroPublicos === 'proximos' ? 'primary' : 'secondary'" size="sm" @click="filtroPublicos = 'proximos'">Próximos</BaseButton>
      </div>
    </section>

    <!-- Vista: Mis Partidos -->
    <section v-if="tabActiva === 'mis-partidos'">
      <section v-if="loading" class="state-loading text-sm text-slate-600">
        Cargando tus partidos...
      </section>

      <section v-else-if="misPartidos.length === 0" class="state-empty">
        <p class="text-slate-700 font-medium">Aún no tienes partidos activos.</p>
        <p class="text-caption mt-1">{{ loadError || 'Puedes unirte a uno público o crear tu propio partido.' }}</p>
        <div class="mt-3 flex flex-wrap gap-2 justify-center">
          <BaseButton variant="secondary" @click="cambiarTab('publicos')">Ver partidos públicos</BaseButton>
          <BaseButton variant="primary" @click="irACrearPartido">Crear partido</BaseButton>
        </div>
      </section>

      <section v-else class="space-y-3">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <span class="text-xs text-slate-500 min-h-[1rem] max-w-full">
            {{ vistaMisPartidos === 'calendario' ? 'Toca un día para ver partidos' : `Partidos recientes (${etiquetaRangoSemanal})` }}
          </span>
          <div class="inline-flex w-full sm:w-auto rounded-lg border border-slate-200 bg-white p-1 gap-1">
            <button
              type="button"
              @click="cambiarVistaMis('semanal')"
              :class="[
                'flex-1 sm:flex-none px-3 py-1.5 rounded-md text-xs font-semibold transition',
                vistaMisPartidos === 'semanal' ? 'bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)]' : 'text-slate-600 hover:bg-slate-100'
              ]"
            >
              Partidos recientes
            </button>
            <button
              type="button"
              @click="cambiarVistaMis('calendario')"
              :class="[
                'flex-1 sm:flex-none px-3 py-1.5 rounded-md text-xs font-semibold transition',
                vistaMisPartidos === 'calendario' ? 'bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)]' : 'text-slate-600 hover:bg-slate-100'
              ]"
            >
              Calendario completo
            </button>
          </div>
        </div>

        <section v-if="vistaMisPartidos === 'calendario'" class="space-y-3">
          <div v-if="!mostrandoDetalleDiaMis && calendarioMisMeses.length === 0" class="state-empty">
            <p class="text-slate-700 font-medium">No hay partidos para mostrar en calendario.</p>
          </div>

          <div v-else-if="!mostrandoDetalleDiaMis" class="grid grid-cols-1 xl:grid-cols-2 gap-3">
            <article
              v-for="mes in calendarioMisMeses"
              :key="`mis-cal-${mes.key}`"
              class="rounded-xl border border-slate-200 bg-slate-50 p-3"
            >
              <h4 class="text-sm font-semibold text-slate-800 mb-2 capitalize">{{ mes.label }}</h4>
              <div class="grid grid-cols-7 gap-1 mb-1">
                <span v-for="dia in diasSemana" :key="`mis-cal-week-${mes.key}-${dia}`" class="text-[10px] font-semibold text-slate-500 text-center">{{ dia }}</span>
              </div>
              <div class="grid grid-cols-7 gap-1">
                <div
                  v-for="cell in mes.cells"
                  :key="`mis-cal-cell-${cell.key}`"
                  class="calendar-cell"
                  :class="[
                    claseDiaCalendario(cell),
                    !cell.empty && cell.partidos.length ? 'calendar-cell-clickable' : '',
                    !cell.empty && cell.key === diaMisSeleccionadoEfectivo ? 'calendar-cell-selected' : ''
                  ]"
                  @click="seleccionarDiaCalendarioMis(cell)"
                >
                  <template v-if="!cell.empty">
                    <span class="text-[11px] font-semibold leading-none">{{ cell.day }}</span>
                    <span v-if="cell.partidos.length" class="text-[10px] leading-none mt-0.5">({{ cell.partidos.length }})</span>
                  </template>
                </div>
              </div>
            </article>
          </div>

          <section v-else-if="diaMisSeleccionadoEfectivo" class="space-y-3">
            <div class="rounded-xl border border-slate-200 bg-white p-3 space-y-2">
              <div class="flex items-center justify-between gap-2">
                <h5 class="text-sm font-semibold text-slate-800 capitalize">Partidos del {{ formatearDiaCalendario(diaMisSeleccionadoEfectivo) }}</h5>
                <span class="text-xs text-slate-500">{{ partidosDiaMis.length }} partidos</span>
              </div>
              <BaseButton variant="secondary" size="sm" @click="volverCalendarioMis">Volver al calendario</BaseButton>
            </div>

            <section class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-3 md:gap-4">
              <article
                v-for="partido in partidosDiaMis"
                :key="`mis-dia-card-${partido.id}`"
                :class="[
                  'card-surface overflow-hidden flex flex-col relative',
                  cardAuraClass(partido)
                ]"
              >
                <div
                  v-if="rolIcono(partido)"
                  class="role-badge"
                  :title="rolIcono(partido).label"
                >
                  <AppIcon :name="rolIcono(partido).icon" :size="14" />
                </div>
                <div v-if="partido.estado === 'CANCELADO'" class="cancelled-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="cancelled-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M6 6l12 12M18 6L6 18" />
                  </svg>
                </div>
                <div v-if="isReservationState(partido.estado)" class="paid-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="paid-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M7 4.5h10a1.5 1.5 0 0 1 1.5 1.5V20l-6.5-3.8L5.5 20V6A1.5 1.5 0 0 1 7 4.5z" />
                  </svg>
                </div>
                <div v-if="isInGameStatus(partido.estado)" class="in-game-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="in-game-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M12 4a8 8 0 1 0 0 16a8 8 0 0 0 0-16M12 8v4l2.5 2" />
                  </svg>
                </div>
                <div v-if="partido.estado === 'FINALIZADO'" class="finalized-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="finalized-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M6 4v16M8 6h9l-2 3 2 3H8z" />
                  </svg>
                </div>
                <div class="flex h-24 relative teams-strip">
                  <div :style="getTeamBlockStyle(partido.colorEquipoA)" class="flex-1 flex items-center justify-center font-bold text-lg border-r relative">
                    {{ partido.colorEquipoA }}
                  </div>
                  <div class="w-10 shrink-0 flex items-center justify-center gap-0.5 text-sm font-extrabold shadow-sm bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)] vs-strip">
                    <span>V</span>
                    <span>S</span>
                  </div>
                  <div :style="getTeamBlockStyle(partido.colorEquipoB)" class="flex-1 flex items-center justify-center font-bold text-lg border-l relative">
                    {{ partido.colorEquipoB }}
                  </div>
                  <div
                    v-if="estaInscritoEnPartido(partido)"
                    :class="[
                      'my-team-badge-absolute',
                      equipoBadgePosClass(partido),
                      equipoBadgeStateClass(partido)
                    ]"
                  >
                    {{ equipoBadgeText(partido) }}
                  </div>
                </div>

                <div class="p-4 flex flex-col gap-3">
                  <div class="flex items-start justify-between gap-2">
                    <div>
                      <p class="text-sm font-semibold text-slate-800">{{ formatearFecha(partido.fecha) }}</p>
                      <p class="text-sm text-slate-600 inline-flex items-center gap-1.5"><AppIcon name="pin" :size="14" />{{ partido.lugar }}</p>
                    </div>
                    <StatusBadge :status="statusLabel(partido)" :show-icon="showStatusIconInListCard(partido)" />
                  </div>

                  <div class="flex items-center gap-2 text-xs text-slate-600">
                    <StatusBadge :status="partido.tipo" :show-icon="false" />
                    <span class="inline-flex items-center gap-1.5"><AppIcon name="users" :size="14" />{{ totalJugadores(partido) }}/{{ cupoTotal(partido) }} personas</span>
                  </div>

                  <p class="text-sm text-slate-600">Organiza: {{ obtenerNombreOrganizador(partido) }}</p>

                  <div class="grid grid-cols-2 gap-2">
                    <BaseButton :variant="puedeVerDetalle(partido) ? 'secondary' : 'danger'" block :disabled="!puedeVerDetalle(partido)" @click="irAPartido(partido.id)">
                      {{ puedeVerDetalle(partido) ? 'Ver' : 'Cancelado' }}
                    </BaseButton>

                    <BaseButton
                      v-if="esOrganizador(partido) && partido.tipo === 'PRIVADO' && partido.estado !== 'FINALIZADO' && partido.estado !== 'CANCELADO' && totalJugadores(partido) < cupoTotal(partido)"
                      variant="primary"
                      block
                      :loading="procesando === `abrir-${partido.id}`"
                      @click="abrirPublico(partido)"
                    >
                      Abrir público
                    </BaseButton>

                    <BaseButton
                      v-else-if="esOrganizador(partido) && partido.tipo === 'PUBLICO'"
                      variant="secondary"
                      block
                      :loading="procesando === `privado-${partido.id}`"
                      @click="volverPrivado(partido)"
                    >
                      Volver privado
                    </BaseButton>

                    <BaseButton
                      v-else-if="totalJugadores(partido) === 0"
                      variant="danger"
                      block
                      :loading="procesando === `eliminar-${partido.id}`"
                      @click="eliminarPartido(partido)"
                    >
                      Eliminar
                    </BaseButton>

                    <BaseButton v-else variant="ghost" block disabled>
                      —
                    </BaseButton>
                  </div>
                </div>
              </article>
            </section>
          </section>

          <section v-else class="state-empty">
            <p class="text-slate-700 font-medium">Selecciona un día con partidos para ver las cards.</p>
            <div class="mt-3">
              <BaseButton variant="secondary" @click="volverCalendarioMis">Volver al calendario</BaseButton>
            </div>
          </section>
        </section>

        <section v-else-if="misPartidosVistaSemanal.length === 0" class="state-empty">
          <p class="text-slate-700 font-medium">No tienes partidos en la semana pasada, actual o siguiente.</p>
          <p class="text-caption mt-1">Cambia a calendario completo para ver todos tus partidos.</p>
          <div class="mt-3">
            <BaseButton variant="secondary" @click="cambiarVistaMis('calendario')">Ver calendario completo</BaseButton>
          </div>
        </section>

        <section v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-3 md:gap-4">
        <article
          v-for="partido in misPartidosVistaSemanal"
          :key="partido.id"
          :class="[
            'card-surface overflow-hidden flex flex-col relative',
            cardAuraClass(partido)
          ]"
        >
          <div
            v-if="rolIcono(partido)"
            class="role-badge"
            :title="rolIcono(partido).label"
          >
            <AppIcon :name="rolIcono(partido).icon" :size="14" />
          </div>
          <div v-if="partido.estado === 'CANCELADO'" class="cancelled-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="cancelled-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </div>
          <div v-if="isReservationState(partido.estado)" class="paid-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="paid-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M7 4.5h10a1.5 1.5 0 0 1 1.5 1.5V20l-6.5-3.8L5.5 20V6A1.5 1.5 0 0 1 7 4.5z" />
            </svg>
          </div>
          <div v-if="isInGameStatus(partido.estado)" class="in-game-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="in-game-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M12 4a8 8 0 1 0 0 16a8 8 0 0 0 0-16M12 8v4l2.5 2" />
            </svg>
          </div>
          <div v-if="partido.estado === 'FINALIZADO'" class="finalized-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="finalized-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M6 4v16M8 6h9l-2 3 2 3H8z" />
            </svg>
          </div>
          <!-- Sección dividida con colores de equipos -->
          <div class="flex h-24 relative teams-strip">
            <div :style="getTeamBlockStyle(partido.colorEquipoA)" class="flex-1 flex items-center justify-center font-bold text-lg border-r relative">
              {{ partido.colorEquipoA }}
            </div>
            <div class="w-10 shrink-0 flex items-center justify-center gap-0.5 text-sm font-extrabold shadow-sm bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)] vs-strip">
              <span>V</span>
              <span>S</span>
            </div>
            <div :style="getTeamBlockStyle(partido.colorEquipoB)" class="flex-1 flex items-center justify-center font-bold text-lg border-l relative">
              {{ partido.colorEquipoB }}
            </div>
            <div
              v-if="estaInscritoEnPartido(partido)"
              :class="[
                'my-team-badge-absolute',
                equipoBadgePosClass(partido),
                equipoBadgeStateClass(partido)
              ]"
            >
              {{ equipoBadgeText(partido) }}
            </div>
          </div>

          <!-- Información del partido -->
          <div class="p-4 flex flex-col gap-3">
            <div class="flex items-start justify-between gap-2">
              <div>
                <p class="text-sm font-semibold text-slate-800">{{ formatearFecha(partido.fecha) }}</p>
                <p class="text-sm text-slate-600 inline-flex items-center gap-1.5"><AppIcon name="pin" :size="14" />{{ partido.lugar }}</p>
              </div>
              <StatusBadge :status="statusLabel(partido)" :show-icon="showStatusIconInListCard(partido)" />
            </div>

            <div class="flex items-center gap-2 text-xs text-slate-600">
              <StatusBadge :status="partido.tipo" :show-icon="false" />
              <span class="inline-flex items-center gap-1.5"><AppIcon name="users" :size="14" />{{ totalJugadores(partido) }}/{{ cupoTotal(partido) }} personas</span>
            </div>

            <p class="text-sm text-slate-600">Organiza: {{ obtenerNombreOrganizador(partido) }}</p>

            <div class="grid grid-cols-2 gap-2">
              <BaseButton :variant="puedeVerDetalle(partido) ? 'secondary' : 'danger'" block :disabled="!puedeVerDetalle(partido)" @click="irAPartido(partido.id)">
                {{ puedeVerDetalle(partido) ? 'Ver' : 'Cancelado' }}
              </BaseButton>

              <BaseButton
                v-if="esOrganizador(partido) && partido.tipo === 'PRIVADO' && partido.estado !== 'FINALIZADO' && partido.estado !== 'CANCELADO' && totalJugadores(partido) < cupoTotal(partido)"
                variant="primary"
                block
                :loading="procesando === `abrir-${partido.id}`"
                @click="abrirPublico(partido)"
              >
                Abrir público
              </BaseButton>

              <BaseButton
                v-else-if="esOrganizador(partido) && partido.tipo === 'PUBLICO'"
                variant="secondary"
                block
                :loading="procesando === `privado-${partido.id}`"
                @click="volverPrivado(partido)"
              >
                Volver privado
              </BaseButton>

              <BaseButton
                v-else-if="totalJugadores(partido) === 0"
                variant="danger"
                block
                :loading="procesando === `eliminar-${partido.id}`"
                @click="eliminarPartido(partido)"
              >
                Eliminar
              </BaseButton>

              <BaseButton v-else variant="ghost" block disabled>
                —
              </BaseButton>
            </div>
          </div>
        </article>
        </section>
      </section>
    </section>

    <!-- Vista: Partidos públicos -->
    <section v-if="tabActiva === 'publicos'">
      <section v-if="loading" class="state-loading text-sm text-slate-600">
        Cargando partidos públicos...
      </section>

      <section v-else-if="partidosPublicosFiltrados.length === 0" class="state-empty">
        <p class="text-slate-700 font-medium">No hay partidos públicos para este filtro.</p>
        <p class="text-caption mt-1">Prueba cambiando el filtro o creando un nuevo partido.</p>
        <div class="mt-3">
          <BaseButton variant="secondary" @click="irACrearPartido">Crear partido</BaseButton>
        </div>
      </section>

      <section v-else class="space-y-3">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <span class="text-xs text-slate-500 min-h-[1rem] max-w-full">
            {{ vistaPartidosPublicos === 'calendario' ? 'Toca un día para ver partidos' : `Partidos públicos recientes (${etiquetaRangoSemanal})` }}
          </span>
          <div class="inline-flex w-full sm:w-auto rounded-lg border border-slate-200 bg-white p-1 gap-1">
            <button
              type="button"
              @click="cambiarVistaPublicos('semanal')"
              :class="[
                'flex-1 sm:flex-none px-3 py-1.5 rounded-md text-xs font-semibold transition',
                vistaPartidosPublicos === 'semanal' ? 'bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)]' : 'text-slate-600 hover:bg-slate-100'
              ]"
            >
              Partidos recientes
            </button>
            <button
              type="button"
              @click="cambiarVistaPublicos('calendario')"
              :class="[
                'flex-1 sm:flex-none px-3 py-1.5 rounded-md text-xs font-semibold transition',
                vistaPartidosPublicos === 'calendario' ? 'bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)]' : 'text-slate-600 hover:bg-slate-100'
              ]"
            >
              Calendario completo
            </button>
          </div>
        </div>

        <section v-if="vistaPartidosPublicos === 'calendario'" class="space-y-3">
          <div v-if="!mostrandoDetalleDiaPublicos && calendarioPublicosMeses.length === 0" class="state-empty">
            <p class="text-slate-700 font-medium">No hay partidos públicos para mostrar en calendario.</p>
          </div>

          <div v-else-if="!mostrandoDetalleDiaPublicos" class="grid grid-cols-1 xl:grid-cols-2 gap-3">
            <article
              v-for="mes in calendarioPublicosMeses"
              :key="`pub-cal-${mes.key}`"
              class="rounded-xl border border-slate-200 bg-slate-50 p-3"
            >
              <h4 class="text-sm font-semibold text-slate-800 mb-2 capitalize">{{ mes.label }}</h4>
              <div class="grid grid-cols-7 gap-1 mb-1">
                <span v-for="dia in diasSemana" :key="`pub-cal-week-${mes.key}-${dia}`" class="text-[10px] font-semibold text-slate-500 text-center">{{ dia }}</span>
              </div>
              <div class="grid grid-cols-7 gap-1">
                <div
                  v-for="cell in mes.cells"
                  :key="`pub-cal-cell-${cell.key}`"
                  class="calendar-cell"
                  :class="[
                    claseDiaCalendario(cell),
                    !cell.empty && cell.partidos.length ? 'calendar-cell-clickable' : '',
                    !cell.empty && cell.key === diaPublicosSeleccionadoEfectivo ? 'calendar-cell-selected' : ''
                  ]"
                  @click="seleccionarDiaCalendarioPublicos(cell)"
                >
                  <template v-if="!cell.empty">
                    <span class="text-[11px] font-semibold leading-none">{{ cell.day }}</span>
                    <span v-if="cell.partidos.length" class="text-[10px] leading-none mt-0.5">({{ cell.partidos.length }})</span>
                  </template>
                </div>
              </div>
            </article>
          </div>

          <section v-else-if="diaPublicosSeleccionadoEfectivo" class="space-y-3">
            <div class="rounded-xl border border-slate-200 bg-white p-3 space-y-2">
              <div class="flex items-center justify-between gap-2">
                <h5 class="text-sm font-semibold text-slate-800 capitalize">Partidos del {{ formatearDiaCalendario(diaPublicosSeleccionadoEfectivo) }}</h5>
                <span class="text-xs text-slate-500">{{ partidosDiaPublicos.length }} partidos</span>
              </div>
              <BaseButton variant="secondary" size="sm" @click="volverCalendarioPublicos">Volver al calendario</BaseButton>
            </div>

            <section class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-3 md:gap-4">
              <article
                v-for="partido in partidosDiaPublicos"
                :key="`pub-dia-card-${partido.id}`"
                :class="[
                  'card-surface overflow-hidden flex flex-col relative',
                  cardAuraClass(partido)
                ]"
              >
                <div
                  v-if="rolIcono(partido)"
                  class="role-badge"
                  :title="rolIcono(partido).label"
                >
                  <AppIcon :name="rolIcono(partido).icon" :size="14" />
                </div>
                <div v-if="partido.estado === 'CANCELADO'" class="cancelled-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="cancelled-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M6 6l12 12M18 6L6 18" />
                  </svg>
                </div>
                <div v-if="isReservationState(partido.estado)" class="paid-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="paid-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M7 4.5h10a1.5 1.5 0 0 1 1.5 1.5V20l-6.5-3.8L5.5 20V6A1.5 1.5 0 0 1 7 4.5z" />
                  </svg>
                </div>
                <div v-if="isInGameStatus(partido.estado)" class="in-game-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="in-game-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M12 4a8 8 0 1 0 0 16a8 8 0 0 0 0-16M12 8v4l2.5 2" />
                  </svg>
                </div>
                <div v-if="partido.estado === 'FINALIZADO'" class="finalized-mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" class="finalized-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
                    <path d="M6 4v16M8 6h9l-2 3 2 3H8z" />
                  </svg>
                </div>

                <div class="flex h-24 relative teams-strip">
                  <div :style="getTeamBlockStyle(partido.colorEquipoA)" class="flex-1 flex items-center justify-center font-bold text-lg border-r relative">
                    {{ partido.colorEquipoA }}
                  </div>
                  <div class="w-10 shrink-0 flex items-center justify-center gap-0.5 text-sm font-extrabold shadow-sm bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)] vs-strip">
                    <span>V</span>
                    <span>S</span>
                  </div>
                  <div :style="getTeamBlockStyle(partido.colorEquipoB)" class="flex-1 flex items-center justify-center font-bold text-lg border-l relative">
                    {{ partido.colorEquipoB }}
                  </div>
                  <div
                    v-if="estaInscritoEnPartido(partido)"
                    :class="[
                      'my-team-badge-absolute',
                      equipoBadgePosClass(partido),
                      equipoBadgeStateClass(partido)
                    ]"
                  >
                    {{ equipoBadgeText(partido) }}
                  </div>
                </div>

                <div class="p-4 flex flex-col gap-3">
                  <div class="flex items-start justify-between gap-2">
                    <div>
                      <p class="text-sm font-semibold text-slate-800">{{ formatearFecha(partido.fecha) }}</p>
                      <p class="text-sm text-slate-600 inline-flex items-center gap-1.5"><AppIcon name="pin" :size="14" />{{ partido.lugar }}</p>
                    </div>
                    <StatusBadge :status="statusLabel(partido)" :show-icon="showStatusIconInListCard(partido)" />
                  </div>

                  <div class="text-center bg-slate-50 rounded-xl p-3">
                    <p class="text-xs text-slate-500 mb-1">Jugadores</p>
                    <p class="text-2xl font-bold text-slate-700">{{ totalJugadores(partido) }}/{{ cupoTotal(partido) }}</p>
                  </div>

                  <p class="text-sm text-slate-600">Organiza: {{ obtenerNombreOrganizador(partido) }}</p>

                  <div class="grid grid-cols-2 gap-2 mt-auto">
                    <BaseButton :variant="puedeVerDetalle(partido) ? 'secondary' : 'danger'" block :disabled="!puedeVerDetalle(partido)" @click="irAPartido(partido.id)">
                      {{ puedeVerDetalle(partido) ? 'Ver' : 'Cancelado' }}
                    </BaseButton>
                    <BaseButton
                      v-if="esOrganizador(partido) && partido.tipo === 'PUBLICO'"
                      variant="secondary"
                      block
                      :loading="procesando === `privado-${partido.id}`"
                      @click="volverPrivado(partido)"
                    >
                      Volver privado
                    </BaseButton>
                    <BaseButton
                      v-else
                      :variant="!puedeVerDetalle(partido) ? 'danger' : (disponible(partido) ? 'primary' : 'ghost')"
                      :disabled="!disponible(partido) || !puedeVerDetalle(partido)"
                      block
                      @click="irAPartido(partido.id)"
                    >
                      {{ !puedeVerDetalle(partido) ? 'Cancelado' : (disponible(partido) ? 'Unirme' : 'Completo') }}
                    </BaseButton>
                  </div>
                </div>
              </article>
            </section>
          </section>

          <section v-else class="state-empty">
            <p class="text-slate-700 font-medium">Selecciona un día con partidos para ver las cards.</p>
            <div class="mt-3">
              <BaseButton variant="secondary" @click="volverCalendarioPublicos">Volver al calendario</BaseButton>
            </div>
          </section>
        </section>

        <section v-else-if="partidosPublicosVistaSemanal.length === 0" class="state-empty">
          <p class="text-slate-700 font-medium">No hay partidos públicos en la semana pasada, actual o siguiente.</p>
          <p class="text-caption mt-1">Cambia a calendario completo para ver todos los partidos públicos.</p>
          <div class="mt-3">
            <BaseButton variant="secondary" @click="cambiarVistaPublicos('calendario')">Ver calendario completo</BaseButton>
          </div>
        </section>

        <section v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-3 md:gap-4">
        <article
          v-for="partido in partidosPublicosVistaSemanal"
          :key="partido.id"
          :class="[
            'card-surface overflow-hidden flex flex-col relative',
            cardAuraClass(partido)
          ]"
        >
          <div
            v-if="rolIcono(partido)"
            class="role-badge"
            :title="rolIcono(partido).label"
          >
            <AppIcon :name="rolIcono(partido).icon" :size="14" />
          </div>
          <div v-if="partido.estado === 'CANCELADO'" class="cancelled-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="cancelled-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </div>
          <div v-if="isReservationState(partido.estado)" class="paid-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="paid-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M7 4.5h10a1.5 1.5 0 0 1 1.5 1.5V20l-6.5-3.8L5.5 20V6A1.5 1.5 0 0 1 7 4.5z" />
            </svg>
          </div>
          <div v-if="isInGameStatus(partido.estado)" class="in-game-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="in-game-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M12 4a8 8 0 1 0 0 16a8 8 0 0 0 0-16M12 8v4l2.5 2" />
            </svg>
          </div>
          <div v-if="partido.estado === 'FINALIZADO'" class="finalized-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="finalized-mark-icon" xmlns="http://www.w3.org/2000/svg" focusable="false">
              <path d="M6 4v16M8 6h9l-2 3 2 3H8z" />
            </svg>
          </div>
          <!-- Sección dividida con colores de equipos -->
          <div class="flex h-24 relative teams-strip">
            <div :style="getTeamBlockStyle(partido.colorEquipoA)" class="flex-1 flex items-center justify-center font-bold text-lg border-r relative">
              {{ partido.colorEquipoA }}
            </div>
            <div class="w-10 shrink-0 flex items-center justify-center gap-0.5 text-sm font-extrabold shadow-sm bg-[color:var(--color-secondary)] text-[color:var(--color-on-accent)] vs-strip">
              <span>V</span>
              <span>S</span>
            </div>
            <div :style="getTeamBlockStyle(partido.colorEquipoB)" class="flex-1 flex items-center justify-center font-bold text-lg border-l relative">
              {{ partido.colorEquipoB }}
            </div>
            <div
              v-if="estaInscritoEnPartido(partido)"
              :class="[
                'my-team-badge-absolute',
                equipoBadgePosClass(partido),
                equipoBadgeStateClass(partido)
              ]"
            >
              {{ equipoBadgeText(partido) }}
            </div>
          </div>

          <!-- Información del partido -->
          <div class="p-4 flex flex-col gap-3">
            <div class="flex items-start justify-between gap-2">
              <div>
                <p class="text-sm font-semibold text-slate-800">{{ formatearFecha(partido.fecha) }}</p>
                <p class="text-sm text-slate-600 inline-flex items-center gap-1.5"><AppIcon name="pin" :size="14" />{{ partido.lugar }}</p>
              </div>
              <StatusBadge :status="statusLabel(partido)" :show-icon="showStatusIconInListCard(partido)" />
            </div>

            <div class="text-center bg-slate-50 rounded-xl p-3">
              <p class="text-xs text-slate-500 mb-1">Jugadores</p>
              <p class="text-2xl font-bold text-slate-700">{{ totalJugadores(partido) }}/{{ cupoTotal(partido) }}</p>
            </div>

            <p class="text-sm text-slate-600">Organiza: {{ obtenerNombreOrganizador(partido) }}</p>

            <div class="grid grid-cols-2 gap-2 mt-auto">
              <BaseButton :variant="puedeVerDetalle(partido) ? 'secondary' : 'danger'" block :disabled="!puedeVerDetalle(partido)" @click="irAPartido(partido.id)">
                {{ puedeVerDetalle(partido) ? 'Ver' : 'Cancelado' }}
              </BaseButton>
              <BaseButton
                v-if="esOrganizador(partido) && partido.tipo === 'PUBLICO'"
                variant="secondary"
                block
                :loading="procesando === `privado-${partido.id}`"
                @click="volverPrivado(partido)"
              >
                Volver privado
              </BaseButton>
              <BaseButton
                v-else
                :variant="!puedeVerDetalle(partido) ? 'danger' : (disponible(partido) ? 'primary' : 'ghost')"
                :disabled="!disponible(partido) || !puedeVerDetalle(partido)"
                block
                @click="irAPartido(partido.id)"
              >
                {{ !puedeVerDetalle(partido) ? 'Cancelado' : (disponible(partido) ? 'Unirme' : 'Completo') }}
              </BaseButton>
            </div>
          </div>
        </article>
        </section>
      </section>
    </section>
  </div>
</template>

<style scoped>
@keyframes organizerPulse {
  0% {
    box-shadow:
      0 0 0 1px rgba(151, 240, 125, 0.45),
      0 0 0 0 rgba(151, 240, 125, 0.24),
      0 8px 20px rgba(151, 240, 125, 0.14);
  }
  60% {
    box-shadow:
      0 0 0 1px rgba(151, 240, 125, 0.34),
      0 0 0 10px rgba(151, 240, 125, 0),
      0 8px 20px rgba(151, 240, 125, 0.1);
  }
  100% {
    box-shadow:
      0 0 0 1px rgba(151, 240, 125, 0.45),
      0 0 0 0 rgba(151, 240, 125, 0),
      0 8px 20px rgba(151, 240, 125, 0.14);
  }
}

@keyframes cancelPulse {
  0% {
    box-shadow:
      0 0 0 1px rgba(220, 38, 38, 0.45),
      0 0 0 0 rgba(220, 38, 38, 0.22),
      0 8px 20px rgba(220, 38, 38, 0.16);
  }
  60% {
    box-shadow:
      0 0 0 1px rgba(220, 38, 38, 0.35),
      0 0 0 10px rgba(220, 38, 38, 0),
      0 8px 20px rgba(220, 38, 38, 0.1);
  }
  100% {
    box-shadow:
      0 0 0 1px rgba(220, 38, 38, 0.45),
      0 0 0 0 rgba(220, 38, 38, 0),
      0 8px 20px rgba(220, 38, 38, 0.16);
  }
}

@keyframes reservedPulse {
  0% {
    box-shadow:
      0 0 0 1px rgba(37, 99, 235, 0.44),
      0 0 0 0 rgba(37, 99, 235, 0.22),
      0 8px 20px rgba(37, 99, 235, 0.14);
  }
  60% {
    box-shadow:
      0 0 0 1px rgba(37, 99, 235, 0.34),
      0 0 0 10px rgba(37, 99, 235, 0),
      0 8px 20px rgba(37, 99, 235, 0.1);
  }
  100% {
    box-shadow:
      0 0 0 1px rgba(37, 99, 235, 0.44),
      0 0 0 0 rgba(37, 99, 235, 0),
      0 8px 20px rgba(37, 99, 235, 0.14);
  }
}

@keyframes inGamePulse {
  0% {
    box-shadow:
      0 0 0 1px rgba(249, 115, 22, 0.44),
      0 0 0 0 rgba(249, 115, 22, 0.22),
      0 8px 20px rgba(249, 115, 22, 0.16);
  }
  60% {
    box-shadow:
      0 0 0 1px rgba(249, 115, 22, 0.34),
      0 0 0 10px rgba(249, 115, 22, 0),
      0 8px 20px rgba(249, 115, 22, 0.1);
  }
  100% {
    box-shadow:
      0 0 0 1px rgba(249, 115, 22, 0.44),
      0 0 0 0 rgba(249, 115, 22, 0),
      0 8px 20px rgba(249, 115, 22, 0.16);
  }
}

.organizer-pulse {
  border: 1px solid rgba(151, 240, 125, 0.6);
  box-shadow:
    0 0 0 1px rgba(151, 240, 125, 0.45),
    0 8px 20px rgba(151, 240, 125, 0.14);
  animation: organizerPulse 2.2s ease-in-out infinite;
}

.reserved-aura {
  border: 1px solid rgba(37, 99, 235, 0.6);
  box-shadow:
    0 0 0 1px rgba(37, 99, 235, 0.45),
    0 8px 20px rgba(37, 99, 235, 0.14);
  animation: reservedPulse 2.2s ease-in-out infinite;
}

.in-game-aura {
  border: 1px solid rgba(249, 115, 22, 0.6);
  box-shadow:
    0 0 0 1px rgba(249, 115, 22, 0.45),
    0 8px 20px rgba(249, 115, 22, 0.16);
  animation: inGamePulse 2.2s ease-in-out infinite;
}

.cancelled-aura {
  border: 1px solid #dc2626;
  box-shadow:
    0 0 0 1px rgba(220, 38, 38, 0.45),
    0 8px 20px rgba(220, 38, 38, 0.16);
  animation: cancelPulse 2.2s ease-in-out infinite;
}

.role-badge {
  position: absolute;
  top: 0.6rem;
  left: 0.6rem;
  background: var(--color-secondary);
  color: var(--color-on-accent);
  padding: 0.2rem 0.35rem;
  border-radius: 0.5rem;
  line-height: 1;
  box-shadow: 0 1px 2px rgba(151, 240, 125, 0.28);
  z-index: 6;
}

.participant-badge {
  position: absolute;
  top: 0.6rem;
  right: 0.6rem;
  width: 1.45rem;
  height: 1.45rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: rgba(34, 197, 94, 0.16);
  color: #15803d;
  border: 1px solid rgba(34, 197, 94, 0.38);
  box-shadow: 0 1px 2px rgba(34, 197, 94, 0.22);
  z-index: 5;
}

.participant-badge-icon {
  width: 12px;
  height: 12px;
}

.participant-badge-icon path {
  fill: none;
  stroke: currentColor;
  stroke-width: 2.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.my-team-badge-absolute {
  position: absolute;
  bottom: 0.4rem;
  transform: translateX(-50%);
  font-size: 10px;
  line-height: 1;
  font-weight: 700;
  padding: 0.16rem 0.45rem;
  border-radius: 9999px;
  pointer-events: none;
  z-index: 6;
}

.my-team-badge-assigned {
  background: #97f07d;
  color: #050505;
  border: 1px solid rgba(5, 5, 5, 0.45);
}

.my-team-badge-unassigned {
  background: #050505;
  color: #97f07d;
  border: 1px solid rgba(151, 240, 125, 0.6);
}

.teams-strip {
  --vs-width: 2.5rem;
}

.vs-strip {
  width: var(--vs-width);
}

.my-team-badge-a {
  left: calc(25% - (var(--vs-width) / 4));
}

.my-team-badge-b {
  left: calc(75% + (var(--vs-width) / 4));
}

.my-team-badge-center {
  left: 50%;
}

.cancelled-mark {
  position: absolute;
  inset: 0;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 4;
}

.cancelled-mark-icon {
  width: 100%;
  height: 100%;
  opacity: 0.26;
  filter: drop-shadow(0 0 8px rgba(220, 38, 38, 0.2));
}

.cancelled-mark-icon path {
  fill: none;
  stroke: rgba(239, 68, 68, 0.95);
  stroke-width: 2.2;
  stroke-linecap: round;
}

.paid-mark {
  position: absolute;
  inset: 0;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 4;
}

.paid-mark-icon {
  width: 100%;
  height: 100%;
  opacity: 0.32;
  filter: drop-shadow(0 0 10px rgba(37, 99, 235, 0.24));
}

.paid-mark-icon path {
  fill: none;
  stroke: rgba(59, 130, 246, 0.95);
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.in-game-mark {
  position: absolute;
  inset: 0;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 4;
}

.in-game-mark-icon {
  width: 100%;
  height: 100%;
  opacity: 0.26;
  filter: drop-shadow(0 0 8px rgba(249, 115, 22, 0.2));
}

.in-game-mark-icon path {
  fill: none;
  stroke: rgba(249, 115, 22, 0.95);
  stroke-width: 2.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.finalized-mark {
  position: absolute;
  inset: 0;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 4;
}

.finalized-mark-icon {
  width: 100%;
  height: 100%;
  opacity: 0.26;
  filter: drop-shadow(0 0 8px rgba(71, 85, 105, 0.18));
}

.finalized-mark-icon path {
  fill: none;
  stroke: rgba(71, 85, 105, 0.94);
  stroke-width: 2.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.calendar-cell {
  min-height: 2.25rem;
  border-radius: 0.5rem;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.calendar-cell-empty {
  background: transparent;
}

.calendar-cell-idle {
  background: rgba(148, 163, 184, 0.12);
  border-color: rgba(148, 163, 184, 0.2);
  color: #334155;
}

.calendar-cell-with-match {
  background: rgba(147, 197, 253, 0.42);
  border-color: rgba(96, 165, 250, 0.88);
  color: #ffffff;
}

.calendar-cell-today {
  background: rgba(151, 240, 125, 0.46);
  border-color: rgba(151, 240, 125, 0.95);
  color: #ffffff;
}

.calendar-cell-today-with-match {
  background: linear-gradient(90deg, rgba(151, 240, 125, 0.6) 0 50%, rgba(147, 197, 253, 0.56) 50% 100%);
  border-color: rgba(151, 240, 125, 0.95);
  color: #ffffff;
}

.calendar-cell-clickable {
  cursor: pointer;
  transition: transform 120ms ease, filter 120ms ease;
}

.calendar-cell-clickable:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
}

.calendar-cell-selected {
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.42);
}
</style>
