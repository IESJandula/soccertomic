import apiService from './apiService'
import { ENDPOINTS } from '../config'

class InvitacionService {
  // Invitar usuario a partido
  async invitarAPartido(partidoId, usuarioId) {
    const url = `${ENDPOINTS.INVITACIONES}?partidoId=${partidoId}&usuarioId=${usuarioId}`
    return apiService.request(url, {
      method: 'POST',
      useCache: false,
    })
  }

  // Obtener todas mis invitaciones
  async obtenerMisInvitaciones() {
    return apiService.request(ENDPOINTS.INVITACIONES_MIS, {
      method: 'GET',
      useCache: false,
    })
  }

  // Obtener invitaciones pendientes
  async obtenerInvitacionesPendientes() {
    return apiService.request(ENDPOINTS.INVITACIONES_PENDIENTES, {
      method: 'GET',
      useCache: false,
    })
  }

  // Aceptar invitación
  async aceptarInvitacion(invitacionId) {
    const url = `${ENDPOINTS.INVITACIONES}/${invitacionId}/aceptar`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  // Rechazar invitación
  async rechazarInvitacion(invitacionId) {
    const url = `${ENDPOINTS.INVITACIONES}/${invitacionId}/rechazar`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  async marcarReservaComoPagada(invitacionId) {
    const url = `${ENDPOINTS.INVITACIONES}/${invitacionId}/marcar-pagada`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  async invitarAEquipo(equipoRapidoId, usuarioId) {
    const url = `${ENDPOINTS.EQUIPO_INVITACIONES}?equipoRapidoId=${equipoRapidoId}&usuarioId=${usuarioId}`
    return apiService.request(url, {
      method: 'POST',
      useCache: false,
    })
  }

  async obtenerMisInvitacionesEquipo() {
    return apiService.request(ENDPOINTS.EQUIPO_INVITACIONES_MIS, {
      method: 'GET',
      useCache: false,
    })
  }

  async aceptarInvitacionEquipo(invitacionId) {
    const url = `${ENDPOINTS.EQUIPO_INVITACIONES}/${invitacionId}/aceptar`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  async rechazarInvitacionEquipo(invitacionId) {
    const url = `${ENDPOINTS.EQUIPO_INVITACIONES}/${invitacionId}/rechazar`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }
}

export default new InvitacionService()
