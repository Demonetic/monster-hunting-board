import apiClient from './apiClient'

export function getCurrentHunter() {
  return apiClient.get('/hunters/me')
}

export function updateAppearance(appearance) {
  return apiClient.patch('/hunters/me/appearance', { appearance })
}
