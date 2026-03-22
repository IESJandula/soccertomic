import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import invitacionService from '../services/invitacionService'
import amistadService from '../services/amistadService'

export const useNotificationsStore = defineStore('notifications', () => {
  const invitacionesPendientes = ref([])
  const notificacionesReserva = ref([])
  const solicitudesPendientes = ref([])
  const loading = ref(false)

  const totalNotificaciones = computed(() => {
    return invitacionesPendientes.value.length + notificacionesReserva.value.length + solicitudesPendientes.value.length
  })

  const cargarNotificaciones = async () => {
    loading.value = true
    try {
      const [invitaciones, solicitudes] = await Promise.all([
        invitacionService.obtenerMisInvitaciones(),
        amistadService.obtenerSolicitudesPendientes(),
      ])
      const listaInvitaciones = invitaciones || []
      invitacionesPendientes.value = listaInvitaciones.filter(i => i.estado === 'PENDIENTE')
      notificacionesReserva.value = listaInvitaciones.filter(i => i.precioTotalPista !== null && i.precioTotalPista !== undefined && !Boolean(i.pagada))
      solicitudesPendientes.value = solicitudes || []
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
  }

  return {
    invitacionesPendientes,
    notificacionesReserva,
    solicitudesPendientes,
    totalNotificaciones,
    loading,
    cargarNotificaciones,
    limpiar,
  }
})
