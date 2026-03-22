import apiService from './apiService'

class PlayerProfileService {
  async obtenerMiPerfil() {
    return apiService.getPlayerProfile()
  }

  async obtenerPerfilPublico(usuarioId) {
    return apiService.getPublicPlayerProfile(usuarioId)
  }

  async guardarMiPerfil(payload) {
    return apiService.savePlayerProfile(payload)
  }
}

export default new PlayerProfileService()
