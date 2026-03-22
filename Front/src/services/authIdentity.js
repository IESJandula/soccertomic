const AUTH_TOKEN_KEY = 'auth-token'

export function getAuthToken() {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export function setAuthToken(token) {
  if (!token || typeof token !== 'string') {
    throw new Error('Auth token must be a non-empty string')
  }
  localStorage.setItem(AUTH_TOKEN_KEY, token)
}

export function clearIdentity() {
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
