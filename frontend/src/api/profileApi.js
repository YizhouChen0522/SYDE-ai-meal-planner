import { apiClient, unwrapResult } from './http'

export const getProfile = async () => {
  const response = await apiClient.get('/profile')
  return unwrapResult(response)
}

export const updateProfile = async (payload) => {
  const response = await apiClient.put('/profile', payload)
  return unwrapResult(response)
}
