import { apiClient, unwrapResult } from './http'

export const generateMealPlanDraft = async (payload) => {
  const response = await apiClient.post('/meal-plans/generate', payload)
  return unwrapResult(response)
}

export const getCurrentMealPlanDraft = async () => {
  const response = await apiClient.get('/meal-plans/current')
  return unwrapResult(response)
}

export const deleteCurrentMealPlanDraft = async () => {
  const response = await apiClient.delete('/meal-plans/current')
  return unwrapResult(response)
}

export const replaceMealPlanRecipe = async (recipeId) => {
  const response = await apiClient.put(`/meal-plans/current/recipes/${recipeId}/replace`)
  return unwrapResult(response)
}

export const addMealPlanRecipe = async () => {
  const response = await apiClient.post('/meal-plans/current/recipes')
  return unwrapResult(response)
}

export const deleteMealPlanRecipe = async (recipeId) => {
  const response = await apiClient.delete(`/meal-plans/current/recipes/${recipeId}`)
  return unwrapResult(response)
}

export const confirmCurrentMealPlanDraft = async () => {
  const response = await apiClient.post('/meal-plans/current/confirm')
  return unwrapResult(response)
}
