<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import partidoService from '../services/partidoService'
import amistadService from '../services/amistadService'
import invitacionService from '../services/invitacionService'
import partidoVotacionService from '../services/partidoVotacionService'
import BaseButton from './ui/BaseButton.vue'
import AppIcon from './ui/AppIcon.vue'
import StatusBadge from './ui/StatusBadge.vue'
import MatchOrganizerView from './match/MatchOrganizerView.vue'
import MatchParticipantView from './match/MatchParticipantView.vue'
import { formatDateTimeEs } from '../utils/dateFormat'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const uiStore = useUiStore()

const partido = ref(null)
const loading = ref(true)
const error = ref('')
const inscribiendo = ref(false)
const feedbackInscripcion = ref('')
const amigos = ref([])
const mostrarModalInvitar = ref(false)
const invitandoA = ref(null)
const balanceando = ref(false)
const mensajeBalance = ref('')
const analisisBalance = ref(null)
const cambiosBalance = ref([])
const cargandoVotacion = ref(false)
const guardandoVotacion = ref(false)
const miVoto = ref(null)
const resumenVotacion = ref(null)
const procesandoRating = ref(false)
const resultadoProcesoRating = ref(null)
const mensajeProcesoRating = ref('')
const ultimaEjecucionRating = ref(null)
const companerosAsignados = ref([])
const mensajeVotacion = ref('')
const mostrarModalDiscusionResultado = ref(false)
const guardandoResultadoOficial = ref(false)
const cerrandoActa = ref(false)
const enviandoSolicitudA = ref(null)
const solicitudesEnviadas = ref([])
const amigosActuales = ref([])
const estadoPagoReserva = ref({})
const marcadorPropuestoSeleccionado = ref('')
const votacionForm = ref({
  golesEquipoAPropuesto: 0,
  golesEquipoBPropuesto: 0,
  intensidadPartido: 'MEDIO',
  partidoFueParejo: true,
  partidoAlterado: false,
  jugadoresDiferenciales: [],
  valoracionesCompaneros: [],
})

const resultadoOficialForm = ref({
  golesEquipoA: 0,
  golesEquipoB: 0,
})

const resetVotacionForm = () => {
  votacionForm.value = {
    golesEquipoAPropuesto: 0,
    golesEquipoBPropuesto: 0,
    intensidadPartido: 'MEDIO',
    partidoFueParejo: true,
    partidoAlterado: false,
    jugadoresDiferenciales: [],
    valoracionesCompaneros: [],
  }
}

const sincronizarResultadoOficialForm = () => {
  resultadoOficialForm.value = {
    golesEquipoA: Number(partido.value?.golesEquipoA) || 0,
    golesEquipoB: Number(partido.value?.golesEquipoB) || 0,
  }
}

const teamPalette = {
  Blanco: { jersey: '#ffffff', onJersey: '#111827', border: 'rgba(255, 255, 255, 0.62)' },
  Negro: { jersey: '#050505', onJersey: '#f8fafc', border: 'rgba(255, 255, 255, 0.24)' },
  Rojo: { jersey: '#ef4444', onJersey: '#ffffff', border: 'rgba(239, 68, 68, 0.58)' },
  Azul: { jersey: '#2563eb', onJersey: '#ffffff', border: 'rgba(37, 99, 235, 0.58)' },
  Verde: { jersey: '#16a34a', onJersey: '#ffffff', border: 'rgba(22, 163, 74, 0.58)' },
  Amarillo: { jersey: '#facc15', onJersey: '#111827', border: 'rgba(250, 204, 21, 0.72)' },
  Naranja: { jersey: '#f97316', onJersey: '#ffffff', border: 'rgba(249, 115, 22, 0.58)' },
  Morado: { jersey: '#a855f7', onJersey: '#ffffff', border: 'rgba(168, 85, 247, 0.58)' },
}

const normalizeTeamColorKey = (value) => {
  if (!value) return ''
  return String(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase()
}

const rgbStringToHex = (value) => {
  const match = String(value).match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/i)
  if (!match) return null
  const r = Math.max(0, Math.min(255, Number(match[1])))
  const g = Math.max(0, Math.min(255, Number(match[2])))
  const b = Math.max(0, Math.min(255, Number(match[3])))
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
}

