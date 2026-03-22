/**
 * Configuración centralizada de la aplicación
 * Las URLs se cargan desde variables de entorno (variables-entorno/frontend.env)
 *
 * En desarrollo: http://localhost:8080 (si es localhost)
 * En producción: URL configurada en variables-entorno/frontend.env
 */

export const CORE_API_BASE_URL = import.meta.env.VITE_CORE_API_URL

// Endpoints de la API - Solo Core (usuarios, partidos, invitaciones, amistades)
export const ENDPOINTS = {
  // ============ AUTH ============
  AUTH_ME: `${CORE_API_BASE_URL}/api/usuarios/me`,
  AUTH_ME_VOTACION_RESUMEN: `${CORE_API_BASE_URL}/api/usuarios/me/votacion-resumen`,
  AUTH_VOTACION_RESUMEN_PUBLICO_BY_ID: (id) => `${CORE_API_BASE_URL}/api/usuarios/${id}/votacion-resumen-publico`,
  AUTH_RASGOS: `${CORE_API_BASE_URL}/api/usuarios/me/rasgos`,
  AUTH_USUARIOS: `${CORE_API_BASE_URL}/api/usuarios`,
  AUTH_USUARIO_BY_ID: (id) => `${CORE_API_BASE_URL}/api/usuarios/${id}`,

  // ============ PLAYER PROFILE ============
  PLAYER_PROFILE_ME: `${CORE_API_BASE_URL}/api/player-profile/me`,
  PLAYER_PROFILE_PUBLIC_BY_USER: (usuarioId) => `${CORE_API_BASE_URL}/api/player-profile/public/${usuarioId}`,

  // ============ PARTIDOS (APP SOCIAL) ============
  PARTIDOS: `${CORE_API_BASE_URL}/api/partidos`,
  PARTIDOS_MIS: `${CORE_API_BASE_URL}/api/partidos/mis-partidos`,
  PARTIDOS_HISTORIAL: `${CORE_API_BASE_URL}/api/partidos/mi-historial`,
  PARTIDOS_HISTORIAL_PUBLICO_BY_USER: (usuarioId) => `${CORE_API_BASE_URL}/api/partidos/historial/public/${usuarioId}`,
  PARTIDOS_FUTUROS: `${CORE_API_BASE_URL}/api/partidos/futuros`,
  PARTIDOS_BALANCEAR: (partidoId) => `${CORE_API_BASE_URL}/api/partidos/${partidoId}/balancear-equipos`,
  PARTIDOS_VOTACION_ME: (partidoId) => `${CORE_API_BASE_URL}/api/partidos/${partidoId}/votaciones/me`,
  PARTIDOS_VOTACION_ASIGNACION: (partidoId) => `${CORE_API_BASE_URL}/api/partidos/${partidoId}/votaciones/asignacion`,
  PARTIDOS_VOTACION_PANEL_COMPARTIDO: (partidoId) => `${CORE_API_BASE_URL}/api/partidos/${partidoId}/votaciones/panel-compartido`,

  // ============ INVITACIONES ============
  INVITACIONES: `${CORE_API_BASE_URL}/api/invitaciones`,
  INVITACIONES_MIS: `${CORE_API_BASE_URL}/api/invitaciones/mis-invitaciones`,
  INVITACIONES_PENDIENTES: `${CORE_API_BASE_URL}/api/invitaciones/pendientes`,

  // ============ AMISTADES ============
  AMISTADES: `${CORE_API_BASE_URL}/api/amistades`,
  AMISTADES_MIS: `${CORE_API_BASE_URL}/api/amistades/mis-amigos`,
  AMISTADES_SOLICITUDES: `${CORE_API_BASE_URL}/api/amistades/solicitudes-pendientes`,
  AMISTADES_SOLICITUDES_ENVIADAS: `${CORE_API_BASE_URL}/api/amistades/solicitudes-enviadas`,
}

// Timeout para requests (en ms) - Aumentado para mobile
export const REQUEST_TIMEOUT = 30000

// Headers por defecto
export const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
}

