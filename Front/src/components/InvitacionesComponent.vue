<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '../stores/ui'
import { useNotificationsStore } from '../stores/notifications'
import invitacionService from '../services/invitacionService'
import partidoService from '../services/partidoService'
import amistadService from '../services/amistadService'
import BaseButton from './ui/BaseButton.vue'
import StatusBadge from './ui/StatusBadge.vue'
import AppIcon from './ui/AppIcon.vue'
import { formatDateTimeEs } from '../utils/dateFormat'

const router = useRouter()
const uiStore = useUiStore()
const notificationsStore = useNotificationsStore()

const invitaciones = ref([])
const invitacionesEquipo = ref([])
const notificacionesInfo = ref([])
const solicitudesAmistad = ref([])
const invitacionesAceptadas = ref([])
const invitacionesCanceladas = ref([])
const invitacionesReservadas = ref([])
const invitacionesReservadasPagadas = ref([])
const mostrarInvitacionesAceptadas = ref(false)
const mostrarReservasPagadas = ref(false)
const loading = ref(true)
const procesando = ref(null)

onMounted(async () => {
  await cargarTodo()
})

const cargarTodo = async () => {
  try {
    loading.value = true
    const [todasInvitaciones, solicitudes] = await Promise.all([
      invitacionService.obtenerMisInvitaciones(),
      amistadService.obtenerSolicitudesPendientes(),
    ])
    invitacionesEquipo.value = await invitacionService.obtenerMisInvitacionesEquipo()
    const esReserva = (inv) => inv && (inv.precioTotalPista !== null && inv.precioTotalPista !== undefined)
    invitaciones.value = todasInvitaciones.filter(i => i.estado === 'PENDIENTE')
    invitacionesAceptadas.value = todasInvitaciones.filter(i => i.estado === 'ACEPTADA' && !esReserva(i) && !i.mensaje)
    notificacionesInfo.value = todasInvitaciones.filter(i => i.estado === 'ACEPTADA' && !esReserva(i) && Boolean(i.mensaje))
    invitacionesCanceladas.value = todasInvitaciones.filter(i => i.estado === 'CANCELADA')
    invitacionesReservadas.value = todasInvitaciones.filter(i => esReserva(i) && !Boolean(i.pagada))
    invitacionesReservadasPagadas.value = todasInvitaciones.filter(i => esReserva(i) && Boolean(i.pagada))
    solicitudesAmistad.value = solicitudes
    
    // Recargar contador de notificaciones
    await notificationsStore.cargarNotificaciones()
  } catch (error) {
    console.error('Error cargando notificaciones:', error)
  } finally {
    loading.value = false
  }
}

const formatearFecha = (fecha) => {
  return formatDateTimeEs(fecha)
}

