import apiService from './apiService'
import { ENDPOINTS } from '../config'

class PartidoVotacionService {
  async guardarMiVoto(partidoId, payload) {
    return apiService.request(ENDPOINTS.PARTIDOS_VOTACION_ME(partidoId), {
      method: 'POST',
      useCache: false,
      body: JSON.stringify(payload),
    })
  }

  async obtenerMiVoto(partidoId) {
    return apiService.request(ENDPOINTS.PARTIDOS_VOTACION_ME(partidoId), {
      method: 'GET',
      useCache: false,
    })
  }

  async obtenerAsignacion(partidoId) {
    return apiService.request(ENDPOINTS.PARTIDOS_VOTACION_ASIGNACION(partidoId), {
      method: 'GET',
      useCache: false,
    })
  }

  async obtenerPanelCompartido(partidoId) {
    return apiService.request(ENDPOINTS.PARTIDOS_VOTACION_PANEL_COMPARTIDO(partidoId), {
      method: 'GET',
      useCache: false,
    })
  }
}

export default new PartidoVotacionService()
