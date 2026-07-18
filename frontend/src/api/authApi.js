import { apiClient, unwrapResult } from './http'

export const loginUser = async (payload) => {
  const response = await apiClient.post('/auth/login', payload)
  return unwrapResult(response)
}

export const registerUser = async (payload) => {
  const response = await apiClient.post('/auth/register', payload)
  return unwrapResult(response)
}
