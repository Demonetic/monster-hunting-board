import apiClient from './apiClient'

export function login(credentials) {
  return apiClient.post('/auth/login', credentials)
}

export function register(data) {
  return apiClient.post('/auth/register', data)
}

export function getAppearanceOptions() {
  return apiClient.get('/auth/appearance-options')
}
