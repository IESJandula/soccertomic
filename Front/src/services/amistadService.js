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

  async obtenerMisEquipos() {
    return apiService.request(ENDPOINTS.EQUIPOS_RAPIDOS_MIS, {
      method: 'GET',
      useCache: false,
    })
  }

  async actualizarEquipoRapido(equipoId, nombre, capacidad) {
    return apiService.request(`${ENDPOINTS.EQUIPOS_RAPIDOS}/${equipoId}`, {
      method: 'PUT',
      body: JSON.stringify({
        nombre,
        capacidad,
        miembroIds: [],
      }),
      useCache: false,
    })
  }

  async agregarMiembroEquipo(equipoId, usuarioId) {
    return apiService.request(`${ENDPOINTS.EQUIPOS_RAPIDOS}/${equipoId}/miembros?usuarioId=${usuarioId}`, {
      method: 'POST',
      useCache: false,
    })
  }

  async quitarMiembroEquipo(equipoId, usuarioId) {
    return apiService.request(`${ENDPOINTS.EQUIPOS_RAPIDOS}/${equipoId}/miembros/${usuarioId}`, {
      method: 'DELETE',
      useCache: false,
    })
  }

  async salirDeEquipo(equipoId) {
    return apiService.request(`${ENDPOINTS.EQUIPOS_RAPIDOS}/${equipoId}/salir`, {
      method: 'POST',
      useCache: false,
    })
  }

  async crearEquipoRapido(nombre, miembroIds = [], capacidad = 7) {
    return apiService.request(ENDPOINTS.EQUIPOS_RAPIDOS, {
      method: 'POST',
      body: JSON.stringify({
        nombre,
        miembroIds,
        capacidad,
      }),
      useCache: false,
    })
  }

  async eliminarEquipoRapido(equipoId) {
    return apiService.request(`${ENDPOINTS.EQUIPOS_RAPIDOS}/${equipoId}`, {
      method: 'DELETE',
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
