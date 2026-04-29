import apiClient from './apiClient'

export function getAllBeasts() {
  return apiClient.get('/beasts')
}

export function createBeast(data) {
  return apiClient.post('/beasts', data)
}

export function deleteBeast(beastId) {
  return apiClient.delete(`/beasts/${beastId}`)
}