const formatearMoneda = (valor) => {
  if (valor === null || valor === undefined || Number.isNaN(Number(valor))) {
    return null
  }
  return Number(valor).toLocaleString('es-ES', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

const aceptarInvitacion = async (invitacionId, partidoId) => {
  procesando.value = invitacionId
  try {
    await invitacionService.aceptarInvitacion(invitacionId)

    try {
      await partidoService.inscribirseAPartido(partidoId)
    } catch (error) {
      console.warn('No se pudo inscribir automáticamente al partido:', error)
    }

    uiStore.showToast({ message: 'Invitación aceptada.', type: 'success' })
    await cargarTodo()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al aceptar invitación.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const aceptarInvitacionEquipo = async (invitacionId) => {
  procesando.value = `equipo-${invitacionId}`
  try {
    await invitacionService.aceptarInvitacionEquipo(invitacionId)
    uiStore.showToast({ message: 'Te uniste al equipo correctamente.', type: 'success' })
    await cargarTodo()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al aceptar invitacion de equipo.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const rechazarInvitacionEquipo = async (invitacionId) => {
  procesando.value = `equipo-${invitacionId}`
  try {
    await invitacionService.rechazarInvitacionEquipo(invitacionId)
    uiStore.showToast({ message: 'Invitacion de equipo rechazada.', type: 'info' })
    await cargarTodo()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al rechazar invitacion de equipo.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const rechazarInvitacion = async (invitacionId) => {
  procesando.value = invitacionId
  try {
    await invitacionService.rechazarInvitacion(invitacionId)
    uiStore.showToast({ message: 'Invitación rechazada.', type: 'info' })
    await cargarTodo()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al rechazar invitación.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const aceptarSolicitudAmistad = async (solicitudId) => {
  procesando.value = `amistad-${solicitudId}`
  try {
    await amistadService.aceptarSolicitud(solicitudId)
    uiStore.showToast({ message: 'Solicitud de amistad aceptada.', type: 'success' })
    await cargarTodo()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al aceptar solicitud.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const rechazarSolicitudAmistad = async (solicitudId) => {
  procesando.value = `amistad-${solicitudId}`
  try {
    await amistadService.rechazarSolicitud(solicitudId)
    uiStore.showToast({ message: 'Solicitud de amistad rechazada.', type: 'info' })
    await cargarTodo()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al rechazar solicitud.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const marcarReservaComoPagada = async (invitacionId) => {
  procesando.value = `reserva-${invitacionId}`
  try {
    await invitacionService.marcarReservaComoPagada(invitacionId)
    uiStore.showToast({ message: 'Pago confirmado. Notificación archivada.', type: 'success' })
    await cargarTodo()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'No se pudo marcar como pagada.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const irAPartido = (partidoId) => {
  router.push(`/dashboard/partidos/${partidoId}`)
}
</script>

<template>
  <div class="space-y-4 md:space-y-6">

    <section v-if="loading" class="state-loading text-sm text-slate-600">
      Cargando notificaciones...
    </section>

    <template v-else>
      <!-- Solicitudes de amistad pendientes -->
      <section v-if="solicitudesAmistad.length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="users" :size="18" />Solicitudes de amistad ({{ solicitudesAmistad.length }})</h3>
        <div class="space-y-3 mt-3">
          <article v-for="solicitud in solicitudesAmistad" :key="solicitud.id" class="border border-slate-200 bg-slate-50 rounded-xl p-3 space-y-2">
            <div class="flex items-start justify-between gap-2">
              <div>
                <p class="text-sm font-semibold text-slate-800">{{ solicitud.usuarioA.nombre }}</p>
                <p class="text-xs text-slate-600">{{ solicitud.usuarioA.email }}</p>
                <p class="text-xs text-slate-500 mt-1">{{ formatearFecha(solicitud.creadaEn) }}</p>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-2">
              <BaseButton
                block
                size="sm"
                :loading="procesando === `amistad-${solicitud.id}`"
                @click="aceptarSolicitudAmistad(solicitud.id)"
              >
                Aceptar
              </BaseButton>
              <BaseButton
                block
                size="sm"
                variant="danger"
                :loading="procesando === `amistad-${solicitud.id}`"
                @click="rechazarSolicitudAmistad(solicitud.id)"
              >
                Rechazar
              </BaseButton>
            </div>
          </article>
        </div>
      </section>

      <!-- Pistas reservadas -->
      <section v-if="invitacionesReservadas.length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="check" :size="18" />Pistas reservadas ({{ invitacionesReservadas.length }})</h3>
        <div class="space-y-3 mt-3">
          <article v-for="inv in invitacionesReservadas" :key="inv.id" class="border border-blue-200 bg-blue-50 rounded-xl p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">Partido reservado</p>
              <StatusBadge status="RESERVADA" />
            </div>
            <p class="text-sm text-slate-700">{{ inv.mensaje || 'La pista de este partido ya está reservada.' }}</p>
            <p v-if="inv.parteIndividual !== null && inv.parteIndividual !== undefined" class="text-sm text-slate-700 font-semibold">
              Tu parte: {{ formatearMoneda(inv.parteIndividual) }}
            </p>
            <p v-if="inv.precioTotalPista !== null && inv.precioTotalPista !== undefined" class="text-xs text-slate-600">
              Total pista: {{ formatearMoneda(inv.precioTotalPista) }}
            </p>
            <p class="text-xs text-slate-700 font-medium">
              Pagar a: {{ inv.reservadoPorNombre || 'Organización' }}
            </p>
            <p class="text-xs text-slate-500">{{ formatearFecha(inv.respondidaEn || inv.creadaEn) }}</p>
            <BaseButton
              block
              size="sm"
              :loading="procesando === `reserva-${inv.id}`"
              @click="marcarReservaComoPagada(inv.id)"
            >
              Marcar como pagado
            </BaseButton>
            <BaseButton block size="sm" variant="secondary" @click="irAPartido(inv.partido?.id || inv.partidoId)">
              Ir al partido
            </BaseButton>
          </article>
        </div>
      </section>

      <!-- Reservas pagadas (historial) -->
      <section v-if="invitacionesReservadasPagadas.length > 0" class="card-surface p-4 md:p-5">
        <button
          type="button"
          class="w-full flex items-center justify-between gap-3"
          @click="mostrarReservasPagadas = !mostrarReservasPagadas"
        >
          <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="check" :size="18" />Reservas pagadas ({{ invitacionesReservadasPagadas.length }})</h3>
          <span class="text-sm font-semibold text-slate-600">{{ mostrarReservasPagadas ? 'Ocultar' : 'Mostrar' }}</span>
        </button>

        <div v-if="mostrarReservasPagadas" class="space-y-3 mt-3">
          <article v-for="inv in invitacionesReservadasPagadas" :key="`pagada-${inv.id}`" class="border border-emerald-200 bg-emerald-50 rounded-xl p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">Pago confirmado</p>
              <StatusBadge status="ACEPTADA" />
            </div>
            <p class="text-sm text-slate-700">{{ inv.mensaje || 'Reserva de pista pagada.' }}</p>
            <p v-if="inv.parteIndividual !== null && inv.parteIndividual !== undefined" class="text-sm text-slate-700 font-semibold">
              Tu parte: {{ formatearMoneda(inv.parteIndividual) }}
            </p>
            <p v-if="inv.precioTotalPista !== null && inv.precioTotalPista !== undefined" class="text-xs text-slate-600">
              Total pista: {{ formatearMoneda(inv.precioTotalPista) }}
            </p>
            <p class="text-xs text-slate-700 font-medium">
              Pagar a: {{ inv.reservadoPorNombre || 'Organización' }}
            </p>
            <p class="text-xs text-slate-500">{{ formatearFecha(inv.respondidaEn || inv.creadaEn) }}</p>
            <BaseButton block size="sm" variant="secondary" @click="irAPartido(inv.partido?.id || inv.partidoId)">
              Ir al partido
            </BaseButton>
          </article>
        </div>
      </section>

      <!-- Invitaciones a partidos pendientes -->
      <section v-if="invitaciones.length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="soccer" :size="18" />Invitaciones a partidos ({{ invitaciones.length }})</h3>
        <div class="space-y-3 mt-3">
          <article v-for="inv in invitaciones" :key="inv.id" class="border border-amber-200 bg-amber-50 rounded-xl p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">Pendiente</p>
              <StatusBadge status="PENDIENTE" />
            </div>
            <p class="text-sm text-slate-600">{{ inv.usuario.nombre }} te invitó • {{ formatearFecha(inv.creadaEn) }}</p>

            <div class="grid grid-cols-2 gap-2">
              <BaseButton
                block
                size="sm"
                :loading="procesando === inv.id"
                @click="aceptarInvitacion(inv.id, inv.partido?.id || inv.partidoId)"
              >
                Aceptar
              </BaseButton>
              <BaseButton
                block
                size="sm"
                variant="danger"
                :loading="procesando === inv.id"
                @click="rechazarInvitacion(inv.id)"
              >
                Rechazar
              </BaseButton>
            </div>
          </article>
        </div>
      </section>

      <!-- Notificaciones informativas -->
      <section v-if="notificacionesInfo.length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="bell" :size="18" />Notificaciones informativas ({{ notificacionesInfo.length }})</h3>
        <div class="space-y-3 mt-3">
          <article v-for="inv in notificacionesInfo" :key="`info-${inv.id}`" class="border border-indigo-200 bg-indigo-50 rounded-xl p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">Info de equipo</p>
              <StatusBadge status="ACEPTADA" />
            </div>
            <p class="text-sm text-slate-700">{{ inv.mensaje }}</p>
            <p class="text-xs text-slate-500">{{ formatearFecha(inv.respondidaEn || inv.creadaEn) }}</p>
            <BaseButton block size="sm" variant="secondary" @click="irAPartido(inv.partido?.id || inv.partidoId)">
              Ir al partido
            </BaseButton>
          </article>
        </div>
      </section>

      <!-- Invitaciones a equipos pendientes -->
      <section v-if="invitacionesEquipo.filter(i => i.estado === 'PENDIENTE').length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="users" :size="18" />Invitaciones a equipos ({{ invitacionesEquipo.filter(i => i.estado === 'PENDIENTE').length }})</h3>
        <div class="space-y-3 mt-3">
          <article v-for="inv in invitacionesEquipo.filter(i => i.estado === 'PENDIENTE')" :key="`equipo-${inv.id}`" class="border border-indigo-200 bg-indigo-50 rounded-xl p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">Invitacion de equipo</p>
              <StatusBadge status="PENDIENTE" />
            </div>
            <p class="text-sm text-slate-700">{{ inv.mensaje }}</p>
            <p class="text-xs text-slate-600">Equipo: {{ inv.equipoRapidoNombre }}</p>
            <p class="text-xs text-slate-600">Te invita: {{ inv.emisor?.nombre }}</p>
            <p class="text-xs text-slate-500">{{ formatearFecha(inv.creadaEn) }}</p>

            <div class="grid grid-cols-2 gap-2">
              <BaseButton
                block
                size="sm"
                :loading="procesando === `equipo-${inv.id}`"
                @click="aceptarInvitacionEquipo(inv.id)"
              >
                Aceptar
              </BaseButton>
              <BaseButton
                block
                size="sm"
                variant="danger"
                :loading="procesando === `equipo-${inv.id}`"
                @click="rechazarInvitacionEquipo(inv.id)"
              >
                Rechazar
              </BaseButton>
            </div>
          </article>
        </div>
      </section>

      <!-- Invitaciones aceptadas -->
      <section v-if="invitacionesAceptadas.length > 0" class="card-surface p-4 md:p-5">
        <button
          type="button"
          class="w-full flex items-center justify-between gap-3"
          @click="mostrarInvitacionesAceptadas = !mostrarInvitacionesAceptadas"
        >
          <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="check" :size="18" />Invitaciones aceptadas ({{ invitacionesAceptadas.length }})</h3>
          <span class="text-sm font-semibold text-slate-600">{{ mostrarInvitacionesAceptadas ? 'Ocultar' : 'Mostrar' }}</span>
        </button>

        <div v-if="mostrarInvitacionesAceptadas" class="space-y-3 mt-3">
          <article v-for="inv in invitacionesAceptadas" :key="inv.id" class="border border-emerald-200 bg-emerald-50 rounded-xl p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">Invitación aceptada</p>
              <StatusBadge status="ACEPTADA" />
            </div>
            <p class="text-sm text-slate-600">Respondida {{ formatearFecha(inv.respondidaEn) }}</p>
            <BaseButton block size="sm" variant="secondary" @click="irAPartido(inv.partido?.id || inv.partidoId)">
              Ir al partido
            </BaseButton>
          </article>
        </div>
      </section>

      <!-- Partidos cancelados -->
      <section v-if="invitacionesCanceladas.length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="warning" :size="18" />Partidos cancelados ({{ invitacionesCanceladas.length }})</h3>
        <div class="space-y-3 mt-3">
          <article v-for="inv in invitacionesCanceladas" :key="inv.id" class="border border-slate-200 bg-slate-50 rounded-xl p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-800">Partido cancelado</p>
              <StatusBadge status="CANCELADA" />
            </div>
            <p class="text-sm text-slate-600">El partido asociado a esta invitación fue cancelado.</p>
            <p class="text-xs text-slate-500">{{ formatearFecha(inv.respondidaEn || inv.creadaEn) }}</p>
          </article>
        </div>
      </section>

      <!-- Estado vacío -->
      <section v-if="invitaciones.length === 0 && invitacionesEquipo.filter(i => i.estado === 'PENDIENTE').length === 0 && notificacionesInfo.length === 0 && invitacionesAceptadas.length === 0 && invitacionesCanceladas.length === 0 && invitacionesReservadas.length === 0 && invitacionesReservadasPagadas.length === 0 && solicitudesAmistad.length === 0" class="state-empty">
        <p class="text-slate-700 font-medium">No tienes notificaciones por ahora.</p>
        <p class="text-caption mt-1">Las invitaciones y solicitudes de amistad aparecerán aquí.</p>
      </section>
    </template>
  </div>
</template>