const toHexColor = (value) => {
  if (!value) return null
  const raw = String(value).trim().toLowerCase()
  if (/^#[0-9a-f]{6}$/i.test(raw)) return raw
  if (/^#[0-9a-f]{3}$/i.test(raw)) {
    return `#${raw[1]}${raw[1]}${raw[2]}${raw[2]}${raw[3]}${raw[3]}`
  }
  return rgbStringToHex(raw)
}

const foregroundForHex = (hexColor) => {
  if (!hexColor || !hexColor.startsWith('#') || hexColor.length !== 7) return '#111827'
  const r = parseInt(hexColor.slice(1, 3), 16)
  const g = parseInt(hexColor.slice(3, 5), 16)
  const b = parseInt(hexColor.slice(5, 7), 16)
  const luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
  return luminance >= 0.68 ? '#111827' : '#f8fafc'
}

const resolveTeamTone = (name, fallback) => {
  const explicitHex = toHexColor(name)
  if (explicitHex) {
    return {
      jersey: explicitHex,
      onJersey: foregroundForHex(explicitHex),
      border: 'rgba(148, 163, 184, 0.48)',
    }
  }

  const normalizedName = normalizeTeamColorKey(name)
  const normalizedFallback = normalizeTeamColorKey(fallback)

  const directTone = Object.entries(teamPalette).find(([key]) => normalizeTeamColorKey(key) === normalizedName)?.[1]
  if (directTone) return directTone

  const fallbackTone = Object.entries(teamPalette).find(([key]) => normalizeTeamColorKey(key) === normalizedFallback)?.[1]
  return fallbackTone || teamPalette.Blanco
}

const isLightJersey = (hexColor) => {
  if (!hexColor || !hexColor.startsWith('#') || hexColor.length !== 7) return false
  const r = parseInt(hexColor.slice(1, 3), 16)
  const g = parseInt(hexColor.slice(3, 5), 16)
  const b = parseInt(hexColor.slice(5, 7), 16)
  const luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
  return luminance >= 0.68
}

const createTeamTone = (name, fallback) => {
  const tone = resolveTeamTone(name, fallback)
  const darkText = isLightJersey(tone.jersey)
  return {
    cardStyle: {
      backgroundColor: tone.jersey,
      borderColor: tone.border,
      color: darkText ? '#111827' : '#f8fafc',
    },
    titleStyle: {
      color: darkText ? '#111827' : tone.onJersey,
    },
    subtitleStyle: {
      color: darkText ? '#111827' : 'rgba(248, 250, 252, 0.88)',
    },
    playerRowStyle: {
      borderColor: tone.border,
      backgroundColor: 'rgba(0, 0, 0, 0.14)',
    },
    controlIdleStyle: {
      backgroundColor: darkText ? 'rgba(226, 232, 240, 0.9)' : 'rgba(0, 0, 0, 0.28)',
      color: darkText ? '#111827' : '#f8fafc',
      borderColor: darkText ? 'rgba(17, 24, 39, 0.4)' : 'rgba(255, 255, 255, 0.45)',
    },
  }
}

const teamAColor = computed(() => createTeamTone(partido.value?.colorEquipoA, 'Blanco'))

const teamBColor = computed(() => createTeamTone(partido.value?.colorEquipoB, 'Negro'))

const totalJugadoresCount = computed(() => {
  if (!partido.value) return 0
  const inscritos = partido.value.jugadoresInscritos?.length || 0
  const equipoA = partido.value.equipoA?.length || 0
  const equipoB = partido.value.equipoB?.length || 0
  return inscritos + equipoA + equipoB
})

const organizadores = computed(() => partido.value?.organizadores || [])

const organizadoresDetalle = computed(() => {
  return organizadores.value.map((org) => ({
    id: org.usuario?.id,
    nombre: org.usuario?.nombre || '—',
    rolClase: org.rol === 'OWNER' ? 'Responsable' : 'Coorganización',
    rolEmoji: org.rol === 'OWNER' ? 'crown' : 'settings',
    rolTitle: org.rol === 'OWNER' ? 'Responsable principal' : 'Coorganización',
  }))
})

onMounted(async () => {
  const partidoId = parseInt(route.params.id)
  
  try {
    const detalle = await partidoService.obtenerDetallePartido(partidoId)

    if (detalle?.estado === 'CANCELADO') {
      uiStore.showToast({ message: 'Este partido está cancelado y no tiene detalle disponible.', type: 'info' })
      router.replace('/dashboard/partidos')
      return
    }

    partido.value = detalle
    sincronizarResultadoOficialForm()

    await cargarAnalisisBalance(partidoId)
    await cargarPanelVotacion(partidoId)
    await cargarEstadoPagoReserva(partidoId)
    
    // Cargar amigos si es OWNER
    if (detalle.owner?.id === authStore.user?.id && detalle.tipo === 'PRIVADO') {
      await cargarAmigos()
    }
    loading.value = false
  } catch (err) {
    console.error('Error al cargar partido:', err)
    error.value = err.message || 'Error al cargar el partido'
    loading.value = false
  }
})

const cargarAmigos = async () => {
  try {
    amigos.value = await amistadService.obtenerMisAmigos()
  } catch (error) {
    console.error('Error cargando amigos:', error)
  }
}

const inscribirse = async () => {
  inscribiendo.value = true
  try {
    const detalle = await partidoService.inscribirseAPartido(partido.value.id)
    partido.value = detalle
    feedbackInscripcion.value = 'Te has inscrito correctamente. Estás en la lista de pendientes de asignación.'
    uiStore.showToast({ message: 'Te has inscrito correctamente.', type: 'join-success', duration: 5000 })
    setTimeout(() => {
      feedbackInscripcion.value = ''
    }, 4500)
    await cargarAnalisisBalance(partido.value.id)
    await cargarPanelVotacion(partido.value.id)
  } catch (err) {
    error.value = err.message || 'Error al inscribirse'
    uiStore.showToast({ message: error.value, type: 'error' })
  } finally {
    inscribiendo.value = false
  }
}

const desinscribirse = async () => {
  inscribiendo.value = true
  try {
    const detalle = await partidoService.desinscribirseAPartido(partido.value.id)
    partido.value = detalle
    await cargarAnalisisBalance(partido.value.id)
    await cargarPanelVotacion(partido.value.id)
  } catch (err) {
    error.value = err.message || 'Error al desinscribirse'
  } finally {
    inscribiendo.value = false
  }
}

const abandonarComoCreador = async () => {
  // Cuando se sale de la organización, recargar para actualizar la vista
  await recargarPartido()
}

const handlePartidoCancelado = () => {
  router.push('/dashboard/partidos?cancelado=1')
}

const recargarPartido = async () => {
  try {
    const detalle = await partidoService.obtenerDetallePartido(partido.value.id)
    partido.value = detalle
    sincronizarResultadoOficialForm()
    await cargarAnalisisBalance(partido.value.id)
    await cargarPanelVotacion(partido.value.id)
    await cargarEstadoPagoReserva(partido.value.id)
  } catch (err) {
    error.value = err.message || 'Error al recargar partido'
  }
}

const moverJugador = async (usuarioId, equipo) => {
  try {
    const detalle = await partidoService.moverJugadorAEquipo(partido.value.id, usuarioId, equipo)
    partido.value = detalle
    await cargarAnalisisBalance(partido.value.id)
    await cargarPanelVotacion(partido.value.id)
    await cargarEstadoPagoReserva(partido.value.id)
  } catch (err) {
    error.value = err.message || 'Error al mover jugador'
  }
}

const cargarEstadoPagoReserva = async (partidoId) => {
  if (!partidoId) {
    estadoPagoReserva.value = {}
    return
  }

  try {
    estadoPagoReserva.value = await partidoService.obtenerEstadoPagoReserva(partidoId)
  } catch (e) {
    estadoPagoReserva.value = {}
  }
}

const usuarioPagoConfirmado = (jugadorId) => Boolean(estadoPagoReserva.value?.[jugadorId])

const eliminarPartido = async () => {
  const accepted = await uiStore.askConfirm({
    title: 'Eliminar partido',
    message: 'Esta acción no se puede deshacer. ¿Deseas continuar?',
    confirmLabel: 'Eliminar',
    cancelLabel: 'Cancelar',
    variant: 'danger',
  })

  if (!accepted) {
    return
  }

  try {
    await partidoService.eliminarPartido(partido.value.id)
    uiStore.showToast({ message: 'Partido eliminado.', type: 'success' })
    router.push('/dashboard/partidos')
  } catch (err) {
    error.value = err.message || 'Error al eliminar el partido'
    uiStore.showToast({ message: error.value, type: 'error' })
  }
}

const invitarAmigo = async (amigoId) => {
  invitandoA.value = amigoId
  try {
    await invitacionService.invitarAPartido(partido.value.id, amigoId)
    // Remover de la lista de amigos para invitar
    amigos.value = amigos.value.filter(a => {
      const otroId = a.usuarioA.id === authStore.user?.id ? a.usuarioB.id : a.usuarioA.id
      return otroId !== amigoId
    })
  } catch (error) {
    uiStore.showToast({ message: 'Error al invitar: ' + (error.message || 'Intenta nuevamente'), type: 'error' })
  } finally {
    invitandoA.value = null
  }
}

const balancearEquiposAutomaticamente = async () => {
  if (!partido.value) return

  balanceando.value = true
  mensajeBalance.value = ''
  cambiosBalance.value = []

  try {
    const resultado = await partidoService.asignarPosiciones(partido.value.id)
    
    // DON'T update partido teams - let positions be assigned independently
    // partido.value.equipoA = resultado.equipoA
    // partido.value.equipoB = resultado.equipoB
    
    // Store analysis data
    analisisBalance.value = {
      balanceado: resultado.balanceado,
      razonesDesbalance: resultado.razonesDesbalance || []
    }
    
    const balanceado = Boolean(resultado.balanceadoDespues ?? resultado.balanceado)
    mensajeBalance.value = balanceado
      ? 'Equipos balanceados correctamente'
      : 'Balance aplicado, pero existen diferencias de nivel'
    
    // Return result so organizer views can react to assigned positions
    return resultado
    
  } catch (err) {
    mensajeBalance.value = err.message || 'No se pudieron asignar las posiciones.'
    uiStore.showToast({ message: mensajeBalance.value, type: 'error' })
    console.error('Balance error:', err)
    throw err
  } finally {
    balanceando.value = false
  }
}

const cargarAnalisisBalance = async (partidoId) => {
  try {
    analisisBalance.value = await partidoService.obtenerAnalisisBalance(partidoId)
  } catch (err) {
    analisisBalance.value = null
  }
}

const esParticipante = () => {
  if (!partido.value || !authStore.user?.id) return false
  return [...partido.value.equipoA, ...partido.value.equipoB].some(j => j.id === authStore.user.id)
}

const esOwner = () => {
  return organizadores.value.some(o => o.usuario?.id === authStore.user?.id && o.rol === 'OWNER')
}

const esOrganizador = () => {
  return organizadores.value.some(o => o.usuario?.id === authStore.user?.id)
}

const todosJugadores = () => {
  if (!partido.value) return []
  return [...partido.value.equipoA, ...partido.value.equipoB]
}

const companerosVotables = () => {
  return companerosAsignados.value || []
}

const esCompaneroAsignado = (jugadorId) => {
  return String(jugadorId) !== String(authStore.user?.id)
}

const esUsuarioActual = (jugadorId) => {
  return String(jugadorId) === String(authStore.user?.id)
}

const toggleJugadorDiferencial = (jugadorId) => {
  if (esUsuarioActual(jugadorId)) return

  const current = new Set(votacionForm.value.jugadoresDiferenciales)
  if (current.has(jugadorId)) {
    current.delete(jugadorId)
  } else {
    current.add(jugadorId)
  }
  votacionForm.value.jugadoresDiferenciales = Array.from(current)
}

const setValoracionCompanero = (jugadorId, puntuacion) => {
  if (!puntuacion) {
    votacionForm.value.valoracionesCompaneros = votacionForm.value.valoracionesCompaneros.filter(v => v.jugadorId !== jugadorId)
    return
  }

  const existente = votacionForm.value.valoracionesCompaneros.find(v => v.jugadorId === jugadorId)
  if (existente) {
    existente.puntuacion = puntuacion
    return
  }

  votacionForm.value.valoracionesCompaneros.push({ jugadorId, puntuacion })
}

const getValoracionCompanero = (jugadorId) => {
  const existente = votacionForm.value.valoracionesCompaneros.find(v => v.jugadorId === jugadorId)
  return existente?.puntuacion ?? null
}

const isSolicitudEnviada = (jugadorId) => {
  return solicitudesEnviadas.value.some((id) => String(id) === String(jugadorId))
}

const isYaAmigo = (jugadorId) => {
  return amigosActuales.value.some((id) => String(id) === String(jugadorId))
}

const obtenerEstadoAmistadJugador = (jugadorId) => {
  if (String(jugadorId) === String(authStore.user?.id)) return 'self'
  if (isYaAmigo(jugadorId)) return 'amigo'
  if (isSolicitudEnviada(jugadorId)) return 'enviada'
  if (enviandoSolicitudA.value === jugadorId) return 'enviando'
  return 'disponible'
}

const textoEstadoAmistadJugador = (jugadorId) => {
  const estado = obtenerEstadoAmistadJugador(jugadorId)
  if (estado === 'amigo') return 'Ya sois amigos'
  if (estado === 'enviada') return 'Solicitud enviada'
  if (estado === 'enviando') return 'Enviando...'
  return ''
}

const cargarEstadoAmistadesVotacion = async () => {
  try {
    const [amigosData, solicitudesData] = await Promise.all([
      amistadService.obtenerMisAmigos().catch(() => []),
      amistadService.obtenerSolicitudesEnviadas().catch(() => []),
    ])

    amigosActuales.value = (amigosData || []).map((amistad) => {
      const usuarioAId = amistad?.usuarioA?.id
      const usuarioBId = amistad?.usuarioB?.id
      return String(usuarioAId) === String(authStore.user?.id) ? usuarioBId : usuarioAId
    }).filter((id) => id != null)

    solicitudesEnviadas.value = (solicitudesData || []).map((solicitud) => {
      const usuarioAId = solicitud?.usuarioA?.id
      const usuarioBId = solicitud?.usuarioB?.id
      return String(usuarioAId) === String(authStore.user?.id) ? usuarioBId : usuarioAId
    }).filter((id) => id != null)
  } catch (error) {
    amigosActuales.value = []
    solicitudesEnviadas.value = []
  }
}

const enviarSolicitudAmistadVotacion = async (jugadorId) => {
  if (!jugadorId || String(jugadorId) === String(authStore.user?.id)) return
  if (enviandoSolicitudA.value === jugadorId || isSolicitudEnviada(jugadorId) || isYaAmigo(jugadorId)) return

  enviandoSolicitudA.value = jugadorId
  try {
    await amistadService.enviarSolicitud(jugadorId)
    solicitudesEnviadas.value = [...solicitudesEnviadas.value, jugadorId]
    uiStore.showToast({ message: 'Solicitud de amistad enviada', type: 'success' })
  } catch (err) {
    uiStore.showToast({ message: err?.message || 'No se pudo enviar la solicitud', type: 'error' })
  } finally {
    enviandoSolicitudA.value = null
  }
}

const cargarPanelVotacion = async (partidoId) => {
  if (!partido.value || partido.value.estado !== 'FINALIZADO') {
    miVoto.value = null
    resumenVotacion.value = null
    companerosAsignados.value = []
    resetVotacionForm()
    return
  }

  if (!esParticipante() && !esOwner()) {
    return
  }

  cargandoVotacion.value = true
  mensajeVotacion.value = ''

  try {
    await cargarEstadoAmistadesVotacion()

    try {
      companerosAsignados.value = await partidoVotacionService.obtenerAsignacion(partidoId)
    } catch (err) {
      companerosAsignados.value = []
    }

    try {
      const voto = await partidoVotacionService.obtenerMiVoto(partidoId)
      const votoValido = voto && typeof voto === 'object' && voto.golesEquipoAPropuesto !== undefined

      if (votoValido) {
        miVoto.value = voto
        votacionForm.value = {
          golesEquipoAPropuesto: voto.golesEquipoAPropuesto,
          golesEquipoBPropuesto: voto.golesEquipoBPropuesto,
          intensidadPartido: voto.intensidadPartido || 'MEDIO',
          partidoFueParejo: Boolean(voto.partidoFueParejo),
          partidoAlterado: Boolean(voto.partidoAlterado),
          jugadoresDiferenciales: (voto.jugadoresDiferenciales || []).filter((id) => !esUsuarioActual(id)),
          valoracionesCompaneros: (voto.valoracionesCompaneros || []).map(v => ({ jugadorId: v.jugadorId, puntuacion: v.puntuacion })),
        }
        if (Array.isArray(voto.companerosAsignados) && voto.companerosAsignados.length > 0) {
          companerosAsignados.value = voto.companerosAsignados
        }
      } else {
        miVoto.value = null
        resetVotacionForm()
      }
    } catch (err) {
      miVoto.value = null
      resetVotacionForm()
    }

    try {
      resumenVotacion.value = await partidoVotacionService.obtenerPanelCompartido(partidoId)
    } catch (err) {
      resumenVotacion.value = null
    }
  } finally {
    cargandoVotacion.value = false
  }
}

const guardarVotacion = async () => {
  if (!partido.value) return
  if (actaCerrada.value) {
    uiStore.showToast({ message: 'El acta está cerrada. Ya no se pueden enviar votos.', type: 'warning' })
    return
  }
  guardandoVotacion.value = true
  mensajeVotacion.value = ''

  try {
    const payload = {
      golesEquipoAPropuesto: Number(votacionForm.value.golesEquipoAPropuesto),
      golesEquipoBPropuesto: Number(votacionForm.value.golesEquipoBPropuesto),
      intensidadPartido: votacionForm.value.intensidadPartido,
      partidoFueParejo: Boolean(votacionForm.value.partidoFueParejo),
      jugadoresDiferenciales: (votacionForm.value.jugadoresDiferenciales || []).filter((id) => !esUsuarioActual(id)),
      valoracionesCompaneros: votacionForm.value.valoracionesCompaneros,
    }

    if (esOrganizador()) {
      payload.partidoAlterado = Boolean(votacionForm.value.partidoAlterado)
    }

    miVoto.value = await partidoVotacionService.guardarMiVoto(partido.value.id, payload)
    mensajeVotacion.value = 'Tu votación se guardó correctamente.'
    mostrarModalDiscusionResultado.value = false

    resumenVotacion.value = await partidoVotacionService.obtenerPanelCompartido(partido.value.id)
  } catch (err) {
    mensajeVotacion.value = err.message || 'No se pudo guardar la votación'
    uiStore.showToast({ message: mensajeVotacion.value, type: 'error' })
  } finally {
    guardandoVotacion.value = false
  }
}

const obtenerIdAmigo = (amigo) => {
  return amigo.usuarioA.id === authStore.user?.id ? amigo.usuarioB.id : amigo.usuarioA.id
}

const obtenerNombreAmigo = (amigo) => {
  return amigo.usuarioA.id === authStore.user?.id ? amigo.usuarioB.nombre : amigo.usuarioA.nombre
}

const formatearFecha = (fecha) => {
  return formatDateTimeEs(fecha)
}

const textoVotosResumen = computed(() => {
  const total = resumenVotacion.value?.totalVotos || 0
  const sujeto = total === 1 ? 'jugador ha' : 'jugadores han'
  return `${total} ${sujeto} votado · ¡Anímate a compartir tu opinión!`
})

const actaCerrada = computed(() => Boolean(partido.value?.archivado))

const marcadoresPropuestos = computed(() => {
  const lista = Array.isArray(resumenVotacion.value?.marcadoresPropuestos)
    ? resumenVotacion.value.marcadoresPropuestos
    : []

  return lista
    .filter((item) => Number.isInteger(Number(item?.golesEquipoA)) && Number.isInteger(Number(item?.golesEquipoB)))
    .map((item) => ({
      golesEquipoA: Number(item.golesEquipoA),
      golesEquipoB: Number(item.golesEquipoB),
      votos: Number(item.votos || 0),
      value: `${item.golesEquipoA}:${item.golesEquipoB}`,
      label: `${item.golesEquipoA}-${item.golesEquipoB} (${item.votos || 0} votos)`,
    }))
})

const aplicarMarcadorPropuesto = () => {
  const selected = String(marcadorPropuestoSeleccionado.value || '').trim()
  if (!selected) return

  const [golesA, golesB] = selected.split(':').map((raw) => Number(raw))
  if (!Number.isInteger(golesA) || !Number.isInteger(golesB)) return

  resultadoOficialForm.value.golesEquipoA = golesA
  resultadoOficialForm.value.golesEquipoB = golesB
}

const marcadorPromedio = computed(() => {
  if (Number.isFinite(Number(partido.value?.golesEquipoA)) && Number.isFinite(Number(partido.value?.golesEquipoB))) {
    return {
      golesA: Number(partido.value.golesEquipoA),
      golesB: Number(partido.value.golesEquipoB),
    }
  }

  const golesA = Number(resumenVotacion.value?.promedioGolesEquipoA)
  const golesB = Number(resumenVotacion.value?.promedioGolesEquipoB)
  return {
    golesA: Number.isFinite(golesA) ? golesA : 0,
    golesB: Number.isFinite(golesB) ? golesB : 0,
  }
})

const formatIntensidad = (intensidad) => {
  if (!intensidad) return 'Sin datos'
  return String(intensidad).replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())
}

