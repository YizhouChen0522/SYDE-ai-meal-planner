import { apiClient, unwrapResult } from './http'

export const getInventoryItems = async () => {
  const response = await apiClient.get('/inventory')
  return unwrapResult(response)
}

export const createInventoryItem = async (payload) => {
  const response = await apiClient.post('/inventory', payload)
  return unwrapResult(response)
}

export const updateInventoryItem = async (id, payload) => {
  const response = await apiClient.put(`/inventory/${id}`, payload)
  return unwrapResult(response)
}

export const deleteInventoryItem = async (id) => {
  const response = await apiClient.delete(`/inventory/${id}`)
  return unwrapResult(response)
}
