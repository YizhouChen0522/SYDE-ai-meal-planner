import axios from 'axios'

const TOKEN_KEY = 'syde-auth-token'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export const unwrapResult = (response) => {
  const result = response.data

  if (!result || typeof result.code !== 'number') {
    return result
  }

  if (result.code !== 200) {
    throw new Error(result.message || 'Request failed')
  }

  return result.data
}

export const getApiErrorMessage = (error, fallback = 'Request failed') => {
  return error?.response?.data?.message || error?.message || fallback
}
