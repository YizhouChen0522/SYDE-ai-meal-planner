import { apiClient, unwrapResult } from './http'

export const getMealHistory = async () => {
  const response = await apiClient.get('/meal-history')
  return unwrapResult(response)
}

export const getMealHistoryById = async (id) => {
  const response = await apiClient.get(`/meal-history/${id}`)
  return unwrapResult(response)
}
