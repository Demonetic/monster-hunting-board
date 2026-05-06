import apiClient from './apiClient'

export function getCurrentHunter() {
  return apiClient.get('/hunters/me')
}

export function getShop() {
  return apiClient.get('/hunters/me/shop')
}

export function updateAppearance(appearance) {
  return apiClient.patch('/hunters/me/appearance', { appearance })
}

export function purchaseShopItem(itemType) {
  return apiClient.post('/hunters/me/shop/purchase', { itemType })
}
