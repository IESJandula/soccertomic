import apiService from './apiService'
import { ENDPOINTS } from '../config'

class AmistadService {
  // Enviar solicitud de amistad
  async enviarSolicitud(usuarioBId) {
    const url = `${ENDPOINTS.AMISTADES}?usuarioBId=${usuarioBId}`
    return apiService.request(url, {
      method: 'POST',
      useCache: false,
    })
  }

  // Obtener mis amigos aceptados
  async obtenerMisAmigos() {
    return apiService.request(ENDPOINTS.AMISTADES_MIS, {
      method: 'GET',
      useCache: false,
    })
  }

  // Obtener solicitudes pendientes
  async obtenerSolicitudesPendientes() {
    return apiService.request(ENDPOINTS.AMISTADES_SOLICITUDES, {
      method: 'GET',
      useCache: false,
    })
  }

  async obtenerSolicitudesEnviadas() {
    return apiService.request(ENDPOINTS.AMISTADES_SOLICITUDES_ENVIADAS, {
      method: 'GET',
      useCache: false,
    })
  }

  // Aceptar solicitud de amistad
  async aceptarSolicitud(amistadId) {
    const url = `${ENDPOINTS.AMISTADES}/${amistadId}/aceptar`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  // Rechazar solicitud de amistad
  async rechazarSolicitud(amistadId) {
    const url = `${ENDPOINTS.AMISTADES}/${amistadId}/rechazar`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  // Eliminar amistad
  async eliminarAmistad(amistadId) {
    const url = `${ENDPOINTS.AMISTADES}/${amistadId}`
    return apiService.request(url, {
      method: 'DELETE',
      useCache: false,
    })
  }
}

export default new AmistadService()
