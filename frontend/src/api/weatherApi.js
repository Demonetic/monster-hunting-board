import apiClient from './apiClient'

export function getCurrentWeather() {
  return apiClient.get('/weather/current')
}
