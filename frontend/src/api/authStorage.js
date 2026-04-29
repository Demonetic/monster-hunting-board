const TOKEN_KEY = 'token'
const ROLE_KEY = 'role'

export function saveToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function saveRole(role) {
  localStorage.setItem(ROLE_KEY, role)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getRole() {
  return localStorage.getItem(ROLE_KEY) ?? ''
}

function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  return atob(`${normalized}${padding}`)
}

export function getCurrentUsername() {
  const token = getToken()

  if (!token) {
    return ''
  }

  try {
    const [, payload] = token.split('.')

    if (!payload) {
      return ''
    }

    const parsedPayload = JSON.parse(decodeBase64Url(payload))
    return parsedPayload.sub ?? ''
  } catch {
    return ''
  }
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function clearRole() {
  localStorage.removeItem(ROLE_KEY)
}

export function isAuthenticated() {
  return Boolean(getToken())
}
