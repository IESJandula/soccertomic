import { defineStore } from 'pinia'
import { ref } from 'vue'
import playerProfileService from '../services/playerProfileService'

export const usePlayerProfileStore = defineStore('playerProfile', () => {
  const profile = ref(null)
  const loading = ref(false)
  const error = ref('')

  const playStyleOptions = [
    { value: 'O', label: 'Ofensivo' },
    { value: 'D', label: 'Defensivo' },
    { value: 'A', label: 'Adaptable' },
    { value: 'G', label: 'Portero' },
  ]

  const cargarMiPerfil = async () => {
    loading.value = true
    error.value = ''
    try {
      const data = await playerProfileService.obtenerMiPerfil()
      profile.value = data
      return data
    } catch (err) {
      if (err?.status === 404) {
        profile.value = null
        return null
      }
      error.value = err?.message || 'No se pudo cargar el perfil'
      throw err
    } finally {
      loading.value = false
    }
  }

  const guardarMiPerfil = async (payload) => {
    loading.value = true
    error.value = ''
    try {
      const data = await playerProfileService.guardarMiPerfil(payload)
      profile.value = data
      return { success: true, data }
    } catch (err) {
      error.value = err?.message || 'No se pudo guardar el perfil'
      return { success: false, message: error.value }
    } finally {
      loading.value = false
    }
  }

  return {
    profile,
    loading,
    error,
    playStyleOptions,
    cargarMiPerfil,
    guardarMiPerfil,
  }
})
