import apiClient from './apiClient'

export function getRecentGlobalMessages() {
  return apiClient.get('/chat/global/recent')
}

export function getRecentLobbyMessages(lobbyId) {
  return apiClient.get(`/chat/lobby/${lobbyId}/recent`)
}
