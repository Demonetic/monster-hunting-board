import apiClient from './apiClient'

export function getRecentGlobalMessages() {
  return apiClient.get('/chat/global/recent')
}

export function sendGlobalMessage(message) {
  return apiClient.post('/chat/global', { message })
}

export function getRecentLobbyMessages(lobbyId) {
  return apiClient.get(`/chat/lobby/${lobbyId}/recent`)
}

export function sendLobbyMessage(lobbyId, message) {
  return apiClient.post(`/chat/lobby/${lobbyId}`, { message })
}