const resultadoBanner = computed(() => {
  const golesA = marcadorPromedio.value.golesA
  const golesB = marcadorPromedio.value.golesB
  const usaResultadoOficial = Number.isFinite(Number(partido.value?.golesEquipoA)) && Number.isFinite(Number(partido.value?.golesEquipoB))

  const usuarioEnA = partido.value?.equipoA?.some((j) => String(j.id) === String(authStore.user?.id))
  const usuarioEnB = partido.value?.equipoB?.some((j) => String(j.id) === String(authStore.user?.id))

  if (!resumenVotacion.value || !Number.isFinite(golesA) || !Number.isFinite(golesB)) {
    return {
      titulo: 'Sin resultado',
      subtitulo: 'Aún no hay suficientes votos.',
      clase: 'bg-orange-500 text-white',
    }
  }

  if (golesA === golesB) {
    return {
      titulo: 'Empate',
      subtitulo: usaResultadoOficial ? 'Resultado oficial fijado por organización.' : 'Resultado promedio equilibrado.',
      clase: 'bg-orange-500 text-white',
    }
  }

  if (usuarioEnA || usuarioEnB) {
    const victoria = (usuarioEnA && golesA > golesB) || (usuarioEnB && golesB > golesA)
    if (victoria) {
      return {
        titulo: 'Victoria',
        subtitulo: usaResultadoOficial ? 'Resultado oficial fijado por organización.' : 'Tu equipo sale ganador en el promedio.',
        clase: 'bg-emerald-600 text-white',
      }
    }

    return {
      titulo: 'Derrota',
      subtitulo: usaResultadoOficial ? 'Resultado oficial fijado por organización.' : 'Tu equipo queda por debajo en el promedio.',
      clase: 'bg-rose-600 text-white',
    }
  }

  return golesA > golesB
    ? {
      titulo: `Gana equipo ${partido.value?.colorEquipoA || 'A'}`,
      subtitulo: usaResultadoOficial ? 'Resultado oficial fijado por organización.' : 'Resultado promedio del panel compartido.',
      clase: 'bg-emerald-600 text-white',
    }
    : {
      titulo: `Gana equipo ${partido.value?.colorEquipoB || 'B'}`,
      subtitulo: usaResultadoOficial ? 'Resultado oficial fijado por organización.' : 'Resultado promedio del panel compartido.',
      clase: 'bg-rose-600 text-white',
    }
})

const guardarResultadoOficial = async () => {
  if (!partido.value || !esOrganizador() || partido.value.estado !== 'FINALIZADO' || actaCerrada.value) return

  const golesA = Number(resultadoOficialForm.value.golesEquipoA)
  const golesB = Number(resultadoOficialForm.value.golesEquipoB)
  if (!Number.isInteger(golesA) || !Number.isInteger(golesB) || golesA < 0 || golesB < 0) {
    uiStore.showToast({ message: 'El resultado oficial debe tener goles válidos (0 o más).', type: 'warning' })
    return
  }

  guardandoResultadoOficial.value = true
  try {
    const detalle = await partidoService.actualizarResultadoOficial(partido.value.id, golesA, golesB)
    partido.value = detalle
    sincronizarResultadoOficialForm()
    uiStore.showToast({ message: 'Resultado oficial actualizado.', type: 'success' })
  } catch (err) {
    uiStore.showToast({ message: err?.message || 'No se pudo actualizar el resultado oficial', type: 'error' })
  } finally {
    guardandoResultadoOficial.value = false
  }
}

