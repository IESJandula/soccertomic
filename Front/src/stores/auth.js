import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiService from '../services/apiService'
import { clearIdentity, getAuthToken, setAuthToken } from '../services/authIdentity'
import { getActiveFirebaseSession, logoutFirebaseSession } from '../services/firebaseClient'
import { formatDateTimeEs } from '../utils/dateFormat'

let unauthorizedHandlerBound = false

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const hasPlayerProfile = ref(null)
  const authStatus = ref('bootstrapping')
  const isAuthenticated = computed(() => authStatus.value === 'authenticated' && user.value !== null && Boolean(getAuthToken()))
  const isBootstrapping = computed(() => authStatus.value === 'bootstrapping')

  let bootstrapPromise = null
  let expiringSession = false

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
      if (error?.status === 404) {
        hasPlayerProfile.value = false
        return false
      }

      console.warn('Could not validate player profile:', error?.message)
      hasPlayerProfile.value = false
      return false
    }
  }

  const resetIdentity = () => {
    user.value = null
    hasPlayerProfile.value = null
    sessionStorage.removeItem('user')
    clearIdentity()
  }

  const initializeSession = async () => {
    if (bootstrapPromise) {
      return bootstrapPromise
    }

    bootstrapPromise = (async () => {
      authStatus.value = 'bootstrapping'

      try {
        const firebaseSession = await getActiveFirebaseSession({ forceRefresh: true, timeoutMs: 4000 })

        if (!firebaseSession?.idToken || !firebaseSession?.email) {
          resetIdentity()
          authStatus.value = 'anonymous'
          return { authenticated: false }
        }

        setAuthToken(firebaseSession.idToken)

        const resumen = await apiService.getUsuarioResumen(null)
        const newUser = hydrateSessionUser(resumen, firebaseSession.email)
        user.value = newUser
        sessionStorage.setItem('user', JSON.stringify(newUser))
        authStatus.value = 'authenticated'

        await checkPlayerProfileCompleted()
        return { authenticated: true }
      } catch (error) {
        resetIdentity()
        authStatus.value = 'anonymous'
        return { authenticated: false, error }
      } finally {
        bootstrapPromise = null
      }
    })()

    return bootstrapPromise
  }

  const loadUser = () => {
    void initializeSession()
  }

  const loginWithFirebaseToken = async ({ idToken, email, displayName }, isRegistration = false) => {
    if (!idToken || !email) {
      return { success: false, message: 'Token y email son obligatorios para iniciar sesión' }
    }

    try {
      setAuthToken(idToken)
      // Only persist displayName during signup; regular login must not overwrite custom names.
      const nombre = isRegistration
        ? String(displayName || email.split('@')[0] || 'Usuario').trim()
        : null
      const response = await apiService.upsertPerfil(nombre, email)
      const newUser = hydrateSessionUser(response, email)
      user.value = newUser
      sessionStorage.setItem('user', JSON.stringify(newUser))
      authStatus.value = 'authenticated'
      const profileCompleted = await checkPlayerProfileCompleted()
      return { success: true, message: 'Sesión iniciada', profileCompleted }
    } catch (error) {
      resetIdentity()
      authStatus.value = 'anonymous'
      return {
        success: false,
        message: error?.message || 'No se pudo completar el inicio de sesión con Firebase',
      }
    }
  }

  const logout = async ({ signOutFirebase = true } = {}) => {
    if (signOutFirebase) {
      try {
        await logoutFirebaseSession()
      } catch (error) {
        console.warn('No se pudo cerrar sesión en Firebase:', error?.message)
      }
    }

    resetIdentity()
    authStatus.value = 'anonymous'
  }

  const expireSession = async () => {
    if (expiringSession) {
      return
    }

    expiringSession = true

    try {
      await logout({ signOutFirebase: true })

      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        const redirectTarget = `${window.location.pathname}${window.location.search}`
        const redirectQuery = redirectTarget && redirectTarget !== '/login'
          ? `&redirect=${encodeURIComponent(redirectTarget)}`
          : ''
        window.location.assign(`/login?reason=expired${redirectQuery}`)
      }
    } finally {
      expiringSession = false
    }
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

  if (!unauthorizedHandlerBound) {
    apiService.setUnauthorizedHandler(async () => {
      await expireSession()
    })
    unauthorizedHandlerBound = true
  }

  return {
    user,
    hasPlayerProfile,
    authStatus,
    isBootstrapping,
    isAuthenticated,
    loginWithFirebaseToken,
    logout,
    expireSession,
    updateRasgos,
    refreshUsuario,
    loadUser,
    initializeSession,
    checkPlayerProfileCompleted,
    markPlayerProfileCompleted,
  }
})
