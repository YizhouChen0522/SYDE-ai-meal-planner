import { apiClient, unwrapResult } from './http'

export const getShoppingListItems = async () => {
  const response = await apiClient.get('/shopping-list')
  return unwrapResult(response)
}

export const deleteShoppingListItem = async (id) => {
  const response = await apiClient.delete(`/shopping-list/${id}`)
  return unwrapResult(response)
}

export const placeShoppingListOrder = async () => {
  const response = await apiClient.post('/shopping-list/place-order')
  return unwrapResult(response)
}