const cerrarActaPartido = async () => {
  if (!partido.value || !esOrganizador() || partido.value.estado !== 'FINALIZADO' || actaCerrada.value) return

  const accepted = await uiStore.askConfirm({
    title: 'Cerrar acta del partido',
    message: 'Esta acción procesa el rating y bloquea toda interacción posterior. ¿Deseas continuar?',
    confirmLabel: 'CERRAR ACTA',
    cancelLabel: 'Cancelar',
    variant: 'danger',
  })

  if (!accepted) return

  cerrandoActa.value = true
  try {
    const response = await partidoService.cerrarActaPartido(partido.value.id)
    resultadoProcesoRating.value = response
    mensajeProcesoRating.value = response?.mensaje || 'Acta cerrada correctamente.'
    uiStore.showToast({ message: mensajeProcesoRating.value, type: 'success' })
    await recargarPartido()
    await cargarPanelVotacion(partido.value.id)
  } catch (err) {
    const message = err?.message || 'No se pudo cerrar el acta del partido'
    uiStore.showToast({ message, type: 'error' })
  } finally {
    cerrandoActa.value = false
  }
}

const diferencialesResumen = computed(() => {
  const lista = Array.isArray(resumenVotacion.value?.jugadoresDiferenciales)
    ? resumenVotacion.value.jugadoresDiferenciales
    : []
  return lista.slice(0, 3)
})

const diferencialesRestantes = computed(() => {
  const total = Array.isArray(resumenVotacion.value?.jugadoresDiferenciales)
    ? resumenVotacion.value.jugadoresDiferenciales.length
    : 0
  return Math.max(0, total - diferencialesResumen.value.length)
})

const participacionVotacionTexto = computed(() => {
  const raw = Number(partido.value?.participacionVotacion)
  if (!Number.isFinite(raw)) return '0%'
  return `${Math.round(raw * 100)}%`
})

const scoreCalidadTexto = computed(() => {
  const raw = Number(partido.value?.scoreCalidad)
  if (!Number.isFinite(raw)) return '0.000'
  return raw.toFixed(3)
})

const claseEstadoCalidad = computed(() => {
  const estado = partido.value?.estadoCalidad || 'NORMAL'
  if (estado === 'ALTERADO') {
    return 'bg-rose-600 text-white border-rose-600'
  }
  if (estado === 'SEMI_ALTERADO') {
    return 'bg-amber-500 text-slate-900 border-amber-500'
  }
  return 'bg-emerald-600 text-white border-emerald-600'
})

const resultadoResolucionTexto = computed(() => {
  const resultado = resultadoProcesoRating.value?.resultadoResolucion
  if (!resultado) {
    return partido.value?.ratingProcesado ? 'PROCESADO' : 'SIN_EJECUCION'
  }
  return resultado
})

const claseResultadoResolucion = computed(() => {
  const resultado = resultadoResolucionTexto.value
  if (resultado === 'GANA_A' || resultado === 'GANA_B') {
    return 'bg-emerald-600 text-white border-emerald-600'
  }
  if (resultado === 'EMPATE') {
    return 'bg-amber-500 text-slate-900 border-amber-500'
  }
  if (resultado === 'YA_PROCESADO' || resultado === 'PROCESADO') {
    return 'bg-sky-600 text-white border-sky-600'
  }
  if (resultado === 'SIN_DATOS' || resultado === 'SIN_EQUIPOS' || resultado === 'SIN_EJECUCION') {
    return 'bg-slate-500 text-white border-slate-500'
  }
  return 'bg-rose-600 text-white border-rose-600'
})

const fechaProcesadoTexto = computed(() => {
  const fecha = partido.value?.ratingProcesadoEn
  return fecha ? formatearFecha(fecha) : 'Pendiente'
})

const fechaUltimaEjecucionTexto = computed(() => {
  const fecha = ultimaEjecucionRating.value?.fecha
  if (!fecha) {
    return resultadoProcesoRating.value?.procesadoEn
      ? formatearFecha(resultadoProcesoRating.value.procesadoEn)
      : 'Sin ejecuciones en esta sesión'
  }
  return formatearFecha(fecha)
})

const procesarRatingPartido = async () => {
  if (!partido.value || !esOrganizador() || partido.value.estado !== 'FINALIZADO') return

  procesandoRating.value = true
  mensajeProcesoRating.value = ''

  try {
    const response = await partidoService.procesarRatingPartido(partido.value.id)
    resultadoProcesoRating.value = response
    mensajeProcesoRating.value = response?.mensaje || 'Proceso de rating ejecutado.'
    ultimaEjecucionRating.value = {
      fecha: new Date().toISOString(),
      resultado: response?.resultadoResolucion || 'SIN_RESULTADO',
      mensaje: mensajeProcesoRating.value,
    }

    if (response?.procesado) {
      uiStore.showToast({ message: mensajeProcesoRating.value, type: 'success' })
    } else {
      uiStore.showToast({ message: mensajeProcesoRating.value, type: 'info' })
    }

    await recargarPartido()
  } catch (err) {
    mensajeProcesoRating.value = err?.message || 'No se pudo procesar el rating del partido'
    ultimaEjecucionRating.value = {
      fecha: new Date().toISOString(),
      resultado: 'ERROR',
      mensaje: mensajeProcesoRating.value,
    }
    uiStore.showToast({ message: mensajeProcesoRating.value, type: 'error' })
  } finally {
    procesandoRating.value = false
  }
}

const handleGoBack = () => {
  router.back()
}

const abrirModalDiscusionResultado = () => {
  if (actaCerrada.value) {
    uiStore.showToast({ message: 'El acta está cerrada. Ya no se puede discutir el resultado.', type: 'warning' })
    return
  }
  mostrarModalDiscusionResultado.value = true
}

const cerrarModalDiscusionResultado = () => {
  mostrarModalDiscusionResultado.value = false
}

</script>

