import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiService from '../services/apiService'
import { clearIdentity, getAuthToken, setAuthToken } from '../services/authIdentity'
import { logoutFirebaseSession } from '../services/firebaseClient'
import { formatDateTimeEs } from '../utils/dateFormat'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const hasPlayerProfile = ref(null)
  const isAuthenticated = computed(() => user.value !== null && Boolean(getAuthToken()))

  const hydrateSessionUser = (resumen, email) => ({
    id: resumen?.id || null,
    email: email || '',
    name: resumen?.nombre || email || 'Usuario',
    rasgos: resumen?.rasgos || [],
    conocimiento: {
      predicciones: 0,
      aciertos: 0,
    },
    habilidad: {
      partidosJugados: 0,
      estrellas: 0,
    },
    loginTime: formatDateTimeEs(new Date()),
  })

  const refreshUsuario = async () => {
    if (!user.value) return

    try {
      const resumen = await apiService.getUsuarioResumen(user.value?.id)
      user.value.id = resumen?.id || user.value.id
      user.value.name = resumen?.nombre || user.value.name
      user.value.rasgos = resumen?.rasgos || []
      sessionStorage.setItem('user', JSON.stringify(user.value))
    } catch (error) {
      if (error?.status === 401) {
        logout()
        return
      }
      // Non-blocking - user context remains valid even if backend sync fails
      console.warn('Could not refresh user from backend:', error?.message)
    }
  }

  const checkPlayerProfileCompleted = async () => {
    if (!user.value) {
      hasPlayerProfile.value = false
      return false
    }

    try {
      await apiService.getPlayerProfile()
      hasPlayerProfile.value = true
      return true
    } catch (error) {
      if (error?.status === 401) {
        logout()
        return false
      }
      if (error?.status === 404) {
        hasPlayerProfile.value = false
        return false
      }

      console.warn('Could not validate player profile:', error?.message)
      hasPlayerProfile.value = false
      return false
    }
  }

  const loadUser = () => {
    if (!getAuthToken()) {
      clearIdentity()
      user.value = null
      hasPlayerProfile.value = null
      return
    }

    const storedUser = sessionStorage.getItem('user')
    if (storedUser) {
      user.value = JSON.parse(storedUser)
      refreshUsuario()
      checkPlayerProfileCompleted()
    }
  }

  const loginWithFirebaseToken = async ({ idToken, email, displayName }) => {
    if (!idToken || !email) {
      return { success: false, message: 'Token y email son obligatorios para iniciar sesión' }
    }

    try {
      setAuthToken(idToken)
      const nombre = String(displayName || email.split('@')[0] || 'Usuario').trim()
      const response = await apiService.upsertPerfil(nombre, email)
      const newUser = hydrateSessionUser(response, email)
      user.value = newUser
      sessionStorage.setItem('user', JSON.stringify(newUser))
      const profileCompleted = await checkPlayerProfileCompleted()
      return { success: true, message: 'Sesión iniciada', profileCompleted }
    } catch (error) {
      clearIdentity()
      user.value = null
      hasPlayerProfile.value = null
      return {
        success: false,
        message: error?.message || 'No se pudo completar el inicio de sesión con Firebase',
      }
    }
  }

  const logout = async () => {
    try {
      await logoutFirebaseSession()
    } catch (error) {
      console.warn('No se pudo cerrar sesión en Firebase:', error?.message)
    }

    user.value = null
    hasPlayerProfile.value = null
    sessionStorage.removeItem('user')
    clearIdentity()
  }

  const updateRasgos = async (rasgos) => {
    if (!user.value?.id) {
      return { success: false, message: 'Usuario no autenticado' }
    }

    try {
      await apiService.actualizarRasgos(user.value.id, rasgos)
      user.value.rasgos = rasgos
      sessionStorage.setItem('user', JSON.stringify(user.value))
      return { success: true }
    } catch (error) {
      const message = error?.data?.message || 'No se pudieron guardar los rasgos'
      return { success: false, message }
    }
  }

  const markPlayerProfileCompleted = () => {
    hasPlayerProfile.value = true
  }

  return {
    user,
    hasPlayerProfile,
    isAuthenticated,
    loginWithFirebaseToken,
    logout,
    updateRasgos,
    refreshUsuario,
    loadUser,
    checkPlayerProfileCompleted,
    markPlayerProfileCompleted,
  }
})
