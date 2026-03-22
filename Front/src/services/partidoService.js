import apiService from './apiService'
import { ENDPOINTS } from '../config'

class PartidoService {
  // ======================== PARTIDOS ========================

  async crearPartido(data) {
    return apiService.request(ENDPOINTS.PARTIDOS, {
      method: 'POST',
      useCache: false,
      body: JSON.stringify(data),
    })
  }

  async obtenerMisPartidos() {
    return apiService.request(ENDPOINTS.PARTIDOS_MIS, {
      method: 'GET',
      useCache: false,
    })
  }

  async obtenerMiHistorial() {
    return apiService.request(ENDPOINTS.PARTIDOS_HISTORIAL, {
      method: 'GET',
      useCache: false,
    })
  }

  async obtenerHistorialPublico(usuarioId) {
    return apiService.request(ENDPOINTS.PARTIDOS_HISTORIAL_PUBLICO_BY_USER(usuarioId), {
      method: 'GET',
      useCache: false,
    })
  }

  async obtenerPartidosFuturos() {
    return apiService.request(ENDPOINTS.PARTIDOS_FUTUROS, {
      method: 'GET',
      useCache: false,
    })
  }

  async actualizarPartido(id, data) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${id}`, {
      method: 'PUT',
      useCache: false,
      body: JSON.stringify(data),
    })
  }

  async cambiarTipoPartido(id, tipo) {
    const url = `${ENDPOINTS.PARTIDOS}/${id}/tipo?tipo=${tipo}`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  // ======================== DETALLE PARTIDO & INSCRIPCIONES ========================

  async obtenerDetallePartido(id) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${id}/detalle`, {
      method: 'GET',
      useCache: false,
    })
  }

  async inscribirseAPartido(id) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${id}/inscribirse`, {
      method: 'POST',
      useCache: false,
    })
  }

  async desinscribirseAPartido(id) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${id}/desinscribirse`, {
      method: 'POST',
      useCache: false,
    })
  }

  async moverJugadorAEquipo(partidoId, usuarioId, equipo) {
    const url = `${ENDPOINTS.PARTIDOS}/${partidoId}/jugadores/${usuarioId}/equipo?equipo=${equipo}`
    return apiService.request(url, {
      method: 'PUT',
      useCache: false,
    })
  }

  async moverJugadorASinEquipo(partidoId, usuarioId) {
    const url = `${ENDPOINTS.PARTIDOS}/${partidoId}/jugadores/${usuarioId}/sin-equipo`
    return apiService.request(url, {
      method: 'POST',
      useCache: false,
    })
  }

  async eliminarPartido(id) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${id}`, {
      method: 'DELETE',
      useCache: false,
    })
  }

  async balancearEquipos(partidoId, playerIds) {
    return apiService.request(ENDPOINTS.PARTIDOS_BALANCEAR(partidoId), {
      method: 'POST',
      useCache: false,
      body: JSON.stringify({ playerIds }),
    })
  }

  async asignarPosiciones(partidoId, formacion = 'formation-1') {
    const detalle = await this.obtenerDetallePartido(partidoId)
    const playerIds = [
      ...(detalle.jugadoresInscritos || []),
      ...(detalle.equipoA || []),
      ...(detalle.equipoB || []),
    ].map((j) => j.id)

    // Consolidated balancing contract: single endpoint
    return this.balancearEquipos(partidoId, playerIds)
  }

  async balancearEquiposAutomaticamente(partidoId) {
    const detalle = await this.obtenerDetallePartido(partidoId)
    const jugadoresIds = detalle.jugadoresInscritos.map(j => j.id)
    return this.balancearEquipos(partidoId, jugadoresIds)
  }

  async obtenerAnalisisBalance(partidoId) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${partidoId}/analisis-balance`, {
      method: 'GET',
      useCache: false,
    })
  }

  async asignarOrganizador(partidoId, usuarioId) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${partidoId}/organizadores`, {
      method: 'POST',
      useCache: false,
      body: JSON.stringify({ usuarioId }),
    })
  }

  async revocarOrganizador(partidoId, usuarioId) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${partidoId}/organizadores/${usuarioId}`, {
      method: 'DELETE',
      useCache: false,
    })
  }

  async salirComoOrganizador(partidoId, payload = {}) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${partidoId}/organizadores/salir`, {
      method: 'POST',
      useCache: false,
      body: JSON.stringify(payload),
    })
  }

  async reservarPista(partidoId, precioPista) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${partidoId}/reservar-pista`, {
      method: 'POST',
      useCache: false,
      body: JSON.stringify({ precioPista }),
    })
  }

  async obtenerEstadoPagoReserva(partidoId) {
    return apiService.request(`${ENDPOINTS.PARTIDOS}/${partidoId}/reservas/estado-pago`, {
      method: 'GET',
      useCache: false,
    })
  }
}

export default new PartidoService()
