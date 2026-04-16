const AUTH_TOKEN_KEY = 'auth-token'

export function getAuthToken() {
  const sessionToken = sessionStorage.getItem(AUTH_TOKEN_KEY)
  if (sessionToken) {
    return sessionToken
  }

  // One-time migration from legacy localStorage token.
  const legacyToken = localStorage.getItem(AUTH_TOKEN_KEY)
  if (legacyToken) {
    sessionStorage.setItem(AUTH_TOKEN_KEY, legacyToken)
    localStorage.removeItem(AUTH_TOKEN_KEY)
    return legacyToken
  }

  return null
}

export function setAuthToken(token) {
  if (!token || typeof token !== 'string') {
    throw new Error('Auth token must be a non-empty string')
  }
  sessionStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.removeItem(AUTH_TOKEN_KEY)
}

export function clearIdentity() {
  sessionStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_TOKEN_KEY)
  sessionStorage.removeItem('user')
}

export function buildAuthHeaders() {
  const token = getAuthToken()
  if (!token) {
    return {}
  }

  return {
    Authorization: `Bearer ${token}`,
  }
}