<template>
  <div class="space-y-3 md:space-y-4 relative">
    <section v-if="loading" class="state-loading text-sm text-slate-600">
      Cargando partido...
    </section>

    <section v-else-if="error" class="card-surface p-4 md:p-5">
      <p class="text-sm text-red-700 bg-red-50 border border-red-200 rounded-xl px-4 py-3">{{ error }}</p>
      <div class="mt-3">
        <BaseButton variant="secondary" @click="handleGoBack">Volver</BaseButton>
      </div>
    </section>

    <template v-else-if="partido">
      <button
        type="button"
        class="absolute -top-6 right-0 z-20 h-9 rounded-full border border-slate-100/70 bg-slate-900 text-slate-100 shadow-[0_8px_20px_rgba(0,0,0,0.35)] hover:bg-slate-800 hover:text-white transition inline-flex items-center justify-center gap-1.5 px-3"
        @click="handleGoBack"
        aria-label="Volver atrás"
      >
        <svg viewBox="0 0 24 24" class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
          <path d="M15 5l-7 7 7 7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="text-xs font-semibold">Volver atrás</span>
      </button>

      <!-- Match Organizer View (Only for organizers) -->
      <template v-if="esOrganizador()">
        <MatchOrganizerView
          v-if="partido.estado !== 'FINALIZADO'"
          :partida="partido"
          :amigos="amigos"
          @actualizar="recargarPartido"
          @invitar="mostrarModalInvitar = true"
          @eliminar="eliminarPartido"
          @abandonar="abandonarComoCreador"
          @cancelado="handlePartidoCancelado"
          @volver="handleGoBack"
        />
      </template>

      <!-- Participant View (For non-creators) -->
      <template v-else>
        <!-- Botón volver y header info for participants -->
        <section v-if="partido.estado !== 'FINALIZADO'" class="card-surface p-3 md:p-4">
          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-2">
              <StatusBadge :status="partido.estado" :show-icon="partido.estado === 'FINALIZADO' || partido.estado === 'EN_JUEGO' || partido.estado === 'EN_CURSO'" />
              <span :class="['inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold', partido.tipo === 'PUBLICO' ? 'bg-[color:rgba(151,240,125,0.16)] text-[color:var(--color-secondary)] border border-[color:rgba(151,240,125,0.34)]' : 'bg-[color:rgba(151,240,125,0.12)] text-[color:var(--color-secondary)] border border-[color:rgba(151,240,125,0.28)]']">
                {{ partido.tipo === 'PUBLICO' ? 'Público' : 'Privado' }}
              </span>
            </div>
          </div>

          <div class="mt-2.5 flex items-start justify-between gap-3">
            <div class="space-y-1 min-w-0">
              <h1 class="text-base md:text-lg font-bold text-slate-800 leading-tight">{{ formatearFecha(partido.fecha) }}</h1>
              <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-600">
                <span>{{ partido.lugar }}</span>
              </div>
              <div class="text-xs md:text-sm text-slate-600">
                <span class="font-medium text-slate-700">Organización:</span>
                <span v-for="(org, index) in organizadoresDetalle" :key="org.id || index" class="ml-2 inline-flex items-center gap-1.5">
                  <span class="text-slate-700">{{ org.nombre }}</span>
                  <span class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 bg-slate-100 text-slate-700 text-[11px] md:text-xs font-semibold">
                    <span :title="org.rolTitle" class="inline-flex items-center"><AppIcon :name="org.rolEmoji" :size="12" /></span>
                    <span>{{ org.rolClase }}</span>
                  </span>
                </span>
              </div>
            </div>

            <div class="shrink-0">
              <BaseButton
                v-if="!partido.estaInscrito && partido.puedeInscribirse"
                @click="inscribirse"
                :disabled="inscribiendo"
                :loading="inscribiendo"
                size="sm"
              >
                Unirme al partido
              </BaseButton>

              <BaseButton
                v-else-if="partido.estaInscrito && partido.estado !== 'FINALIZADO'"
                @click="desinscribirse"
                :disabled="inscribiendo"
                :loading="inscribiendo"
                variant="danger"
                size="sm"
              >
                Cancelar asistencia
              </BaseButton>

            </div>
          </div>
        </section>

        <MatchParticipantView
          v-if="partido.estado !== 'FINALIZADO'"
          :partida="partido"
          :current-user-id="authStore.user?.id"
          :feedback-message="feedbackInscripcion"
          :organizadores="organizadores"
          :estado-pago-reserva="estadoPagoReserva"
        />
      </template>

      <!-- Voting Panel (Only for finished matches) -->
      <section v-if="partido.estado === 'FINALIZADO' && (esParticipante() || esOwner())" class="card-surface p-3 md:p-4 border border-[color:var(--color-border)]">
        <div class="mb-2 flex flex-wrap items-center justify-between gap-2">
          <span class="inline-flex items-center rounded-full bg-[color:rgba(151,240,125,0.16)] text-[color:var(--color-secondary)] text-[11px] font-semibold px-2.5 py-0.5 border border-[color:rgba(151,240,125,0.34)]">
            Votación post-partido
          </span>
          <span class="inline-flex items-center rounded-full bg-fuchsia-600 text-white text-[11px] font-semibold px-3 py-1 shadow">
            {{ textoVotosResumen }}
          </span>
        </div>
        <h3 class="text-xl font-bold text-slate-800 mb-2">Panel de votación</h3>
        <p class="text-sm text-slate-600 mb-4">Al finalizar, registra tu evaluación rápida y envía tu voto.</p>

        <div v-if="cargandoVotacion" class="text-slate-600 text-sm">Cargando votaciones...</div>

        <div v-else-if="esOrganizador()" class="space-y-3">
          <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <div>
                <p class="text-xs font-semibold text-slate-700">Resultado oficial (solo organización)</p>
                <p class="text-[11px] text-slate-500">Este marcador tiene prioridad sobre el promedio de votos para resolver el resultado.</p>
              </div>
              <span
                v-if="Number.isFinite(Number(partido?.golesEquipoA)) && Number.isFinite(Number(partido?.golesEquipoB))"
                class="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700"
              >
                Fijado
              </span>
            </div>
            <div class="grid grid-cols-2 gap-2">
              <label class="block">
                <span class="block text-xs text-slate-600 mb-1">Goles equipo A</span>
                <input v-model.number="resultadoOficialForm.golesEquipoA" :disabled="actaCerrada" type="number" min="0" class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-slate-100" />
              </label>
              <label class="block">
                <span class="block text-xs text-slate-600 mb-1">Goles equipo B</span>
                <input v-model.number="resultadoOficialForm.golesEquipoB" :disabled="actaCerrada" type="number" min="0" class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-slate-100" />
              </label>
            </div>
            <div v-if="marcadoresPropuestos.length" class="grid grid-cols-1 md:grid-cols-[1fr_auto] gap-2 items-end">
              <label class="block">
                <span class="block text-xs text-slate-600 mb-1">Marcadores propuestos por participantes</span>
                <select v-model="marcadorPropuestoSeleccionado" :disabled="actaCerrada" class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-slate-100">
                  <option value="">Selecciona un marcador</option>
                  <option v-for="marcador in marcadoresPropuestos" :key="marcador.value" :value="marcador.value">
                    {{ marcador.label }}
                  </option>
                </select>
              </label>
              <BaseButton size="sm" variant="secondary" :disabled="actaCerrada || !marcadorPropuestoSeleccionado" @click="aplicarMarcadorPropuesto">
                Usar marcador
              </BaseButton>
            </div>
            <div class="flex justify-end">
              <BaseButton size="sm" :loading="guardandoResultadoOficial" :disabled="guardandoResultadoOficial || actaCerrada" @click="guardarResultadoOficial">
                Guardar resultado oficial
              </BaseButton>
            </div>
          </div>

          <div class="grid grid-cols-3 gap-2">
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Intensidad del partido</label>
              <select v-model="votacionForm.intensidadPartido" :disabled="actaCerrada" class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white disabled:cursor-not-allowed disabled:bg-slate-100">
                <option value="BAJO">Bajo</option>
                <option value="MEDIO">Medio</option>
                <option value="ALTO">Alto</option>
                <option value="MUY_ALTO">Muy alto</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Balanceado</label>
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  :disabled="actaCerrada"
                  @click="votacionForm.partidoFueParejo = true"
                  :class="[
                    'px-2.5 py-1.5 rounded-lg border text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed',
                    votacionForm.partidoFueParejo ? 'bg-emerald-600 text-white border-emerald-600' : 'bg-white text-slate-700 border-slate-300'
                  ]"
                >
                  Sí
                </button>
                <button
                  type="button"
                  :disabled="actaCerrada"
                  @click="votacionForm.partidoFueParejo = false"
                  :class="[
                    'px-2.5 py-1.5 rounded-lg border text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed',
                    !votacionForm.partidoFueParejo ? 'bg-rose-600 text-white border-rose-600' : 'bg-white text-slate-700 border-slate-300'
                  ]"
                >
                  No
                </button>
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">¿Partido alterado por incidencias?</label>
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  :disabled="actaCerrada"
                  @click="votacionForm.partidoAlterado = false"
                  :class="[
                    'px-2.5 py-1.5 rounded-lg border text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed',
                    !votacionForm.partidoAlterado ? 'bg-emerald-600 text-white border-emerald-600' : 'bg-white text-slate-700 border-slate-300'
                  ]"
                >
                  No
                </button>
                <button
                  type="button"
                  :disabled="actaCerrada"
                  @click="votacionForm.partidoAlterado = true"
                  :class="[
                    'px-2.5 py-1.5 rounded-lg border text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed',
                    votacionForm.partidoAlterado ? 'bg-rose-600 text-white border-rose-600' : 'bg-white text-slate-700 border-slate-300'
                  ]"
                >
                  Sí
                </button>
              </div>
            </div>
          </div>

          <div class="rounded-lg border border-[color:var(--color-border)] bg-[color:rgba(102,63,77,0.28)] p-3">
            <p class="text-xs font-semibold text-slate-700 mb-2">Leyenda</p>
            <div class="flex flex-wrap items-center gap-3 text-xs text-slate-600">
              <span class="inline-flex items-center gap-1"><svg class="w-4 h-4 text-amber-500" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg> Jugador diferencial</span>
              <span class="inline-flex items-center gap-1"><svg class="w-4 h-4 text-emerald-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.3l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 6.59 7.41C6.22 7.78 6 8.3 6 8.83V19c0 1.1.9 2 2 2h9c.82 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg> Buena actitud</span>
              <span class="inline-flex items-center gap-1"><svg class="w-4 h-4 text-rose-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M15 3H6c-.82 0-1.54.5-1.84 1.22L1.14 11.27c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.3l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10 23l6.41-6.41c.37-.37.59-.89.59-1.42V5c0-1.1-.9-2-2-2zm3 0v12h4V3h-4z"/></svg> Mala actitud</span>
              <span class="inline-flex items-center gap-1"><AppIcon name="check" :size="14" class="text-emerald-700" /> Pista pagada</span>
              <span class="inline-flex items-center gap-1"><AppIcon name="users" :size="14" /> Enviar amistad</span>
            </div>
          </div>

          <section class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <article class="rounded-lg border-2 p-3 space-y-2" :style="teamAColor.cardStyle">
              <div class="flex items-center justify-between gap-2">
                <h4 class="font-bold" :style="teamAColor.titleStyle">Equipo {{ partido.colorEquipoA || 'A' }}</h4>
              </div>
              <div v-if="!partido.equipoA || partido.equipoA.length === 0" class="text-xs" :style="teamAColor.subtitleStyle">Sin jugadores</div>
              <div v-else class="space-y-1.5">
                <div v-for="jugador in partido.equipoA" :key="`voto-a-${jugador.id}`" :class="['rounded-lg border px-2 py-1.5 flex items-center justify-between gap-1.5', esUsuarioActual(jugador.id) ? 'border-[color:var(--color-primary)] shadow-[0_0_0_2px_rgba(151,240,125,0.3)]' : '']" :style="teamAColor.playerRowStyle">
                  <span class="inline-flex items-center gap-1 text-xs font-medium truncate" :style="teamAColor.titleStyle">
                    <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-4 h-4 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada"><AppIcon name="check" :size="10" /></span>
                    <span>{{ jugador.nombre }}</span>
                  </span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      @click="toggleJugadorDiferencial(jugador.id)"
                      :disabled="esUsuarioActual(jugador.id)"
                      :class="[
                        'h-7 w-7 rounded-md border flex items-center justify-center transition disabled:opacity-50 disabled:cursor-not-allowed',
                        esUsuarioActual(jugador.id)
                          ? 'bg-slate-100 text-slate-400 border-slate-200'
                            : (votacionForm.jugadoresDiferenciales.includes(jugador.id) ? 'bg-amber-500 text-white border-amber-500' : '')
                      ]"
                          :style="!esUsuarioActual(jugador.id) && !votacionForm.jugadoresDiferenciales.includes(jugador.id) ? teamAColor.controlIdleStyle : null"
                      :title="esUsuarioActual(jugador.id) ? 'No puedes marcarte como diferencial' : 'Jugador diferencial'"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === 1 ? null : 1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === 1 ? 'bg-emerald-600 text-white border-emerald-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === 1 ? null : teamAColor.controlIdleStyle"
                      title="Buena actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.3l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 6.59 7.41C6.22 7.78 6 8.3 6 8.83V19c0 1.1.9 2 2 2h9c.82 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === -1 ? null : -1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === -1 ? 'bg-rose-600 text-white border-rose-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === -1 ? null : teamAColor.controlIdleStyle"
                      title="Mala actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M15 3H6c-.82 0-1.54.5-1.84 1.22L1.14 11.27c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.3l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10 23l6.41-6.41c.37-.37.59-.89.59-1.42V5c0-1.1-.9-2-2-2zm3 0v12h4V3h-4z"/></svg>
                    </button>
                    <div class="flex flex-col items-center gap-0.5">
                      <button
                        type="button"
                        :disabled="obtenerEstadoAmistadJugador(jugador.id) !== 'disponible'"
                        @click="enviarSolicitudAmistadVotacion(jugador.id)"
                        :class="[
                          'h-7 min-w-7 px-1.5 rounded-md border flex items-center justify-center transition disabled:cursor-not-allowed text-[10px] font-semibold',
                          obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'bg-emerald-50 border-emerald-300 text-emerald-700' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'bg-slate-100 border-slate-300 text-slate-500' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviando' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? '' : '',
                        ]"
                        :style="obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? teamAColor.controlIdleStyle : null"
                        :title="obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'No puedes enviarte amistad' : (obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'Ya sois amigos' : (obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'Solicitud ya enviada' : 'Enviar solicitud de amistad'))"
                      >
                        <AppIcon v-if="obtenerEstadoAmistadJugador(jugador.id) === 'amigo'" name="users" :size="12" class="text-emerald-700" />
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviada'">↗</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'self'">—</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviando'">...</span>
                        <AppIcon v-else name="users" :size="12" />
                      </button>
                      <span
                        v-if="textoEstadoAmistadJugador(jugador.id)"
                        class="text-[9px] leading-none font-medium"
                        :class="obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'text-emerald-700' : 'text-[color:var(--color-secondary)]'"
                      >
                        {{ textoEstadoAmistadJugador(jugador.id) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </article>

            <article class="rounded-lg border-2 p-3 space-y-2" :style="teamBColor.cardStyle">
              <div class="flex items-center justify-between gap-2">
                <h4 class="font-bold" :style="teamBColor.titleStyle">Equipo {{ partido.colorEquipoB || 'B' }}</h4>
              </div>
              <div v-if="!partido.equipoB || partido.equipoB.length === 0" class="text-xs" :style="teamBColor.subtitleStyle">Sin jugadores</div>
              <div v-else class="space-y-1.5">
                <div v-for="jugador in partido.equipoB" :key="`voto-b-${jugador.id}`" :class="['rounded-lg border px-2 py-1.5 flex items-center justify-between gap-1.5', esUsuarioActual(jugador.id) ? 'border-[color:var(--color-primary)] shadow-[0_0_0_2px_rgba(151,240,125,0.3)]' : '']" :style="teamBColor.playerRowStyle">
                  <span class="inline-flex items-center gap-1 text-xs font-medium truncate" :style="teamBColor.titleStyle">
                    <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-4 h-4 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada"><AppIcon name="check" :size="10" /></span>
                    <span>{{ jugador.nombre }}</span>
                  </span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      @click="toggleJugadorDiferencial(jugador.id)"
                      :disabled="esUsuarioActual(jugador.id)"
                      :class="[
                        'h-7 w-7 rounded-md border flex items-center justify-center transition disabled:opacity-50 disabled:cursor-not-allowed',
                        esUsuarioActual(jugador.id)
                          ? 'bg-slate-100 text-slate-400 border-slate-200'
                            : (votacionForm.jugadoresDiferenciales.includes(jugador.id) ? 'bg-amber-500 text-white border-amber-500' : '')
                      ]"
                          :style="!esUsuarioActual(jugador.id) && !votacionForm.jugadoresDiferenciales.includes(jugador.id) ? teamBColor.controlIdleStyle : null"
                      :title="esUsuarioActual(jugador.id) ? 'No puedes marcarte como diferencial' : 'Jugador diferencial'"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === 1 ? null : 1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === 1 ? 'bg-emerald-600 text-white border-emerald-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === 1 ? null : teamBColor.controlIdleStyle"
                      title="Buena actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.3l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 6.59 7.41C6.22 7.78 6 8.3 6 8.83V19c0 1.1.9 2 2 2h9c.82 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === -1 ? null : -1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === -1 ? 'bg-rose-600 text-white border-rose-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === -1 ? null : teamBColor.controlIdleStyle"
                      title="Mala actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M15 3H6c-.82 0-1.54.5-1.84 1.22L1.14 11.27c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.3l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10 23l6.41-6.41c.37-.37.59-.89.59-1.42V5c0-1.1-.9-2-2-2zm3 0v12h4V3h-4z"/></svg>
                    </button>
                    <div class="flex flex-col items-center gap-0.5">
                      <button
                        type="button"
                        :disabled="obtenerEstadoAmistadJugador(jugador.id) !== 'disponible'"
                        @click="enviarSolicitudAmistadVotacion(jugador.id)"
                        :class="[
                          'h-7 min-w-7 px-1.5 rounded-md border flex items-center justify-center transition disabled:cursor-not-allowed text-[10px] font-semibold',
                          obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'bg-emerald-50 border-emerald-300 text-emerald-700' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'bg-slate-100 border-slate-300 text-slate-500' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviando' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? '' : '',
                        ]"
                        :style="obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? teamBColor.controlIdleStyle : null"
                        :title="obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'No puedes enviarte amistad' : (obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'Ya sois amigos' : (obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'Solicitud ya enviada' : 'Enviar solicitud de amistad'))"
                      >
                        <AppIcon v-if="obtenerEstadoAmistadJugador(jugador.id) === 'amigo'" name="users" :size="12" class="text-emerald-700" />
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviada'">↗</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'self'">—</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviando'">...</span>
                        <AppIcon v-else name="users" :size="12" />
                      </button>
                      <span
                        v-if="textoEstadoAmistadJugador(jugador.id)"
                        class="text-[9px] leading-none font-medium"
                        :class="obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'text-emerald-700' : 'text-[color:var(--color-secondary)]'"
                      >
                        {{ textoEstadoAmistadJugador(jugador.id) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </article>
          </section>

          <div class="text-xs text-slate-500">
            Puedes votar a cualquier jugador del partido excepto a ti mismo.
          </div>

          <BaseButton @click="guardarVotacion" :loading="guardandoVotacion" :disabled="actaCerrada">
            {{ miVoto ? 'Actualizar votación' : 'Enviar votación' }}
          </BaseButton>

          <p v-if="mensajeVotacion" class="text-sm font-medium text-slate-700">{{ mensajeVotacion }}</p>

          <div v-if="resumenVotacion" class="mt-3 bg-white border border-slate-200 rounded-xl p-2.5 space-y-2">
            <div class="flex items-start justify-between gap-2">
              <h4 class="text-sm font-semibold text-slate-800">Resumen compartido de votación</h4>
            </div>

            <div v-if="partido?.estado === 'FINALIZADO' && !actaCerrada" class="flex items-center justify-between gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <p class="text-xs text-slate-600">El partido ya finalizó. Cierra el acta para bloquear cambios y procesar el cierre oficial.</p>
              <BaseButton size="sm" :loading="cerrandoActa" :disabled="cerrandoActa" @click="cerrarActaPartido">
                Cerrar acta
              </BaseButton>
            </div>
            <div v-else-if="actaCerrada" class="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
              Acta cerrada: cambios y votaciones bloqueados.
            </div>

            <div class="grid grid-cols-3 gap-1.5">
              <div :class="['rounded-xl px-3 py-2.5 border border-transparent min-w-0', resultadoBanner.clase]">
                <p class="text-[10px] md:text-[11px] uppercase tracking-wide opacity-90">Resultado</p>
                <p class="text-base md:text-lg font-extrabold leading-tight mt-0.5 truncate">{{ resultadoBanner.titulo }}</p>
                <p class="text-xl md:text-2xl font-black leading-none mt-1">{{ marcadorPromedio.golesA.toFixed(0) }} - {{ marcadorPromedio.golesB.toFixed(0) }}</p>
              </div>
              <div class="bg-slate-50 rounded-xl p-2.5 border border-slate-200 min-w-0 flex flex-col">
                <p class="text-[11px] text-slate-600">Intensidad</p>
                <p class="text-lg md:text-2xl font-extrabold text-slate-900 leading-none flex-1 flex items-center">{{ formatIntensidad(resumenVotacion.intensidadMasVotada) }}</p>
              </div>
              <div class="bg-slate-50 rounded-xl p-2.5 border border-slate-200 min-w-0 flex flex-col">
                <p class="text-[11px] text-slate-600">Balanceo</p>
                <p class="text-lg md:text-2xl font-extrabold text-slate-900 leading-none flex-1 flex items-center">{{ Number(resumenVotacion.porcentajePartidoParejo || 0).toFixed(0) }}%</p>
              </div>
            </div>

            <div class="rounded-lg border border-slate-200 bg-slate-50 p-2.5">
              <p class="text-xs font-semibold text-slate-800 mb-1.5">Diferenciales (número de veces)</p>
              <div v-if="diferencialesResumen.length" class="flex flex-wrap gap-1.5">
                <span
                  v-for="jugador in diferencialesResumen"
                  :key="jugador.jugadorId"
                  :class="['inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs', esUsuarioActual(jugador.jugadorId) ? 'border-[color:var(--color-primary)] bg-[color:rgba(151,240,125,0.2)] text-[color:var(--color-secondary)] shadow-[0_0_0_1px_rgba(151,240,125,0.45)]' : 'border-amber-200 bg-amber-50 text-amber-800']"
                >
                  <span class="truncate max-w-[120px]">{{ jugador.jugadorNombre }}</span>
                  <span class="font-bold">{{ jugador.votos }}</span>
                </span>
              </div>
              <p v-else class="text-xs text-slate-500">Sin votos de diferenciales por ahora.</p>
              <p v-if="diferencialesRestantes > 0" class="text-xs text-slate-500 mt-1">+{{ diferencialesRestantes }} más</p>
            </div>
          </div>
        </div>

        <div v-else class="space-y-3">
          <div v-if="resumenVotacion" class="bg-white border border-slate-200 rounded-xl p-2.5 space-y-2">
            <div class="flex items-start justify-between gap-2">
              <h4 class="text-sm font-semibold text-slate-800">Resumen compartido de votación</h4>
              <BaseButton size="sm" variant="secondary" :disabled="actaCerrada" @click="abrirModalDiscusionResultado">
                Discutir resultado
              </BaseButton>
            </div>

            <div class="grid grid-cols-3 gap-1.5">
              <div :class="['rounded-xl px-3 py-2.5 border border-transparent min-w-0', resultadoBanner.clase]">
                <p class="text-[10px] md:text-[11px] uppercase tracking-wide opacity-90">Resultado</p>
                <p class="text-base md:text-lg font-extrabold leading-tight mt-0.5 truncate">{{ resultadoBanner.titulo }}</p>
                <p class="text-xl md:text-2xl font-black leading-none mt-1">{{ marcadorPromedio.golesA.toFixed(0) }} - {{ marcadorPromedio.golesB.toFixed(0) }}</p>
              </div>
              <div class="bg-slate-50 rounded-xl p-2.5 border border-slate-200 min-w-0 flex flex-col">
                <p class="text-[11px] text-slate-600">Intensidad</p>
                <p class="text-lg md:text-2xl font-extrabold text-slate-900 leading-none flex-1 flex items-center">{{ formatIntensidad(resumenVotacion.intensidadMasVotada) }}</p>
              </div>
              <div class="bg-slate-50 rounded-xl p-2.5 border border-slate-200 min-w-0 flex flex-col">
                <p class="text-[11px] text-slate-600">Balanceo</p>
                <p class="text-lg md:text-2xl font-extrabold text-slate-900 leading-none flex-1 flex items-center">{{ Number(resumenVotacion.porcentajePartidoParejo || 0).toFixed(0) }}%</p>
              </div>
            </div>

            <div class="rounded-lg border border-slate-200 bg-slate-50 p-2.5">
              <p class="text-xs font-semibold text-slate-800 mb-1.5">Diferenciales (número de veces)</p>
              <div v-if="diferencialesResumen.length" class="flex flex-wrap gap-1.5">
                <span
                  v-for="jugador in diferencialesResumen"
                  :key="jugador.jugadorId"
                  :class="['inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs', esUsuarioActual(jugador.jugadorId) ? 'border-[color:var(--color-primary)] bg-[color:rgba(151,240,125,0.2)] text-[color:var(--color-secondary)] shadow-[0_0_0_1px_rgba(151,240,125,0.45)]' : 'border-amber-200 bg-amber-50 text-amber-800']"
                >
                  <span class="truncate max-w-[120px]">{{ jugador.jugadorNombre }}</span>
                  <span class="font-bold">{{ jugador.votos }}</span>
                </span>
              </div>
              <p v-else class="text-xs text-slate-500">Sin votos de diferenciales por ahora.</p>
              <p v-if="diferencialesRestantes > 0" class="text-xs text-slate-500 mt-1">+{{ diferencialesRestantes }} más</p>
            </div>
          </div>

          <div class="rounded-lg border border-[color:var(--color-border)] bg-[color:rgba(102,63,77,0.28)] p-3">
            <p class="text-xs font-semibold text-slate-700 mb-2">Leyenda</p>
            <div class="flex flex-wrap items-center gap-3 text-xs text-slate-600">
              <span class="inline-flex items-center gap-1"><svg class="w-4 h-4 text-amber-500" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg> Jugador diferencial</span>
              <span class="inline-flex items-center gap-1"><svg class="w-4 h-4 text-emerald-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.3l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 6.59 7.41C6.22 7.78 6 8.3 6 8.83V19c0 1.1.9 2 2 2h9c.82 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg> Buena actitud</span>
              <span class="inline-flex items-center gap-1"><svg class="w-4 h-4 text-rose-600" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M15 3H6c-.82 0-1.54.5-1.84 1.22L1.14 11.27c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.3l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10 23l6.41-6.41c.37-.37.59-.89.59-1.42V5c0-1.1-.9-2-2-2zm3 0v12h4V3h-4z"/></svg> Mala actitud</span>
              <span class="inline-flex items-center gap-1"><AppIcon name="check" :size="14" class="text-emerald-700" /> Pista pagada</span>
              <span class="inline-flex items-center gap-1"><AppIcon name="users" :size="14" /> Enviar amistad</span>
            </div>
          </div>

          <section class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <article class="rounded-lg border-2 p-3 space-y-2" :style="teamAColor.cardStyle">
              <div class="flex items-center justify-between gap-2">
                <h4 class="font-bold" :style="teamAColor.titleStyle">Equipo {{ partido.colorEquipoA || 'A' }}</h4>
              </div>
              <div v-if="!partido.equipoA || partido.equipoA.length === 0" class="text-xs" :style="teamAColor.subtitleStyle">Sin jugadores</div>
              <div v-else class="space-y-1.5">
                <div v-for="jugador in partido.equipoA" :key="`voto-a-part-${jugador.id}`" :class="['rounded-lg border px-2 py-1.5 flex items-center justify-between gap-1.5', esUsuarioActual(jugador.id) ? 'border-[color:var(--color-primary)] shadow-[0_0_0_2px_rgba(151,240,125,0.3)]' : '']" :style="teamAColor.playerRowStyle">
                  <span class="inline-flex items-center gap-1 text-xs font-medium truncate" :style="teamAColor.titleStyle">
                    <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-4 h-4 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada"><AppIcon name="check" :size="10" /></span>
                    <span>{{ jugador.nombre }}</span>
                  </span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      @click="toggleJugadorDiferencial(jugador.id)"
                      :disabled="esUsuarioActual(jugador.id)"
                      :class="[
                        'h-7 w-7 rounded-md border flex items-center justify-center transition disabled:opacity-50 disabled:cursor-not-allowed',
                        esUsuarioActual(jugador.id)
                          ? 'bg-slate-100 text-slate-400 border-slate-200'
                            : (votacionForm.jugadoresDiferenciales.includes(jugador.id) ? 'bg-amber-500 text-white border-amber-500' : '')
                      ]"
                      :style="!esUsuarioActual(jugador.id) && !votacionForm.jugadoresDiferenciales.includes(jugador.id) ? teamAColor.controlIdleStyle : null"
                      :title="esUsuarioActual(jugador.id) ? 'No puedes marcarte como diferencial' : 'Jugador diferencial'"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === 1 ? null : 1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === 1 ? 'bg-emerald-600 text-white border-emerald-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === 1 ? null : teamAColor.controlIdleStyle"
                      title="Buena actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.3l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 6.59 7.41C6.22 7.78 6 8.3 6 8.83V19c0 1.1.9 2 2 2h9c.82 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === -1 ? null : -1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === -1 ? 'bg-rose-600 text-white border-rose-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === -1 ? null : teamAColor.controlIdleStyle"
                      title="Mala actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M15 3H6c-.82 0-1.54.5-1.84 1.22L1.14 11.27c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.3l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10 23l6.41-6.41c.37-.37.59-.89.59-1.42V5c0-1.1-.9-2-2-2zm3 0v12h4V3h-4z"/></svg>
                    </button>
                    <div class="flex flex-col items-center gap-0.5">
                      <button
                        type="button"
                        :disabled="obtenerEstadoAmistadJugador(jugador.id) !== 'disponible'"
                        @click="enviarSolicitudAmistadVotacion(jugador.id)"
                        :class="[
                          'h-7 min-w-7 px-1.5 rounded-md border flex items-center justify-center transition disabled:cursor-not-allowed text-[10px] font-semibold',
                          obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'bg-emerald-50 border-emerald-300 text-emerald-700' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'bg-slate-100 border-slate-300 text-slate-500' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviando' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? '' : '',
                        ]"
                        :style="obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? teamAColor.controlIdleStyle : null"
                        :title="obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'No puedes enviarte amistad' : (obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'Ya sois amigos' : (obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'Solicitud ya enviada' : 'Enviar solicitud de amistad'))"
                      >
                        <AppIcon v-if="obtenerEstadoAmistadJugador(jugador.id) === 'amigo'" name="users" :size="12" class="text-emerald-700" />
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviada'">↗</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'self'">—</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviando'">...</span>
                        <AppIcon v-else name="users" :size="12" />
                      </button>
                      <span
                        v-if="textoEstadoAmistadJugador(jugador.id)"
                        class="text-[9px] leading-none font-medium"
                        :class="obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'text-emerald-700' : 'text-[color:var(--color-secondary)]'"
                      >
                        {{ textoEstadoAmistadJugador(jugador.id) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </article>

            <article class="rounded-lg border-2 p-3 space-y-2" :style="teamBColor.cardStyle">
              <div class="flex items-center justify-between gap-2">
                <h4 class="font-bold" :style="teamBColor.titleStyle">Equipo {{ partido.colorEquipoB || 'B' }}</h4>
              </div>
              <div v-if="!partido.equipoB || partido.equipoB.length === 0" class="text-xs" :style="teamBColor.subtitleStyle">Sin jugadores</div>
              <div v-else class="space-y-1.5">
                <div v-for="jugador in partido.equipoB" :key="`voto-b-part-${jugador.id}`" :class="['rounded-lg border px-2 py-1.5 flex items-center justify-between gap-1.5', esUsuarioActual(jugador.id) ? 'border-[color:var(--color-primary)] shadow-[0_0_0_2px_rgba(151,240,125,0.3)]' : '']" :style="teamBColor.playerRowStyle">
                  <span class="inline-flex items-center gap-1 text-xs font-medium truncate" :style="teamBColor.titleStyle">
                    <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-4 h-4 rounded-full bg-emerald-100 text-emerald-700" title="Pista pagada"><AppIcon name="check" :size="10" /></span>
                    <span>{{ jugador.nombre }}</span>
                  </span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      @click="toggleJugadorDiferencial(jugador.id)"
                      :disabled="esUsuarioActual(jugador.id)"
                      :class="[
                        'h-7 w-7 rounded-md border flex items-center justify-center transition disabled:opacity-50 disabled:cursor-not-allowed',
                        esUsuarioActual(jugador.id)
                          ? 'bg-slate-100 text-slate-400 border-slate-200'
                            : (votacionForm.jugadoresDiferenciales.includes(jugador.id) ? 'bg-amber-500 text-white border-amber-500' : '')
                      ]"
                      :style="!esUsuarioActual(jugador.id) && !votacionForm.jugadoresDiferenciales.includes(jugador.id) ? teamBColor.controlIdleStyle : null"
                      :title="esUsuarioActual(jugador.id) ? 'No puedes marcarte como diferencial' : 'Jugador diferencial'"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.3l-6.16 3.24 1.18-6.88L2 8.86l6.92-1L12 1.6l3.08 6.26 6.92 1-5.02 4.8 1.18 6.88z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === 1 ? null : 1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === 1 ? 'bg-emerald-600 text-white border-emerald-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === 1 ? null : teamBColor.controlIdleStyle"
                      title="Buena actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.3l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 6.59 7.41C6.22 7.78 6 8.3 6 8.83V19c0 1.1.9 2 2 2h9c.82 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                    </button>
                    <button
                      v-if="esCompaneroAsignado(jugador.id)"
                      type="button"
                      @click="setValoracionCompanero(jugador.id, getValoracionCompanero(jugador.id) === -1 ? null : -1)"
                      :class="['h-7 w-7 rounded-md border flex items-center justify-center transition', getValoracionCompanero(jugador.id) === -1 ? 'bg-rose-600 text-white border-rose-600' : '']"
                      :style="getValoracionCompanero(jugador.id) === -1 ? null : teamBColor.controlIdleStyle"
                      title="Mala actitud"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M15 3H6c-.82 0-1.54.5-1.84 1.22L1.14 11.27c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.3l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10 23l6.41-6.41c.37-.37.59-.89.59-1.42V5c0-1.1-.9-2-2-2zm3 0v12h4V3h-4z"/></svg>
                    </button>
                    <div class="flex flex-col items-center gap-0.5">
                      <button
                        type="button"
                        :disabled="obtenerEstadoAmistadJugador(jugador.id) !== 'disponible'"
                        @click="enviarSolicitudAmistadVotacion(jugador.id)"
                        :class="[
                          'h-7 min-w-7 px-1.5 rounded-md border flex items-center justify-center transition disabled:cursor-not-allowed text-[10px] font-semibold',
                          obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'bg-emerald-50 border-emerald-300 text-emerald-700' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'bg-slate-100 border-slate-300 text-slate-500' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'enviando' ? 'bg-[color:rgba(151,240,125,0.16)] border-[color:rgba(151,240,125,0.36)] text-[color:var(--color-secondary)]' : '',
                          obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? '' : '',
                        ]"
                        :style="obtenerEstadoAmistadJugador(jugador.id) === 'disponible' ? teamBColor.controlIdleStyle : null"
                        :title="obtenerEstadoAmistadJugador(jugador.id) === 'self' ? 'No puedes enviarte amistad' : (obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'Ya sois amigos' : (obtenerEstadoAmistadJugador(jugador.id) === 'enviada' ? 'Solicitud ya enviada' : 'Enviar solicitud de amistad'))"
                      >
                        <AppIcon v-if="obtenerEstadoAmistadJugador(jugador.id) === 'amigo'" name="users" :size="12" class="text-emerald-700" />
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviada'">↗</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'self'">—</span>
                        <span v-else-if="obtenerEstadoAmistadJugador(jugador.id) === 'enviando'">...</span>
                        <AppIcon v-else name="users" :size="12" />
                      </button>
                      <span
                        v-if="textoEstadoAmistadJugador(jugador.id)"
                        class="text-[9px] leading-none font-medium"
                        :class="obtenerEstadoAmistadJugador(jugador.id) === 'amigo' ? 'text-emerald-700' : 'text-[color:var(--color-secondary)]'"
                      >
                        {{ textoEstadoAmistadJugador(jugador.id) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </article>
          </section>

          <div class="text-xs text-slate-500">
            Puedes votar a cualquier jugador del partido excepto a ti mismo.
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <BaseButton variant="secondary" :loading="guardandoVotacion" :disabled="actaCerrada" @click="guardarVotacion">
              Guardar votos de jugadores
            </BaseButton>
          </div>

          <p v-if="mensajeVotacion" class="text-sm font-medium text-slate-700">{{ mensajeVotacion }}</p>

        </div>

      </section>
    </template>

    <div v-if="mostrarModalDiscusionResultado && !actaCerrada" class="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4">
      <div class="card-surface w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div class="sticky top-0 z-10 flex items-center justify-between gap-3 border-b border-slate-200 bg-white/95 px-4 py-3 backdrop-blur">
          <div>
            <h3 class="text-lg font-bold text-slate-900">Discutir resultado</h3>
            <p class="text-xs text-slate-600">Tu resultado se sumará a la media compartida.</p>
          </div>
          <BaseButton size="sm" variant="ghost" @click="cerrarModalDiscusionResultado">Cerrar</BaseButton>
        </div>

        <div class="space-y-4 p-4">
          <div class="grid grid-cols-2 gap-2">
            <label class="block">
              <span class="block text-xs font-medium text-slate-700 mb-1">Goles equipo A</span>
              <input v-model.number="votacionForm.golesEquipoAPropuesto" :disabled="actaCerrada" type="number" min="0" class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-slate-100" />
            </label>
            <label class="block">
              <span class="block text-xs font-medium text-slate-700 mb-1">Goles equipo B</span>
              <input v-model.number="votacionForm.golesEquipoBPropuesto" :disabled="actaCerrada" type="number" min="0" class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm disabled:cursor-not-allowed disabled:bg-slate-100" />
            </label>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Intensidad del partido</label>
              <select v-model="votacionForm.intensidadPartido" :disabled="actaCerrada" class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white disabled:cursor-not-allowed disabled:bg-slate-100">
                <option value="BAJO">Bajo</option>
                <option value="MEDIO">Medio</option>
                <option value="ALTO">Alto</option>
                <option value="MUY_ALTO">Muy alto</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Balanceado</label>
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  :disabled="actaCerrada"
                  @click="votacionForm.partidoFueParejo = true"
                  :class="[
                    'px-2.5 py-1.5 rounded-lg border text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed',
                    votacionForm.partidoFueParejo ? 'bg-emerald-600 text-white border-emerald-600' : 'bg-white text-slate-700 border-slate-300'
                  ]"
                >
                  Sí
                </button>
                <button
                  type="button"
                  :disabled="actaCerrada"
                  @click="votacionForm.partidoFueParejo = false"
                  :class="[
                    'px-2.5 py-1.5 rounded-lg border text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed',
                    !votacionForm.partidoFueParejo ? 'bg-rose-600 text-white border-rose-600' : 'bg-white text-slate-700 border-slate-300'
                  ]"
                >
                  No
                </button>
              </div>
            </div>
          </div>

          <div class="flex items-center justify-end gap-2">
            <BaseButton variant="secondary" @click="cerrarModalDiscusionResultado">Cancelar</BaseButton>
            <BaseButton :loading="guardandoVotacion" :disabled="actaCerrada" @click="guardarVotacion">Enviar votación</BaseButton>
          </div>
        </div>
      </div>
    </div>

    <section v-if="!loading && !error && !partido" class="state-empty">
      <p class="text-slate-700 font-medium">Partido no encontrado.</p>
      <div class="mt-3">
        <BaseButton variant="secondary" @click="handleGoBack">Volver</BaseButton>
      </div>
    </section>

    <div v-if="mostrarModalInvitar" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="card-surface max-w-md w-full max-h-96 flex flex-col">
        <div class="p-4 border-b border-slate-200 flex justify-between items-center">
          <h3 class="text-lg font-bold text-slate-800">Invitar amistades</h3>
          <BaseButton size="sm" variant="ghost" @click="mostrarModalInvitar = false">Cerrar</BaseButton>
        </div>

        <div class="flex-1 overflow-y-auto p-4">
          <div v-if="amigos.length === 0" class="text-center text-slate-500 py-8">
            <p>No tienes amistades para invitar.</p>
          </div>

          <div v-else class="space-y-3">
            <div v-for="amigo in amigos" :key="amigo.id" class="bg-slate-50 rounded-xl p-3 flex justify-between items-center border border-slate-200 gap-2">
              <div>
                <p class="font-semibold text-slate-800">{{ obtenerNombreAmigo(amigo) }}</p>
              </div>
              <BaseButton
                size="sm"
                :loading="invitandoA === obtenerIdAmigo(amigo)"
                @click="invitarAmigo(obtenerIdAmigo(amigo))"
              >
                Invitar
              </BaseButton>
            </div>
          </div>
        </div>

        <div class="p-4 border-t border-slate-200">
          <BaseButton block variant="secondary" @click="mostrarModalInvitar = false">Cerrar</BaseButton>
        </div>
      </div>
    </div>
  </div>
</template>
