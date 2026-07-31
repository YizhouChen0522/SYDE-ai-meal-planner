import { apiClient, unwrapResult } from './http'

export const getMealHistory = async ({ page = 1, size = 5 } = {}) => {
  const response = await apiClient.get('/meal-history', {
    params: {
      page,
      size,
    },
  })
  return unwrapResult(response)
}

export const getMealHistoryById = async (id) => {
  const response = await apiClient.get(`/meal-history/${id}`)
  return unwrapResult(response)
}
