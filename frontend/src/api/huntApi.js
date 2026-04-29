import apiClient from './apiClient'

export function getAllHunts() {
  return apiClient.get('/hunts')
}

export function createHunt(data) {
  return apiClient.post('/hunts', data)
}

export function joinHunt(huntId) {
  return apiClient.post(`/hunts/${huntId}/join`)
}

export function completeHunt(huntId, won) {
  return apiClient.post(`/hunts/${huntId}/complete`, { won })
}

export function startSoloHunt(huntId, won) {
  return apiClient.post(`/hunts/${huntId}/solo/start`, { won })
}

export function deleteHunt(huntId) {
  return apiClient.delete(`/hunts/${huntId}`)
}

export function updateHunt(huntId, data) {
  return apiClient.put(`/hunts/${huntId}`, data)
}
