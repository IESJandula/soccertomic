/**
 * Servicio centralizado para llamadas a la API
 * Proporciona métodos reutilizables para todas las operaciones CRUD
 * Maneja errores y autenticación automáticamente
 * Usa token Bearer cuando existe sesión autenticada
 */

import { ENDPOINTS, DEFAULT_HEADERS, REQUEST_TIMEOUT } from '../config'
import { buildAuthHeaders } from './authIdentity'

class APIService {
  constructor() {
    this.DEFAULT_CACHE_TTL_MS = 60_000
    this.CACHE_PREFIX = 'apiCache:'
    this.REQUEST_TIMEOUT = REQUEST_TIMEOUT
  }

  isExpectedNotFound(url, status) {
    if (status !== 404) return false
    // 404 es esperado cuando usuario nuevo no tiene perfil o votaciones
    return url.includes('/player-profile') || url.includes('/votaciones') || url.includes('/rasgos')
  }

  getCacheKey(url) {
    return `${this.CACHE_PREFIX}${url}`
  }

  readCache(key) {
    const raw = localStorage.getItem(key)
    if (!raw) return null

    try {
      const parsed = JSON.parse(raw)
      if (!parsed || !parsed.timestamp) return null
      if (Date.now() - parsed.timestamp > parsed.ttl) return null
      return parsed.data
    } catch (error) {
      return null
    }
  }

  writeCache(key, data, ttl) {
    try {
      localStorage.setItem(
        key,
        JSON.stringify({
          timestamp: Date.now(),
          ttl,
          data,
        })
      )
    } catch (error) {
      // Ignorar errores de quota o JSON
    }
  }

  /**
   * Método auxiliar para hacer requests HTTP
   * Maneja errores, timeouts y autenticación
   *
   * @param {string} url - URL del endpoint
   * @param {Object} options - Opciones de fetch (method, body, headers, etc)
   * @returns {Promise<Object>} Response parseada como JSON
   * @throws {Error} Si la respuesta no es satisfactoria
   */
  async request(url, options = {}) {
    try {
      const method = (options.method || 'GET').toUpperCase()
      const useCache = method === 'GET' && options.useCache !== false
      const cacheTtl = Number(options.cacheTtlMs || this.DEFAULT_CACHE_TTL_MS)
      const cacheKey = useCache ? (options.cacheKey || this.getCacheKey(url)) : null

      const {
        useCache: _useCache,
        cacheTtlMs: _cacheTtlMs,
        cacheKey: _cacheKey,
        ...fetchOptions
      } = options

      if (useCache && cacheKey) {
        const cached = this.readCache(cacheKey)
        if (cached) {
          return cached
        }
      }

      // Crear AbortController para timeout
      const abortController = new AbortController()
      const timeoutId = setTimeout(() => {
        abortController.abort()
      }, this.REQUEST_TIMEOUT)

      try {
        const response = await fetch(url, {
          signal: abortController.signal,
          headers: {
            ...DEFAULT_HEADERS,
            ...buildAuthHeaders(),
            ...fetchOptions.headers,
          },
          ...fetchOptions,
        })

        clearTimeout(timeoutId)

        // Si no es exitosa, procesar el error
        if (!response.ok) {
          let errorData = {}
          const errorText = await response.text().catch(() => '')
          if (errorText) {
            try {
              errorData = JSON.parse(errorText)
            } catch (e) {
              errorData = { message: errorText }
            }
          }
          
          const error = new Error(
            errorData.message || `Error ${response.status}: ${response.statusText}`
          )
          error.status = response.status
          error.data = errorData
          throw error
        }

        // Manejar respuestas sin contenido (204 No Content, etc.)
        if (response.status === 204 || response.headers.get('content-length') === '0') {
          return { success: true }
        }

        const rawText = await response.text().catch(() => '')
        const data = rawText ? JSON.parse(rawText) : { success: true }

        if (useCache && cacheKey) {
          this.writeCache(cacheKey, data, cacheTtl)
        }

        return data
      } catch (error) {
        clearTimeout(timeoutId)
        
        if (error.name === 'AbortError') {
          const timeoutError = new Error(`Timeout de solicitud (${this.REQUEST_TIMEOUT}ms) - conexión lenta`)
          timeoutError.isTimeout = true
          console.error('⏱️ Timeout:', timeoutError.message)
          throw timeoutError
        }
        
        throw error
      }
    } catch (error) {
      // Solo loguear errores NO esperados (no loguear 404 de perfil incompleto, votaciones, etc)
      if (!this.isExpectedNotFound(url, error.status) && error.isTimeout !== true) {
        console.error('API Error:', {
          message: error.message,
          status: error.status,
          data: error.data,
          isTimeout: error.isTimeout,
          stack: error.stack,
        })
      }
      throw error
    }
  }

  /**
   * Upsert user profile (creates or updates user in backend)
   * @param {string} nombre - User's display name
   * @param {string} email - User's email address (required for database)
   */
  async upsertPerfil(nombre, email) {
    return this.request(ENDPOINTS.AUTH_ME, {
      method: 'PUT',
      useCache: false,
      body: JSON.stringify({ nombre, email }),
    })
  }

  /**
   * Actualiza rasgos del usuario
   * @param {number} usuarioId
   * @param {Array<string>} rasgos
   * @returns {Promise<Object>} Usuario actualizado
   */
  async actualizarRasgos(usuarioId, rasgos) {
    return this.request(ENDPOINTS.AUTH_RASGOS, {
      method: 'PUT',
      useCache: false,
      body: JSON.stringify({ rasgos }),
    })
  }

  /**
   * Obtiene resumen de usuario (incluye rasgos)
   * @param {number} usuarioId
   * @returns {Promise<Object>} { id, nombre, rasgos }
   */
  async getUsuarioResumen(usuarioId) {
    const endpoint = usuarioId ? ENDPOINTS.AUTH_USUARIO_BY_ID(usuarioId) : ENDPOINTS.AUTH_ME
    return this.request(endpoint, {
      useCache: false,
    })
  }

  async getMiResumenVotacion() {
    return this.request(ENDPOINTS.AUTH_ME_VOTACION_RESUMEN, {
      useCache: false,
    })
  }

  async getResumenVotacionPublico(usuarioId) {
    return this.request(ENDPOINTS.AUTH_VOTACION_RESUMEN_PUBLICO_BY_ID(usuarioId), {
      useCache: false,
    })
  }

  /**
   * Obtiene usuarios publicos con puntos y rasgos
   * @returns {Promise<Array>} Array de usuarios
   */
  async getUsuariosPublicos() {
    return this.request(ENDPOINTS.AUTH_USUARIOS, {
      useCache: false,
    })
  }

  async savePlayerProfile(payload) {
    return this.request(ENDPOINTS.PLAYER_PROFILE_ME, {
      method: 'PUT',
      useCache: false,
      body: JSON.stringify(payload),
    })
  }

  async getPlayerProfile() {
    return this.request(ENDPOINTS.PLAYER_PROFILE_ME, {
      method: 'GET',
      useCache: false,
    })
  }

  async getPublicPlayerProfile(usuarioId) {
    return this.request(ENDPOINTS.PLAYER_PROFILE_PUBLIC_BY_USER(usuarioId), {
      method: 'GET',
      useCache: false,
    })
  }
}

// Exportar instancia única del servicio (singleton)
export default new APIService()
