import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import invitacionService from '../services/invitacionService'
import amistadService from '../services/amistadService'

export const useNotificationsStore = defineStore('notifications', () => {
  const invitacionesPendientes = ref([])
  const notificacionesReserva = ref([])
  const solicitudesPendientes = ref([])
  const notificacionesEquipo = ref([])
  const loading = ref(false)

  const totalNotificaciones = computed(() => {
    return invitacionesPendientes.value.length + notificacionesReserva.value.length + solicitudesPendientes.value.length + notificacionesEquipo.value.length
  })

  const cargarNotificaciones = async () => {
    loading.value = true
    try {
      const [invitaciones, solicitudes] = await Promise.all([
        invitacionService.obtenerMisInvitaciones(),
        amistadService.obtenerSolicitudesPendientes(),
      ])
      const notificacionesEquipoRespuesta = await invitacionService.obtenerMisNotificacionesEquipo()
      const listaInvitaciones = invitaciones || []
      invitacionesPendientes.value = listaInvitaciones.filter(i => i.estado === 'PENDIENTE')
      notificacionesReserva.value = listaInvitaciones.filter(i => i.precioTotalPista !== null && i.precioTotalPista !== undefined && !Boolean(i.pagada))
      solicitudesPendientes.value = solicitudes || []
      notificacionesEquipo.value = notificacionesEquipoRespuesta || []
    } catch (error) {
      console.error('Error cargando notificaciones:', error)
    } finally {
      loading.value = false
    }
  }

  const limpiar = () => {
    invitacionesPendientes.value = []
    notificacionesReserva.value = []
    solicitudesPendientes.value = []
    notificacionesEquipo.value = []
  }

  return {
    invitacionesPendientes,
    notificacionesReserva,
    solicitudesPendientes,
    notificacionesEquipo,
    totalNotificaciones,
    loading,
    cargarNotificaciones,
    limpiar,
  }
})
